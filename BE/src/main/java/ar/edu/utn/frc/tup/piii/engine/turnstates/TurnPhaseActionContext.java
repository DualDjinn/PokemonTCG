package ar.edu.utn.frc.tup.piii.engine.turnstates;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;

/**
 * Operations exposed by {@code TurnManager} to phase-state objects.
 *
 * <p>The state implementations stay focused on phase-specific routing while the concrete engine
 * mutations remain centralized in the turn manager.
 */
public interface TurnPhaseActionContext {

    /**
     * Puts a Basic Pokémon from the current player's hand onto the bench.
     *
     * @param action play-basic command to execute
     */
    void playBasicPokemon(GameAction action);

    /**
     * Evolves an in-play Pokémon using a card from the current player's hand.
     *
     * @param action evolve command to execute
     */
    void evolvePokemon(GameAction action);

    /**
     * Attaches an energy card from the current player's hand to an in-play Pokémon.
     *
     * @param action attach-energy command to execute
     */
    void attachEnergy(GameAction action);

    /**
     * Plays an Item card from the current player's hand.
     *
     * @param action play-item command to execute
     */
    void playItem(GameAction action);

    /**
     * Plays a Supporter card from the current player's hand.
     *
     * @param action play-supporter command to execute
     */
    void playSupporter(GameAction action);

    /**
     * Plays a Stadium card from the current player's hand.
     *
     * @param action play-stadium command to execute
     */
    void playStadium(GameAction action);

    /**
     * Attaches a Pokémon Tool card from the current player's hand to a target Pokémon.
     *
     * @param action attach-tool command to execute
     */
    void attachTool(GameAction action);

    /**
     * Retreats the active Pokémon to the bench, discarding energy equal to its retreat cost,
     * and marks the player for pending active selection.
     *
     * @param action retreat command to execute
     */
    void retreat(GameAction action);

    /**
     * Transitions the current turn from the main phase into the attack phase.
     */
    void advanceToAttack();

    /**
     * Resolves an attack declaration inside the attack phase.
     *
     * @param action attack command to execute
     */
    void resolveAttack(GameAction action);

    /**
     * Finishes the current turn without declaring an attack.
     */
    void endTurn();

    /**
     * Activates a Pokémon's "Once during your turn" ability.
     *
     * @param action activate-ability command; {@code cardId} is the instance ID of the
     *               Pokémon holding the ability, {@code attackName} carries the ability name
     */
    void activateAbility(GameAction action);
}
