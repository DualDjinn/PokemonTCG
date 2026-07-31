package ar.edu.utn.frc.tup.piii.engine.events;

/**
 * Emitted each time a coin is flipped during attack or status resolution.
 *
 * @param heads  {@code true} if the flip landed heads
 * @param reason human-readable label describing why the coin was flipped
 *               (e.g. {@code "burn"}, {@code "sleep"}, {@code "attack_effect"})
 */
public record CoinFlipGameEvent(boolean heads, String reason) implements MatchGameEvent {
}
