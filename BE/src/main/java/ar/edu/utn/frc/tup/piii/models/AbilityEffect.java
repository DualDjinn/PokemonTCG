package ar.edu.utn.frc.tup.piii.models;

/**
 * Strategy interface for Pokémon ability effects.
 *
 * <p>Each implementation encodes the behaviour of one ability attached to a
 * {@link PokemonCard}. The engine's passive-ability pipeline calls
 * {@link #apply(GameContext, InPlayPokemon, Ability)} for every
 * {@link AbilityType#PASSIVE} ability on the relevant Pokémon while that
 * Pokémon is in play.
 *
 * <p>TRIGGERED abilities receive additional context about the event that fired
 * them via the overloaded {@link #apply(GameContext, InPlayPokemon, Ability, TriggerContext)}
 * method. PASSIVE/ACTIVATED registrations don't need to override it.
 *
 * <p>Implementations must be side-effect free when the holder's
 * {@link InPlayPokemon#isAbilitiesDisabled()} flag is set: the engine gates
 * that case before dispatch so effects do not need to re-check it.
 */
@FunctionalInterface
public interface AbilityEffect {

    /**
     * Applies this ability to the current game state.
     *
     * @param ctx    mutable battle context — effects read and write state here
     * @param holder the in-play Pokémon that owns the ability
     * @param ability the {@link Ability} record carrying the effect metadata
     */
    void apply(GameContext ctx, InPlayPokemon holder, Ability ability);

    /**
     * Applies this ability with additional trigger context (for TRIGGERED abilities).
     * Falls back to the standard {@link #apply} so PASSIVE/ACTIVATED lambdas don't need changes.
     *
     * @param ctx     mutable battle context
     * @param holder  the in-play Pokémon that owns the ability
     * @param ability the {@link Ability} record carrying the effect metadata
     * @param trigger event-specific context (e.g. who the attacker is)
     */
    default void apply(GameContext ctx, InPlayPokemon holder, Ability ability, TriggerContext trigger) {
        apply(ctx, holder, ability);
    }

    /**
     * Extra context supplied to TRIGGERED ability effects.
     *
     * @param attacker the Pokémon that initiated the attack (may be null for non-attack triggers)
     */
    record TriggerContext(InPlayPokemon attacker) { }
}
