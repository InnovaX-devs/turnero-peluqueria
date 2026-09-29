package com.innovax.barberia.atencion.dto;

import com.innovax.barberia.atencion.MetodoPago;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Corte registrado en el local (cliente sin turno). Lo único obligatorio: servicio, precio y pago. */
public record RegistrarCorteRequest(

        /** null = corte anónimo */
        UUID clienteId,

        @NotNull(message = "Elegí el servicio")
        UUID servicioId,

        /** null = el barbero logueado */
        UUID barberoId,

        /** null = la única sucursal del barbero */
        UUID sucursalId,

        @NotNull(message = "El precio es obligatorio")
        @Min(value = 0, message = "El precio no puede ser negativo")
        Long precioFinal,

        @NotNull(message = "Elegí el método de pago")
        MetodoPago metodoPago,

        @Size(max = 2000, message = "Las notas pueden tener hasta 2000 caracteres")
        String notas,

        /** fecha y hora null = ahora. Si vienen, en la zona horaria de la barbería. */
        LocalDate fecha,
        LocalTime hora
) {
}