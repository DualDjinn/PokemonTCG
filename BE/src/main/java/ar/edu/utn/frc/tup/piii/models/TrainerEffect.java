package ar.edu.utn.frc.tup.piii.models;

/**
 * Strategy interface for Trainer card effects.
 *
 * <p>Each implementation encodes the game-rule text of one Trainer card
 * (Item, Supporter, Stadium or Pokémon Tool). The engine calls
 * {@link #apply(GameContext)} when a player plays the card from hand.
 *
 * <p>Analogous to {@link AttackEffect} for attack side-effects.
 */
@FunctionalInterface
public interface TrainerEffect {

    /**
     * Applies this trainer card's effect to the current game state.
     *
     * @param ctx mutable battle context — effects read and write state here
     */
    void apply(GameContext ctx);
}
