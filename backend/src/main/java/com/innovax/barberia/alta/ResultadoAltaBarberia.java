package com.innovax.barberia.alta;

import java.util.UUID;

/** Lo que hay que pasarle al dueño (más la contraseña, que la cargó quien hizo el alta). */
public record ResultadoAltaBarberia(UUID barberiaId, String nombre, String slug, String emailAdmin) {
}
