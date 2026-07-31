package ar.edu.utn.frc.tup.piii.models.effects;

import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Strategy interface for trainer card effects.
 *
 * <p>Each implementation encodes the side effect of one trainer card (Item, Supporter,
 * Stadium, or Pokémon Tool). New effects are registered in {@link TrainerEffectRegistry}
 * without modifying existing engine code (Open/Closed Principle).
 */
@FunctionalInterface
public interface TrainerEffect {

    /**
     * Applies this trainer effect to the current game state.
     *
     * @param ctx mutable battle context — effects read and write state here
     */
    void apply(GameContext ctx);
}
