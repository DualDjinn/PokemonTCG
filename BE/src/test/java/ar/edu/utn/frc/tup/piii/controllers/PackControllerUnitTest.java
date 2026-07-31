package ar.edu.utn.frc.tup.piii.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import ar.edu.utn.frc.tup.piii.dtos.pack.OpenPackRequest;
import ar.edu.utn.frc.tup.piii.dtos.pack.OpenedPackResponse;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.PackService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PackController} that exercise the controller's branching
 * logic in isolation, sin levantar el contexto de Spring.
 *
 * <p>Complementa a {@link PackControllerTest} (que es integración real con cookies
 * y BD) cubriendo el branch CONFLICT (409) — disparado cuando el service lanza
 * {@code IllegalStateException} con un mensaje distinto a "Insufficient coins"
 * (ej. catálogo vacío). En integración real ese path es muy difícil de provocar
 * sin alterar la BD, pero aquí lo testeamos directamente mockeando el service.
 */
class PackControllerUnitTest {

    private PackService packService;
    private PackController controller;
    private Player player;

    @BeforeEach
    void setUp() {
        packService = mock(PackService.class);
        controller = new PackController(packService);

        player = new Player();
        player.setId("p1");
    }

    @Test
    void listPacksDelegatesToService() {
        when(packService.listPacks()).thenReturn(List.of());

        var response = controller.listPacks();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }

    @Test
    void openPackHappyPathReturns200WithBody() {
        OpenedPackResponse mockResponse = new OpenedPackResponse("BRONZE", List.of(), 400);
        when(packService.openPack("p1", "BRONZE", null)).thenReturn(mockResponse);

        var response = controller.openPack(player, null, new OpenPackRequest("BRONZE"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mockResponse);
    }

    @Test
    void openPackWhenIllegalArgumentExceptionReturns400() {
        when(packService.openPack(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Invalid pack type: PLATINUM"));

        var response = controller.openPack(player, null, new OpenPackRequest("PLATINUM"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorApi.class);
        ErrorApi err = (ErrorApi) response.getBody();
        assertThat(err.getStatus()).isEqualTo(400);
        assertThat(err.getMessage()).contains("Invalid pack type");
    }

    @Test
    void openPackWhenInsufficientCoinsReturns402() {
        when(packService.openPack(any(), any(), any()))
                .thenThrow(new IllegalStateException("Insufficient coins"));

        var response = controller.openPack(player, null, new OpenPackRequest("GOLD"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        ErrorApi err = (ErrorApi) response.getBody();
        assertThat(err.getStatus()).isEqualTo(402);
        assertThat(err.getMessage()).isEqualTo("Insufficient coins");
    }

    @Test
    void openPackWhenIllegalStateExceptionWithOtherMessageReturns409() {
        // Cubre el branch CONFLICT del ternario en openPack().
        // Mensaje cualquiera distinto a "Insufficient coins" → 409.
        when(packService.openPack(any(), any(), any()))
                .thenThrow(new IllegalStateException("Card catalog is empty"));

        var response = controller.openPack(player, null, new OpenPackRequest("BRONZE"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorApi err = (ErrorApi) response.getBody();
        assertThat(err.getStatus()).isEqualTo(409);
        assertThat(err.getMessage()).isEqualTo("Card catalog is empty");
    }

    @Test
    void openPackErrorResponseHasTimestamp() {
        when(packService.openPack(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("bad input"));

        var response = controller.openPack(player, null, new OpenPackRequest("BRONZE"));

        ErrorApi err = (ErrorApi) response.getBody();
        assertThat(err.getTimestamp()).isNotNull().isNotBlank();
        assertThat(err.getError()).isEqualTo("Bad Request");
    }

    @Test
    void openPackWhenIllegalStateExceptionWithNullMessageReturns409() {
        // null message → "Insufficient coins".equals(null) is false → CONFLICT
        when(packService.openPack(any(), any(), any()))
                .thenThrow(new IllegalStateException((String) null));

        var response = controller.openPack(player, null, new OpenPackRequest("BRONZE"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ErrorApi err = (ErrorApi) response.getBody();
        assertThat(err.getStatus()).isEqualTo(409);
        assertThat(err.getMessage()).isNull();
    }

    @Test
    void openPackWhenInsufficientCoinsLowercaseReturns409NotPaymentRequired() {
        // Case-sensitive: "insufficient coins" ≠ "Insufficient coins" → CONFLICT
        when(packService.openPack(any(), any(), any()))
                .thenThrow(new IllegalStateException("insufficient coins"));

        var response = controller.openPack(player, null, new OpenPackRequest("GOLD"));

        assertThat(response.getStatusCode())
                .as("comparison is case-sensitive; lowercase doesn't match")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void openPackPaymentRequiredErrorHasCorrectFields() {
        when(packService.openPack(any(), any(), any()))
                .thenThrow(new IllegalStateException("Insufficient coins"));

        var response = controller.openPack(player, null, new OpenPackRequest("GOLD"));

        ErrorApi err = (ErrorApi) response.getBody();
        assertThat(err.getTimestamp()).isNotNull().isNotBlank();
        assertThat(err.getError()).isEqualTo("Payment Required");
    }

    @Test
    void openPackConflictErrorHasCorrectFields() {
        when(packService.openPack(any(), any(), any()))
                .thenThrow(new IllegalStateException("Something else"));

        var response = controller.openPack(player, null, new OpenPackRequest("BRONZE"));

        ErrorApi err = (ErrorApi) response.getBody();
        assertThat(err.getTimestamp()).isNotNull().isNotBlank();
        assertThat(err.getError()).isEqualTo("Conflict");
        assertThat(err.getStatus()).isEqualTo(409);
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
