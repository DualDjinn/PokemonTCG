package ar.edu.utn.frc.tup.piii.engine.bot;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Strategy interface for AI bots that pick a single action given the current battle context.
 *
 * <p>Implementations differ by intelligence level:
 * <ul>
 *   <li>{@code RandomBotStrategy} — random choice over legal actions.</li>
 *   <li>{@code GreedyBotStrategy} — picks the action with the highest immediate heuristic score.</li>
 *   <li>{@code MinimaxBotStrategy} — minimax with alpha-beta pruning at limited depth.</li>
 * </ul>
 *
 * <p>The {@link GameContext} is expected to be oriented from the bot's perspective
 * (the bot occupies the {@code attacker} side), so implementations can read
 * {@code context.getAttacker()} and friends without further translation.
 */
public interface BotStrategy {

    /**
     * Returns the action the bot wants to perform now.
     *
     * @param context current battle context, oriented from the bot's perspective
     * @return a legal {@link GameAction} the engine can submit
     */
    GameAction chooseAction(GameContext context);
}
