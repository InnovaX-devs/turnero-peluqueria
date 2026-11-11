package com.innovax.barberia.alta;

import com.innovax.barberia.auth.Rol;
import com.innovax.barberia.auth.Usuario;
import com.innovax.barberia.auth.UsuarioRepository;
import com.innovax.barberia.negocio.Negocio;
import com.innovax.barberia.negocio.NegocioRepository;
import com.innovax.barberia.negocio.Rubro;
import com.innovax.barberia.sucursal.Sucursal;
import com.innovax.barberia.sucursal.SucursalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Da de alta un negocio nuevo (barbería, peluquería, centro de estética o consultorio) con su primera sucursal y su usuario admin, con la contraseña que
 * elige Innovax (lo usa al vender el sistema). Todo lo demás (barberos, servicios, horarios) lo carga el admin desde el panel.
 *
 * <p>El negocio arranca con la reserva online APAGADA: el admin la prende cuando terminó de
 * configurar barberos y servicios, así nadie ve una página de reservas vacía.
 */
@Service
@RequiredArgsConstructor
public class AltaNegocioService {

    static final String ZONA_POR_DEFECTO = "America/Argentina/Cordoba";

    // Las mismas reglas que la base (chk_negocio_slug) y la pantalla de configuración
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?[0-9 ()-]{6,20}$");
    private static final int MINIMO_PASSWORD = 8;

    private final NegocioRepository negocios;
    private final SucursalRepository sucursales;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ResultadoAltaNegocio crear(DatosAltaNegocio datos) {
        String nombre = obligatorio(datos.nombre(), "el nombre del negocio", 120);
        Rubro rubro = rubroDe(datos.rubro());
        String slug = vacio(datos.slug()) ? slugDe(nombre) : datos.slug().trim().toLowerCase(Locale.ROOT);
        String zona = vacio(datos.zonaHoraria()) ? ZONA_POR_DEFECTO : datos.zonaHoraria().trim();
        String sucursalNombre = vacio(datos.sucursalNombre()) ? nombre : datos.sucursalNombre().trim();
        String direccion = obligatorio(datos.direccion(), "la dirección de la sucursal", 255);
        String ciudad = obligatorio(datos.ciudad(), "la ciudad de la sucursal", 120);
        String telefono = vacio(datos.telefono()) ? null : datos.telefono().trim();
        String email = obligatorio(datos.emailAdmin(), "el email del admin", 255).toLowerCase(Locale.ROOT);

        if (slug.length() > 80 || !SLUG.matcher(slug).matches()) {
            throw new AltaInvalidaException("El slug \"" + slug + "\" no sirve para una URL: usá minúsculas, "
                    + "números y guiones (por ejemplo: el-tano)");
        }
        if (negocios.existsBySlug(slug)) {
            throw new AltaInvalidaException("Ya hay un negocio con el slug \"" + slug + "\". Elegí otro.");
        }
        try {
            ZoneId.of(zona);
        } catch (DateTimeException e) {
            throw new AltaInvalidaException("La zona horaria \"" + zona + "\" no existe (por ejemplo: "
                    + ZONA_POR_DEFECTO + ")");
        }
        if (sucursalNombre.length() > 120) {
            throw new AltaInvalidaException("El nombre de la sucursal puede tener hasta 120 caracteres");
        }
        if (telefono != null && !TELEFONO.matcher(telefono).matches()) {
            throw new AltaInvalidaException("El teléfono \"" + telefono + "\" no parece válido (por ejemplo: "
                    + "+5493584123456)");
        }
        if (!EMAIL.matcher(email).matches()) {
            throw new AltaInvalidaException("El email \"" + email + "\" no parece válido");
        }
        String password = datos.passwordAdmin() == null ? "" : datos.passwordAdmin();
        if (password.length() < MINIMO_PASSWORD) {
            throw new AltaInvalidaException("La contraseña tiene que tener al menos " + MINIMO_PASSWORD + " caracteres");
        }
        // BCrypt solo usa los primeros 72 bytes (y Spring tira error si hay más)
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AltaInvalidaException("La contraseña es demasiado larga");
        }
        if (usuarios.findByEmailIgnoreCase(email).isPresent()) {
            // El email es único en todo el sistema, no por barbería: es el login
            throw new AltaInvalidaException("Ya hay un usuario con el email \"" + email + "\"");
        }

        Negocio negocio = new Negocio();
        negocio.setNombre(nombre);
        negocio.setSlug(slug);
        negocio.setRubro(rubro);
        negocio.setZonaHoraria(zona);
        negocio.setReservasOnline(false);
        negocio = negocios.save(negocio);

        Sucursal sucursal = new Sucursal();
        sucursal.setNegocio(negocio);
        sucursal.setNombre(sucursalNombre);
        sucursal.setDireccion(direccion);
        sucursal.setCiudad(ciudad);
        sucursal.setTelefono(telefono);
        sucursales.save(sucursal);

        Usuario admin = new Usuario();
        admin.setNegocio(negocio);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRol(Rol.ADMIN);
        usuarios.save(admin);

        return new ResultadoAltaNegocio(negocio.getId(), nombre, slug, email, rubro);
    }

    /** Sin rubro es BARBERIA (lo que había antes de que existiera el rubro). */
    private static Rubro rubroDe(String texto) {
        if (vacio(texto)) {
            return Rubro.BARBERIA;
        }
        try {
            return Rubro.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new AltaInvalidaException("El rubro \"" + texto.trim() + "\" no existe. Los rubros son: "
                    + java.util.Arrays.stream(Rubro.values()).map(Enum::name).collect(java.util.stream.Collectors.joining(", ")));
        }
    }

    /** "Barbería El Tano" → "barberia-el-tano" */
    static String slugDe(String nombre) {
        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.length() > 80 ? slug.substring(0, 80).replaceAll("-$", "") : slug;
    }


    private static String obligatorio(String valor, String que, int largoMaximo) {
        if (vacio(valor)) {
            throw new AltaInvalidaException("Falta " + que);
        }
        String limpio = valor.trim();
        if (limpio.length() > largoMaximo) {
            throw new AltaInvalidaException(Character.toUpperCase(que.charAt(0)) + que.substring(1)
                    + " puede tener hasta " + largoMaximo + " caracteres");
        }
        return limpio;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
