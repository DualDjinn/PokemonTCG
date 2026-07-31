package ar.edu.utn.frc.tup.piii.configs;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorApi> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorApi error = ErrorApi.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(400)
                .error("Bad Request")
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorApi> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        logger.warn("Concurrent modification conflict: {}", ex.getMessage());
        ErrorApi error = ErrorApi.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(409)
                .error("Conflict")
                .message("La operación entró en conflicto con otra concurrente. Reintentá.")
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorApi> handleResponseStatus(ResponseStatusException ex) {
        ErrorApi error = ErrorApi.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(ex.getStatusCode().value())
                .error(ex.getReason())
                .message(ex.getReason())
                .build();
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorApi> handleRuntime(RuntimeException ex) {
        logger.error("Unhandled exception", ex);
        ErrorApi error = ErrorApi.builder()
                .timestamp(LocalDateTime.now().toString())
                .status(500)
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
