package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * In-memory aggregate representing one engine-managed match.
 *
 * <p>The {@link GameContext} is always oriented from the current turn player's
 * perspective: attacker fields belong to the player whose turn it is, and
 * defender fields belong to the opponent. Swapping turns rotates that view while
 * preserving the underlying battlefield state.
 */
@Getter
@Setter
class Battle {

    private final String id;
    private final Player playerOne;
    private final Player playerTwo;
    private final Set<String> pendingActiveSelections;
    private final Set<String> setupReadyPlayers;
    private GameContext context;
    private String currentTurnPlayerId;
    private String winnerPlayerId;
    private boolean finished;
    private String openingPhase;
    private String openingChooserPlayerId;
    private String openingCoinChooserPlayerId;
    private String openingCoinChoice;
    private String openingCoinResult;
    private String openingStarterPlayerId;
    private boolean dealt;
    private boolean setupComplete;
    private String entityMatchId;
    private String mulliganPlayerId;
    private String mulliganBeneficiaryPlayerId;
    private int mulliganBenefitCount;
    private int mulliganBenefitChosen = -1;
    private boolean mulliganResolutionPending;
    private String pendingPrizeSelectionPlayerId;
    private long playerOneRemainingMs = 2_340_000L;
    private long playerTwoRemainingMs = 2_340_000L;
    private java.time.Instant turnStartedAt = java.time.Instant.now();

    private static final long SETUP_TIMEOUT_SECONDS = 40L;
    private final Map<String, LocalDateTime> setupDeadlines = new ConcurrentHashMap<>();

    Battle(String id, Player playerOne, Player playerTwo) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.playerOne = Objects.requireNonNull(playerOne, "playerOne must not be null");
        this.playerTwo = Objects.requireNonNull(playerTwo, "playerTwo must not be null");
        this.pendingActiveSelections = new LinkedHashSet<>();
        this.setupReadyPlayers = new LinkedHashSet<>();
        this.finished = false;
        this.dealt = false;
        this.setupComplete = false;
        this.mulliganResolutionPending = false;
    }

    /**
     * Returns whether the player participates in this match.
     *
     * @param playerId player identifier to check
     * @return {@code true} when the player belongs to the match
     */
    boolean containsPlayer(String playerId) {
        return Objects.equals(playerOne.getId(), playerId) || Objects.equals(playerTwo.getId(), playerId);
    }

    /**
     * Returns whether the given player is the one allowed to act right now.
     *
     * @param playerId player identifier to check
     * @return {@code true} when that player currently owns the turn
     */
    boolean isCurrentTurnPlayer(String playerId) {
        return Objects.equals(currentTurnPlayerId, playerId);
    }

    /**
     * Returns the opponent id for the provided player.
     *
     * @param playerId player whose opponent is required
     * @return opponent identifier
     * @throws IllegalArgumentException if the player does not belong to the match
     */
    String getOpponentPlayerId(String playerId) {
        if (Objects.equals(playerOne.getId(), playerId)) {
            return playerTwo.getId();
        }
        if (Objects.equals(playerTwo.getId(), playerId)) {
            return playerOne.getId();
        }
        throw new IllegalArgumentException("Player does not belong to match: " + playerId);
    }

    /**
     * Returns whether any player still needs to choose an active Pokémon.
     *
     * @return {@code true} while the match is waiting for an active selection
     */
    boolean isWaitingForActiveSelection() {
        return !pendingActiveSelections.isEmpty();
    }

    /**
     * Rotates the current context so the opponent becomes the next attacker.
     */
    void swapTurn() {
        currentTurnPlayerId = getOpponentPlayerId(currentTurnPlayerId);

        InPlayPokemon previousAttacker = context.getAttacker();
        InPlayPokemon previousDefender = context.getDefender();
        List<InPlayPokemon> previousAttackerBench = context.getAttackerBench();
        List<InPlayPokemon> previousDefenderBench = context.getDefenderBench();
        List<Card> previousAttackerHand = context.getAttackerHand();
        List<Card> previousDefenderHand = context.getDefenderHand();
        List<Card> previousAttackerDeck = context.getAttackerDeck();
        List<Card> previousDefenderDeck = context.getDefenderDeck();
        List<Card> previousAttackerDiscard = context.getAttackerDiscard();
        List<Card> previousDefenderDiscard = context.getDefenderDiscard();
        List<Card> previousAttackerPrizes = context.getAttackerPrizes();
        List<Card> previousDefenderPrizes = context.getDefenderPrizes();

        context.setAttacker(previousDefender);
        context.setDefender(previousAttacker);
        context.setAttackerBench(previousDefenderBench);
        context.setDefenderBench(previousAttackerBench);
        context.setAttackerHand(previousDefenderHand);
        context.setDefenderHand(previousAttackerHand);
        context.setAttackerDeck(previousDefenderDeck);
        context.setDefenderDeck(previousAttackerDeck);
        context.setAttackerDiscard(previousDefenderDiscard);
        context.setDefenderDiscard(previousAttackerDiscard);
        context.setAttackerPrizes(previousDefenderPrizes);
        context.setDefenderPrizes(previousAttackerPrizes);

        // Turn-scoped trackers always belong to the player who becomes the new attacker.
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setFirstTurn(false);
        context.setTurnNumber(context.getTurnNumber() + 1);
        context.setAttackerCantPlaySupporters(false);
        context.setAttackerEnergyAttachmentsThisTurn(0);
        context.setAttackerAdditionalEnergyAttachments(0);
        context.setAttackerSupportersPlayedThisTurn(0);
        context.setAttackerPokemonPlayedThisTurn(new ArrayList<>());
        context.setAttackerEvolvedThisTurn(new HashSet<>());
        context.setAttackerRetreatedThisTurn(new HashSet<>());
        context.setAbilitiesUsedThisTurn(new HashSet<>());
        context.setPendingDamage(0);
        context.setIgnoreWeakness(false);
        context.setIgnoreResistance(false);
        context.clearDamageCalculationModifiers();

        resetTeamTurnFlags(context.getAttacker(), context.getAttackerBench());
    }

    /**
     * Reorients the battle context so the supplied player becomes the starting player.
     *
     * <p>This keeps the current turn phase untouched and only swaps the attacker/defender
     * zones when the requested starter differs from the current orientation.
     *
     * @param starterPlayerId player who should own the opening turn
     */
    void setStartingPlayer(String starterPlayerId) {
        if (Objects.equals(currentTurnPlayerId, starterPlayerId)) {
            return;
        }

        String opponentPlayerId = getOpponentPlayerId(starterPlayerId);
        if (!Objects.equals(currentTurnPlayerId, opponentPlayerId)) {
            throw new IllegalArgumentException("Starting player does not match current match orientation: "
                    + starterPlayerId);
        }

        InPlayPokemon previousAttacker = context.getAttacker();
        InPlayPokemon previousDefender = context.getDefender();
        List<InPlayPokemon> previousAttackerBench = context.getAttackerBench();
        List<InPlayPokemon> previousDefenderBench = context.getDefenderBench();
        List<Card> previousAttackerHand = context.getAttackerHand();
        List<Card> previousDefenderHand = context.getDefenderHand();
        List<Card> previousAttackerDeck = context.getAttackerDeck();
        List<Card> previousDefenderDeck = context.getDefenderDeck();
        List<Card> previousAttackerDiscard = context.getAttackerDiscard();
        List<Card> previousDefenderDiscard = context.getDefenderDiscard();
        List<Card> previousAttackerPrizes = context.getAttackerPrizes();
        List<Card> previousDefenderPrizes = context.getDefenderPrizes();

        context.setAttacker(previousDefender);
        context.setDefender(previousAttacker);
        context.setAttackerBench(previousDefenderBench);
        context.setDefenderBench(previousAttackerBench);
        context.setAttackerHand(previousDefenderHand);
        context.setDefenderHand(previousAttackerHand);
        context.setAttackerDeck(previousDefenderDeck);
        context.setDefenderDeck(previousAttackerDeck);
        context.setAttackerDiscard(previousDefenderDiscard);
        context.setDefenderDiscard(previousAttackerDiscard);
        context.setAttackerPrizes(previousDefenderPrizes);
        context.setDefenderPrizes(previousAttackerPrizes);
        currentTurnPlayerId = starterPlayerId;
    }

    void beginOpening(String chooserPlayerId) {
        this.openingPhase = "COIN_CHOICE";
        this.openingChooserPlayerId = chooserPlayerId;
        this.openingCoinChooserPlayerId = chooserPlayerId;
        this.openingCoinChoice = null;
        this.openingCoinResult = null;
        this.openingStarterPlayerId = null;
    }

    void registerCoinChoice(String choice, String result) {
        this.openingCoinChoice = choice;
        this.openingCoinResult = result;
        this.openingPhase = "COIN_RESULT";
    }

    void registerStarterChoice(String starterPlayerId) {
        this.openingStarterPlayerId = starterPlayerId;
        this.openingPhase = "DEALING";
    }

    void completeOpening() {
        this.openingPhase = "COMPLETE";
    }

    void completeOpeningWithResult(String chooserPlayerId, String coinResult, String starterPlayerId) {
        this.openingPhase = "COIN_CHOICE";
        this.openingChooserPlayerId = chooserPlayerId;
        this.openingCoinChoice = coinResult;
        this.openingCoinResult = coinResult;
        this.openingStarterPlayerId = starterPlayerId;
        this.openingPhase = "COMPLETE";
    }

    boolean isOpeningInProgress() {
        return openingPhase != null && !"COMPLETE".equals(openingPhase);
    }

    private void resetTeamTurnFlags(InPlayPokemon activePokemon, List<InPlayPokemon> bench) {
        if (activePokemon != null) {
            activePokemon.resetTurnFlags();
        }
        if (bench == null) {
            return;
        }
        for (InPlayPokemon benchedPokemon : bench) {
            if (benchedPokemon != null) {
                benchedPokemon.resetTurnFlags();
            }
        }
    }

    /**
     * Tracks that the given player still needs to choose an active Pokémon.
     *
     * @param playerId player that must make the selection
     */
    void markPendingActiveSelection(String playerId) {
        pendingActiveSelections.add(playerId);
    }

    /**
     * Clears the pending active selection requirement for one player.
     *
     * @param playerId player whose selection is now resolved
     */
    void clearPendingActiveSelection(String playerId) {
        pendingActiveSelections.remove(playerId);
    }

    /**
     * Returns whether the given player has confirmed their full setup ready.
     *
     * @param playerId player identifier to inspect
     * @return {@code true} when that player has confirmed setup
     */
    boolean isSetupReady(String playerId) {
        return setupReadyPlayers.contains(playerId);
    }

    /**
     * Returns whether both players have confirmed their setup.
     *
     * @return {@code true} when both players are setup-ready
     */
    boolean isBothSetupReady() {
        return setupReadyPlayers.size() >= 2;
    }

    /**
     * Returns whether the full setup phase is complete for both players.
     *
     * @return {@code true} when both players have confirmed setup
     */
    boolean isSetupComplete() {
        return setupComplete;
    }

    /**
     * Marks the setup phase as complete (both players have confirmed).
     */
    void setSetupComplete(boolean setupComplete) {
        this.setupComplete = setupComplete;
    }

    /**
     * Marks a player as having completed their full setup (active + bench + ready).
     *
     * @param playerId player who confirmed setup
     */
    void markSetupReady(String playerId) {
        setupReadyPlayers.add(playerId);
    }

    /**
     * Starts the 40-second setup countdown for a player (idempotent).
     *
     * @param playerId player whose setup deadline should start
     */
    void startSetupCountdown(String playerId) {
        setupDeadlines.putIfAbsent(playerId, LocalDateTime.now().plusSeconds(SETUP_TIMEOUT_SECONDS));
    }

    /**
     * Remaining setup time in milliseconds for a player (0 if none or expired).
     *
     * @param playerId player whose remaining setup time is requested
     * @return remaining milliseconds, never negative
     */
    long getSetupRemainingMs(String playerId) {
        LocalDateTime deadline = setupDeadlines.get(playerId);
        if (deadline == null) {
            return 0L;
        }
        long ms = java.time.Duration.between(LocalDateTime.now(), deadline).toMillis();
        return Math.max(0L, ms);
    }

    /**
     * Whether a player's setup time has expired (deadline set and passed).
     *
     * @param playerId player whose deadline is checked
     * @return {@code true} when the 40 seconds are up
     */
    boolean isSetupExpired(String playerId) {
        LocalDateTime deadline = setupDeadlines.get(playerId);
        return deadline != null && !LocalDateTime.now().isBefore(deadline);
    }

    /**
     * Clears the setup countdown for a player (called when setup is confirmed).
     *
     * @param playerId player whose countdown should be removed
     */
    void clearSetupCountdown(String playerId) {
        setupDeadlines.remove(playerId);
    }

    /**
     * Returns whether the opening hand has been dealt for both players.
     *
     * @return {@code true} when dealCards has already been called
     */
    boolean isDealt() {
        return dealt;
    }

    /**
     * Marks the opening hand as dealt. This is idempotent.
     */
    void markDealt() {
        this.dealt = true;
    }

    /**
     * Returns whether the given player still must choose an active Pokémon.
     *
     * @param playerId player identifier to inspect
     * @return {@code true} when that player still has a pending active selection
     */
    boolean requiresActiveSelection(String playerId) {
        return pendingActiveSelections.contains(playerId);
    }

    boolean isPendingPrizeSelection() {
        return pendingPrizeSelectionPlayerId != null;
    }

    boolean isPendingPrizeSelectionForPlayer(String playerId) {
        return Objects.equals(pendingPrizeSelectionPlayerId, playerId);
    }

    /**
     * Returns the active Pokémon that belongs to the supplied player regardless of turn order.
     *
     * @param playerId player whose active Pokémon is required
     * @return current active Pokémon, or {@code null} when none has been chosen yet
     */
    InPlayPokemon getActiveForPlayer(String playerId) {
        if (isCurrentTurnPlayer(playerId)) {
            return context.getAttacker();
        }
        return context.getDefender();
    }

    /**
     * Marks the match as completed and stores the winner.
     *
     * @param winnerId winning player identifier
     */
    void finish(String winnerId) {
        this.finished = true;
        this.winnerPlayerId = winnerId;
    }

    void advanceTurnTimer() {
        long elapsed = java.time.Duration.between(turnStartedAt, java.time.Instant.now()).toMillis();
        if (Objects.equals(currentTurnPlayerId, playerOne.getId())) {
            playerOneRemainingMs = Math.max(0, playerOneRemainingMs - elapsed);
        } else {
            playerTwoRemainingMs = Math.max(0, playerTwoRemainingMs - elapsed);
        }
        turnStartedAt = java.time.Instant.now();
    }

    long getRemainingMs(String playerId) {
        if (Objects.equals(playerOne.getId(), playerId)) {
            return playerOneRemainingMs;
        }
        return playerTwoRemainingMs;
    }
}
