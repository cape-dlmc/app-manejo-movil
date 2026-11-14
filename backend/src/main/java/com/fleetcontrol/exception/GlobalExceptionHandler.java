package com.fleetcontrol.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;
import java.util.stream.Collectors;

/** Respuestas de error uniformes: { "error": "mensaje", "status": 400 } (RNF-09). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> negocio(ApiException e) {
        return respuesta(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> permisos(AccessDeniedException e) {
        return respuesta(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> conflicto(DataIntegrityViolationException e) {
        return respuesta(HttpStatus.CONFLICT, "Los datos entran en conflicto con registros existentes");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> archivoGrande(MaxUploadSizeExceededException e) {
        return respuesta(HttpStatus.PAYLOAD_TOO_LARGE, "El comprobante supera el tamaño máximo de 5 MB");
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje, "status", status.value()));
    }
}
