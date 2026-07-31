package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.configs.SessionHandshakeInterceptor;
import ar.edu.utn.frc.tup.piii.models.ChatMessage;
import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MatchRepository matchRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          MatchRepository matchRepository) {
        this.messagingTemplate = messagingTemplate;
        this.matchRepository = matchRepository;
    }

    @MessageMapping("/chat/{matchId}/send")
    public void sendMessage(
            @DestinationVariable String matchId,
            @Payload ChatMessage message,
            SimpMessageHeaderAccessor headerAccessor) {

        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        String playerName = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_NAME_ATTR);

        if (playerId == null) {
            return;
        }

        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null || !match.containsPlayer(playerId)) {
            return;
        }

        // El nombre mostrado lo fija el servidor desde la sesión (no se puede suplantar);
        // el senderId se conserva del cliente como eco/correlación (diseño de dev).
        ChatMessage response = new ChatMessage(
                matchId,
                message.senderId(),
                playerName != null ? playerName : playerId,
                message.content(),
                LocalDateTime.now()
        );
        messagingTemplate.convertAndSend(
                "/topic/matches/" + matchId + "/chat",
                response
        );
    }
}
