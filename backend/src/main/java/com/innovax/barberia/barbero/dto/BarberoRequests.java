package com.innovax.barberia.barbero.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Requests del ABM de barberos (solo admin). */
public final class BarberoRequests {

    private BarberoRequests() {
    }

    /** Solo el email: la contraseña la elige el barbero desde el link de invitación que recibe por mail. */
    public record CrearUsuario(
            @NotBlank(message = "El email es obligatorio")
            @Email(message = "El email no es válido")
            @Size(max = 255, message = "El email puede tener hasta 255 caracteres")
            String email) {
    }

    /** {@code usuario} es opcional: si viene, se crea el login con rol BARBERO y se le manda la invitación. */
    public record Crear(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 80, message = "El nombre puede tener hasta 80 caracteres")
            String nombre,

            @NotBlank(message = "El apellido es obligatorio")
            @Size(max = 80, message = "El apellido puede tener hasta 80 caracteres")
            String apellido,

            @NotNull(message = "Indicá las sucursales")
            Set<UUID> sucursalIds,

            @NotNull(message = "Indicá los servicios")
            Set<UUID> servicioIds,

            @Valid
            CrearUsuario usuario) {
    }

    public record Actualizar(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 80, message = "El nombre puede tener hasta 80 caracteres")
            String nombre,

            @NotBlank(message = "El apellido es obligatorio")
            @Size(max = 80, message = "El apellido puede tener hasta 80 caracteres")
            String apellido,

            @NotNull(message = "Indicá las sucursales")
            Set<UUID> sucursalIds,

            @NotNull(message = "Indicá los servicios")
            Set<UUID> servicioIds) {
    }

    public record CambiarActivo(
            @NotNull(message = "Indicá si el barbero queda activo")
            Boolean activo) {
    }

    /** Un tramo de horario. diaSemana: 1 = lunes ... 7 = domingo. */
    public record Horario(
            @NotNull(message = "La sucursal es obligatoria")
            UUID sucursalId,

            @NotNull(message = "El día es obligatorio")
            @Min(value = 1, message = "El día tiene que ser de 1 (lunes) a 7 (domingo)")
            @Max(value = 7, message = "El día tiene que ser de 1 (lunes) a 7 (domingo)")
            Integer diaSemana,

            @NotNull(message = "La hora de inicio es obligatoria")
            LocalTime horaInicio,

            @NotNull(message = "La hora de fin es obligatoria")
            LocalTime horaFin) {
    }

    public record GuardarHorarios(
            @NotNull(message = "Indicá los horarios")
            @Size(max = 100, message = "Demasiados tramos de horario")
            List<@Valid Horario> horarios) {
    }
}