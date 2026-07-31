package ar.edu.utn.frc.tup.piii.engine.events;

/**
 * Emitted when a Trainer card effect is successfully applied.
 *
 * <p>This event lets the <em>opponent</em> know which Trainer was played and
 * can trigger visual feedback on their client.
 *
 * @param cardId   the trainer card's identifier (e.g. {@code "xy1-122"})
 * @param cardName the human-readable name of the Trainer card
 */
public record TrainerEffectGameEvent(String cardId, String cardName) implements MatchGameEvent {
}
