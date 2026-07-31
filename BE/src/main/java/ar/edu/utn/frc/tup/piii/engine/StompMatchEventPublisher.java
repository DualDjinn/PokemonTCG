package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.engine.events.MatchGameEvent;
import ar.edu.utn.frc.tup.piii.services.CardBackService;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * STOMP/WebSocket implementation of {@link MatchEventPublisher}.
 *
 * <p>This concrete observer sends each player's filtered view to their specific
 * topic destination via {@link SimpMessagingTemplate#convertAndSend}.
 * The destination pattern is "/topic/matches/{matchId}/state/{playerId}".
 * It enforces the security rule that a player must never receive the opponent's
 * hand: the payloads provided to this publisher must already be filtered.
 *
 * <p>Design pattern: Observer — concrete observer (transport). The GameEngine
 * publishes events through {@link MatchEventPublisher} and this class delivers
 * them to connected clients using Spring's messaging template.
 */
@Component
@Profile("!test")
public class StompMatchEventPublisher implements MatchEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final CardBackService cardBackService;

    public StompMatchEventPublisher(SimpMessagingTemplate messagingTemplate,
                                    CardBackService cardBackService) {
        this.messagingTemplate = messagingTemplate;
        this.cardBackService = cardBackService;
    }

    @Override
    public void onMatchUpdated(MatchUpdatedEvent event) {
        if (event == null) {
            return;
        }

        String matchId = event.matchId();
        MatchStateDTO p1View = event.player1View();
        MatchStateDTO p2View = event.player2View();

        enrichCardBackUrl(p1View);
        enrichCardBackUrl(p2View);

        String p1PlayerId = p1View == null ? null : p1View.getRequestedPlayerId();
        String p2PlayerId = p2View == null ? null : p2View.getRequestedPlayerId();

        if (matchId != null && p1PlayerId != null) {
            messagingTemplate.convertAndSend(
                    "/topic/matches/" + matchId + "/state/" + p1PlayerId,
                    p1View
            );
        }
        if (matchId != null && p2PlayerId != null) {
            messagingTemplate.convertAndSend(
                    "/topic/matches/" + matchId + "/state/" + p2PlayerId,
                    p2View
            );
        }
    }

    private void enrichCardBackUrl(MatchStateDTO view) {
        if (view == null) {
            return;
        }
        if (view.getSelf() != null) {
            String url = cardBackService.getActiveCardBackUrl(view.getSelf().getPlayerId());
            view.getSelf().setCardBackUrl(url);
        }
        if (view.getOpponent() != null) {
            String url = cardBackService.getActiveCardBackUrl(view.getOpponent().getPlayerId());
            view.getOpponent().setCardBackUrl(url);
        }
    }

    @Override
    public void onMulligan(MulliganEvent event) {
        if (event == null) {
            return;
        }

        String matchId = event.matchId();
        if (matchId != null) {
            messagingTemplate.convertAndSend(
                    "/topic/matches/" + matchId + "/mulligan",
                    event
            );
        }
    }

    @Override
    public void onMatchGameEvent(String matchId, String p1Id, String p2Id, MatchGameEvent event) {
        if (matchId == null || event == null) {
            return;
        }

        // Determine type tag from the concrete record class name
        String type = event.getClass().getSimpleName()
                .replace("GameEvent", "")
                .replaceAll("([A-Z])", "_$1")
                .replaceFirst("^_", "")
                .toUpperCase();

        GameEventEnvelope envelope = new GameEventEnvelope(type, event);

        if (p1Id != null) {
            messagingTemplate.convertAndSend(
                    "/topic/matches/" + matchId + "/events/" + p1Id, (Object) envelope);
        }
        if (p2Id != null) {
            messagingTemplate.convertAndSend(
                    "/topic/matches/" + matchId + "/events/" + p2Id, (Object) envelope);
        }
    }
}
