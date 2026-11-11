package com.innovax.barberia.negocio;

/**
 * Rubro del negocio. Decide cómo se nombran las cosas en los mensajes y mails
 * (barbero / profesional, cliente / paciente, corte / atención...).
 *
 * <p>Tiene su espejo en el front: frontend/src/lib/terminos.ts. Si se cambia una palabra acá,
 * hay que cambiarla allá también, para que el panel y los mensajes hablen igual.
 */
public enum Rubro {

    BARBERIA(
            new Termino("barbería", "barberías", true),
            new Termino("barbero", "barberos", false),
            new Termino("cliente", "clientes", false),
            new Termino("servicio", "servicios", false),
            new Termino("corte", "cortes", false)),

    PELUQUERIA(
            new Termino("peluquería", "peluquerías", true),
            new Termino("estilista", "estilistas", false),
            new Termino("cliente", "clientes", false),
            new Termino("servicio", "servicios", false),
            new Termino("atención", "atenciones", true)),

    ESTETICA(
            new Termino("centro de estética", "centros de estética", false),
            new Termino("esteticista", "esteticistas", false),
            new Termino("cliente", "clientes", false),
            new Termino("tratamiento", "tratamientos", false),
            new Termino("atención", "atenciones", true)),

    SALUD(
            new Termino("consultorio", "consultorios", false),
            new Termino("profesional", "profesionales", false),
            new Termino("paciente", "pacientes", false),
            new Termino("prestación", "prestaciones", true),
            new Termino("atención", "atenciones", true));

    private final Termino negocio;
    private final Termino profesional;
    private final Termino cliente;
    private final Termino servicio;
    private final Termino atencion;

    Rubro(Termino negocio, Termino profesional, Termino cliente, Termino servicio, Termino atencion) {
        this.negocio = negocio;
        this.profesional = profesional;
        this.cliente = cliente;
        this.servicio = servicio;
        this.atencion = atencion;
    }

    /** "la barbería", "el consultorio". */
    public Termino negocio() {
        return negocio;
    }

    /** Quien atiende: barbero, estilista, esteticista, profesional. */
    public Termino profesional() {
        return profesional;
    }

    /** Quien se atiende: cliente o paciente. */
    public Termino cliente() {
        return cliente;
    }

    /** Lo que se reserva: servicio, tratamiento o prestación. */
    public Termino servicio() {
        return servicio;
    }

    /** Cada vez que se atiende a alguien: corte (barbería) o atención. */
    public Termino atencion() {
        return atencion;
    }

    /** Una palabra con su plural y su género, para armar frases con el artículo correcto. */
    public record Termino(String singular, String plural, boolean femenino) {

        /** "barbero" → "Barbero". */
        public String capitalizado() {
            return mayuscula(singular);
        }

        public String pluralCapitalizado() {
            return mayuscula(plural);
        }

        /** "la barbería" / "el barbero". */
        public String el() {
            return (femenino ? "la " : "el ") + singular;
        }

        /** "de la barbería" / "del barbero". */
        public String del() {
            return (femenino ? "de la " : "del ") + singular;
        }

        /** "a la barbería" / "al barbero". */
        public String al() {
            return (femenino ? "a la " : "al ") + singular;
        }

        /** "esta barbería" / "este barbero". */
        public String este() {
            return (femenino ? "esta " : "este ") + singular;
        }

        /** "esa prestación" / "ese servicio". */
        public String ese() {
            return (femenino ? "esa " : "ese ") + singular;
        }

        /** "La barbería" / "El barbero", para empezar una frase. */
        public String elCapitalizado() {
            return mayuscula(el());
        }

        /** "Esta barbería" / "Este barbero", para empezar una frase. */
        public String esteCapitalizado() {
            return mayuscula(este());
        }

        /** "Esa prestación" / "Ese servicio", para empezar una frase. */
        public String eseCapitalizado() {
            return mayuscula(ese());
        }

        private static String mayuscula(String s) {
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}
