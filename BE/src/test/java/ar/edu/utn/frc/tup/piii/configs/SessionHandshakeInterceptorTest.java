package ar.edu.utn.frc.tup.piii.configs;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class SessionHandshakeInterceptorTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    private SessionHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new SessionHandshakeInterceptor(playerService);
    }

    @Test
    void beforeHandshake_ValidSession_StoresPlayerAttributes() {
        Player player = new Player();
        player.setId("p1");
        player.setTrainerName("Ash");
        when(playerService.getPlayerBySession("sess-1")).thenReturn(Optional.of(player));

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie("sessionId", "sess-1"));
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpRequest);
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertTrue(result);
        assertEquals("p1", attributes.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR));
        assertEquals("Ash", attributes.get(SessionHandshakeInterceptor.PLAYER_NAME_ATTR));
    }

    @Test
    void beforeHandshake_InvalidSession_AllowsButNoAttributes() {
        when(playerService.getPlayerBySession("bad")).thenReturn(Optional.empty());

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie("sessionId", "bad"));
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpRequest);
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertTrue(result);
        assertFalse(attributes.containsKey(SessionHandshakeInterceptor.PLAYER_ID_ATTR));
    }

    @Test
    void beforeHandshake_NoCookie_AllowsButNoAttributes() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpRequest);
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertTrue(result);
        assertFalse(attributes.containsKey(SessionHandshakeInterceptor.PLAYER_ID_ATTR));
    }

    @Test
    void beforeHandshake_OtherCookiesOnly_AllowsButNoAttributes() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie("other", "value"));
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpRequest);
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertTrue(result);
        assertFalse(attributes.containsKey(SessionHandshakeInterceptor.PLAYER_ID_ATTR));
    }

    @Test
    void beforeHandshake_NonServletRequest_AllowsAndSkipsLookup() {
        ServerHttpRequest nonServletRequest = mock(ServerHttpRequest.class);
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(nonServletRequest, response, wsHandler, attributes);

        assertTrue(result);
        assertTrue(attributes.isEmpty());
        verifyNoInteractions(playerService);
    }

    @Test
    void beforeHandshake_EmptyCookieArray_AllowsButNoAttributes() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie[0]);
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpRequest);
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        assertTrue(result);
        assertTrue(attributes.isEmpty());
        verifyNoInteractions(playerService);
    }

    @Test
    void beforeHandshake_SessionAmongOtherCookies_StoresAttributes() {
        Player player = new Player();
        player.setId("p9");
        player.setTrainerName("Misty");
        when(playerService.getPlayerBySession("real")).thenReturn(Optional.of(player));

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(
                new Cookie("trace", "abc"),
                new Cookie("sessionId", "real"),
                new Cookie("locale", "es"));
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpRequest);
        Map<String, Object> attributes = new HashMap<>();

        assertTrue(interceptor.beforeHandshake(request, response, wsHandler, attributes));
        assertEquals("p9", attributes.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR));
        assertEquals("Misty", attributes.get(SessionHandshakeInterceptor.PLAYER_NAME_ATTR));
    }

    @Test
    void beforeHandshake_CookieNameCaseSensitive_DoesNotResolve() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie("SESSIONID", "real"));
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpRequest);
        Map<String, Object> attributes = new HashMap<>();

        assertTrue(interceptor.beforeHandshake(request, response, wsHandler, attributes));
        assertTrue(attributes.isEmpty());
        verifyNoInteractions(playerService);
    }

    @Test
    void beforeHandshake_PlayerWithNullTrainerName_StoresNullAttribute() {
        Player player = new Player();
        player.setId("p2");
        player.setTrainerName(null);
        when(playerService.getPlayerBySession("sess-2")).thenReturn(Optional.of(player));

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setCookies(new Cookie("sessionId", "sess-2"));
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpRequest);
        Map<String, Object> attributes = new HashMap<>();

        assertTrue(interceptor.beforeHandshake(request, response, wsHandler, attributes));
        assertEquals("p2", attributes.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR));
        assertTrue(attributes.containsKey(SessionHandshakeInterceptor.PLAYER_NAME_ATTR));
        assertNull(attributes.get(SessionHandshakeInterceptor.PLAYER_NAME_ATTR));
    }

    @Test
    void afterHandshake_IsNoOp_DoesNotThrow() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        ServletServerHttpRequest request = new ServletServerHttpRequest(httpRequest);

        assertThatCode(() -> interceptor.afterHandshake(request, response, wsHandler, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> interceptor.afterHandshake(request, response, wsHandler,
                new RuntimeException("boom"))).doesNotThrowAnyException();
        verifyNoInteractions(playerService);
    }
}
