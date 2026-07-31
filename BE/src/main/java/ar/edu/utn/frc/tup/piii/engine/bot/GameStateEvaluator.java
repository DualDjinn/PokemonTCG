package ar.edu.utn.frc.tup.piii.engine.bot;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.TrainerCard;
import java.util.List;
import java.util.Map;

/**
 * Heuristic evaluator used by Greedy and Minimax bot strategies.
 *
 * <p>The {@link #evaluate(GameContext)} method scores the position from the bot's
 * perspective (the {@code attacker} side of the oriented {@link GameContext}).
 * Higher values are better for the bot.
 *
 * <p>The {@link #scoreAction(GameContext, GameAction)} method scores a candidate
 * action by reasoning about its expected effect on the state, without actually
 * mutating the engine. This lets Greedy and Minimax bots compare options without
 * deep-cloning the full battle state on every node.
 */
public final class GameStateEvaluator {

    private static final int PRIZE_WEIGHT = 1000;
    private static final int WIN_BONUS = 10_000;
    private static final int HP_WEIGHT = 1;          // applied to (currentHp / 10)
    private static final int ENERGY_WEIGHT = 5;
    private static final int HAND_WEIGHT = 2;
    private static final int NO_ACTIVE_PENALTY = 200;
    private static final int BENCH_WEIGHT = 30;

    private GameStateEvaluator() {
        // utility class
    }

    /**
     * Scores the given context from the bot's perspective.
     *
     * <p>Positive values favour the bot (the {@code attacker} side); negative values
     * favour the opponent. Terminal positions (no prizes left for one side) are
     * weighted with {@link #WIN_BONUS} so they dominate intermediate heuristics.
     *
     * @param context oriented context where {@code attacker} is the bot
     * @return integer score
     */
    public static int evaluate(GameContext context) {
        int score = 0;

        // Prize cards: fewer prize cards left for the bot means more prizes taken.
        int botPrizesLeft = sizeOf(context.getAttackerPrizes());
        int oppPrizesLeft = sizeOf(context.getDefenderPrizes());
        score += (6 - botPrizesLeft) * PRIZE_WEIGHT;
        score -= (6 - oppPrizesLeft) * PRIZE_WEIGHT;

        if (botPrizesLeft == 0) score += WIN_BONUS;
        if (oppPrizesLeft == 0) score -= WIN_BONUS;

        // HP totals across active + bench.
        score += hpScore(context.getAttacker(), context.getAttackerBench()) * HP_WEIGHT;
        score -= hpScore(context.getDefender(), context.getDefenderBench()) * HP_WEIGHT;

        // Attached energies (board strength).
        score += energyScore(context.getAttacker(), context.getAttackerBench()) * ENERGY_WEIGHT;
        score -= energyScore(context.getDefender(), context.getDefenderBench()) * ENERGY_WEIGHT;

        // Card economy.
        score += sizeOf(context.getAttackerHand()) * HAND_WEIGHT;
        score -= sizeOf(context.getDefenderHand()) * HAND_WEIGHT;

        // Bench presence (avoid losing by knockout with no replacement).
        score += sizeOf(context.getAttackerBench()) * BENCH_WEIGHT;

        // Active Pokémon presence.
        if (context.getAttacker() == null) score -= NO_ACTIVE_PENALTY;
        if (context.getDefender() == null) score += NO_ACTIVE_PENALTY;

        return score;
    }

    /**
     * Scores a candidate action in the given context, expressing how desirable the
     * action looks without applying it. Larger is better for the bot.
     *
     * <p>The score combines the static {@link #evaluate(GameContext)} baseline plus an
     * action-specific delta that captures the expected gain (e.g. attack damage,
     * extra board presence from playing a basic, etc.).
     *
     * @param context current context oriented from the bot's perspective
     * @param action  candidate action to evaluate
     * @return action score
     */
    public static int scoreAction(GameContext context, GameAction action) {
        int base = evaluate(context);
        if (action == null || action.getType() == null) {
            return base;
        }

        return switch (action.getType()) {
            case ATTACK -> base + scoreAttack(context, action.getAttackName());
            case PLAY_BASIC -> base + scorePlayBasic(context, action.getCardId());
            case ATTACH_ENERGY -> base + scoreAttachEnergy(context, action);
            case EVOLVE -> base + scoreEvolve(context, action.getCardId());
            case PLAY_SUPPORTER, PLAY_ITEM, PLAY_STADIUM, ATTACH_TOOL -> base + 10;
            case ADVANCE_TO_ATTACK -> base + 1;
            case END_TURN -> base - 5;
            case CHOOSE_COIN_SIDE, CHOOSE_FIRST_TURN -> base;
            default -> throw new IllegalArgumentException("Unexpected value: " + action.getType());
        };
    }

    private static int scoreAttack(GameContext context, String attackName) {
        InPlayPokemon attacker = context.getAttacker();
        InPlayPokemon defender = context.getDefender();
        if (attacker == null || attacker.getCard() == null || defender == null) {
            return 0;
        }
        Attack chosen = null;
        for (Attack attack : attacker.getCard().getAttacks()) {
            if (attack != null && attackName != null && attackName.equals(attack.getName())) {
                chosen = attack;
                break;
            }
        }
        if (chosen == null) {
            return 0;
        }
        int damage = chosen.getBaseDamage();
        int defenderHp = defender.getCurrentHp();
        int delta = damage * 4;
        if (damage >= defenderHp) {
            delta += PRIZE_WEIGHT; // anticipa toma de premio
        }
        return delta;
    }

    private static int scorePlayBasic(GameContext context, String cardId) {
        if (cardId == null) return 0;
        Card found = findInHand(context, cardId);
        if (!(found instanceof PokemonCard pokemon) || pokemon.getStage() != PokemonStage.BASIC) {
            return 0;
        }
        // Más pesado si todavía no hay activo, o si el banco está vacío.
        int bonus = 50;
        if (context.getAttacker() == null) bonus += 200;
        if (sizeOf(context.getAttackerBench()) == 0) bonus += 80;
        return bonus + pokemon.getHp() / 10;
    }

    private static int scoreAttachEnergy(GameContext context, GameAction action) {
        Card found = findInHand(context, action.getCardId());
        if (!(found instanceof EnergyCard)) {
            return 0;
        }
        // Adjuntar al activo vale más que al banco (más cerca de atacar).
        InPlayPokemon active = context.getAttacker();
        if (active != null && active.getCard() != null
                && active.getInstanceId().equals(action.getTargetCardId())) {
            return 40;
        }
        return 15;
    }

    private static int scoreEvolve(GameContext context, String cardId) {
        Card found = findInHand(context, cardId);
        if (!(found instanceof PokemonCard pokemon)) {
            return 0;
        }
        return 80 + pokemon.getHp() / 10;
    }

    private static int hpScore(InPlayPokemon active, List<InPlayPokemon> bench) {
        int total = 0;
        if (active != null) total += active.getCurrentHp() / 10;
        if (bench != null) {
            for (InPlayPokemon p : bench) {
                if (p != null) total += p.getCurrentHp() / 10;
            }
        }
        return total;
    }

    private static int energyScore(InPlayPokemon active, List<InPlayPokemon> bench) {
        int total = 0;
        if (active != null) total += active.countEnergies(null);
        if (bench != null) {
            for (InPlayPokemon p : bench) {
                if (p != null) total += p.countEnergies(null);
            }
        }
        return total;
    }

    private static Card findInHand(GameContext context, String cardId) {
        if (cardId == null || context.getAttackerHand() == null) return null;
        for (Card card : context.getAttackerHand()) {
            if (card != null && cardId.equals(card.getId())) return card;
        }
        return null;
    }

    private static int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    // Suppress unused-import warning for types referenced from comments/future hooks.
    @SuppressWarnings("unused")
    private static final Class<?>[] REFERENCED_TYPES = {
        EnergyType.class, TrainerCard.class, Map.class
    };
}
