package ar.edu.utn.frc.tup.piii.engine.events;

/**
 * Emitted when an attack effect applies or stacks a status condition on a Pokémon.
 *
 * @param pokemonName name of the affected Pokémon (for FE display)
 * @param condition   status condition name matching {@link ar.edu.utn.frc.tup.piii.models.StatusCondition}
 *                    (e.g. {@code "POISONED"})
 */
public record StatusAppliedGameEvent(String pokemonName, String condition) implements MatchGameEvent {
}
