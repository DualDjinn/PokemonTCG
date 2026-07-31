package ar.edu.utn.frc.tup.piii.engine.turnstates;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.Set;

/**
 * State contract for one turn phase in the battle flow (GoF <em>State</em> pattern).
 *
 * <p>Each implementation represents a single {@link TurnPhase}. The {@link #enter(GameContext)} and
 * {@link #exit(GameContext)} hooks run the phase's own entry/exit effects against the current
 * {@link GameContext}, {@link #getValidActions()} advertises which actions the phase accepts, and
 * {@link #handle(GameAction, TurnPhaseActionContext)} routes a submitted action while delegating the
 * actual engine mutations back to the provided {@link TurnPhaseActionContext}.
 */
public interface TurnPhaseState {

    /**
     * Returns the phase represented by this state.
     *
     * @return represented turn phase
     */
    TurnPhase getPhase();

    /**
     * Returns the action types a player may legally submit while this phase is active.
     *
     * @return immutable set of accepted action types (possibly empty for transient phases)
     */
    Set<GameActionType> getValidActions();

    /**
     * Runs the phase's entry effect against the supplied context.
     *
     * @param context current game context; must not be {@code null}
     */
    void enter(GameContext context);

    /**
     * Runs the phase's exit effect against the supplied context.
     *
     * @param context current game context; must not be {@code null}
     */
    void exit(GameContext context);

    /**
     * Handles one incoming game action for this phase.
     *
     * @param action submitted action
     * @param context phase-action bridge back to the turn manager
     */
    void handle(GameAction action, TurnPhaseActionContext context);
}
