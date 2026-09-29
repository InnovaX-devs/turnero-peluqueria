package com.innovax.barberia.caja;

import com.innovax.barberia.atencion.EstadoAtencion;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.caja.dto.CajaResponse;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.common.RecursoNoEncontradoException;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class CajaService {

    private static final int RANGO_MAXIMO_DIAS = 366;
    private static final DateTimeFormatter FECHA_CSV = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_CSV = DateTimeFormatter.ofPattern("HH:mm");

    private final UsuarioActual usuarioActual;
    private final BarberiaRepository barberias;
    private final SucursalRepository sucursales;
    private final CajaRepository caja;

    private record Periodo(ZoneId zona, List<FilaCaja> filas) {
    }

    @Transactional(readOnly = true)
    public CajaResponse consultar(LocalDate desde, LocalDate hasta, UUID sucursalId) {
        Periodo p = cargar(desde, hasta, sucursalId);
        List<FilaCaja> filas = p.filas();

        long total = filas.stream().mapToLong(FilaCaja::precioFinal).sum();
        int cantidad = filas.size();

        List<CajaResponse.Grupo> porBarbero = agrupar(filas, FilaCaja::barberoId,
                f -> f.barberoNombre() + " " + f.barberoApellido());
        List<CajaResponse.Grupo> porServicio = agrupar(filas, FilaCaja::servicioId, FilaCaja::servicioNombre);

        Map<String, long[]> metodos = new LinkedHashMap<>();
        for (FilaCaja f : filas) {
            long[] acc = metodos.computeIfAbsent(f.metodoPago() == null ? null : f.metodoPago().name(), k -> new long[2]);
            acc[0]++;
            acc[1] += f.precioFinal();
        }
        List<CajaResponse.GrupoMetodo> porMetodo = metodos.entrySet().stream()
                .map(e -> new CajaResponse.GrupoMetodo(e.getKey(), (int) e.getValue()[0], e.getValue()[1]))
                .sorted(Comparator.comparingLong(CajaResponse.GrupoMetodo::total).reversed())
                .toList();

        Map<LocalDate, long[]> dias = new LinkedHashMap<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            dias.put(d, new long[2]);
        }
        for (FilaCaja f : filas) {
            long[] acc = dias.get(f.inicio().atZone(p.zona()).toLocalDate());
            if (acc != null) {
                acc[0]++;
                acc[1] += f.precioFinal();
            }
        }
        List<CajaResponse.Dia> porDia = dias.entrySet().stream()
                .map(e -> new CajaResponse.Dia(e.getKey(), (int) e.getValue()[0], e.getValue()[1]))
                .toList();

        return new CajaResponse(desde, hasta, total, cantidad, cantidad == 0 ? 0 : total / cantidad,
                porBarbero, porServicio, porMetodo, porDia);
    }

    /** Detalle de cada corte del período, para abrir en Excel (separador ";" y montos con coma). */
    @Transactional(readOnly = true)
    public String exportarCsv(LocalDate desde, LocalDate hasta, UUID sucursalId) {
        Periodo p = cargar(desde, hasta, sucursalId);
        StringBuilder csv = new StringBuilder("\uFEFF"); // BOM: Excel lo abre con tildes y ñ bien
        csv.append("Fecha;Hora;Barbero;Servicio;Cliente;Método de pago;Precio\n");
        for (FilaCaja f : p.filas()) {
            var inicio = f.inicio().atZone(p.zona());
            csv.append(FECHA_CSV.format(inicio)).append(';')
                    .append(HORA_CSV.format(inicio)).append(';')
                    .append(celda(f.barberoNombre() + " " + f.barberoApellido())).append(';')
                    .append(celda(f.servicioNombre())).append(';')
                    .append(celda(f.clienteNombre() == null ? "Sin cliente" : f.clienteNombre())).append(';')
                    .append(f.metodoPago() == null ? "" : f.metodoPago().name()).append(';')
                    .append(pesos(f.precioFinal())).append('\n');
        }
        return csv.toString();
    }

    private Periodo cargar(LocalDate desde, LocalDate hasta, UUID sucursalId) {
        if (hasta.isBefore(desde)) {
            throw new DatosInvalidosException("hasta", "La fecha hasta no puede ser anterior a desde");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > RANGO_MAXIMO_DIAS) {
            throw new DatosInvalidosException("hasta", "El período no puede superar un año");
        }

        // Aislamiento: la barbería sale siempre del token
        UUID barberiaId = usuarioActual.barberiaId();
        Barberia barberia = barberias.findById(barberiaId)
                .orElseThrow(() -> new IllegalStateException("La barbería del token no existe"));
        if (sucursalId != null) {
            sucursales.findById(sucursalId)
                    .filter(s -> s.getBarberia().getId().equals(barberiaId))
                    .orElseThrow(() -> new RecursoNoEncontradoException("Sucursal no encontrada"));
        }

        // Los días son de la barbería: "hoy" en Córdoba, no en UTC
        ZoneId zona = ZoneId.of(barberia.getZonaHoraria());
        var inicio = desde.atStartOfDay(zona).toInstant();
        var fin = hasta.plusDays(1).atStartOfDay(zona).toInstant();

        return new Periodo(zona, caja.buscar(barberiaId, EstadoAtencion.COMPLETADA, inicio, fin, sucursalId));
    }

    private static List<CajaResponse.Grupo> agrupar(
            List<FilaCaja> filas, Function<FilaCaja, UUID> clave, Function<FilaCaja, String> nombre) {
        Map<UUID, CajaResponse.Grupo> grupos = new LinkedHashMap<>();
        for (FilaCaja f : filas) {
            grupos.merge(clave.apply(f),
                    new CajaResponse.Grupo(clave.apply(f), nombre.apply(f), 1, f.precioFinal()),
                    (a, b) -> new CajaResponse.Grupo(a.id(), a.nombre(), a.cantidad() + 1, a.total() + b.total()));
        }
        List<CajaResponse.Grupo> lista = new ArrayList<>(grupos.values());
        lista.sort(Comparator.comparingLong(CajaResponse.Grupo::total).reversed());
        return lista;
    }

    /** 1500000 centavos → "15000,00" */
    private static String pesos(long centavos) {
        return (centavos / 100) + "," + String.format("%02d", Math.abs(centavos % 100));
    }

    /** Evita que un ";" o comillas en un nombre rompan las columnas. */
    private static String celda(String texto) {
        if (texto.contains(";") || texto.contains("\"") || texto.contains("\n")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}
