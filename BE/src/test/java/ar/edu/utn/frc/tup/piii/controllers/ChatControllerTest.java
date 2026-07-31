package ar.edu.utn.frc.tup.piii.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import ar.edu.utn.frc.tup.piii.configs.SessionHandshakeInterceptor;
import ar.edu.utn.frc.tup.piii.models.ChatMessage;
import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private static final String MATCH_ID = "match-1";
    private static final String PLAYER_ID = "player-1";
    private static final String PLAYER_NAME = "Ash";

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    @Captor
    private ArgumentCaptor<ChatMessage> messageCaptor;

    private ChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatController(messagingTemplate, matchRepository);
    }

    private Map<String, Object> authenticatedSession() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SessionHandshakeInterceptor.PLAYER_ID_ATTR, PLAYER_ID);
        attrs.put(SessionHandshakeInterceptor.PLAYER_NAME_ATTR, PLAYER_NAME);
        return attrs;
    }

    private Map<String, Object> sessionWithIdOnly() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SessionHandshakeInterceptor.PLAYER_ID_ATTR, PLAYER_ID);
        return attrs;
    }

    private Match matchWithPlayer(String playerId) {
        Match match = new Match();
        match.setId(MATCH_ID);
        match.setPlayerOneId(playerId);
        return match;
    }

    private Match matchWithBothPlayers(String p1, String p2) {
        Match match = new Match();
        match.setId(MATCH_ID);
        match.setPlayerOneId(p1);
        match.setPlayerTwoId(p2);
        return match;
    }

    // --- auth enforcement ---

    @Test
    void sendMessage_NoSessionAttributes_DropsMessage() {
        when(headerAccessor.getSessionAttributes()).thenReturn(null);
        ChatMessage msg = new ChatMessage(MATCH_ID, "fake", "fake", "hi", null);

        controller.sendMessage(MATCH_ID, msg, headerAccessor);

        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/matches/" + MATCH_ID + "/chat"),
                org.mockito.ArgumentMatchers.any(ChatMessage.class));
    }

    @Test
    void sendMessage_NoPlayerId_DropsMessage() {
        Map<String, Object> attrs = new HashMap<>();
        when(headerAccessor.getSessionAttributes()).thenReturn(attrs);
        ChatMessage msg = new ChatMessage(MATCH_ID, "fake", "fake", "hi", null);

        controller.sendMessage(MATCH_ID, msg, headerAccessor);

        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/matches/" + MATCH_ID + "/chat"),
                org.mockito.ArgumentMatchers.any(ChatMessage.class));
    }

    @Test
    void sendMessage_EmptySessionAttributes_DropsMessage() {
        when(headerAccessor.getSessionAttributes()).thenReturn(new HashMap<>());
        ChatMessage msg = new ChatMessage(MATCH_ID, PLAYER_ID, PLAYER_NAME, "hi", null);

        controller.sendMessage(MATCH_ID, msg, headerAccessor);

        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/matches/" + MATCH_ID + "/chat"),
                org.mockito.ArgumentMatchers.any(ChatMessage.class));
    }

    // --- match membership ---

    @Test
    void sendMessage_MatchNotFound_DropsMessage() {
        when(headerAccessor.getSessionAttributes()).thenReturn(authenticatedSession());
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.empty());
        ChatMessage msg = new ChatMessage(MATCH_ID, PLAYER_ID, PLAYER_NAME, "hi", null);

        controller.sendMessage(MATCH_ID, msg, headerAccessor);

        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/matches/" + MATCH_ID + "/chat"),
                org.mockito.ArgumentMatchers.any(ChatMessage.class));
    }

    @Test
    void sendMessage_PlayerNotInMatch_DropsMessage() {
        when(headerAccessor.getSessionAttributes()).thenReturn(authenticatedSession());
        Match match = matchWithPlayer("other-player");
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        ChatMessage msg = new ChatMessage(MATCH_ID, PLAYER_ID, PLAYER_NAME, "hi", null);

        controller.sendMessage(MATCH_ID, msg, headerAccessor);

        verify(messagingTemplate, never()).convertAndSend(
                eq("/topic/matches/" + MATCH_ID + "/chat"),
                org.mockito.ArgumentMatchers.any(ChatMessage.class));
    }

    // --- happy path ---

    @Test
    void sendMessage_Authenticated_BroadcastsMessage() {
        when(headerAccessor.getSessionAttributes()).thenReturn(authenticatedSession());
        Match match = matchWithPlayer(PLAYER_ID);
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

        ChatMessage clientMsg = new ChatMessage(
                MATCH_ID, PLAYER_ID, PLAYER_NAME, "hello", null);

        controller.sendMessage(MATCH_ID, clientMsg, headerAccessor);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/matches/" + MATCH_ID + "/chat"),
                messageCaptor.capture());

        ChatMessage sent = messageCaptor.getValue();
        assertEquals(PLAYER_ID, sent.senderId());
        assertEquals(PLAYER_NAME, sent.senderName());
        assertEquals("hello", sent.content());
        assertEquals(MATCH_ID, sent.matchId());
        assertNotNull(sent.timestamp());
    }

    @Test
    void sendMessage_ServerOverridesSenderName_ButKeepsClientSenderId() {
        when(headerAccessor.getSessionAttributes()).thenReturn(authenticatedSession());
        Match match = matchWithPlayer(PLAYER_ID);
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

        ChatMessage clientMsg = new ChatMessage(
                MATCH_ID, "client-supplied-id", "attacker-name", "msg", null);

        controller.sendMessage(MATCH_ID, clientMsg, headerAccessor);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/matches/" + MATCH_ID + "/chat"),
                messageCaptor.capture());

        ChatMessage sent = messageCaptor.getValue();
        assertEquals("client-supplied-id", sent.senderId(),
                "senderId comes from client message");
        assertEquals(PLAYER_NAME, sent.senderName(),
                "senderName is overridden by server session");
    }

    // --- edge case: playerName null fallback ---

    @Test
    void sendMessage_NullPlayerName_FallsBackToPlayerId() {
        when(headerAccessor.getSessionAttributes()).thenReturn(sessionWithIdOnly());
        Match match = matchWithPlayer(PLAYER_ID);
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

        ChatMessage clientMsg = new ChatMessage(
                MATCH_ID, PLAYER_ID, "whatever", "hi", null);

        controller.sendMessage(MATCH_ID, clientMsg, headerAccessor);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/matches/" + MATCH_ID + "/chat"),
                messageCaptor.capture());

        ChatMessage sent = messageCaptor.getValue();
        assertEquals(PLAYER_ID, sent.senderName(),
                "When playerName is null, senderName should fallback to playerId");
    }

    // --- player two can also send messages ---

    @Test
    void sendMessage_PlayerTwo_BroadcastsSuccessfully() {
        String playerTwoId = "player-2";
        String playerTwoName = "Gary";
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SessionHandshakeInterceptor.PLAYER_ID_ATTR, playerTwoId);
        attrs.put(SessionHandshakeInterceptor.PLAYER_NAME_ATTR, playerTwoName);
        when(headerAccessor.getSessionAttributes()).thenReturn(attrs);

        Match match = matchWithBothPlayers(PLAYER_ID, playerTwoId);
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

        ChatMessage clientMsg = new ChatMessage(
                MATCH_ID, playerTwoId, playerTwoName, "gg", null);

        controller.sendMessage(MATCH_ID, clientMsg, headerAccessor);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/matches/" + MATCH_ID + "/chat"),
                messageCaptor.capture());

        ChatMessage sent = messageCaptor.getValue();
        assertEquals(playerTwoId, sent.senderId());
        assertEquals(playerTwoName, sent.senderName());
        assertEquals("gg", sent.content());
    }

    // --- destination uses matchId from path, not from message body ---

    @Test
    void sendMessage_UsesPathMatchId_ForDestination() {
        when(headerAccessor.getSessionAttributes()).thenReturn(authenticatedSession());
        Match match = matchWithPlayer(PLAYER_ID);
        when(matchRepository.findById("path-match-id")).thenReturn(Optional.of(match));

        ChatMessage clientMsg = new ChatMessage(
                "body-match-id", PLAYER_ID, PLAYER_NAME, "test", null);

        controller.sendMessage("path-match-id", clientMsg, headerAccessor);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/matches/path-match-id/chat"),
                messageCaptor.capture());
    }
}
