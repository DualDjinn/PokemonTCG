package ar.edu.utn.frc.tup.piii.configs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import ar.edu.utn.frc.tup.piii.services.PlayerService;

import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private MessageBrokerRegistry brokerRegistry;

    @Mock
    private StompEndpointRegistry stompRegistry;

    @Mock
    private StompWebSocketEndpointRegistration endpointRegistration;

    @Mock
    private SimpleBrokerRegistration brokerRegistration;

    private WebSocketConfig config;

    @BeforeEach
    void setUp() {
        config = new WebSocketConfig(playerService);
        ReflectionTestUtils.setField(config, "allowedOrigins",
                new String[]{"http://localhost:4200"});
    }

    /**
     * El broker encadena {@code enableSimpleBroker(...).setHeartbeatValue(...).setTaskScheduler(...)};
     * sin estos stubs el primer eslabón devuelve null y la cadena tira NPE.
     */
    private void stubBrokerChain() {
        when(brokerRegistry.enableSimpleBroker("/topic", "/queue")).thenReturn(brokerRegistration);
        when(brokerRegistration.setHeartbeatValue(any())).thenReturn(brokerRegistration);
    }

    @Test
    void configureMessageBroker_EnablesSimpleBrokerOnTopicAndQueue() {
        stubBrokerChain();

        config.configureMessageBroker(brokerRegistry);

        verify(brokerRegistry).enableSimpleBroker("/topic", "/queue");
    }

    @Test
    void configureMessageBroker_ConfiguresHeartbeatWithTaskScheduler() {
        stubBrokerChain();

        config.configureMessageBroker(brokerRegistry);

        // El broker simple necesita heartbeats + un TaskScheduler propio para mantener vivo
        // el WebSocket detrás del devtunnel.
        verify(brokerRegistration).setHeartbeatValue(new long[] {10000, 10000});
        verify(brokerRegistration).setTaskScheduler(any());
    }

    @Test
    void configureMessageBroker_SetsAppDestinationPrefix() {
        stubBrokerChain();

        config.configureMessageBroker(brokerRegistry);

        verify(brokerRegistry).setApplicationDestinationPrefixes("/app");
    }

    @Test
    void configureMessageBroker_SetsUserDestinationPrefix() {
        stubBrokerChain();

        config.configureMessageBroker(brokerRegistry);

        verify(brokerRegistry).setUserDestinationPrefix("/user");
    }

    @Test
    void webSocketHeartbeatScheduler_ReturnsInitializedScheduler() {
        var scheduler = config.webSocketHeartbeatScheduler();
        assertThat(scheduler).isNotNull();
    }

    @Test
    void registerStompEndpoints_RegistersWsEndpoint() {
        when(stompRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(endpointRegistration);

        config.registerStompEndpoints(stompRegistry);

        verify(stompRegistry).addEndpoint("/ws");
    }

    @Test
    void registerStompEndpoints_AttachesSessionHandshakeInterceptor() {
        when(stompRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(endpointRegistration);

        config.registerStompEndpoints(stompRegistry);

        ArgumentCaptor<HandshakeInterceptor> captor =
                ArgumentCaptor.forClass(HandshakeInterceptor.class);
        verify(endpointRegistration).addInterceptors(captor.capture());

        assertThat(captor.getValue())
                .as("STOMP /ws endpoint must wire SessionHandshakeInterceptor "
                  + "or anonymous clients can pass identity from the client")
                .isInstanceOf(SessionHandshakeInterceptor.class);
    }

    @Test
    void registerStompEndpoints_PassesAllowedOriginsFromProperties() {
        when(stompRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(endpointRegistration);

        config.registerStompEndpoints(stompRegistry);

        verify(endpointRegistration).setAllowedOriginPatterns("http://localhost:4200");
    }

    @Test
    void registerStompEndpoints_SpreadsMultipleAllowedOrigins() {
        ReflectionTestUtils.setField(config, "allowedOrigins", new String[]{
                "http://localhost:4200",
                "https://tunnel-4200.example.com",
                "https://tunnel-8080.example.com"
        });
        when(stompRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(endpointRegistration);

        config.registerStompEndpoints(stompRegistry);

        verify(endpointRegistration).setAllowedOriginPatterns(
                "http://localhost:4200",
                "https://tunnel-4200.example.com",
                "https://tunnel-8080.example.com");
    }

    @Test
    void registerStompEndpoints_EmptyAllowedOriginsArray_StillRegistersEndpoint() {
        ReflectionTestUtils.setField(config, "allowedOrigins", new String[0]);
        when(stompRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(endpointRegistration);

        config.registerStompEndpoints(stompRegistry);

        verify(stompRegistry).addEndpoint("/ws");
        verify(endpointRegistration).setAllowedOriginPatterns();
    }

    @Test
    void registerStompEndpoints_NullAllowedOrigins_SilentlyAccepted() {
        // Finding: Spring accepts a null patterns array without throwing. This means
        // a missing `app.websocket.allowed-origins` property won't crash the app —
        // it will start with no patterns configured. Pinning this down so a future
        // change to fail-fast doesn't go unnoticed.
        ReflectionTestUtils.setField(config, "allowedOrigins", (Object) null);
        when(stompRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(endpointRegistration);

        org.assertj.core.api.Assertions.assertThatCode(
                        () -> config.registerStompEndpoints(stompRegistry))
                .doesNotThrowAnyException();
        verify(endpointRegistration).setAllowedOriginPatterns((String[]) null);
    }

    @Test
    void registerStompEndpoints_RegistersExactlyOneInterceptor() {
        when(stompRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.addInterceptors(any(HandshakeInterceptor[].class)))
                .thenReturn(endpointRegistration);

        config.registerStompEndpoints(stompRegistry);

        ArgumentCaptor<HandshakeInterceptor[]> captor =
                ArgumentCaptor.forClass(HandshakeInterceptor[].class);
        verify(endpointRegistration).addInterceptors(captor.capture());

        HandshakeInterceptor[] interceptors = captor.getValue();
        assertThat(interceptors)
                .as("registering the handshake interceptor twice would run session "
                  + "lookup twice per connect — keep it to exactly one")
                .hasSize(1);
        assertThat(interceptors[0]).isInstanceOf(SessionHandshakeInterceptor.class);
    }

    // ---- configureWebSocketTransport: decorador que trackea sesiones (WebSocketConfig$1$1) ----

    private WebSocketHandler captureDecorator() {
        WebSocketTransportRegistration registration = mock(WebSocketTransportRegistration.class);
        config.configureWebSocketTransport(registration);
        ArgumentCaptor<WebSocketHandlerDecoratorFactory> factoryCaptor =
                ArgumentCaptor.forClass(WebSocketHandlerDecoratorFactory.class);
        verify(registration).addDecoratorFactory(factoryCaptor.capture());
        return factoryCaptor.getValue().decorate(mock(WebSocketHandler.class));
    }

    @Test
    void transportDecorator_OnConnect_WithPlayerId_RegistersSession() throws Exception {
        WebSocketConfig.sessionRegistry.clear();
        WebSocketHandler decorator = captureDecorator();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-1");
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SessionHandshakeInterceptor.PLAYER_ID_ATTR, "player-1");
        when(session.getAttributes()).thenReturn(attrs);

        decorator.afterConnectionEstablished(session);

        assertThat(WebSocketConfig.sessionRegistry).containsEntry("ws-1", "player-1");
        WebSocketConfig.sessionRegistry.clear();
    }

    @Test
    void transportDecorator_OnConnect_WithoutPlayerId_DoesNotRegister() throws Exception {
        WebSocketConfig.sessionRegistry.clear();
        WebSocketHandler decorator = captureDecorator();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-2");
        when(session.getAttributes()).thenReturn(new HashMap<>()); // sin PLAYER_ID_ATTR

        decorator.afterConnectionEstablished(session);

        assertThat(WebSocketConfig.sessionRegistry).doesNotContainKey("ws-2");
    }

    @Test
    void transportDecorator_OnConnect_NullAttributes_DoesNotRegister() throws Exception {
        WebSocketConfig.sessionRegistry.clear();
        WebSocketHandler decorator = captureDecorator();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-3");
        when(session.getAttributes()).thenReturn(null);

        decorator.afterConnectionEstablished(session);

        assertThat(WebSocketConfig.sessionRegistry).doesNotContainKey("ws-3");
    }

    @Test
    void transportDecorator_OnClose_RemovesTrackedSession() throws Exception {
        WebSocketConfig.sessionRegistry.clear();
        WebSocketConfig.sessionRegistry.put("ws-4", "player-4");
        WebSocketHandler decorator = captureDecorator();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-4");

        decorator.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(WebSocketConfig.sessionRegistry).doesNotContainKey("ws-4");
    }

    @Test
    void transportDecorator_OnClose_UnknownSession_NoError() throws Exception {
        WebSocketConfig.sessionRegistry.clear();
        WebSocketHandler decorator = captureDecorator();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-unknown");

        org.assertj.core.api.Assertions.assertThatCode(
                () -> decorator.afterConnectionClosed(session, CloseStatus.NORMAL))
                .doesNotThrowAnyException();
    }

    @Test
    void transportDecorator_DelegatesToWrappedHandler() throws Exception {
        WebSocketConfig.sessionRegistry.clear();
        WebSocketTransportRegistration registration = mock(WebSocketTransportRegistration.class);
        config.configureWebSocketTransport(registration);
        ArgumentCaptor<WebSocketHandlerDecoratorFactory> factoryCaptor =
                ArgumentCaptor.forClass(WebSocketHandlerDecoratorFactory.class);
        verify(registration).addDecoratorFactory(factoryCaptor.capture());
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        WebSocketHandler decorator = factoryCaptor.getValue().decorate(delegate);

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("ws-5");
        when(session.getAttributes()).thenReturn(new HashMap<>());

        decorator.afterConnectionEstablished(session);
        decorator.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(delegate).afterConnectionEstablished(session);
        verify(delegate).afterConnectionClosed(session, CloseStatus.NORMAL);
        WebSocketConfig.sessionRegistry.clear();
    }
}
