package ar.edu.utn.frc.tup.piii.configs;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResponseStatus_Unauthorized_Returns401() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");

        ResponseEntity<ErrorApi> response = handler.handleResponseStatus(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Not authenticated");
        assertThat(response.getBody().getError()).isEqualTo("Not authenticated");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleResponseStatus_NotFound_Returns404() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found");

        ResponseEntity<ErrorApi> response = handler.handleResponseStatus(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Card not found");
    }

    @Test
    void handleResponseStatus_WithStatusReason_ReturnsCorrectError() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input");

        ResponseEntity<ErrorApi> response = handler.handleResponseStatus(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Invalid input");
    }

    @Test
    void handleIllegalArgument_Returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Username already exists");

        ResponseEntity<ErrorApi> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Username already exists");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleOptimisticLock_Returns409() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("optimistic lock failed", new RuntimeException("v mismatch"));

        ResponseEntity<ErrorApi> response = handler.handleOptimisticLock(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).contains("conflicto");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleOptimisticLock_DoesNotLeakInternalDetails() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("Row was updated by another transaction; UPDATE player SET coins", null);

        ResponseEntity<ErrorApi> response = handler.handleOptimisticLock(ex);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .as("409 body must surface a user-facing message, not the raw exception text")
                .doesNotContain("UPDATE player", "transaction")
                .isEqualTo("La operación entró en conflicto con otra concurrente. Reintentá.");
    }

    @Test
    void handleRuntime_Returns500() {
        RuntimeException ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorApi> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleIllegalArgument_NullMessage_DoesNotThrow() {
        IllegalArgumentException ex = new IllegalArgumentException((String) null);

        ResponseEntity<ErrorApi> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isNull();
    }

    @Test
    void handleResponseStatus_NullReason_PropagatesNull() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN);

        ResponseEntity<ErrorApi> response = handler.handleResponseStatus(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getError()).isNull();
        assertThat(response.getBody().getMessage()).isNull();
    }

    @Test
    void handleRuntime_DoesNotLeakInternalExceptionMessage() {
        RuntimeException ex = new RuntimeException(
                "SQLState[42000]: password=hunter2 at jdbc:postgres://prod-db:5432");

        ResponseEntity<ErrorApi> response = handler.handleRuntime(ex);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .as("500 body must not leak internal exception details")
                .doesNotContain("SQLState", "password", "jdbc")
                .isEqualTo("An unexpected error occurred");
    }

    @Test
    void handleRuntime_AcceptsNullMessage() {
        ResponseEntity<ErrorApi> response = handler.handleRuntime(new RuntimeException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void allHandlers_ProduceParseableTimestamp() {
        ErrorApi illegal = handler.handleIllegalArgument(new IllegalArgumentException("x")).getBody();
        ErrorApi status = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "y")).getBody();
        ErrorApi runtime = handler.handleRuntime(new RuntimeException("z")).getBody();

        assertThat(illegal).isNotNull();
        assertThat(status).isNotNull();
        assertThat(runtime).isNotNull();
        assertThatCode(() -> LocalDateTime.parse(illegal.getTimestamp())).doesNotThrowAnyException();
        assertThatCode(() -> LocalDateTime.parse(status.getTimestamp())).doesNotThrowAnyException();
        assertThatCode(() -> LocalDateTime.parse(runtime.getTimestamp())).doesNotThrowAnyException();
    }
}
