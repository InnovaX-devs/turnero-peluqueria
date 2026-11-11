package com.innovax.barberia.alta;

/**
 * Lo mínimo para que una barbería nueva pueda entrar al panel y configurarse sola.
 * slug, zonaHoraria, telefono y rubro son opcionales (el slug se arma con el nombre si no viene;
 * sin rubro, el negocio es una BARBERIA). El rubro llega como texto: BARBERIA, PELUQUERIA, ESTETICA o SALUD.
 * La contraseña la elige Innovax al dar el alta; el admin la cambia después desde "Mi cuenta".
 */
public record DatosAltaNegocio(
        String nombre,
        String slug,
        String zonaHoraria,
        String sucursalNombre,
        String direccion,
        String ciudad,
        String telefono,
        String emailAdmin,
        String passwordAdmin,
        String rubro) {
}
