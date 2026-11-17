package com.innovax.barberia.common;

import com.innovax.barberia.auth.CredencialesInvalidasException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiError> credencialesInvalidas(CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, ex.getMessage()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> recursoNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        String mensaje = "El parámetro «%s» tiene un formato inválido".formatted(ex.getName());
        return ResponseEntity.badRequest().body(ApiError.of(400, mensaje));
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

    /**
     * Una restricción de la base que el código no esperaba (ej. dos reservas de un mismo DNI al mismo tiempo).
     * Issue #73: el mensaje de Postgres trae los valores que chocaron (ej. el DNI del paciente), así que
     * NO se loguea ni se devuelve: solo la clase de la causa y el código SQLSTATE, que alcanzan para diagnosticar.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridadDeDatos(DataIntegrityViolationException ex) {
        Throwable causa = NestedExceptionUtils.getMostSpecificCause(ex);
        String sqlState = causa instanceof java.sql.SQLException sql ? sql.getSQLState() : "n/a";
        log.warn("Violación de integridad de datos (causa: {}, SQLSTATE: {})", causa.getClass().getSimpleName(), sqlState);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "Los datos entran en conflicto con otros ya cargados"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> archivoMuyGrande(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(413).body(ApiError.of(413, "El archivo pesa más de 5 MB"));
    }
}