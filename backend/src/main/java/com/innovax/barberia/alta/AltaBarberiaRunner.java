package com.innovax.barberia.alta;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Comando para dar de alta una barbería desde la terminal (scripts/alta-barberia.sh).
 * Solo existe si ALTA_EJECUTAR=true: al levantar la app normal o en los tests no hace nada.
 * Los datos llegan por variables de entorno (ALTA_NOMBRE, ALTA_SLUG, ...) para que los nombres
 * con espacios o tildes no se rompan en el camino. Al terminar, cierra la aplicación.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "alta", name = "ejecutar", havingValue = "true")
@RequiredArgsConstructor
public class AltaBarberiaRunner implements ApplicationRunner {

    private final AltaBarberiaService altaBarberiaService;
    private final ConfigurableApplicationContext contexto;

    @Value("${alta.nombre:}") private String nombre;
    @Value("${alta.slug:}") private String slug;
    @Value("${alta.zona-horaria:}") private String zonaHoraria;
    @Value("${alta.sucursal:}") private String sucursal;
    @Value("${alta.direccion:}") private String direccion;
    @Value("${alta.ciudad:}") private String ciudad;
    @Value("${alta.telefono:}") private String telefono;
    @Value("${alta.email:}") private String email;
    @Value("${alta.password:}") private String password;
    @Value("${spring.datasource.url:}") private String baseDeDatos;
    @Value("${app.frontend-url:http://localhost:5173}") private String frontendUrl;

    @Override
    public void run(ApplicationArguments args) {
        int codigo;
        try {
            ResultadoAltaBarberia r = altaBarberiaService.crear(new DatosAltaBarberia(
                    nombre, slug, zonaHoraria, sucursal, direccion, ciudad, telefono, email, password));
            String base = frontendUrl.replaceAll("/+$", "");
            System.out.println("""

                    ============================================================
                     Barbería creada: %s
                    ============================================================
                     Base de datos:      %s
                     Página de reservas: %s/b/%s  (apagada hasta que el admin la prenda)
                     Panel:              %s/panel/login
                     Usuario admin:      %s
                     Contraseña:         la que cargaste

                     Pasale estos datos al dueño y pedile que cambie la
                     contraseña desde "Mi cuenta" la primera vez que entre.

                     Próximos pasos del admin, desde el panel:
                       1. Servicios: cargar servicios, duraciones y precios.
                       2. Barberos: cargar barberos, sus servicios y su horario.
                       3. Configuración: logo, reglas de reserva, y prender la
                          reserva online.
                    ============================================================
                    """.formatted(r.nombre(), baseDeDatos, base, r.slug(), base, r.emailAdmin()));
            codigo = 0;
        } catch (AltaInvalidaException e) {
            System.out.println("\nNo se creó la barbería: " + e.getMessage() + "\n");
            codigo = 1;
        } catch (RuntimeException e) {
            log.error("Error inesperado al dar de alta la barbería", e);
            codigo = 2;
        }
        int salida = codigo;
        System.exit(SpringApplication.exit(contexto, () -> salida));
    }
}
