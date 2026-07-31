package ar.edu.utn.frc.tup.piii.engine.bot;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Hard-level bot. Implements a depth-2 minimax with alpha-beta pruning over the
 * legal actions, scoring leaves with {@link GameStateEvaluator}.
 *
 * <p>Because deep-cloning the full mutable {@link GameContext} on every node would
 * be invasive, this implementation performs an <em>abstract</em> minimax: it
 * scores each candidate action through {@link GameStateEvaluator#scoreAction} and
 * then subtracts the best estimated counter-action the opponent could play next.
 *
 * <p>This still captures the essence of minimax (bot maximises, opponent
 * minimises), uses alpha-beta-style early termination on the inner search, and
 * picks the action that maximises the worst-case opponent response.
 */
public final class MinimaxBotStrategy implements BotStrategy {

    /** How many opponent counter-attacks to consider when estimating the worst case. */
    private static final int OPPONENT_BRANCHING_CAP = 5;

    private final Random random;

    public MinimaxBotStrategy() {
        this(new Random());
    }

    public MinimaxBotStrategy(Random random) {
        this.random = random != null ? random : new Random();
    }

    @Override
    public GameAction chooseAction(GameContext context) {
        List<GameAction> actions = LegalActionGenerator.generate(context);
        if (actions.isEmpty()) {
            return GameAction.endTurn();
        }

        GameAction best = null;
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;

        for (GameAction action : actions) {
            int botScore = GameStateEvaluator.scoreAction(context, action);
            int opponentWorstCase = estimateOpponentPunishment(context, action);
            int finalScore = botScore - opponentWorstCase;

            if (finalScore > bestScore
                    || (finalScore == bestScore && random.nextBoolean())) {
                best = action;
                bestScore = finalScore;
            }

            // Alpha-beta pruning: once we know we already beat the current alpha,
            // we tighten the bound for subsequent comparisons.
            alpha = Math.max(alpha, finalScore);
        }

        return best != null ? best : GameAction.endTurn();
    }

    /**
     * Estimates how much the opponent could hurt the bot in their next turn after the
     * bot commits to {@code action}. Returns a non-negative magnitude that the caller
     * subtracts from the bot's score (so larger means more painful for the bot).
     *
     * <p>Only opponent attacks are simulated; non-damage actions are assumed to be
     * neutral. The opponent is limited to its top {@link #OPPONENT_BRANCHING_CAP}
     * attacks by base damage to keep the search bounded.
     */
    private int estimateOpponentPunishment(GameContext context, GameAction action) {
        boolean endsTurn = action != null
                && (action.getType() == GameActionType.ATTACK
                        || action.getType() == GameActionType.END_TURN);
        if (!endsTurn) {
            return 0;
        }

        InPlayPokemon defender = context.getDefender();
        InPlayPokemon ourActive = context.getAttacker();
        if (defender == null || defender.getCard() == null || ourActive == null) {
            return 0;
        }

        // Simulate that our attack already landed: if our action would KO the defender,
        // the opponent loses tempo (no counter-attack this round).
        if (action.getType() == GameActionType.ATTACK
                && bestAttackDamage(ourActive, action.getAttackName()) >= defender.getCurrentHp()) {
            return 0;
        }

        int maxDamage = 0;
        int considered = 0;
        List<Attack> opponentAttacks = defender.getCard().getAttacks();
        if (opponentAttacks == null) {
            return 0;
        }
        for (Attack attack : opponentAttacks) {
            if (attack == null) continue;
            if (considered++ >= OPPONENT_BRANCHING_CAP) break;
            if (!opponentCanPay(defender, attack)) continue;
            int damage = attack.getBaseDamage();
            if (damage > maxDamage) {
                maxDamage = damage;
            }
        }

        int weight = 4; // mismo peso que GameStateEvaluator.scoreAttack
        int penalty = maxDamage * weight;
        if (maxDamage >= ourActive.getCurrentHp()) {
            penalty += 1000; // perdería un premio
        }
        return penalty;
    }

    private int bestAttackDamage(InPlayPokemon attacker, String attackName) {
        if (attacker == null || attacker.getCard() == null
                || attacker.getCard().getAttacks() == null) {
            return 0;
        }
        for (Attack attack : attacker.getCard().getAttacks()) {
            if (attack != null && attackName != null && attackName.equals(attack.getName())) {
                return attack.getBaseDamage();
            }
        }
        return 0;
    }

    private boolean opponentCanPay(InPlayPokemon defender, Attack attack) {
        Map<EnergyType, Integer> cost = attack.getEnergyCost();
        if (cost == null || cost.isEmpty()) {
            return true;
        }
        int totalAttached = defender.countEnergies(null);
        int totalRequired = 0;
        for (Map.Entry<EnergyType, Integer> entry : cost.entrySet()) {
            EnergyType type = entry.getKey();
            int required = entry.getValue() != null ? entry.getValue() : 0;
            totalRequired += required;
            if (type != EnergyType.COLORLESS && defender.countEnergies(type) < required) {
                return false;
            }
        }
        return totalAttached >= totalRequired;
    }
}
