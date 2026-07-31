package ar.edu.utn.frc.tup.piii.engine.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RandomBotStrategyTest {

    @Test
    void mainPhaseFallsBackToAdvanceWhenNothingElseIsLegal() {
        GameContext context = new GameContext();
        context.setCurrentPhase(TurnPhase.MAIN);
        // empty hand + no active = the only legal action is ADVANCE_TO_ATTACK

        RandomBotStrategy strategy = new RandomBotStrategy(new Random(42));
        GameAction action = strategy.chooseAction(context);

        assertNotNull(action);
        assertEquals(GameActionType.ADVANCE_TO_ATTACK, action.getType());
    }

    @Test
    void attackPhaseFallsBackToEndTurnWhenNoAttackPossible() {
        GameContext context = new GameContext();
        context.setCurrentPhase(TurnPhase.ATTACK);
        // no active pokemon → only END_TURN is legal

        RandomBotStrategy strategy = new RandomBotStrategy(new Random(0));
        GameAction action = strategy.chooseAction(context);

        assertEquals(GameActionType.END_TURN, action.getType());
    }

    @Test
    void betweenTurnsHasNoLegalActionsAndReturnsEndTurn() {
        GameContext context = new GameContext();
        context.setCurrentPhase(TurnPhase.BETWEEN_TURNS);

        RandomBotStrategy strategy = new RandomBotStrategy();
        GameAction action = strategy.chooseAction(context);

        // Defensive fallback - we never crash even when no actions are available
        assertNotNull(action);
        assertTrue(action.getType() == GameActionType.END_TURN
                || action.getType() == GameActionType.ADVANCE_TO_ATTACK);
    }
}
