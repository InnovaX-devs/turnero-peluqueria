package com.innovax.barberia.common;

import java.util.Map;

/** Formato único de error para toda la API. */
public record ApiError(int status, String mensaje, Map<String, String> errores) {

    public static ApiError of(int status, String mensaje) {
        return new ApiError(status, mensaje, Map.of());
    }
}
