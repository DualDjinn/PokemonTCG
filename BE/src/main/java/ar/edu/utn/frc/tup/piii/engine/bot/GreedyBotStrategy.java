package ar.edu.utn.frc.tup.piii.engine.bot;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import java.util.List;
import java.util.Random;

/**
 * Medium-level bot. Picks the legal action with the highest immediate heuristic
 * score, with random tie-breaking.
 *
 * <p>Equivalent to a 1-ply minimax: looks one move ahead and chooses whatever
 * looks best right now according to {@link GameStateEvaluator}.
 */
public final class GreedyBotStrategy implements BotStrategy {

    private final Random random;

    public GreedyBotStrategy() {
        this(new Random());
    }

    public GreedyBotStrategy(Random random) {
        this.random = random != null ? random : new Random();
    }

    @Override
    public GameAction chooseAction(GameContext context) {
        List<GameAction> actions = LegalActionGenerator.generate(context);
        if (actions.isEmpty()) {
            return GameAction.endTurn();
        }

        GameAction best = actions.get(0);
        int bestScore = GameStateEvaluator.scoreAction(context, best);
        int ties = 1;

        for (int i = 1; i < actions.size(); i++) {
            GameAction candidate = actions.get(i);
            int score = GameStateEvaluator.scoreAction(context, candidate);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
                ties = 1;
            } else if (score == bestScore) {
                ties++;
                // Reservoir sampling: pick the new candidate with probability 1/ties
                if (random.nextInt(ties) == 0) {
                    best = candidate;
                }
            }
        }

        return best;
    }
}
