package com.innovax.barberia.listaespera;

public enum EstadoListaEspera {
    /** Sigue esperando que se libere un lugar. */
    ESPERANDO,
    /** Ya se le dio turno o se lo sacó de la lista. */
    RESUELTO
}
