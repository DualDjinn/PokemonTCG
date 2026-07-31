package ar.edu.utn.frc.tup.piii.engine.turnstates;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.Objects;
import java.util.Set;

/**
 * State object for the draw phase at the start of a turn.
 *
 * <p>This is a transient phase: on {@link #enter(GameContext)} the current turn player draws the top
 * card of their deck, after which the turn manager immediately advances to the main phase. The
 * deck-out victory condition is evaluated by the turn manager before this phase is entered, so a
 * {@code null} draw here represents an inconsistent state and is rejected. No player action is
 * accepted while the turn rests in this phase.
 */
public final class DrawPhaseState implements TurnPhaseState {

    @Override
    public TurnPhase getPhase() {
        return TurnPhase.DRAW;
    }

    @Override
    public Set<GameActionType> getValidActions() {
        return Set.of();
    }

    @Override
    public void enter(GameContext context) {
        Objects.requireNonNull(context, "context must not be null");
        if (context.isFirstTurn()) {
            return;
        }
        Card drawnCard = context.attackerDrawCard();
        if (drawnCard == null) {
            throw new IllegalStateException("Current turn player should have drawn a card");
        }
    }

    @Override
    public void exit(GameContext context) {
        Objects.requireNonNull(context, "context must not be null");
    }

    @Override
    public void handle(GameAction action, TurnPhaseActionContext context) {
        throw new IllegalArgumentException("No actions can be submitted during the draw phase");
    }
}
