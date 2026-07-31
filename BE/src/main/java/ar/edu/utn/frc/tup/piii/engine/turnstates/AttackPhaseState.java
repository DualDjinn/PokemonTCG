package ar.edu.utn.frc.tup.piii.engine.turnstates;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.Objects;
import java.util.Set;

/**
 * State object for the player's attack phase.
 *
 * <p>Once the turn has reached this phase the player either declares an attack or ends the turn.
 * Re-entering the attack phase is rejected because the transition only happens once per turn.
 */
public final class AttackPhaseState implements TurnPhaseState {

    private static final String ADVANCE_FROM_ATTACK_MESSAGE =
            "Attack phase has already started for the current turn";
    private static final Set<GameActionType> VALID_ACTIONS = Set.of(
            GameActionType.ATTACK,
            GameActionType.END_TURN
    );

    @Override
    public TurnPhase getPhase() {
        return TurnPhase.ATTACK;
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
        if (actionType == GameActionType.ADVANCE_TO_ATTACK) {
            throw new IllegalArgumentException(ADVANCE_FROM_ATTACK_MESSAGE);
        }
        if (actionType == GameActionType.ATTACK || actionType == GameActionType.EXECUTE_ATTACK) {
            context.resolveAttack(action);
            return;
        }
        if (actionType == GameActionType.END_TURN) {
            context.endTurn();
            return;
        }
        throw new IllegalArgumentException("Unsupported action type: " + actionType);
    }
}
