package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.player.ForgotPasswordRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.GoogleLoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerResponse;
import ar.edu.utn.frc.tup.piii.dtos.player.ResetPasswordRequest;
import ar.edu.utn.frc.tup.piii.services.CollectionService;
import ar.edu.utn.frc.tup.piii.services.DeckService;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PlayerController} that drive the controller directly
 * with mocked services. Complements the integration {@link PlayerControllerTest}
 * by covering the happy paths of {@code googleLogin} and {@code resetPassword}
 * (which need external services to succeed) and the catch branch of
 * {@code forgotPassword} (which only fires when the service throws).
 */
class PlayerControllerUnitTest {

    private PlayerService playerService;
    private DeckService deckService;
    private CollectionService collectionService;
    private PlayerController controller;

    @BeforeEach
    void setUp() {
        playerService = mock(PlayerService.class);
        deckService = mock(DeckService.class);
        collectionService = mock(CollectionService.class);
        controller = new PlayerController(playerService, deckService, collectionService);
    }

    // -----------------------------------------------------------------------
    // googleLogin — happy path. Real Google OAuth verification can't run in
    // a normal test, so this is the only practical place to exercise it.
    // -----------------------------------------------------------------------

    @Test
    void googleLogin_ReturnsPlayerWithSessionCookie() {
        PlayerResponse player = new PlayerResponse();
        player.setId("p-google");
        player.setUsername("ash@gmail.com");

        when(playerService.googleLogin("valid-google-token")).thenReturn(player);
        when(playerService.createSession("p-google")).thenReturn("sess-google");

        GoogleLoginRequest req = new GoogleLoginRequest();
        req.setCredential("valid-google-token");

        ResponseEntity<PlayerResponse> response = controller.googleLogin(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(player);

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .isNotNull()
                .contains("sessionId=sess-google")
                .containsIgnoringCase("HttpOnly")
                .contains("Path=/")
                .contains("Max-Age=86400");
    }

    @Test
    void googleLogin_CreatesSessionForReturnedPlayerId() {
        PlayerResponse player = new PlayerResponse();
        player.setId("p-42");
        when(playerService.googleLogin(anyString())).thenReturn(player);
        when(playerService.createSession(anyString())).thenReturn("sess-x");

        GoogleLoginRequest req = new GoogleLoginRequest();
        req.setCredential("tok");

        controller.googleLogin(req);

        verify(playerService).createSession("p-42");
    }

    // -----------------------------------------------------------------------
    // resetPassword — happy path. The integration test only covers the
    // invalid-token failure; this covers the success branch.
    // -----------------------------------------------------------------------

    @Test
    void resetPassword_ValidToken_DelegatesToServiceAndReturns200() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("valid-token");
        req.setNewPassword("newPass123");

        ResponseEntity<Void> response = controller.resetPassword(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(playerService).confirmPasswordReset("valid-token", "newPass123");
    }

    // -----------------------------------------------------------------------
    // forgotPassword — catch branch. Pinpoints the documented security
    // guarantee: even if the underlying service throws, the endpoint must
    // return 200 to avoid email-existence enumeration.
    // -----------------------------------------------------------------------

    @Test
    void forgotPassword_ServiceThrows_StillReturns200() {
        doThrow(new RuntimeException("SMTP down: connection refused to mail.example.com"))
                .when(playerService).requestPasswordReset("victim@test.com");

        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("victim@test.com");

        ResponseEntity<Void> response = controller.forgotPassword(req);

        assertThat(response.getStatusCode())
                .as("forgot-password must swallow service errors so the response "
                  + "doesn't leak whether the email is registered or the mail "
                  + "infrastructure is reachable")
                .isEqualTo(HttpStatus.OK);
        verify(playerService).requestPasswordReset("victim@test.com");
    }

    @Test
    void forgotPassword_ServiceSucceeds_Returns200() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("normal@test.com");

        ResponseEntity<Void> response = controller.forgotPassword(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(playerService).requestPasswordReset("normal@test.com");
    }
}
