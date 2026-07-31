package ar.edu.utn.frc.tup.piii.engine.events;

/**
 * Emitted when a player activates a Pokémon's "Once during your turn" ability.
 *
 * @param cardId      the Pokémon card identifier (e.g. {@code "xy1-26"})
 * @param cardName    the Pokémon's name (e.g. {@code "Delphox"})
 * @param abilityName the printed ability name (e.g. {@code "Mystical Fire"})
 */
public record AbilityActivatedGameEvent(String cardId, String cardName, String abilityName)
        implements MatchGameEvent {
}
