package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.MatchStatus;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Periodically checks the WebSocket session registry for disconnected players
 * and marks the match accordingly, notifying the opponent.
 *
 * <p>Uses {@link WebSocketConfig#sessionRegistry} which is populated by the
 * WebSocket handler decorator on connection/disconnection.
 */
@Component
public class WebSocketDisconnectHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketDisconnectHandler.class);

    private final MatchRepository matchRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketDisconnectHandler(MatchRepository matchRepository,
                                       SimpMessagingTemplate messagingTemplate) {
        this.matchRepository = matchRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Periodically scans active matches and checks if either player has lost
     * their WebSocket connection (their session is no longer in the registry).
     */
    @Scheduled(fixedRate = 2000)
    public void checkDisconnectedMatches() {
        var liveStatuses = List.of(MatchStatus.ACTIVE, MatchStatus.SETUP);
        var activeMatches = matchRepository.findByStatusInOrderByCreatedAtDesc(liveStatuses);
        log.debug("Scanning {} matches for disconnects (registry size={})",
                activeMatches.size(), WebSocketConfig.sessionRegistry.size());
        for (Match match : activeMatches) {

            // Check if player one is still connected (skip bots)
            if (match.getPlayerOneId() != null
                    && !isBot(match.getPlayerOneId())
                    && match.getDisconnectedPlayerId() == null
                    && !isSessionAlive(match.getPlayerOneId())) {
                handleDisconnect(match, match.getPlayerOneId());
            }

            // Check if player two is still connected (skip bots)
            if (match.getPlayerTwoId() != null
                    && !isBot(match.getPlayerTwoId())
                    && match.getDisconnectedPlayerId() == null
                    && !isSessionAlive(match.getPlayerTwoId())) {
                handleDisconnect(match, match.getPlayerTwoId());
            }

            // If someone was marked disconnected and reconnected, clear the flag and notify
            if (match.getDisconnectedPlayerId() != null
                    && isSessionAlive(match.getDisconnectedPlayerId())) {
                String reconnectedId = match.getDisconnectedPlayerId();
                log.info("Player reconnected: {} in match {}", reconnectedId, match.getId());
                match.setDisconnectedPlayerId(null);
                match.setDisconnectedAt(null);
                matchRepository.save(match);

                String opponentId = reconnectedId.equals(match.getPlayerOneId())
                        ? match.getPlayerTwoId() : match.getPlayerOneId();
                if (opponentId != null) {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("type", "OPPONENT_RECONNECTED");
                    payload.put("matchId", match.getId());
                    Map<String, Object> inner = new LinkedHashMap<>();
                    inner.put("reconnectedPlayerId", reconnectedId);
                    payload.put("payload", inner);
                    messagingTemplate.convertAndSend(
                            "/topic/matches/" + match.getId() + "/state/" + opponentId,
                            (Object) payload
                    );
                }
            }
        }
    }

    private boolean isBot(String playerId) {
        return playerId != null && playerId.startsWith("bot-");
    }

    private boolean isSessionAlive(String playerId) {
        return WebSocketConfig.sessionRegistry.containsValue(playerId);
    }

    private void handleDisconnect(Match match, String playerId) {
        log.info("Detected disconnect via registry scan: player={} match={}", playerId, match.getId());
        match.setDisconnectedPlayerId(playerId);
        match.setDisconnectedAt(LocalDateTime.now());
        matchRepository.save(match);

        String opponentId = playerId.equals(match.getPlayerOneId())
                ? match.getPlayerTwoId() : match.getPlayerOneId();
        if (opponentId == null) return;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "OPPONENT_DISCONNECTED");
        payload.put("matchId", match.getId());
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("secondsRemaining", 120);
        inner.put("disconnectedPlayerId", playerId);
        payload.put("payload", inner);

        messagingTemplate.convertAndSend(
                "/topic/matches/" + match.getId() + "/state/" + opponentId,
                (Object) payload
        );
    }
}
