package ar.edu.utn.frc.tup.piii.engine.events;

/**
 * Marker interface for granular in-game events published to the FE via STOMP.
 *
 * <p>Each concrete event type is a record carrying only what the FE needs to
 * render a visual effect: coin flips, status conditions applied, or trainer
 * effects resolved. The match state (HP, hand, etc.) is always delivered
 * separately via {@link ar.edu.utn.frc.tup.piii.engine.MatchUpdatedEvent}.
 *
 * <p>Design pattern: Observer — these are the event payloads.
 */
public sealed interface MatchGameEvent
        permits AbilityActivatedGameEvent, CoinFlipGameEvent, StatusAppliedGameEvent, TrainerEffectGameEvent {
}
