package com.innovax.barberia.agenda;

import com.innovax.barberia.agenda.dto.AgendaResponse;
import com.innovax.barberia.agenda.dto.ProfesionalResumen;
import com.innovax.barberia.agenda.dto.BloqueoAgendaResponse;
import com.innovax.barberia.agenda.dto.TurnoAgendaResponse;
import com.innovax.barberia.atencion.AtencionRepository;
import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.UsuarioActual;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.profesional.ProfesionalRepository;
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
 * <p>REGLA DE PERMISOS: un usuario con rol PROFESIONAL ve siempre y solo su propia agenda, sin
 * importar qué {@code profesionalId} venga en el request (ver {@link UsuarioActual}). Un ADMIN puede
 * pedir un barbero puntual o dejar el filtro vacío para ver la agenda completa de la barbería.
 */
@Service
@RequiredArgsConstructor
public class AgendaService {

    /** Una vista semana son 7 días; se deja margen para no atarse a ese número exacto. */
    private static final int RANGO_MAXIMO_DIAS = 31;

    private final UsuarioActual usuarioActual;
    private final NegocioRepository negocios;
    private final ProfesionalRepository profesionales;
    private final AtencionRepository atenciones;
    private final BloqueoRepository bloqueos;

    @Transactional(readOnly = true)
    public AgendaResponse consultar(LocalDate desde, LocalDate hasta, UUID profesionalIdPedido) {
        if (hasta.isBefore(desde)) {
            throw new DatosInvalidosException("hasta", "No puede ser anterior a «desde»");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > RANGO_MAXIMO_DIAS) {
            throw new DatosInvalidosException("hasta", "El rango no puede superar los " + RANGO_MAXIMO_DIAS + " días");
        }

        UUID negocioId = usuarioActual.negocioId();
        Negocio negocio = negocios.findById(negocioId)
                .orElseThrow(() -> new IllegalStateException("La barbería del token no existe"));

        UUID profesionalId = usuarioActual.rol() == Rol.PROFESIONAL
                ? profesionalPropio()
                : profesionalIdPedido;

        ZoneId zonaHoraria = ZoneId.of(negocio.getZonaHoraria());
        Instant inicioRango = ZonedDateTime.of(desde, LocalTime.MIN, zonaHoraria).toInstant();
        Instant finRango = ZonedDateTime.of(hasta.plusDays(1), LocalTime.MIN, zonaHoraria).toInstant();

        List<TurnoAgendaResponse> turnos = atenciones.findParaAgenda(negocioId, profesionalId, inicioRango, finRango)
                .stream()
                .map(TurnoAgendaResponse::de)
                .toList();

        List<BloqueoAgendaResponse> bloqueosDelRango = bloqueos.findParaAgenda(negocioId, profesionalId, inicioRango, finRango)
                .stream()
                .map(BloqueoAgendaResponse::de)
                .toList();

        List<ProfesionalResumen> profesionalesParaFiltro = profesionales.findByNegocioIdAndActivoTrueOrderByNombreAscApellidoAsc(negocioId)
                .stream()
                .map(ProfesionalResumen::de)
                .toList();

        return new AgendaResponse(desde, hasta, profesionalesParaFiltro, turnos, bloqueosDelRango);
    }

    private UUID profesionalPropio() {
        return usuarioActual.profesionalId()
                .orElseThrow(() -> new IllegalStateException("El usuario barbero no está vinculado a un barbero"));
    }
}