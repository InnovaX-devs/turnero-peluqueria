package com.innovax.barberia.notificacion;

import com.innovax.barberia.atencion.Atencion;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Arma el texto de los mensajes al cliente.
 * Recorre relaciones lazy de la atención (barbería, sucursal, barbero, servicio, cliente),
 * así que hay que llamarlo dentro de una transacción.
 */
@Component
public class MensajeBuilder {

    private static final Locale ES_AR = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES_AR);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm", ES_AR);

    private final NotificacionLinks links;

    public MensajeBuilder(NotificacionLinks links) {
        this.links = links;
    }

    public String confirmacion(Atencion a) {
        return saludo(a) + "Tu turno en " + a.getBarberia().getNombre() + " quedó confirmado.\n\n"
                + detalle(a)
                + "\n" + pieGestion(a);
    }

    public String recordatorio(Atencion a) {
        return saludo(a) + "Te recordamos tu turno en " + a.getBarberia().getNombre() + ".\n\n"
                + detalle(a)
                + "\n" + pieGestion(a);
    }

    private String saludo(Atencion a) {
        if (a.getCliente() == null) {
            return "¡Hola! ";
        }
        return "¡Hola " + a.getCliente().getNombre() + "! ";
    }

    private String detalle(Atencion a) {
        ZonedDateTime inicio = a.getInicio().atZone(ZoneId.of(a.getBarberia().getZonaHoraria()));
        return "Fecha: " + inicio.format(DIA) + "\n"
                + "Hora: " + inicio.format(HORA) + " hs\n"
                + "Barbero: " + a.getBarbero().getNombre() + " " + a.getBarbero().getApellido() + "\n"
                + "Servicio: " + a.getServicio().getNombre() + "\n"
                + "Dirección: " + a.getSucursal().getDireccion() + ", " + a.getSucursal().getCiudad() + "\n";
    }

    private String pieGestion(Atencion a) {
        return "Si necesitás cancelarlo o cambiarlo (hasta "
                + textoAnticipacion(a.getBarberia().getAnticipacionCancelacionMin()) + " antes), entrá acá:\n"
                + links.gestionTurno(a.getTokenGestion());
    }

    /** 120 → "2 horas", 90 → "1 h 30 min", 30 → "30 minutos". */
    private static String textoAnticipacion(int minutos) {
        int horas = minutos / 60;
        int resto = minutos % 60;
        if (horas == 0) {
            return resto + (resto == 1 ? " minuto" : " minutos");
        }
        if (resto == 0) {
            return horas + (horas == 1 ? " hora" : " horas");
        }
        return horas + " h " + resto + " min";
    }
}