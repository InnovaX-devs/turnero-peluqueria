package com.innovax.barberia.agenda;

import com.innovax.barberia.agenda.dto.AgendaResponse;
import com.innovax.barberia.agenda.dto.BarberoResumen;
import com.innovax.barberia.agenda.dto.BloqueoAgendaResponse;
import com.innovax.barberia.agenda.dto.TurnoAgendaResponse;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.barberia.Barberia;
import com.innovax.barberia.barberia.BarberiaRepository;
import com.innovax.barberia.barbero.BarberoRepository;
import com.innovax.barberia.common.DatosInvalidosException;
import com.innovax.barberia.horario.BloqueoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Agenda del panel (turnos + bloqueos), para la vista día/semana.
 *
 * <p>REGLA DE PERMISOS: un usuario con rol BARBERO ve siempre y solo su propia agenda, sin
 * importar qué {@code barberoId} venga en el request (ver {@link UsuarioActual}). Un ADMIN puede
 * pedir un barbero puntual o dejar el filtro vacío para ver la agenda completa de la barbería.
 */
@Service
@RequiredArgsConstructor
public class AgendaService {

    /** Una vista semana son 7 días; se deja margen para no atarse a ese número exacto. */
    private static final int RANGO_MAXIMO_DIAS = 31;

    private final UsuarioActual usuarioActual;
    private final BarberiaRepository barberias;
    private final BarberoRepository barberos;
    private final AtencionRepository atenciones;
    private final BloqueoRepository bloqueos;

    @Transactional(readOnly = true)
    public AgendaResponse consultar(LocalDate desde, LocalDate hasta, UUID barberoIdPedido) {
        if (hasta.isBefore(desde)) {
            throw new DatosInvalidosException("hasta", "No puede ser anterior a «desde»");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > RANGO_MAXIMO_DIAS) {
            throw new DatosInvalidosException("hasta", "El rango no puede superar los " + RANGO_MAXIMO_DIAS + " días");
        }

        UUID barberiaId = usuarioActual.barberiaId();
        Barberia barberia = barberias.findById(barberiaId)
                .orElseThrow(() -> new IllegalStateException("La barbería del token no existe"));

        UUID barberoId = usuarioActual.rol() == Rol.BARBERO
                ? barberoPropio()
                : barberoIdPedido;

        ZoneId zonaHoraria = ZoneId.of(barberia.getZonaHoraria());
        Instant inicioRango = ZonedDateTime.of(desde, LocalTime.MIN, zonaHoraria).toInstant();
        Instant finRango = ZonedDateTime.of(hasta.plusDays(1), LocalTime.MIN, zonaHoraria).toInstant();

        List<TurnoAgendaResponse> turnos = atenciones.findParaAgenda(barberiaId, barberoId, inicioRango, finRango)
                .stream()
                .map(TurnoAgendaResponse::de)
                .toList();

        List<BloqueoAgendaResponse> bloqueosDelRango = bloqueos.findParaAgenda(barberiaId, barberoId, inicioRango, finRango)
                .stream()
                .map(BloqueoAgendaResponse::de)
                .toList();

        List<BarberoResumen> barberosParaFiltro = barberos.findByBarberiaIdAndActivoTrueOrderByNombreAscApellidoAsc(barberiaId)
                .stream()
                .map(BarberoResumen::de)
                .toList();

        return new AgendaResponse(desde, hasta, barberosParaFiltro, turnos, bloqueosDelRango);
    }

    private UUID barberoPropio() {
        return usuarioActual.barberoId()
                .orElseThrow(() -> new IllegalStateException("El usuario barbero no está vinculado a un barbero"));
    }
}