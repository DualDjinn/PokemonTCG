package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import java.util.Objects;

/**
 * Immutable event describing that a match has been updated.
 *
 * <p>This record is the payload that the engine emits whenever a match state
 * mutates. It contains the match identifier and two player-specific views.
 * Each view is expected to be filtered so the opponent's hand is hidden
 * (represented as an empty list or null) prior to constructing this event.
 *
 * <p>Design pattern: Observer — this object is the immutable event sent from the
 * subject (GameEngine) to observers (transport publishers) via the
 * {@link MatchEventPublisher} contract.
 */
public record MatchUpdatedEvent(
        String matchId,
        MatchStateDTO player1View,
        MatchStateDTO player2View
) {
    public MatchUpdatedEvent {
        Objects.requireNonNull(matchId, "matchId must not be null");
        Objects.requireNonNull(player1View, "player1View must not be null");
        Objects.requireNonNull(player2View, "player2View must not be null");
    }
}
