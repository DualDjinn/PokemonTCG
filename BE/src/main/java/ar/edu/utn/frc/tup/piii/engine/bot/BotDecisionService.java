package ar.edu.utn.frc.tup.piii.engine.bot;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Spring service that turns a {@link BotDifficulty} into a concrete {@link GameAction}.
 *
 * <p>Holds one {@link BotStrategy} instance per difficulty so the engine can simply
 * ask for the next action without knowing which algorithm is behind it.
 */
@Service
public class BotDecisionService {

    private final Map<BotDifficulty, BotStrategy> strategies;

    public BotDecisionService() {
        this.strategies = new EnumMap<>(BotDifficulty.class);
        this.strategies.put(BotDifficulty.EASY, new RandomBotStrategy());
        this.strategies.put(BotDifficulty.MEDIUM, new GreedyBotStrategy());
        this.strategies.put(BotDifficulty.HARD, new MinimaxBotStrategy());
    }

    /**
     * Chooses the next action for the given difficulty.
     *
     * @param difficulty bot difficulty (never null)
     * @param context    current battle context oriented from the bot's perspective
     * @return action the bot wants to perform
     */
    public GameAction chooseAction(BotDifficulty difficulty, GameContext context) {
        BotStrategy strategy = strategies.getOrDefault(difficulty, strategies.get(BotDifficulty.EASY));
        return strategy.chooseAction(context);
    }
}
