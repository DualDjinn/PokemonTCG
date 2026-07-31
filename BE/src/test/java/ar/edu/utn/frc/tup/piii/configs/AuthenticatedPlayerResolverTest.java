package ar.edu.utn.frc.tup.piii.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthenticatedPlayerResolverTest {

    @Mock
    private PlayerService playerService;

    private AuthenticatedPlayerResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AuthenticatedPlayerResolver(playerService);
    }

    // --- supportsParameter ---

    @Test
    void supportsParameter_AnnotatedPlayer_ReturnsTrue() throws Exception {
        MethodParameter param = parameterOf("authenticated");
        assertTrue(resolver.supportsParameter(param));
    }

    @Test
    void supportsParameter_NotAnnotated_ReturnsFalse() throws Exception {
        MethodParameter param = parameterOf("notAnnotated");
        assertFalse(resolver.supportsParameter(param));
    }

    @Test
    void supportsParameter_WrongType_ReturnsFalse() throws Exception {
        MethodParameter param = parameterOf("wrongType");
        assertFalse(resolver.supportsParameter(param));
    }

    @Test
    void supportsParameter_PlayerSubclass_ReturnsTrue() throws Exception {
        MethodParameter param = parameterOf("playerSubclass");
        assertTrue(resolver.supportsParameter(param));
    }

    // --- resolveArgument ---

    @Test
    void resolveArgument_ValidSession_ReturnsPlayer() throws Exception {
        Player player = new Player();
        player.setId("p1");
        when(playerService.getPlayerBySession("valid-session")).thenReturn(Optional.of(player));

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie("sessionId", "valid-session"));
        ServletWebRequest webRequest = new ServletWebRequest(httpRequest);

        Object result = resolver.resolveArgument(parameterOf("authenticated"), null, webRequest, null);
        assertEquals(player, result);
    }

    @Test
    void resolveArgument_NoCookie_Throws401() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        ServletWebRequest webRequest = new ServletWebRequest(httpRequest);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(parameterOf("authenticated"), null, webRequest, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void resolveArgument_BlankSession_Throws401() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie("sessionId", "   "));
        ServletWebRequest webRequest = new ServletWebRequest(httpRequest);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(parameterOf("authenticated"), null, webRequest, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void resolveArgument_InvalidSession_Throws401() {
        when(playerService.getPlayerBySession("expired")).thenReturn(Optional.empty());

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie("sessionId", "expired"));
        ServletWebRequest webRequest = new ServletWebRequest(httpRequest);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(parameterOf("authenticated"), null, webRequest, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void resolveArgument_OtherCookiesOnly_Throws401() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie("other", "value"));
        ServletWebRequest webRequest = new ServletWebRequest(httpRequest);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(parameterOf("authenticated"), null, webRequest, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void resolveArgument_NullNativeRequest_Throws401() throws Exception {
        NativeWebRequest webRequest = mock(NativeWebRequest.class);
        when(webRequest.getNativeRequest(HttpServletRequest.class)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(parameterOf("authenticated"), null, webRequest, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void resolveArgument_EmptyCookieArray_Throws401() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie[0]);
        ServletWebRequest webRequest = new ServletWebRequest(httpRequest);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(parameterOf("authenticated"), null, webRequest, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void resolveArgument_SessionAmongOtherCookies_ResolvesPlayer() throws Exception {
        Player player = new Player();
        player.setId("p1");
        when(playerService.getPlayerBySession("real")).thenReturn(Optional.of(player));

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(
                new Cookie("trace", "abc"),
                new Cookie("sessionId", "real"),
                new Cookie("locale", "es"));
        ServletWebRequest webRequest = new ServletWebRequest(httpRequest);

        Object result = resolver.resolveArgument(parameterOf("authenticated"), null, webRequest, null);
        assertEquals(player, result);
    }

    @Test
    void resolveArgument_CookieNameCaseSensitive_Throws401() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie("SESSIONID", "real"));
        ServletWebRequest webRequest = new ServletWebRequest(httpRequest);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> resolver.resolveArgument(parameterOf("authenticated"), null, webRequest, null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    // --- helper: fake controller methods for MethodParameter extraction ---

    private static class PlayerSubclass extends Player {
    }

    @SuppressWarnings("unused")
    private static void sampleMethod(
            @Authenticated Player authenticated,
            Player notAnnotated,
            @Authenticated String wrongType,
            @Authenticated PlayerSubclass playerSubclass) {
    }

    private MethodParameter parameterOf(String name) throws NoSuchMethodException {
        int index = switch (name) {
            case "authenticated" -> 0;
            case "notAnnotated" -> 1;
            case "wrongType" -> 2;
            case "playerSubclass" -> 3;
            default -> throw new IllegalArgumentException(name);
        };
        return new MethodParameter(
                AuthenticatedPlayerResolverTest.class.getDeclaredMethod(
                        "sampleMethod", Player.class, Player.class, String.class, PlayerSubclass.class),
                index);
    }
}
