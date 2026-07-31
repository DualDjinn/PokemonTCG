package ar.edu.utn.frc.tup.piii.engine.bot;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import java.util.List;
import java.util.Random;

/**
 * Easy-level bot. Picks a legal action uniformly at random.
 *
 * <p>This intentionally ignores all heuristics; the goal is to provide a baseline
 * that newer players can beat consistently.
 */
public final class RandomBotStrategy implements BotStrategy {

    private final Random random;

    public RandomBotStrategy() {
        this(new Random());
    }

    /**
     * @param random injectable randomness source (helpful for deterministic tests)
     */
    public RandomBotStrategy(Random random) {
        this.random = random != null ? random : new Random();
    }

    @Override
    public GameAction chooseAction(GameContext context) {
        List<GameAction> actions = LegalActionGenerator.generate(context);
        if (actions.isEmpty()) {
            // No legal actions: fall back to ending the turn.
            return GameAction.endTurn();
        }
        return actions.get(random.nextInt(actions.size()));
    }
}
