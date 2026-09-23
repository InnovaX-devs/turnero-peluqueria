package com.innovax.barberia.common;

import com.innovax.barberia.auth.CredencialesInvalidasException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiError> credencialesInvalidas(CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new ApiError(400, "Datos inválidos", errores));
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<ApiError> datosInvalidos(DatosInvalidosException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiError(400, "Datos inválidos", Map.of(ex.getCampo(), ex.getMessage())));
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ApiError> conflicto(ConflictoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(409, ex.getMessage(), Map.of(ex.getCampo(), ex.getMessage())));
    }
}
