package ar.edu.utn.frc.tup.piii.models;

/**
 * Categorises a Pokémon ability according to how it is triggered during play.
 *
 * <p>These categories drive engine dispatch:
 * <ul>
 *     <li>{@link #PASSIVE} abilities are evaluated continuously by the
 *     {@code PassiveAbilityHandler} while the holder is in play.</li>
 *     <li>{@link #ACTIVATED} abilities are invoked manually by the player
 *     (not yet wired in v1).</li>
 *     <li>{@link #TRIGGERED} abilities fire on game events such as on-play
 *     or on-knockout (not yet wired in v1).</li>
 * </ul>
 */
public enum AbilityType {
    /** Always active while the holder is in play. */
    PASSIVE,
    /** The player activates it manually, usually once per turn. */
    ACTIVATED,
    /** Fires on a specific game event. */
    TRIGGERED
}
