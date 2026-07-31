package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.MatchStatus;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Periodically checks for disconnected players and forfeits matches
 * after 2 minutes of inactivity.
 */
@Component
public class DisconnectScheduler {

    private static final Logger log = LoggerFactory.getLogger(DisconnectScheduler.class);
    private static final long DISCONNECT_TIMEOUT_SECONDS = 120;

    private final MatchRepository matchRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public DisconnectScheduler(MatchRepository matchRepository,
                                SimpMessagingTemplate messagingTemplate) {
        this.matchRepository = matchRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 1000)
    public void checkDisconnectedPlayers() {
        List<Match> activeMatches = matchRepository.findByStatusInOrderByCreatedAtDesc(List.of(MatchStatus.ACTIVE, MatchStatus.SETUP));
        LocalDateTime now = LocalDateTime.now();

        for (Match match : activeMatches) {
            String disconnectedId = match.getDisconnectedPlayerId();
            LocalDateTime disconnectedAt = match.getDisconnectedAt();
            if (disconnectedId == null || disconnectedAt == null) continue;

            long elapsedSeconds = java.time.Duration.between(disconnectedAt, now).getSeconds();
            long remaining = DISCONNECT_TIMEOUT_SECONDS - elapsedSeconds;

            if (remaining <= 0) {
                // Forfeit: the opponent wins
                String winnerId = disconnectedId.equals(match.getPlayerOneId())
                        ? match.getPlayerTwoId() : match.getPlayerOneId();
                match.setStatus(MatchStatus.FINISHED);
                match.setWinnerId(winnerId);
                match.setDisconnectedPlayerId(null);
                match.setDisconnectedAt(null);
                matchRepository.save(match);

                // Notify both players
                Map<String, Object> forfeitEvent = new LinkedHashMap<>();
                forfeitEvent.put("type", "OPPONENT_FORFEITED");
                forfeitEvent.put("matchId", match.getId());
                Map<String, Object> inner = new LinkedHashMap<>();
                inner.put("winnerId", winnerId);
                forfeitEvent.put("payload", inner);

                String p1Id = match.getPlayerOneId();
                String p2Id = match.getPlayerTwoId();
                if (p1Id != null) {
                    messagingTemplate.convertAndSend(
                            "/topic/matches/" + match.getId() + "/state/" + p1Id, (Object) forfeitEvent);
                }
                if (p2Id != null) {
                    messagingTemplate.convertAndSend(
                            "/topic/matches/" + match.getId() + "/state/" + p2Id, (Object) forfeitEvent);
                }
                log.info("Match {} forfeited: {} disconnected for >2min", match.getId(), disconnectedId);
            } else {
                // Send periodic countdown to the opponent
                String opponentId = disconnectedId.equals(match.getPlayerOneId())
                        ? match.getPlayerTwoId() : match.getPlayerOneId();
                if (opponentId != null) {
                    Map<String, Object> tickEvent = new LinkedHashMap<>();
                    tickEvent.put("type", "DISCONNECT_TICK");
                    tickEvent.put("matchId", match.getId());
                    Map<String, Object> tickInner = new LinkedHashMap<>();
                    tickInner.put("secondsRemaining", (int) remaining);
                    tickInner.put("disconnectedPlayerId", disconnectedId);
                    tickEvent.put("payload", tickInner);

                    messagingTemplate.convertAndSend(
                            "/topic/matches/" + match.getId() + "/state/" + opponentId, (Object) tickEvent);
                }
            }
        }
    }
}
