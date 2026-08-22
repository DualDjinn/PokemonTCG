package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.MatchStatus;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Marks a player as disconnected in any live match they belong to and notifies
 * the opponent immediately.
 *
 * <p>Used on WebSocket session close so the disconnection is detected right away,
 * regardless of how the player left (navigation to the lobby, browser close, F5,
 * etc.). The periodic {@link WebSocketDisconnectHandler} scan remains as a safety
 * net for missed cases.
 */
@Component
public class MatchDisconnectService {

    private static final Logger log = LoggerFactory.getLogger(MatchDisconnectService.class);

    private final MatchRepository matchRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MatchDisconnectService(MatchRepository matchRepository,
                                  SimpMessagingTemplate messagingTemplate) {
        this.matchRepository = matchRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Marks the player as disconnected in every active match they are a member of
     * and notifies the opponent with {@code OPPONENT_DISCONNECTED}.
     *
     * @param playerId identifier of the player whose session closed
     */
    public void markDisconnected(String playerId) {
        if (playerId == null || playerId.startsWith("bot-")) {
            return;
        }
        List<MatchStatus> live = List.of(MatchStatus.ACTIVE, MatchStatus.SETUP);
        List<Match> asOne = matchRepository.findByPlayerOneIdAndStatusIn(playerId, live);
        List<Match> asTwo = matchRepository.findByPlayerTwoIdAndStatusIn(playerId, live);
        for (Match match : asOne) {
            handle(match, playerId);
        }
        for (Match match : asTwo) {
            handle(match, playerId);
        }
    }

    private void handle(Match match, String playerId) {
        if (match.getDisconnectedPlayerId() != null) {
            return;
        }
        match.setDisconnectedPlayerId(playerId);
        match.setDisconnectedAt(LocalDateTime.now());
        matchRepository.save(match);

        String opponentId = playerId.equals(match.getPlayerOneId())
                ? match.getPlayerTwoId() : match.getPlayerOneId();
        if (opponentId == null) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "OPPONENT_DISCONNECTED");
        payload.put("matchId", match.getId());
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("secondsRemaining", 120);
        inner.put("disconnectedPlayerId", playerId);
        payload.put("payload", inner);
        messagingTemplate.convertAndSend(
                "/topic/matches/" + match.getId() + "/state/" + opponentId,
                (Object) payload);

        log.info("Match {} player {} disconnected (immediate)", match.getId(), playerId);
    }
}
