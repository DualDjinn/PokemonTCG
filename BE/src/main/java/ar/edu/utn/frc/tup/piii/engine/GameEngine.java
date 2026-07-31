package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.engine.bot.BotDecisionService;
import ar.edu.utn.frc.tup.piii.engine.bot.BotDifficulty;
import ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.TrainerEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.DamageModifier;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.TrainerCard;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.Resistance;
import ar.edu.utn.frc.tup.piii.models.StadiumCard;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.Weakness;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Central facade of the battle engine.
 *
 * <p>Controllers and external services must interact only with this class. Match
 * setup, action submission and state reads are exposed here, while turn handling,
 * attack resolution and match storage stay delegated to dedicated collaborators.
 */
@Service
public class GameEngine {

    private static final Logger log = LoggerFactory.getLogger(GameEngine.class);
    private static final int OPENING_HAND_SIZE = 7;
    private static final int PRIZE_CARD_COUNT = 6;
    private static final int MAX_BENCH_SIZE = 5;

    /** Hard cap on the consecutive bot actions per submitAction call to avoid infinite loops. */
    private static final int MAX_BOT_ACTIONS_PER_TURN = 25;

    private final BattleRegistry battleRegistry;
    private final TurnManager turnManager;

    /**
     * Optional transport-layer observer used to publish match updates to connected
     * clients. This is intentionally not final so tests that construct the
     * GameEngine using the package-private constructor can skip providing a
     * publisher. When present, the engine will notify the observer after each
     * state mutation.
     */
    private MatchEventPublisher matchEventPublisher;

    /** Optional bot decision service: when present, bots play automatically. */
    @Autowired(required = false)
    private BotDecisionService botDecisionService;

    /**
     * Creates a GameEngine with only a MatchService, wiring all pure engine
     * collaborators internally via {@link #buildTurnManager()}.
     * Useful for tests and non-Spring contexts where a
     * {@link MatchEventPublisher} is not needed.
     */
 
    /**
     * Creates the Spring-facing engine facade and wires all pure engine collaborators.
     *
     * @param battleRegistry repository-style match storage service
     * @param matchEventPublisher transport-layer observer
     */
    @Autowired
    public GameEngine(BattleRegistry battleRegistry, MatchEventPublisher matchEventPublisher,
                      BotDecisionService botDecisionService) {
        this(battleRegistry, buildTurnManager());
        this.matchEventPublisher = matchEventPublisher;
    }

    GameEngine(BattleRegistry battleRegistry, TurnManager turnManager) {
        this.battleRegistry = battleRegistry;
        this.turnManager = turnManager;
    }

    GameEngine(BattleRegistry battleRegistry) {
        this(battleRegistry, buildTurnManager());
    }

    private static TurnManager buildTurnManager() {
        TriggeredAbilityDispatcher triggeredAbilityDispatcher = new TriggeredAbilityDispatcher();
        AttackResolutionPipeline attackResolutionPipeline = new AttackResolutionPipeline(
                new DamageCalculator(),
                new CardEffectRegistry(),
                new PassiveAbilityHandler(),
                triggeredAbilityDispatcher
        );
        return new TurnManager(
                new RuleValidator(),
                attackResolutionPipeline,
                triggeredAbilityDispatcher,
                new StatusEffectManager(),
                new VictoryConditionChecker(),
                new TrainerEffectRegistry(),
                new AbilityRegistry()
        );
    }

    /**
     * Creates a new match and performs the full opening setup.
     *
     * <p>The engine validates both players, picks one playable deck per player,
     * initializes the battle context, shuffles decks, deals the opening hands,
     * places six prize cards for each side and leaves the match waiting for both
     * active Pokémon to be chosen.
     *
     * @param playerOne first player
     * @param playerTwo second player
     * @param deckOne   deck chosen by player one in the lobby
     * @param deckTwo   deck chosen by player two in the lobby
     * @return generated match identifier
     * @throws IllegalArgumentException if either player or deck is invalid
     */
    public synchronized String createMatch(Player playerOne, Player playerTwo, Deck deckOne, Deck deckTwo) {
        validatePlayers(playerOne, playerTwo);
        if (deckOne == null) {
            throw new IllegalArgumentException("Player one deck must not be null");
        }
        if (deckTwo == null) {
            throw new IllegalArgumentException("Player two deck must not be null");
        }

        Battle match = battleRegistry.create(playerOne, playerTwo);
        GameContext context = buildInitialContext(deckOne, deckTwo);

        match.setContext(context);
        match.setCurrentTurnPlayerId(playerOne.getId());
        match.markPendingActiveSelection(playerOne.getId());
        match.markPendingActiveSelection(playerTwo.getId());

        boolean hostStarts = (match.getId().hashCode() & 1) == 0;
        String chooserId = hostStarts ? playerOne.getId() : playerTwo.getId();
        match.beginOpening(chooserId);

        battleRegistry.save(match);
        autoPlayBotOpeningIfNeeded(match);
        publishMatchUpdated(match);
        return match.getId();
    }

    /**
     * Restores a battle from a previously persisted GameContext (for reconnection).
     */
    public synchronized String restoreMatch(GameContext context, Player playerOne, Player playerTwo) {
        validatePlayers(playerOne, playerTwo);
        Battle match = battleRegistry.create(playerOne, playerTwo);
        match.setContext(context);
        match.setCurrentTurnPlayerId(playerOne.getId());
        battleRegistry.save(match);
        publishMatchUpdated(match);
        return match.getId();
    }

    /**
     * Associates the engine-internal match ID with the persistent entity match ID
     * so that all subsequent WebSocket publications use the correct topic path
     * (the entity ID) that clients are subscribed to.
     *
     * @param engineMatchId engine-internal match identifier returned by {@link #createMatch}
     * @param entityMatchId persistent entity identifier from the lobby layer
     */
    public void setEntityMatchId(String engineMatchId, String entityMatchId) {
        Battle battle = battleRegistry.getBattleOrThrow(engineMatchId);
        battle.setEntityMatchId(entityMatchId);
        battleRegistry.save(battle);
        publishMatchUpdated(battle);
    }

    /**
     * Removes a match from the in-memory battle registry.
     *
     * @param engineMatchId engine-internal match identifier
     */
    public void removeMatch(String engineMatchId) {
        battleRegistry.remove(engineMatchId);
    }

    /**
     * Returns a snapshot of all engine match IDs mapped to their entity match IDs.
     * Used by the scheduled cleanup to detect orphaned battles.
     */
    public Map<String, String> getBattleEntityMap() {
        Map<String, String> result = new java.util.HashMap<>();
        for (Battle battle : battleRegistry.getAll()) {
            String eid = battle.getEntityMatchId();
            if (eid != null) {
                result.put(battle.getId(), eid);
            }
        }
        return result;
    }

    /**
     * Returns the current {@link GameContext} for snapshotting / persistence.
     * @param matchId engine match identifier
     */
    public GameContext getMatchContext(String matchId) {
        return battleRegistry.getBattleOrThrow(matchId).getContext();
    }

    /**
     * Submits a player action to the engine.
     *
     * <p>The facade validates that the target match exists and then delegates turn
     * validation plus action resolution to the dedicated turn manager.
     *
     * @param matchId match identifier
     * @param playerId acting player identifier
     * @param action action being submitted
     * @throws IllegalArgumentException if the match does not exist or the action is illegal
     */
    public synchronized void submitAction(String matchId, String playerId, GameAction action) {
        Battle match = battleRegistry.getBattleOrThrow(matchId);
        if (!match.isSetupComplete()) {
            throw new IllegalArgumentException("Match setup is not complete");
        }
        turnManager.submitAction(match, playerId, action);
        battleRegistry.save(match);
        publishPendingGameEvents(match);
        autoSelectActiveForBots(match);
        playBotActionsUntilHumanTurn(match);
        publishMatchUpdated(match);
    }

    /**
     * Registers the coin side chosen by the opening player and resolves the toss.
     */
    public synchronized void chooseCoinSide(String matchId, String playerId, String coinSide) {
        Battle match = battleRegistry.getBattleOrThrow(matchId);
        validatePlayerMembership(match, playerId);
        if (!match.isOpeningInProgress()) {
            throw new IllegalArgumentException("Opening flow is not active");
        }
        if (!Objects.equals(match.getOpeningChooserPlayerId(), playerId)) {
            throw new IllegalArgumentException("Only the opening chooser can pick a coin side");
        }

        String normalizedSide = normalizeCoinSide(coinSide);
        String result = match.getContext().flipCoin() ? "heads" : "tails";
        match.registerCoinChoice(normalizedSide, result);

        if (!Objects.equals(normalizedSide, result)) {
            String chooserId = match.getOpeningChooserPlayerId();
            String opponentId = match.getOpponentPlayerId(chooserId);
            match.setOpeningChooserPlayerId(opponentId);
        }

        battleRegistry.save(match);
        autoPlayBotOpeningIfNeeded(match);
        publishMatchUpdated(match);
    }

    /**
     * Confirms which player starts after a successful opening toss.
     */
    public synchronized void chooseFirstTurn(String matchId, String playerId, String starterPlayerId) {
        Battle match = battleRegistry.getBattleOrThrow(matchId);
        validatePlayerMembership(match, playerId);
        if (!match.isOpeningInProgress()) {
            throw new IllegalArgumentException("Opening flow is not active");
        }
        if (!Objects.equals(match.getOpeningChooserPlayerId(), playerId)) {
            throw new IllegalArgumentException("Only the current chooser can decide the starter");
        }

        String normalizedStarter = normalizeStarter(match, starterPlayerId);
        match.registerStarterChoice(normalizedStarter);
        match.setStartingPlayer(normalizedStarter);
        battleRegistry.save(match);

        dealCards(match.getId());

        match.completeOpening();
        battleRegistry.save(match);
        publishMatchUpdated(match);
    }

    /**
     * Selects one Basic Pokémon from the player's hand as the active Pokémon.
     *
     * <p>This is used for the opening setup and any later replacement where the
     * engine is waiting for that player to choose a new active Pokémon.
     *
     * @param matchId match identifier
     * @param playerId player making the selection
     * @param cardId hand card identifier to promote
     * @throws IllegalArgumentException if the match does not exist, the player does not belong to
     *                                  it, the player is not waiting for selection or the chosen
     *                                  card is not a valid Basic Pokémon in that player's hand
     */
    public synchronized void selectActivePokemon(String matchId, String playerId, String cardId) {
        Battle match = battleRegistry.getBattleOrThrow(matchId);
        validatePlayerMembership(match, playerId);
        validateCardSelection(cardId);

        if (!match.requiresActiveSelection(playerId)) {
            throw new IllegalArgumentException("Player does not need to choose an active Pokemon: " + playerId);
        }

        GameContext context = match.getContext();
        boolean useAttackerSide = match.isCurrentTurnPlayer(playerId);
        ensureActivePokemonSlotIsEmpty(playerId, useAttackerSide, context);

        List<Card> hand = requirePlayerHand(useAttackerSide, context);
        InPlayPokemon activePokemon = buildSelectedActivePokemon(hand, cardId);
        if (useAttackerSide) {
            context.setAttacker(activePokemon);
        } else {
            context.setDefender(activePokemon);
        }

        match.clearPendingActiveSelection(playerId);
        battleRegistry.save(match);
        playBotActionsUntilHumanTurn(match);
        publishMatchUpdated(match);
    }

    /**
     * Promotes a benched Pokémon to the active slot after a knockout.
     *
     * @param matchId   match identifier
     * @param playerId  player making the promotion
     * @param benchSlot index into the player's bench (0-based)
     */
    public synchronized void promoteFromBench(String matchId, String playerId, int benchSlot) {
        Battle match = battleRegistry.getBattleOrThrow(matchId);
        validatePlayerMembership(match, playerId);

        if (!match.requiresActiveSelection(playerId)) {
            throw new IllegalArgumentException("Player does not need to choose an active Pokemon: " + playerId);
        }

        GameContext context = match.getContext();
        boolean useAttackerSide = match.isCurrentTurnPlayer(playerId);
        ensureActivePokemonSlotIsEmpty(playerId, useAttackerSide, context);

        List<InPlayPokemon> bench = useAttackerSide
                ? context.getAttackerBench() : context.getDefenderBench();
        if (benchSlot < 0 || benchSlot >= bench.size()) {
            throw new IllegalArgumentException("Invalid bench slot: " + benchSlot);
        }
        InPlayPokemon promoted = bench.get(benchSlot);
        if (promoted == null) {
            throw new IllegalArgumentException("No Pokemon in bench slot " + benchSlot);
        }
        if (useAttackerSide && context.getAttackerRetreatedThisTurn() != null
                && context.getAttackerRetreatedThisTurn().contains(promoted.getInstanceId())) {
            throw new IllegalArgumentException(
                    "This Pokemon retreated this turn and cannot return to the active spot");
        }
        bench.set(benchSlot, null);

        // If the retreating Pokémon was stored as pending bench placement (bench was full),
        // place it in the slot vacated by the promoted Pokémon.
        if (useAttackerSide && context.getPendingBenchPlacement() != null) {
            bench.set(benchSlot, context.getPendingBenchPlacement());
            context.setPendingBenchPlacement(null);
        }

        if (useAttackerSide) {
            context.setAttacker(promoted);
        } else {
            context.setDefender(promoted);
        }

        match.clearPendingActiveSelection(playerId);
        context.addLogEntry(playerId, "PROMOTE",
                promoted.getCard().getName() + " pasó a ser activo");
        battleRegistry.save(match);
        playBotActionsUntilHumanTurn(match);
        publishMatchUpdated(match);
    }

    /**
     * Selects a prize card to take after a knockout.
     *
     * @param matchId    match identifier
     * @param playerId   player taking the prize
     * @param prizeIndex index into the player's prize card list
     */
    public synchronized void selectPrize(String matchId, String playerId, int prizeIndex) {
        Battle match = battleRegistry.getBattleOrThrow(matchId);
        validatePlayerMembership(match, playerId);

        if (!match.isPendingPrizeSelectionForPlayer(playerId)) {
            throw new IllegalArgumentException("Player is not pending prize selection: " + playerId);
        }

        selectPrizeInternal(match, playerId, prizeIndex);
        battleRegistry.save(match);
        playBotActionsUntilHumanTurn(match);
        publishMatchUpdated(match);
    }

    private void selectPrizeInternal(Battle match, String playerId, int prizeIndex) {
        GameContext context = match.getContext();
        boolean useAttackerSide = match.isCurrentTurnPlayer(playerId);
        List<Card> prizes = useAttackerSide
                ? context.getAttackerPrizes() : context.getDefenderPrizes();
        if (prizeIndex < 0 || prizeIndex >= prizes.size()) {
            throw new IllegalArgumentException("Invalid prize index: " + prizeIndex);
        }

        Card selectedPrize = prizes.remove(prizeIndex);
        List<Card> hand = useAttackerSide
                ? context.getAttackerHand() : context.getDefenderHand();
        hand.add(selectedPrize);

        match.setPendingPrizeSelectionPlayerId(null);
        context.addLogEntry(playerId, "TAKE_PRIZE",
                "Tomó carta de premio: " + selectedPrize.getName());
    }

    /**
     * Places a Basic Pokémon from the player's hand onto their bench during setup.
     *
     * <p>Only allowed after the player has selected their active Pokémon and before
     * they have confirmed their setup is ready.
     *
     * @param matchId match identifier
     * @param playerId player making the placement
     * @param cardId hand card identifier to bench
     * @throws IllegalArgumentException if the match does not exist, the player does not belong to
     *                                  it, the player still needs an active, or the chosen
     *                                  card is not a valid Basic Pokémon
     */
    public synchronized void placeBenchPokemon(String matchId, String playerId, String cardId) {
        Battle match = battleRegistry.getBattleOrThrow(matchId);
        validatePlayerMembership(match, playerId);
        validateCardSelection(cardId);

        if (match.requiresActiveSelection(playerId)) {
            throw new IllegalArgumentException("Player must select an active Pokemon first: " + playerId);
        }

        GameContext context = match.getContext();
        boolean useAttackerSide = match.isCurrentTurnPlayer(playerId);
        List<Card> hand = requirePlayerHand(useAttackerSide, context);
        List<InPlayPokemon> bench = useAttackerSide ? context.getAttackerBench() : context.getDefenderBench();

        long occupied = bench.stream().filter(Objects::nonNull).count();
        if (occupied >= MAX_BENCH_SIZE) {
            throw new IllegalArgumentException("Bench is full");
        }

        Card selectedCard = hand.stream()
                .filter(Objects::nonNull)
                .filter(card -> Objects.equals(card.getId(), cardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found in player hand: " + cardId));

        if (!(selectedCard instanceof PokemonCard pokemonCard)) {
            throw new IllegalArgumentException("Selected card is not a Pokemon: " + cardId);
        }
        if (!pokemonCard.isBasic()) {
            throw new IllegalArgumentException("Selected bench Pokemon must be Basic: " + cardId);
        }

        hand.remove(selectedCard);
        for (int i = 0; i < bench.size(); i++) {
            if (bench.get(i) == null) {
                bench.set(i, new InPlayPokemon(pokemonCard));
                break;
            }
        }
        if (useAttackerSide) {
            context.setAttackerBench(bench);
        } else {
            context.setDefenderBench(bench);
        }

        battleRegistry.save(match);
        publishMatchUpdated(match);
    }

    /**
     * Confirms that a player has completed their full setup (active + optional bench).
     *
     * <p>When both players have confirmed, the engine checks for pending mulligan
     * resolution. If a beneficiary needs to decide how many extra cards to draw,
     * the match enters mulligan resolution instead of placing prizes immediately.
     * Once the beneficiary makes their choice via {@link #resolveMulliganDraw},
     * prizes are placed and the match transitions to the main game phase.
     *
     * @param matchId match identifier
     * @param playerId player confirming setup
     * @throws IllegalArgumentException if the match does not exist or the player does not belong
     */
    public synchronized void confirmSetupReady(String matchId, String playerId) {
        Battle match = battleRegistry.getBattleOrThrow(matchId);
        validatePlayerMembership(match, playerId);

        if (match.requiresActiveSelection(playerId)) {
            throw new IllegalArgumentException("Player must select an active Pokemon first: " + playerId);
        }

        match.markSetupReady(playerId);

        autoSetupBotIfNeeded(match);

        if (match.isBothSetupReady()) {
            if (match.isMulliganResolutionPending()) {
                autoResolveMulliganIfBot(match);
                if (match.isMulliganResolutionPending()) {
                    battleRegistry.save(match);
                    publishMatchUpdated(match);
                    return;
                }
            }

            GameContext context = match.getContext();
            placePrizeCards(context.getAttackerDeck(), context.getAttackerPrizes());
            placePrizeCards(context.getDefenderDeck(), context.getDefenderPrizes());
            context.setPrizesDealt(true);
            match.setSetupComplete(true);
            battleRegistry.save(match);
            playBotActionsUntilHumanTurn(match);
        }

        battleRegistry.save(match);
        publishMatchUpdated(match);
    }

    /**
     * Resolves the post-mulligan extra-card decision for the beneficiary.
     *
     * <p>After both players confirm setup and mulligan resolution is pending,
     * the beneficiary chooses how many extra cards to draw (0 to the total
     * mulligan count). The chosen amount is drawn immediately from the
     * beneficiary's deck, prize cards are placed, and the match transitions
     * to the main game phase.
     *
     * @param matchId  match identifier
     * @param playerId beneficiary player identifier
     * @param count    number of extra cards to draw
     * @throws IllegalArgumentException if the match does not exist, the player
     *                                  is not the beneficiary, or the count is
     *                                  outside the valid range
     */
    public synchronized void resolveMulliganDraw(String matchId, String playerId, int count) {
        Battle match = battleRegistry.getBattleOrThrow(matchId);
        validatePlayerMembership(match, playerId);

        if (!match.isMulliganResolutionPending()) {
            throw new IllegalArgumentException("No pending mulligan resolution for this match");
        }
        if (!Objects.equals(match.getMulliganBeneficiaryPlayerId(), playerId)) {
            throw new IllegalArgumentException("Only the beneficiary can resolve mulligan draws");
        }
        if (count < 0 || count > match.getMulliganBenefitCount()) {
            throw new IllegalArgumentException(
                    "Count must be between 0 and " + match.getMulliganBenefitCount());
        }

        GameContext context = match.getContext();
        boolean beneficiaryIsAttacker = match.isCurrentTurnPlayer(playerId);

        for (int i = 0; i < count; i++) {
            if (beneficiaryIsAttacker) {
                context.attackerDrawCard();
            } else {
                context.defenderDrawCard();
            }
        }

        match.setMulliganBenefitChosen(count);
        match.setMulliganResolutionPending(false);

        placePrizeCards(context.getAttackerDeck(), context.getAttackerPrizes());
        placePrizeCards(context.getDefenderDeck(), context.getDefenderPrizes());
        context.setPrizesDealt(true);
        match.setSetupComplete(true);

        battleRegistry.save(match);
        playBotActionsUntilHumanTurn(match);
        publishMatchUpdated(match);
    }

    /**
     * Returns the current match state from one player's perspective.
     *
     * <p>The requesting player's full hand is revealed, while the opponent hand is
     * reduced to its size. Shared battlefield information includes both benches,
     * both active Pokémon and each side's remaining prize count.
     *
     * @param matchId match identifier
     * @param playerId requesting player identifier
     * @return match state projection for that player
     * @throws IllegalArgumentException if the match does not exist or the player is not part of it
     */
    public MatchStateDTO getMatchState(String matchId, String playerId) {
        Battle match = battleRegistry.getBattleOrThrow(matchId);
        validatePlayerMembership(match, playerId);

        boolean requesterIsAttacker = match.isCurrentTurnPlayer(playerId);
        GameContext context = match.getContext();
        Player requester = resolvePlayer(match, playerId);
        Player opponent = resolvePlayer(match, match.getOpponentPlayerId(playerId));

        MatchStateDTO response = new MatchStateDTO();
        response.setMatchId(match.getEntityMatchId() != null ? match.getEntityMatchId() : match.getId());
        response.setRequestedPlayerId(playerId);
        response.setCurrentTurnPlayerId(match.getCurrentTurnPlayerId());
        response.setWinnerPlayerId(match.getWinnerPlayerId());
        response.setCurrentPhase(context.getCurrentPhase());
        response.setCurrentTurn(requesterIsAttacker);
        response.setFinished(match.isFinished());
        response.setFirstTurn(context.isFirstTurn());
        response.setWaitingForActiveSelection(match.isWaitingForActiveSelection());
        response.setRequiresActiveSelection(match.requiresActiveSelection(playerId));
        response.setPendingPrizeSelection(match.isPendingPrizeSelection());
        response.setPendingPrizeSelectionPlayerId(match.getPendingPrizeSelectionPlayerId());
        response.setMulliganIteration(context.getMulliganIteration());
        response.setSetupComplete(match.isSetupComplete());
        response.setMatchLog(context.getMatchLog());
        response.setCurrentTurnNumber(context.getTurnNumber());
        response.setMulliganPlayerId(match.getMulliganPlayerId());
        response.setMulliganBeneficiaryPlayerId(match.getMulliganBeneficiaryPlayerId());
        response.setMulliganBenefitCount(match.getMulliganBenefitCount());
        response.setMulliganResolutionPending(
                match.isMulliganResolutionPending() && match.isBothSetupReady());
        response.setOpeningPhase(match.getOpeningPhase());
        response.setOpeningChooserPlayerId(match.getOpeningChooserPlayerId());
        response.setOpeningCoinChooserPlayerId(match.getOpeningCoinChooserPlayerId());
        response.setOpeningCoinChoice(match.getOpeningCoinChoice());
        response.setOpeningCoinResult(match.getOpeningCoinResult());
        response.setOpeningStarterPlayerId(match.getOpeningStarterPlayerId());
        response.setSelf(buildPlayerState(requester, true, requesterIsAttacker, context, match));
        response.setOpponent(buildPlayerState(opponent, false, !requesterIsAttacker, context, match));

        Card lastPlayed = context.getLastPlayedCard();
        if (lastPlayed != null) {
            response.setLastPlayedCard(mapSingleCard(lastPlayed));
            response.setLastPlayedCardPlayerId(context.getLastPlayedCardPlayerId());
            context.setLastPlayedCard(null);
            context.setLastPlayedCardPlayerId(null);
        }

        StadiumCard activeStadium = context.getActiveStadium();
        if (activeStadium != null) {
            MatchStateDTO.CardViewDTO stadiumDto = new MatchStateDTO.CardViewDTO();
            stadiumDto.setId(activeStadium.getId());
            stadiumDto.setName(activeStadium.getName());
            stadiumDto.setSupertype(activeStadium.getSupertype());
            stadiumDto.setImageUrl(activeStadium.getImageUrl());
            stadiumDto.setSubtypes(List.of(activeStadium.getSubtype()));
            response.setActiveStadium(stadiumDto);
        }

        return response;
    }

    private GameContext buildInitialContext(Deck playerOneDeck, Deck playerTwoDeck) {
        GameContext context = new GameContext();
        context.setAttackerDeck(new ArrayList<>(playerOneDeck.getCards()));
        context.setDefenderDeck(new ArrayList<>(playerTwoDeck.getCards()));
        context.shuffleAttackerDeck();
        context.shuffleDefenderDeck();

        return context;
    }

    /**
     * Deals 7 opening hand cards to both players and resolves mulligans.
     *
     * <h3>Case A — neither player has a Basic Pokémon</h3>
     * Both hands are returned, decks shuffled, and 7 new cards are dealt to both.
     * This repeats synchronously until at least one player draws a Basic. No
     * mulligan counts are tracked in this scenario.
     *
     * <h3>Case B — one player has a Basic, the other does not</h3>
     * The player without a Basic performs individual mulligans (return, shuffle,
     * re-draw 7). Each iteration increments the opponent's benefit counter.
     * Extra cards are <strong>not</strong> awarded automatically; the beneficiary
     * decides how many to take after both players confirm setup.
     *
     * <p>Idempotent: only executes if cards have not been dealt yet for this match.
     *
     * @param matchId match identifier
     * @throws IllegalArgumentException if the match does not exist
     */
    public synchronized void dealCards(String matchId) {
        Battle match = battleRegistry.getBattleOrThrow(matchId);
        if (match.isDealt()) {
            return;
        }

        GameContext context = match.getContext();
        dealOpeningHand(context.getAttackerDeck(), context.getAttackerHand());
        dealOpeningHand(context.getDefenderDeck(), context.getDefenderHand());

        List<MulliganEvent.MulliganStep> mulliganSteps = new ArrayList<>();
        String attackerPlayerId = match.getCurrentTurnPlayerId();
        String defenderPlayerId = match.getOpponentPlayerId(attackerPlayerId);

        boolean attackerHasBasic = hasBasicInHand(context.getAttackerHand());
        boolean defenderHasBasic = hasBasicInHand(context.getDefenderHand());

        // ---- Case A: neither player has a Basic — loop synchronously, no mulligan counts ----
        if (!attackerHasBasic && !defenderHasBasic) {
            int safety = 0;
            while (!hasBasicInHand(context.getAttackerHand())
                    && !hasBasicInHand(context.getDefenderHand())
                    && safety < 100) {
                context.setMulliganIteration(context.getMulliganIteration() + 1);

                MulliganEvent.MulliganStep aStep = new MulliganEvent.MulliganStep(
                        attackerPlayerId, mapHand(new ArrayList<>(context.getAttackerHand())), 0);
                MulliganEvent.MulliganStep dStep = new MulliganEvent.MulliganStep(
                        defenderPlayerId, mapHand(new ArrayList<>(context.getDefenderHand())), 0);
                mulliganSteps.add(aStep);
                mulliganSteps.add(dStep);

                context.getAttackerDeck().addAll(0, context.getAttackerHand());
                context.getAttackerHand().clear();
                context.shuffleAttackerDeck();
                dealOpeningHand(context.getAttackerDeck(), context.getAttackerHand());

                context.getDefenderDeck().addAll(0, context.getDefenderHand());
                context.getDefenderHand().clear();
                context.shuffleDefenderDeck();
                dealOpeningHand(context.getDefenderDeck(), context.getDefenderHand());
                safety++;
            }
            attackerHasBasic = hasBasicInHand(context.getAttackerHand());
            defenderHasBasic = hasBasicInHand(context.getDefenderHand());
        }

        // ---- Case B: one player has a Basic, the other does not ----
        if (!attackerHasBasic || !defenderHasBasic) {
            String mulliganPlayerId = !attackerHasBasic ? attackerPlayerId : defenderPlayerId;
            String beneficiaryPlayerId = !attackerHasBasic ? defenderPlayerId : attackerPlayerId;
            boolean mulliganIsAttacker = mulliganPlayerId.equals(attackerPlayerId);

            List<Card> mulliganHand = mulliganIsAttacker
                    ? context.getAttackerHand() : context.getDefenderHand();
            List<Card> mulliganDeck = mulliganIsAttacker
                    ? context.getAttackerDeck() : context.getDefenderDeck();

            int mulliganCount = 0;
            int safety = 0;
            while (!hasBasicInHand(mulliganHand) && safety < 100) {
                context.setMulliganIteration(context.getMulliganIteration() + 1);
                mulliganCount++;

                MulliganEvent.MulliganStep step = new MulliganEvent.MulliganStep(
                        mulliganPlayerId,
                        mapHand(new ArrayList<>(mulliganHand)),
                        mulliganCount);
                mulliganSteps.add(step);

                mulliganDeck.addAll(0, mulliganHand);
                mulliganHand.clear();
                if (mulliganIsAttacker) {
                    context.shuffleAttackerDeck();
                } else {
                    context.shuffleDefenderDeck();
                }
                dealOpeningHand(mulliganDeck, mulliganHand);
                safety++;
            }

            match.setMulliganPlayerId(mulliganPlayerId);
            match.setMulliganBeneficiaryPlayerId(beneficiaryPlayerId);
            match.setMulliganBenefitCount(mulliganCount);
            match.setMulliganBenefitChosen(-1);
            match.setMulliganResolutionPending(mulliganCount > 0);

            if (mulliganIsAttacker) {
                context.setAttackerMulliganCount(mulliganCount);
                context.setDefenderMulliganCount(0);
            } else {
                context.setDefenderMulliganCount(mulliganCount);
                context.setAttackerMulliganCount(0);
            }
        } else {
            // Both have Basic — no mulligan counts, no resolution needed
            context.setAttackerMulliganCount(0);
            context.setDefenderMulliganCount(0);
        }

        match.markDealt();
        battleRegistry.save(match);

        if (!mulliganSteps.isEmpty()) {
            String entityMatchId = match.getEntityMatchId();
            String publishMatchId = entityMatchId != null ? entityMatchId : match.getId();
            MulliganEvent mulliganEvent = new MulliganEvent(publishMatchId, mulliganSteps);
            publishMulliganEvent(mulliganEvent);
        }
    }

    /**
     * Returns whether at least one card in the given hand is a Basic Pokémon.
     */
    private boolean hasBasicInHand(List<Card> hand) {
        return hand.stream()
                .filter(PokemonCard.class::isInstance)
                .anyMatch(c -> ((PokemonCard) c).isBasic());
    }

    private String normalizeCoinSide(String coinSide) {
        if (coinSide == null) {
            throw new IllegalArgumentException("Coin side must not be null");
        }
        String normalized = coinSide.trim().toLowerCase();
        if (!normalized.equals("heads") && !normalized.equals("tails")) {
            throw new IllegalArgumentException("Coin side must be heads or tails");
        }
        return normalized;
    }

    private String normalizeStarter(Battle match, String starterPlayerId) {
        if (starterPlayerId == null || starterPlayerId.isBlank()) {
            throw new IllegalArgumentException("Starter player id must not be blank");
        }
        if (!match.containsPlayer(starterPlayerId)) {
            throw new IllegalArgumentException("Starter player does not belong to this match");
        }
        return starterPlayerId;
    }

    private void dealOpeningHand(List<Card> deck, List<Card> hand) {
        ensureDeckHasEnoughCards(deck, OPENING_HAND_SIZE, "opening hand");
        for (int i = 0; i < OPENING_HAND_SIZE; i++) {
            hand.add(deck.removeFirst());
        }
    }

    private void placePrizeCards(List<Card> deck, List<Card> prizes) {
        ensureDeckHasEnoughCards(deck, PRIZE_CARD_COUNT, "prize cards");
        for (int i = 0; i < PRIZE_CARD_COUNT; i++) {
            prizes.add(deck.removeFirst());
        }
    }

    private void ensureDeckHasEnoughCards(List<Card> deck, int requiredCards, String purpose) {
        if (deck.size() < requiredCards) {
            throw new IllegalArgumentException("Deck does not contain enough cards for " + purpose);
        }
    }

    private void validatePlayers(Player playerOne, Player playerTwo) {
        Objects.requireNonNull(playerOne, "playerOne must not be null");
        Objects.requireNonNull(playerTwo, "playerTwo must not be null");
        if (playerOne.getId() == null || playerOne.getId().isBlank()) {
            throw new IllegalArgumentException("Player one id must not be blank");
        }
        if (playerTwo.getId() == null || playerTwo.getId().isBlank()) {
            throw new IllegalArgumentException("Player two id must not be blank");
        }
        if (Objects.equals(playerOne.getId(), playerTwo.getId())) {
            throw new IllegalArgumentException("A match requires two different players");
        }
    }

    private void validatePlayerMembership(Battle match, String playerId) {
        if (playerId == null || playerId.isBlank()) {
            throw new IllegalArgumentException("Player id must not be blank");
        }
        if (!match.containsPlayer(playerId)) {
            throw new IllegalArgumentException("Player does not belong to match: " + playerId);
        }
    }

    private Player resolvePlayer(Battle match, String playerId) {
        if (Objects.equals(match.getPlayerOne().getId(), playerId)) {
            return match.getPlayerOne();
        }
        return match.getPlayerTwo();
    }

    private void validateCardSelection(String cardId) {
        if (cardId == null || cardId.isBlank()) {
            throw new IllegalArgumentException("Card id must not be blank");
        }
    }

    private MatchStateDTO.PlayerBattlefieldDTO buildPlayerState(Player player,
                                                                 boolean revealHand,
                                                                 boolean useAttackerSide,
                                                                 GameContext context,
                                                                 Battle match) {
        MatchStateDTO.PlayerBattlefieldDTO state = new MatchStateDTO.PlayerBattlefieldDTO();
        state.setPlayerId(player.getId());
        state.setTrainerName(player.getTrainerName());

        List<Card> hand = useAttackerSide ? context.getAttackerHand() : context.getDefenderHand();
        List<Card> prizes = useAttackerSide ? context.getAttackerPrizes() : context.getDefenderPrizes();
        state.setHandSize(hand == null ? 0 : hand.size());
        state.setRemainingPrizes(prizes == null ? 0 : prizes.size());
        state.setMulliganCount(useAttackerSide
                ? context.getAttackerMulliganCount()
                : context.getDefenderMulliganCount());
        state.setSetupReady(match.isSetupReady(player.getId()));
        state.setRemainingTimeMs(match.getRemainingMs(player.getId()));
        state.setHasAttachedEnergyThisTurn(
                useAttackerSide && context.getAttackerEnergyAttachmentsThisTurn() > 0);
        state.setAbilitiesUsedThisTurn(useAttackerSide
                ? new java.util.ArrayList<>(context.getAbilitiesUsedThisTurn())
                : List.of());
        state.setPendingPlayerChoice(buildPendingChoiceFor(player, context));
        state.setHand(revealHand ? mapHand(hand) : List.of());
        state.setPrizeCards(mapPrizeCards(prizes));
        state.setActivePokemon(mapPokemon(useAttackerSide ? context.getAttacker() : context.getDefender()));
        state.setBench(mapBench(useAttackerSide ? context.getAttackerBench() : context.getDefenderBench()));
        state.setDiscard(mapPrizeCards(useAttackerSide
                ? context.getAttackerDiscard() : context.getDefenderDiscard()));

        return state;
    }

    private ar.edu.utn.frc.tup.piii.dtos.engine.PendingPlayerChoiceDTO buildPendingChoiceFor(
            Player player, GameContext context) {
        ar.edu.utn.frc.tup.piii.engine.choice.PendingPlayerChoice choice = context.getPendingPlayerChoice();
        if (choice == null || !choice.getPlayerId().equals(player.getId())) {
            return null;
        }
        ar.edu.utn.frc.tup.piii.dtos.engine.PendingPlayerChoiceDTO dto =
                new ar.edu.utn.frc.tup.piii.dtos.engine.PendingPlayerChoiceDTO();
        dto.setType(choice.getType().name());
        dto.setMinPick(choice.getMinPick());
        dto.setMaxPick(choice.getMaxPick());
        dto.setPrompt(choice.getPrompt());
        dto.setCoinFlipHeads(choice.getCoinFlipHeads());
        dto.setOptions(choice.getOptions().stream()
                .filter(Objects::nonNull)
                .map(this::mapSingleCard)
                .toList());
        return dto;
    }

    /**
     * Maps prize cards for the owning player. Prize cards are revealed (face-up) so the
     * player knows what they are selecting. The opponent always sees an empty list.
     */
    private List<MatchStateDTO.CardViewDTO> mapPrizeCards(List<Card> prizes) {
        if (prizes == null) return List.of();
        return prizes.stream()
                .filter(Objects::nonNull)
                .map(this::mapSingleCard)
                .toList();
    }

    private List<MatchStateDTO.CardViewDTO> mapHand(List<Card> cards) {
        List<MatchStateDTO.CardViewDTO> mappedCards = new ArrayList<>();
        if (cards == null) {
            return mappedCards;
        }
        for (Card card : cards) {
            if (card == null) {
                throw new IllegalStateException("Hand contains a null card");
            }
            MatchStateDTO.CardViewDTO dto = mapSingleCard(card);
            mappedCards.add(dto);
        }
        return mappedCards;
    }

    private MatchStateDTO.CardViewDTO mapSingleCard(Card card) {
        MatchStateDTO.CardViewDTO dto = new MatchStateDTO.CardViewDTO();
        dto.setId(card.getId());
        dto.setName(card.getName());
        dto.setSupertype(card.getSupertype());
        dto.setImageUrl(card.getImageUrl());
        dto.setSubtypes(extractSubtypes(card));
        if (card instanceof TrainerCard t) {
            dto.setRules(t.getRules());
        }
        if (card instanceof PokemonCard pc) {
            dto.setEvolvesFrom(pc.getEvolvesFrom());
            dto.setHp(pc.getHp());
            dto.setAttacks(mapAttacks(pc.getAttacks()));
            dto.setAbilities(mapAbilities(pc.getAbilities()));
            dto.setWeaknesses(mapWeaknesses(pc.getWeaknesses()));
            dto.setResistances(mapResistances(pc.getResistances()));
            dto.setRetreatCost(pc.getRetreatCost());
            dto.setPokemonType(pc.getPokemonType() != null
                    ? pc.getPokemonType().name() : null);
            dto.setGifUrl(pc.getGifUrl());
        }
        return dto;
    }

    private List<String> extractSubtypes(Card card) {
        if (card instanceof PokemonCard pokemon) {
            List<String> subtypes = new ArrayList<>();
            subtypes.add(pokemon.getStage().getJsonValue());
            if (pokemon.isEx()) subtypes.add("EX");
            if (pokemon.isMega()) subtypes.add("Mega");
            return subtypes;
        }
        if (card instanceof TrainerCard trainer) {
            return List.of(trainer.getSubtype());
        }
        return List.of();
    }

    private List<MatchStateDTO.InPlayPokemonViewDTO> mapBench(List<InPlayPokemon> bench) {
        if (bench == null) {
            return List.of();
        }
        return bench.stream()
                .map(this::mapPokemon)
                .filter(Objects::nonNull)
                .toList();
    }

    private MatchStateDTO.InPlayPokemonViewDTO mapPokemon(InPlayPokemon pokemon) {
        if (pokemon == null) {
            return null;
        }

        PokemonCard pokemonCard = pokemon.getCard();
        if (pokemonCard == null) {
            throw new IllegalStateException("In-play Pokemon must reference a card");
        }

        MatchStateDTO.InPlayPokemonViewDTO dto = new MatchStateDTO.InPlayPokemonViewDTO();
        dto.setCardId(pokemonCard.getId());
        dto.setInstanceId(pokemon.getInstanceId());
        dto.setName(pokemonCard.getName());
        dto.setImageUrl(pokemonCard.getImageUrl());
        dto.setMaxHp(pokemonCard.getHp());
        dto.setCurrentHp(pokemon.getCurrentHp());
        dto.setDamageTaken(pokemon.getDamageTaken());
        dto.setAttachedEnergyCount(pokemon.countEnergies(null));
        dto.setAttachedEnergyTypes(pokemon.getEnergies().stream()
                .map(e -> e.getEnergyType().name())
                .toList());
        dto.setAttachedEnergyCardIds(pokemon.getEnergies().stream()
                .map(e -> e.getId())
                .toList());
        dto.setPokemonType(pokemonCard.getPokemonType() != null
                ? pokemonCard.getPokemonType().name() : null);
        dto.setKnockedOut(pokemon.isKnockedOut());
        dto.setRetreatCost(pokemonCard.getRetreatCost());
        dto.setAttachedEnergyImageUrls(pokemon.getEnergies().stream()
                .map(e -> e.getImageUrl())
                .toList());
        dto.setStatuses(new LinkedHashSet<>(requireStatuses(pokemon)));
        dto.setAttacks(mapAttacks(pokemonCard.getAttacks()));
        dto.setAbilities(mapAbilities(pokemonCard.getAbilities()));
        dto.setWeaknesses(mapWeaknesses(pokemonCard.getWeaknesses()));
        dto.setResistances(mapResistances(pokemonCard.getResistances()));

        dto.setGifUrl(pokemonCard.getGifUrl());

        if (pokemon.getTool() != null) {
            dto.setToolCardName(pokemon.getTool().getName());
            dto.setToolCardId(pokemon.getTool().getId());
            dto.setToolCardImageUrl(pokemon.getTool().getImageUrl());
            if (pokemon.getTool() instanceof TrainerCard t) {
                dto.setToolCardRules(t.getRules());
            }
        }

        return dto;
    }

    private List<MatchStateDTO.AttackViewDTO> mapAttacks(List<Attack> attacks) {
        if (attacks == null) return List.of();
        return attacks.stream()
                .map(a -> MatchStateDTO.AttackViewDTO.builder()
                        .name(a.getName())
                        .damage(formatDamage(a.getBaseDamage(), a.getDamageModifier()))
                        .cost(expandEnergyCost(a.getEnergyCost()))
                        .description(a.getEffectText())
                        .build())
                .toList();
    }

    private List<MatchStateDTO.AbilityViewDTO> mapAbilities(List<Ability> abilities) {
        if (abilities == null) return List.of();
        return abilities.stream()
                .map(a -> MatchStateDTO.AbilityViewDTO.builder()
                        .name(a.getName())
                        .description(a.getDescription())
                        .type(a.getType().name())
                        .build())
                .toList();
    }

    private List<MatchStateDTO.WeaknessDTO> mapWeaknesses(List<Weakness> weaknesses) {
        if (weaknesses == null) return List.of();
        return weaknesses.stream()
                .map(w -> MatchStateDTO.WeaknessDTO.builder()
                        .type(w.getType())
                        .value(w.getValue())
                        .build())
                .toList();
    }

    private List<MatchStateDTO.ResistanceDTO> mapResistances(List<Resistance> resistances) {
        if (resistances == null) return List.of();
        return resistances.stream()
                .map(r -> MatchStateDTO.ResistanceDTO.builder()
                        .type(r.getType())
                        .reduction(r.getReduction())
                        .build())
                .toList();
    }

    private String formatDamage(int baseDamage, DamageModifier modifier) {
        String s = String.valueOf(baseDamage);
        if (modifier == DamageModifier.PLUS) s += "+";
        else if (modifier == DamageModifier.TIMES) s += "×";
        else if (modifier == DamageModifier.MINUS) s += "-";
        return s;
    }

    private List<String> expandEnergyCost(java.util.Map<EnergyType, Integer> cost) {
        if (cost == null) return List.of();
        List<String> result = new ArrayList<>();
        for (java.util.Map.Entry<EnergyType, Integer> entry : cost.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                result.add(entry.getKey().name());
            }
        }
        return result;
    }

    private Set<StatusCondition> requireStatuses(InPlayPokemon pokemon) {
        Set<StatusCondition> statuses = pokemon.getStatuses();
        if (statuses == null) {
            throw new IllegalStateException("In-play Pokemon statuses must not be null");
        }
        return statuses;
    }

    private void ensureActivePokemonSlotIsEmpty(String playerId,
                                                 boolean useAttackerSide,
                                                 GameContext context) {
        InPlayPokemon activePokemon = useAttackerSide ? context.getAttacker() : context.getDefender();
        if (activePokemon != null) {
            throw new IllegalStateException("Active Pokemon is already selected for player: " + playerId);
        }
    }

    private List<Card> requirePlayerHand(boolean useAttackerSide, GameContext context) {
        List<Card> hand = useAttackerSide ? context.getAttackerHand() : context.getDefenderHand();
        if (hand == null) {
            throw new IllegalArgumentException("Player hand must not be null during active selection");
        }
        return hand;
    }

    private InPlayPokemon buildSelectedActivePokemon(List<Card> hand, String cardId) {
        Card selectedCard = hand.stream()
                .filter(Objects::nonNull)
                .filter(card -> Objects.equals(card.getId(), cardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found in player hand: " + cardId));

        if (!(selectedCard instanceof PokemonCard pokemonCard)) {
            throw new IllegalArgumentException("Selected card is not a Pokemon: " + cardId);
        }
        if (!pokemonCard.isBasic()) {
            throw new IllegalArgumentException("Selected active Pokemon must be Basic: " + cardId);
        }

        hand.remove(selectedCard);
        return new InPlayPokemon(pokemonCard);
    }

    /**
     * Helper that builds player-specific views and publishes them via the injected
     * {@link MatchEventPublisher} observer.
     *
     * <p>The views are created using {@link #getMatchState(String, String)} which
     * already guarantees the opposite player's hand is hidden for that view.
     */
    /**
     * Runs bot actions while the current turn belongs to a bot, the match is not
     * finished and the engine is not waiting for an active selection. Bounded by
     * {@link #MAX_BOT_ACTIONS_PER_TURN} to avoid infinite loops in pathological cases.
     */
    private void playBotActionsUntilHumanTurn(Battle match) {
        if (botDecisionService == null) {
            return;
        }

        // Resolve any pending prize selection for a bot, regardless of whose turn it is.
        // After an attack the TurnManager swaps the turn before we get here, so the bot
        // that took the KO is no longer the current turn player even though it still
        // owes a prize pick. Also auto-promote a benched Pokemon if the bot needs one.
        resolveAnyBotPendingSelections(match);

        int safety = 0;
        while (!match.isFinished()
                && !match.isWaitingForActiveSelection()
                && currentTurnPlayerIsBot(match)
                && safety++ < MAX_BOT_ACTIONS_PER_TURN) {
            Player bot = currentTurnPlayer(match);
            GameContext context = match.getContext();

            if (autoResolveBotPrizeSelection(match, bot)) {
                battleRegistry.save(match);
                publishMatchUpdated(match);
                continue;
            }

            if (autoResolveBotPendingChoice(match, bot)) {
                battleRegistry.save(match);
                continue;
            }

            BotDifficulty difficulty = resolveBotDifficulty(bot);
            GameAction action = botDecisionService.chooseAction(difficulty, context);
            log.info("[BOT] {} choosing action: type={}, cardId={}, targetCardId={}, attackName={}, phase={}",
                    bot.getId(), action.getType(), action.getCardId(),
                    action.getTargetCardId(), action.getAttackName(),
                    context.getCurrentPhase());
            try {
                turnManager.submitAction(match, bot.getId(), action);
            } catch (RuntimeException ex) {
                log.warn("[BOT] Action failed for {}: {} — falling back to endTurn",
                        bot.getId(), ex.getMessage());
                turnManager.submitAction(match, bot.getId(), GameAction.endTurn());
                break;
            }
            battleRegistry.save(match);

            // After every bot action, immediately resolve any prize/active selection
            // that just became pending for the bot (e.g. it just KO'd the opponent).
            // Otherwise the turn swap leaves the human stuck behind a bot-owned pick.
            resolveAnyBotPendingSelections(match);
        }

        // Final pass in case the loop exited (turn swap to human) with pending bot picks.
        resolveAnyBotPendingSelections(match);
    }

    private boolean autoResolveBotPrizeSelection(Battle match, Player bot) {
        if (!match.isPendingPrizeSelectionForPlayer(bot.getId())) {
            return false;
        }
        log.info("[BOT] {} auto-selecting prize card", bot.getId());
        selectPrizeInternal(match, bot.getId(), 0);
        return true;
    }

    /**
     * Auto-resolves any pending selections (prize cards, bench promotions) that belong to
     * a bot player. This runs before the main bot-action loop because some pending states
     * persist across turn swaps (e.g. a bot KOs the human → bot gets the prize pick but
     * the turn already swapped to the human, who is now stuck behind the bot's pending pick).
     */
    private void resolveAnyBotPendingSelections(Battle match) {
        if (match.isFinished()) {
            return;
        }
        boolean changed = false;
        for (Player p : List.of(match.getPlayerOne(), match.getPlayerTwo())) {
            if (p == null || !p.isBot()) continue;

            if (match.isPendingPrizeSelectionForPlayer(p.getId())) {
                log.info("[BOT] {} auto-selecting prize card (cross-turn)", p.getId());
                selectPrizeInternal(match, p.getId(), 0);
                changed = true;
            }

            if (match.requiresActiveSelection(p.getId())) {
                int slot = findFirstBenchSlotForPlayer(match, p.getId());
                if (slot >= 0) {
                    log.info("[BOT] {} auto-promoting bench slot {}", p.getId(), slot);
                    promoteFromBenchInternal(match, p.getId(), slot);
                    changed = true;
                }
            }
        }
        if (changed) {
            battleRegistry.save(match);
            publishMatchUpdated(match);
        }
    }

    private int findFirstBenchSlotForPlayer(Battle match, String playerId) {
        GameContext ctx = match.getContext();
        boolean attackerSide = match.isCurrentTurnPlayer(playerId);
        List<InPlayPokemon> bench = attackerSide ? ctx.getAttackerBench() : ctx.getDefenderBench();
        if (bench == null) return -1;
        for (int i = 0; i < bench.size(); i++) {
            if (bench.get(i) != null) return i;
        }
        return -1;
    }

    private void promoteFromBenchInternal(Battle match, String playerId, int benchSlot) {
        GameContext context = match.getContext();
        boolean useAttackerSide = match.isCurrentTurnPlayer(playerId);
        List<InPlayPokemon> bench = useAttackerSide
                ? context.getAttackerBench() : context.getDefenderBench();
        InPlayPokemon promoted = bench.get(benchSlot);
        bench.set(benchSlot, null);
        if (useAttackerSide) {
            context.setAttacker(promoted);
        } else {
            context.setDefender(promoted);
        }
        match.clearPendingActiveSelection(playerId);
        context.addLogEntry(playerId, "PROMOTE",
                promoted.getCard().getName() + " pasó a ser activo");
    }

    private boolean autoResolveBotPendingChoice(Battle match, Player bot) {
        var choice = match.getContext().getPendingPlayerChoice();
        if (choice == null || !choice.getPlayerId().equals(bot.getId())) {
            return false;
        }
        List<String> picked = chooseBotPendingCards(choice);
        log.info("[BOT] {} auto-resolving pending choice: type={}, picked={}",
                bot.getId(), choice.getType(), picked);
        turnManager.submitAction(match, bot.getId(), GameAction.resolvePlayerChoice(picked));
        return true;
    }

    private List<String> chooseBotPendingCards(
            ar.edu.utn.frc.tup.piii.engine.choice.PendingPlayerChoice choice) {
        var options = choice.getOptions();
        int maxPick = choice.getMaxPick();
        if (choice.getType() == ar.edu.utn.frc.tup.piii.engine.choice.ChoiceType.COIN_FLIP_INFO) {
            return List.of();
        }
        List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(maxPick, options.size()); i++) {
            ids.add(options.get(i).getId());
        }
        return ids;
    }

    /**
     * Auto-plays the opening phase actions for bot matches. Covers two cases:
     * (1) the chooser is a bot — the bot picks coin/starter for itself;
     * (2) the chooser is a human but the opponent is a bot — auto-picks the
     * starter on behalf of the human so the human is not stuck on a UI that
     * never asks them to choose. Both cases are safe because there is no
     * second human to disadvantage in a bot match.
     */
    private void autoPlayBotOpeningIfNeeded(Battle match) {
        int safety = 0;
        while (match.isOpeningInProgress() && safety++ < 4) {
            String chooserId = match.getOpeningChooserPlayerId();
            if (chooserId == null) return;
            Player chooser = null;
            Player opponent = null;
            if (Objects.equals(chooserId, match.getPlayerOne().getId())) {
                chooser = match.getPlayerOne();
                opponent = match.getPlayerTwo();
            } else if (Objects.equals(chooserId, match.getPlayerTwo().getId())) {
                chooser = match.getPlayerTwo();
                opponent = match.getPlayerOne();
            }
            if (chooser == null) return;

            boolean isBotMatch = chooser.isBot() || (opponent != null && opponent.isBot());
            if (!isBotMatch) return;

            String phase = match.getOpeningPhase();
            if ("COIN_CHOICE".equals(phase)) {
                if (!chooser.isBot()) return;
                chooseCoinSide(match.getId(), chooserId, "heads");
            } else if ("COIN_RESULT".equals(phase)) {
                chooseFirstTurn(match.getId(), chooserId, chooserId);
                return;
            } else {
                return;
            }
        }
    }

    private void autoResolveMulliganIfBot(Battle match) {
        String beneficiaryId = match.getMulliganBeneficiaryPlayerId();
        if (beneficiaryId == null) return;
        Player beneficiary = Objects.equals(match.getPlayerOne().getId(), beneficiaryId)
                ? match.getPlayerOne() : match.getPlayerTwo();
        if (!beneficiary.isBot()) return;

        int count = match.getMulliganBenefitCount();
        GameContext context = match.getContext();
        boolean beneficiaryIsAttacker = match.isCurrentTurnPlayer(beneficiaryId);

        for (int i = 0; i < count; i++) {
            if (beneficiaryIsAttacker) {
                context.attackerDrawCard();
            } else {
                context.defenderDrawCard();
            }
        }

        match.setMulliganBenefitChosen(count);
        match.setMulliganResolutionPending(false);

        placePrizeCards(context.getAttackerDeck(), context.getAttackerPrizes());
        placePrizeCards(context.getDefenderDeck(), context.getDefenderPrizes());
        context.setPrizesDealt(true);
        match.setSetupComplete(true);
        battleRegistry.save(match);
        playBotActionsUntilHumanTurn(match);
    }

    private void autoSetupBotIfNeeded(Battle match) {
        for (Player player : List.of(match.getPlayerOne(), match.getPlayerTwo())) {
            if (!player.isBot()) continue;
            if (match.isSetupReady(player.getId())) continue;

            autoSelectActiveForBots(match);
            match.markSetupReady(player.getId());
            battleRegistry.save(match);
        }
    }

    /**
     * If any bot still needs to choose their active Pokémon, picks the first Basic
     * in their hand automatically.
     */
    private void autoSelectActiveForBots(Battle match) {
        for (Player player : List.of(match.getPlayerOne(), match.getPlayerTwo())) {
            if (!player.isBot()) continue;
            if (!match.requiresActiveSelection(player.getId())) continue;

            GameContext context = match.getContext();
            boolean useAttackerSide = match.isCurrentTurnPlayer(player.getId());
            List<Card> hand = useAttackerSide ? context.getAttackerHand() : context.getDefenderHand();
            if (hand == null) continue;

            Card chosen = null;
            for (Card card : hand) {
                if (card instanceof PokemonCard pokemon && pokemon.isBasic()) {
                    chosen = card;
                    break;
                }
            }
            if (chosen == null) continue;

            InPlayPokemon active = buildSelectedActivePokemon(hand, chosen.getId());
            if (useAttackerSide) {
                context.setAttacker(active);
            } else {
                context.setDefender(active);
            }
            match.clearPendingActiveSelection(player.getId());
            battleRegistry.save(match);
        }
    }

    private boolean currentTurnPlayerIsBot(Battle match) {
        Player current = currentTurnPlayer(match);
        return current != null && current.isBot();
    }

    private Player currentTurnPlayer(Battle match) {
        String currentId = match.getCurrentTurnPlayerId();
        if (Objects.equals(currentId, match.getPlayerOne().getId())) {
            return match.getPlayerOne();
        }
        if (Objects.equals(currentId, match.getPlayerTwo().getId())) {
            return match.getPlayerTwo();
        }
        return null;
    }

    private BotDifficulty resolveBotDifficulty(Player bot) {
        if (bot == null || bot.getId() == null) {
            return BotDifficulty.EASY;
        }
        // Convention: bot ids are bot-easy / bot-medium / bot-hard
        String id = bot.getId();
        if (id.startsWith("bot-")) {
            return BotDifficulty.fromString(id.substring("bot-".length()));
        }
        return BotDifficulty.EASY;
    }

    private void publishPendingGameEvents(Battle match) {
        if (matchEventPublisher == null || match.getContext() == null) {
            return;
        }
        String engineId = match.getId();
        String entityMatchId = match.getEntityMatchId();
        String matchId = entityMatchId != null ? entityMatchId : engineId;
        String p1Id = match.getPlayerOne().getId();
        String p2Id = match.getPlayerTwo().getId();
        for (var event : match.getContext().drainPendingEvents()) {
            matchEventPublisher.onMatchGameEvent(matchId, p1Id, p2Id, event);
        }
    }

    private void publishMatchUpdated(Battle match) {
        if (matchEventPublisher == null) {
            return;
        }

        GameContext context = match.getContext();
        Card lastPlayedCard = context.getLastPlayedCard();
        String lastPlayedCardPlayerId = context.getLastPlayedCardPlayerId();

        String engineId = match.getId();
        String entityMatchId = match.getEntityMatchId();
        String matchId = entityMatchId != null ? entityMatchId : engineId;
        String playerOneId = match.getPlayerOne().getId();
        String playerTwoId = match.getPlayerTwo().getId();

        MatchStateDTO playerOneView = getMatchState(engineId, playerOneId);
        MatchStateDTO playerTwoView = getMatchState(engineId, playerTwoId);

        if (lastPlayedCard != null) {
            MatchStateDTO.CardViewDTO cardView = mapSingleCard(lastPlayedCard);
            playerOneView.setLastPlayedCard(cardView);
            playerOneView.setLastPlayedCardPlayerId(lastPlayedCardPlayerId);
            playerTwoView.setLastPlayedCard(cardView);
            playerTwoView.setLastPlayedCardPlayerId(lastPlayedCardPlayerId);
            context.setLastPlayedCard(null);
            context.setLastPlayedCardPlayerId(null);
        }

        MatchUpdatedEvent event = new MatchUpdatedEvent(matchId, playerOneView, playerTwoView);
        matchEventPublisher.onMatchUpdated(event);
    }

    private void publishMulliganEvent(MulliganEvent event) {
        if (matchEventPublisher != null) {
            matchEventPublisher.onMulligan(event);
        }
    }
}
