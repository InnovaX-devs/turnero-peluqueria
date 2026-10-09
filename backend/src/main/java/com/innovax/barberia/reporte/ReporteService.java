package com.innovax.barberia.reporte;

import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.atencion.OrigenAtencion;
import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.reporte.dto.ClientesInactivosResponse;
import com.innovax.barberia.reporte.dto.ReportesResponse;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final int RANGO_MAXIMO_DIAS = 366;
    private static final int TOP_CLIENTES = 10;
    private static final int MAX_INACTIVOS = 100;

    private final UsuarioActual usuarioActual;
    private final BarberiaRepository barberias;
    private final SucursalRepository sucursales;
    private final ReporteRepository reportes;

    @Transactional(readOnly = true)
    public ReportesResponse generar(LocalDate desde, LocalDate hasta, UUID sucursalId) {
        if (hasta.isBefore(desde)) {
            throw new DatosInvalidosException("hasta", "La fecha hasta no puede ser anterior a desde");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > RANGO_MAXIMO_DIAS) {
            throw new DatosInvalidosException("hasta", "El período no puede superar un año");
        }

        Barberia barberia = barberiaActual();
        if (sucursalId != null) {
            sucursales.findById(sucursalId)
                    .filter(s -> s.getBarberia().getId().equals(barberia.getId()))
                    .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        }

        ZoneId zona = ZoneId.of(barberia.getZonaHoraria());
        List<FilaReporte> filas = reportes.buscar(barberia.getId(),
                desde.atStartOfDay(zona).toInstant(),
                hasta.plusDays(1).atStartOfDay(zona).toInstant(),
                sucursalId,
                barberoAcotado());

        return new ReportesResponse(desde, hasta,
                porBarbero(filas),
                clientesFrecuentes(filas, zona),
                ausentismo(filas, Instant.now()),
                demanda(filas, zona));
    }

    @Transactional(readOnly = true)
    public ClientesInactivosResponse clientesInactivos(int dias) {
        if (dias < 7 || dias > 365) {
            throw new DatosInvalidosException("dias", "Elegí entre 7 y 365 días");
        }
        Barberia barberia = barberiaActual();
        ZoneId zona = ZoneId.of(barberia.getZonaHoraria());
        Instant ahora = Instant.now();
        LocalDate hoy = LocalDate.now(Clock.system(zona));

        UUID barberoId = barberoAcotado();
        Set<UUID> vuelven = new HashSet<>(
                reportes.clientesConTurnoFuturo(barberia.getId(), EstadoAtencion.RESERVADA, ahora, barberoId));

        List<ClientesInactivosResponse.Cliente> clientes = reportes
                .ultimasVisitasAntesDe(barberia.getId(), EstadoAtencion.COMPLETADA, ahora.minus(Duration.ofDays(dias)), barberoId)
                .stream()
                .filter(v -> !vuelven.contains(v.clienteId()))
                .limit(MAX_INACTIVOS)
                .map(v -> {
                    LocalDate ultima = v.ultima().atZone(zona).toLocalDate();
                    return new ClientesInactivosResponse.Cliente(v.clienteId(), v.nombre(), v.telefono(),
                            ultima, ChronoUnit.DAYS.between(ultima, hoy), v.visitas());
                })
                .toList();

        return new ClientesInactivosResponse(barberia.getNombre(), dias, clientes);
    }

    /**
     * Un barbero ve solo sus propios números: el id sale siempre del token, nunca del request.
     * El admin ve toda la barbería (null = sin filtro).
     */
    private UUID barberoAcotado() {
        if (usuarioActual.rol() != Rol.BARBERO) {
            return null;
        }
        return usuarioActual.barberoId()
                .orElseThrow(() -> new AccessDeniedException("Tu usuario no está vinculado a un barbero"));
    }

    private Barberia barberiaActual() {
        // Aislamiento: la barbería sale siempre del token
        return barberias.findById(usuarioActual.barberiaId())
                .orElseThrow(() -> new IllegalStateException("La barbería del token no existe"));
    }

    private static List<ReportesResponse.Barbero> porBarbero(List<FilaReporte> filas) {
        Map<UUID, int[]> cuentas = new LinkedHashMap<>();
        Map<UUID, Long> facturado = new LinkedHashMap<>();
        Map<UUID, String> nombres = new LinkedHashMap<>();
        for (FilaReporte f : filas) {
            nombres.putIfAbsent(f.barberoId(), f.barberoNombre() + " " + f.barberoApellido());
            int[] c = cuentas.computeIfAbsent(f.barberoId(), k -> new int[3]);
            switch (f.estado()) {
                case COMPLETADA -> {
                    c[0]++;
                    facturado.merge(f.barberoId(), f.precioFinal(), Long::sum);
                }
                case AUSENTE -> c[1]++;
                case CANCELADA -> c[2]++;
                default -> { } // RESERVADA: todavía no pasó nada
            }
        }
        return cuentas.entrySet().stream()
                .map(e -> new ReportesResponse.Barbero(e.getKey(), nombres.get(e.getKey()),
                        e.getValue()[0], e.getValue()[1], e.getValue()[2],
                        facturado.getOrDefault(e.getKey(), 0L)))
                .sorted(Comparator.comparingInt(ReportesResponse.Barbero::atendidos).reversed())
                .toList();
    }

    private static List<ReportesResponse.ClienteFrecuente> clientesFrecuentes(List<FilaReporte> filas, ZoneId zona) {
        record Acumulado(String nombre, String telefono, int visitas, long total, Instant ultima) {
        }
        Map<UUID, Acumulado> porCliente = new LinkedHashMap<>();
        for (FilaReporte f : filas) {
            if (f.clienteId() == null || f.estado() != EstadoAtencion.COMPLETADA) {
                continue;
            }
            porCliente.merge(f.clienteId(),
                    new Acumulado(f.clienteNombre(), f.clienteTelefono(), 1, f.precioFinal(), f.inicio()),
                    (a, b) -> new Acumulado(a.nombre(), a.telefono(), a.visitas() + 1, a.total() + b.total(),
                            a.ultima().isAfter(b.ultima()) ? a.ultima() : b.ultima()));
        }
        return porCliente.entrySet().stream()
                .sorted(Comparator.<Map.Entry<UUID, Acumulado>>comparingInt(e -> e.getValue().visitas()).reversed()
                        .thenComparing(e -> e.getValue().total(), Comparator.reverseOrder()))
                .limit(TOP_CLIENTES)
                .map(e -> new ReportesResponse.ClienteFrecuente(e.getKey(), e.getValue().nombre(),
                        e.getValue().telefono(), e.getValue().visitas(), e.getValue().total(),
                        e.getValue().ultima().atZone(zona).toLocalDate()))
                .toList();
    }

    private static ReportesResponse.Ausentismo ausentismo(List<FilaReporte> filas, Instant ahora) {
        int atendidos = 0, ausentes = 0, cancelados = 0, sinMarcar = 0;
        for (FilaReporte f : filas) {
            if (f.origen() != OrigenAtencion.ONLINE) {
                continue; // un corte registrado a mano nunca puede ser un ausente
            }
            switch (f.estado()) {
                case COMPLETADA -> atendidos++;
                case AUSENTE -> ausentes++;
                case CANCELADA -> cancelados++;
                case RESERVADA -> {
                    if (f.inicio().isBefore(ahora)) {
                        sinMarcar++;
                    }
                }
            }
        }
        return new ReportesResponse.Ausentismo(atendidos, ausentes, cancelados, sinMarcar);
    }

    private static List<ReportesResponse.Demanda> demanda(List<FilaReporte> filas, ZoneId zona) {
        Map<Integer, Integer> celdas = new LinkedHashMap<>();
        for (FilaReporte f : filas) {
            if (f.estado() == EstadoAtencion.CANCELADA) {
                continue;
            }
            ZonedDateTime inicio = f.inicio().atZone(zona);
            celdas.merge(inicio.getDayOfWeek().getValue() * 100 + inicio.getHour(), 1, Integer::sum);
        }
        return celdas.entrySet().stream()
                .map(e -> new ReportesResponse.Demanda(e.getKey() / 100, e.getKey() % 100, e.getValue()))
                .sorted(Comparator.comparingInt(ReportesResponse.Demanda::cantidad).reversed()
                        .thenComparingInt(ReportesResponse.Demanda::diaSemana)
                        .thenComparingInt(ReportesResponse.Demanda::hora))
                .toList();
    }
}