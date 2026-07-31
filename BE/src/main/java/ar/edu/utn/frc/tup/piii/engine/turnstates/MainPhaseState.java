package ar.edu.utn.frc.tup.piii.engine.turnstates;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.Objects;
import java.util.Set;

/**
 * State object for the player's main phase.
 *
 * <p>During the main phase the player may build their board (play Basic Pokémon, evolve, attach an
 * energy, play Item and Supporter cards) and finally declare the transition to the attack phase.
 * Attack declarations and turn endings are rejected here because they belong to the attack phase.
 */
public final class MainPhaseState implements TurnPhaseState {

    private static final String ATTACK_FROM_MAIN_MESSAGE =
            "Attack cannot be declared before entering the attack phase";
    private static final Set<GameActionType> VALID_ACTIONS = Set.of(
            GameActionType.PLAY_BASIC,
            GameActionType.EVOLVE,
            GameActionType.ATTACH_ENERGY,
            GameActionType.RETREAT,
            GameActionType.PLAY_ITEM,
            GameActionType.PLAY_SUPPORTER,
            GameActionType.PLAY_STADIUM,
            GameActionType.ATTACH_TOOL,
            GameActionType.ACTIVATE_ABILITY,
            GameActionType.ADVANCE_TO_ATTACK
    );

    @Override
    public TurnPhase getPhase() {
        return TurnPhase.MAIN;
    }

    @Override
    public Set<GameActionType> getValidActions() {
        return VALID_ACTIONS;
    }

    @Override
    public void enter(GameContext context) {
        Objects.requireNonNull(context, "context must not be null");
    }

    @Override
    public void exit(GameContext context) {
        Objects.requireNonNull(context, "context must not be null");
    }

    @Override
    public void handle(GameAction action, TurnPhaseActionContext context) {
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(context, "context must not be null");

        GameActionType actionType = action.getType();
        switch (actionType) {
            case PLAY_BASIC -> context.playBasicPokemon(action);
            case EVOLVE -> context.evolvePokemon(action);
            case ATTACH_ENERGY -> context.attachEnergy(action);
            case RETREAT -> context.retreat(action);
            case PLAY_ITEM -> context.playItem(action);
            case PLAY_SUPPORTER -> context.playSupporter(action);
            case PLAY_STADIUM -> context.playStadium(action);
            case ATTACH_TOOL -> context.attachTool(action);
            case ACTIVATE_ABILITY -> context.activateAbility(action);
            case ADVANCE_TO_ATTACK -> context.advanceToAttack();
            case ATTACK -> throw new IllegalArgumentException(ATTACK_FROM_MAIN_MESSAGE);
            case EXECUTE_ATTACK -> {
                context.advanceToAttack();
                context.resolveAttack(action);
            }
            case END_TURN -> {
                context.advanceToAttack();
                context.endTurn();
            }
            default -> throw new IllegalArgumentException("Unsupported action type: " + actionType);
        }
    }
}
