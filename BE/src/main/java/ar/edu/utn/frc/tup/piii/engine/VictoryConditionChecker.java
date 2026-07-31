package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import java.util.List;
import java.util.Objects;

/**
 * Evaluates whether a match already has a winner under the XY1 ruleset.
 *
 * <p>The checker is intentionally isolated from turn execution so victory logic stays centralized,
 * deterministic and easy to test. The current {@link GameContext} is always oriented from the
 * current turn player's perspective, therefore attacker-side state belongs to the player whose
 * turn it currently is and defender-side state belongs to the opponent.
 *
 * <p>Two evaluation entry points are exposed because the existing engine models turn rotation
 * explicitly instead of a full draw phase state machine:
 * <ul>
 *     <li>{@link #evaluateAfterAction(Battle)} checks immediate victories caused by prize cards or
 *     by leaving a player without an active Pokémon and no Bench replacement.</li>
 *     <li>{@link #evaluateTurnStart(Battle)} applies the same immediate checks and then evaluates
 *     the XY1 deck-out rule for the player whose turn has just started.</li>
 * </ul>
 */
final class VictoryConditionChecker {

    /**
     * Evaluates victory conditions that can be satisfied immediately after an action resolves.
     *
     * <p>The checks run in deterministic order:
     * <ol>
     *     <li>Prize cards taken.</li>
     *     <li>Total knockout (no active Pokémon and no Bench replacement).</li>
     * </ol>
     *
     * @param match current match aggregate; must not be {@code null}
     * @return the resulting winner, or {@link MatchResult#NO_WINNER_YET} when play may continue
     * @throws NullPointerException if {@code match} or its context is {@code null}
     */
    MatchResult evaluateAfterAction(Battle match) {
        GameContext context = requireContext(match);

        MatchResult prizeResult = evaluatePrizeCards(context, match);
        if (prizeResult != MatchResult.NO_WINNER_YET) {
            return prizeResult;
        }

        return evaluateTotalKnockout(context, match);
    }

    /**
     * Evaluates victory conditions for the player whose turn has just begun.
     *
     * <p>This method first applies the same immediate checks used after an action. If the match is
     * still unresolved, it then verifies whether the current turn player can legally draw a card.
     * When the current turn player's deck is empty at this point, that player loses by deck-out.
     *
     * @param match current match aggregate already oriented to the player whose turn is starting;
     *              must not be {@code null}
     * @return the resulting winner, or {@link MatchResult#NO_WINNER_YET} when the new turn may
     *         proceed normally
     * @throws NullPointerException if {@code match} or its context is {@code null}
     */
    MatchResult evaluateTurnStart(Battle match) {
        MatchResult immediateResult = evaluateAfterAction(match);
        if (immediateResult != MatchResult.NO_WINNER_YET) {
            return immediateResult;
        }

        GameContext context = requireContext(match);
        if (isEmpty(context.getAttackerDeck())) {
            return resultForOpponent(match);
        }

        return MatchResult.NO_WINNER_YET;
    }

    private GameContext requireContext(Battle match) {
        Objects.requireNonNull(match, "match must not be null");
        return Objects.requireNonNull(match.getContext(), "match context must not be null");
    }

    private MatchResult evaluatePrizeCards(GameContext context, Battle match) {
        if (!context.isPrizesDealt()) {
            return MatchResult.NO_WINNER_YET;
        }
        if (isEmpty(context.getAttackerPrizes())) {
            return resultForCurrentPlayer(match);
        }
        if (isEmpty(context.getDefenderPrizes())) {
            return resultForOpponent(match);
        }
        return MatchResult.NO_WINNER_YET;
    }

    private MatchResult evaluateTotalKnockout(GameContext context, Battle match) {
        if (hasNoReplacement(context.getDefender(), context.getDefenderBench())) {
            return resultForCurrentPlayer(match);
        }
        if (hasNoReplacement(context.getAttacker(), context.getAttackerBench())) {
            return resultForOpponent(match);
        }
        return MatchResult.NO_WINNER_YET;
    }

    /**
     * Returns {@code true} when a player has no active Pokémon and no Pokémon on the bench,
     * making them unable to continue the match.
     */
    private boolean hasNoReplacement(InPlayPokemon active, List<InPlayPokemon> bench) {
        if (active != null) {
            return false;
        }
        return !hasAnyLiveBenchPokemon(bench);
    }

    private boolean hasAnyLiveBenchPokemon(List<InPlayPokemon> bench) {
        if (bench == null) {
            return false;
        }
        return bench.stream().anyMatch(Objects::nonNull);
    }

    private boolean isEmpty(List<?> zone) {
        return zone == null || zone.isEmpty();
    }

    private MatchResult resultForCurrentPlayer(Battle match) {
        return isPlayerOneTurn(match) ? MatchResult.PLAYER_ONE_WINS : MatchResult.PLAYER_TWO_WINS;
    }

    private MatchResult resultForOpponent(Battle match) {
        return isPlayerOneTurn(match) ? MatchResult.PLAYER_TWO_WINS : MatchResult.PLAYER_ONE_WINS;
    }

    private boolean isPlayerOneTurn(Battle match) {
        return Objects.equals(match.getPlayerOne().getId(), match.getCurrentTurnPlayerId());
    }
}

