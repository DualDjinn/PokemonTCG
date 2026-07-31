package ar.edu.utn.frc.tup.piii.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.engine.bot.BotDecisionService;
import ar.edu.utn.frc.tup.piii.engine.bot.BotDifficulty;
import ar.edu.utn.frc.tup.piii.engine.choice.ChoiceType;
import ar.edu.utn.frc.tup.piii.engine.choice.PendingPlayerChoice;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.TrainerEffectRegistry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the bot auto-resolution methods on {@link GameEngine}.
 *
 * <p>These methods exist because bots cannot "click" the UI prompts that
 * humans use to take prize cards, promote a benched Pokémon after a KO, or
 * resolve a trainer-card {@link PendingPlayerChoice}. The engine resolves
 * those choices on the bot's behalf inside {@code playBotActionsUntilHumanTurn}
 * so the match keeps flowing instead of stalling.
 *
 * <p>The bug history that motivated this coverage:
 * <ul>
 *   <li>Bot KOs the human → {@code TurnManager} swaps the turn before the
 *       engine can read the pending prize selection, so the human gets
 *       blocked behind a bot-owned pick. {@code resolveAnyBotPendingSelections}
 *       fixes it by scanning both players, not just the current turn player.</li>
 *   <li>Bot is left without an active Pokémon (e.g. its own KO) → engine
 *       must auto-promote one from the bench, otherwise the match deadlocks
 *       waiting for an active that the bot will never pick.</li>
 *   <li>Trainer card creates a {@link PendingPlayerChoice} addressed to the
 *       bot (Great Ball, Roller Skates) → bot must resolve it automatically,
 *       picking the simplest valid response.</li>
 * </ul>
 *
 * <p>The tests stay JUnit-only (no Spring context). The {@link BotDecisionService}
 * is wired via reflection because the {@code @Autowired(required = false)} field
 * cannot be injected through the package-private test constructor.
 */
class GameEngineBotResolutionTest {

    private static final int PLAYABLE_DECK_SIZE = 60;
    private static final int POKEMON_COPIES_PER_SLOT = 4;

    private GameEngine gameEngine;
    private BattleRegistry battleRegistry;

    @BeforeEach
    void setUp() throws Exception {
        battleRegistry = new BattleRegistry();
        TriggeredAbilityDispatcher triggeredAbilityDispatcher = new TriggeredAbilityDispatcher();
        AttackResolutionPipeline attackResolutionPipeline = new AttackResolutionPipeline(
                new DamageCalculator(),
                new CardEffectRegistry(),
                new PassiveAbilityHandler(),
                triggeredAbilityDispatcher
        );
        TurnManager turnManager = new TurnManager(
                new RuleValidator(),
                attackResolutionPipeline,
                triggeredAbilityDispatcher,
                new StatusEffectManager(),
                new VictoryConditionChecker(),
                new TrainerEffectRegistry(),
                new AbilityRegistry()
        );
        gameEngine = new GameEngine(battleRegistry, turnManager);
        // Force the auto-resolution loop on: when botDecisionService is null,
        // playBotActionsUntilHumanTurn short-circuits before any helper runs.
        // We use a stub that always picks END_TURN so the bot does not chain
        // unrelated actions that would mask the state we want to assert.
        injectBotDecisionService(new BotDecisionService() {
            @Override
            public GameAction chooseAction(BotDifficulty difficulty, GameContext context) {
                return GameAction.endTurn();
            }
        });
    }

    // ─────────────────────── selectPrize public API ───────────────────────────

    @Test
    void selectPrize_movesPrizeCardToHandAndClearsPending() {
        // The human just KO'd a bot Pokémon and is choosing a prize. This is the
        // base path through selectPrize → selectPrizeInternal.
        Battle match = setupPlayableMatchWithBot();
        Player human = match.getPlayerOne();
        int prizesBefore = match.getContext().getAttackerPrizes().size();
        int handBefore = match.getContext().getAttackerHand().size();
        Card targetPrize = match.getContext().getAttackerPrizes().get(0);
        match.setPendingPrizeSelectionPlayerId(human.getId());

        gameEngine.selectPrize(match.getId(), human.getId(), 0);

        assertThat(match.getContext().getAttackerPrizes()).hasSize(prizesBefore - 1);
        assertThat(match.getContext().getAttackerHand())
                .hasSize(handBefore + 1)
                .contains(targetPrize);
        assertThat(match.isPendingPrizeSelectionForPlayer(human.getId())).isFalse();
    }

    @Test
    void selectPrize_rejectsActionWhenNoPrizeSelectionIsPending() {
        // Guard rail in selectPrize: cannot pick a prize when none is owed.
        Battle match = setupPlayableMatchWithBot();
        Player human = match.getPlayerOne();
        // pendingPrizeSelectionPlayerId stays null after setup.

        assertThatThrownBy(() -> gameEngine.selectPrize(match.getId(), human.getId(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not pending prize selection");
    }

    @Test
    void selectPrize_rejectsOutOfBoundsIndex() {
        // selectPrizeInternal validates the prize index range.
        Battle match = setupPlayableMatchWithBot();
        Player human = match.getPlayerOne();
        match.setPendingPrizeSelectionPlayerId(human.getId());
        int outOfBounds = match.getContext().getAttackerPrizes().size() + 5;

        assertThatThrownBy(() -> gameEngine.selectPrize(match.getId(), human.getId(), outOfBounds))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid prize index");
    }

    // ─────────────────── resolveAnyBotPendingSelections: prize ─────────────────

    @Test
    void botPendingPrize_isAutoResolvedFromHumanActionTrigger() {
        // The bot owes a prize pick after KO'ing the human. The turn was
        // already swapped to the human, so currentTurnPlayerIsBot is false.
        // resolveAnyBotPendingSelections must still resolve it.
        Battle match = setupPlayableMatchWithBot();
        Player bot = match.getPlayerTwo();
        // The bot is the defender after setup (player one starts). To use
        // the bot's prize pile we read it from the defender side.
        int prizesBefore = match.getContext().getDefenderPrizes().size();
        int handBefore = match.getContext().getDefenderHand().size();
        match.setPendingPrizeSelectionPlayerId(bot.getId());

        triggerBotResolutionLoop(match);

        // After the trigger, the bot's prize was auto-taken.
        assertThat(match.isPendingPrizeSelectionForPlayer(bot.getId())).isFalse();
        assertThat(match.getContext().getDefenderPrizes()).hasSize(prizesBefore - 1);
        assertThat(match.getContext().getDefenderHand()).hasSize(handBefore + 1);
    }

    // ─────────────────── resolveAnyBotPendingSelections: bench ─────────────────

    @Test
    void botMissingActivePokemon_isAutoPromotedFromBench() {
        // The bot's active Pokémon was knocked out and the bench still has at
        // least one Pokémon. The engine must auto-promote the first non-null
        // slot when the human triggers any action.
        Battle match = setupPlayableMatchWithBot();
        Player bot = match.getPlayerTwo();
        Player human = match.getPlayerOne();

        // Move the bot's active Pokémon away and put a placeholder on its bench.
        GameContext ctx = match.getContext();
        InPlayPokemon previouslyActive = ctx.getDefender();
        ctx.setDefender(null);
        InPlayPokemon benchedReplacement = new InPlayPokemon(
                pokemonCard("bench-replace", "BenchedHero", 60));
        ctx.getDefenderBench().add(benchedReplacement);
        match.markPendingActiveSelection(bot.getId());

        triggerBotResolutionLoop(match);

        // The bot has an active again and no longer needs to choose one.
        assertThat(ctx.getDefender()).isSameAs(benchedReplacement);
        assertThat(match.requiresActiveSelection(bot.getId())).isFalse();
        assertThat(ctx.getDefenderBench()).doesNotContain(benchedReplacement);
        // The previously-active reference is still null (we discarded it manually).
        assertThat(previouslyActive).isNotNull(); // sanity on the helper
    }

    @Test
    void botMissingActivePokemon_withEmptyBench_doesNotPromoteAndStaysPending() {
        // Defensive branch of findFirstBenchSlotForPlayer: the bot has no
        // active AND no benched Pokémon, so the engine must NOT throw and
        // must leave the pending active selection in place.
        Battle match = setupPlayableMatchWithBot();
        Player bot = match.getPlayerTwo();
        Player human = match.getPlayerOne();

        GameContext ctx = match.getContext();
        ctx.setDefender(null);
        // Strip the bench of any non-null entries.
        ctx.getDefenderBench().replaceAll(p -> null);
        match.markPendingActiveSelection(bot.getId());

        // The trigger should not raise even though the bot cannot be promoted.
        triggerBotResolutionLoop(match);

        assertThat(ctx.getDefender()).isNull();
        assertThat(match.requiresActiveSelection(bot.getId())).isTrue();
    }

    // ─────────────────────── autoResolveBotPendingChoice ──────────────────────

    @Test
    void botPendingPlayerChoice_isAutoResolvedDuringTurn() {
        // Trainer cards may raise a PendingPlayerChoice. When the chooser is
        // a bot, chooseBotPendingCards picks the first maxPick options and the
        // engine submits a RESOLVE_PLAYER_CHOICE action on the bot's behalf.
        Battle match = setupPlayableMatchWithBot();
        Player bot = match.getPlayerTwo();

        // Force the current turn to the bot so the while loop entry conditions
        // (currentTurnPlayerIsBot) are satisfied.
        forceCurrentTurnPlayer(match, bot.getId());

        GameContext ctx = match.getContext();
        Card option1 = pokemonCard("choice-opt-1", "OptionOne", 60);
        Card option2 = pokemonCard("choice-opt-2", "OptionTwo", 60);
        PendingPlayerChoice choice = new PendingPlayerChoice(
                bot.getId(),
                ChoiceType.PICK_FROM_DECK_TOP_N,
                List.of(option1, option2),
                0,                  // minPick
                1,                  // maxPick
                "test-prompt",
                null,               // coinFlipHeads
                picks -> { }        // callback no-op for the test
        );
        ctx.setPendingPlayerChoice(choice);

        triggerBotResolutionLoop(match);

        // After the trigger, the choice was resolved on the bot's behalf.
        assertThat(ctx.getPendingPlayerChoice()).isNull();
    }

    @Test
    void botPendingChoice_coinFlipInfo_resolvesWithEmptyPick() {
        // COIN_FLIP_INFO is a notification-only choice: chooseBotPendingCards
        // intentionally returns an empty list. The engine still submits the
        // resolve action and clears the pending state.
        Battle match = setupPlayableMatchWithBot();
        Player bot = match.getPlayerTwo();
        forceCurrentTurnPlayer(match, bot.getId());

        GameContext ctx = match.getContext();
        PendingPlayerChoice choice = new PendingPlayerChoice(
                bot.getId(),
                ChoiceType.COIN_FLIP_INFO,
                List.of(pokemonCard("coin-info", "InfoCard", 60)),
                0,                  // minPick
                0,                  // maxPick (info-only, no picks)
                "coin-flip",
                Boolean.TRUE,       // coinFlipHeads — the predetermined result
                picks -> { }        // callback no-op
        );
        ctx.setPendingPlayerChoice(choice);

        triggerBotResolutionLoop(match);

        assertThat(ctx.getPendingPlayerChoice()).isNull();
    }

    // ─────────────────────────── getMatchState smoke ──────────────────────────

    @Test
    void getMatchState_afterSetup_reportsPlayableState() {
        // Sanity that setupPlayableMatchWithBot leaves a coherent state usable
        // by the rest of the test suite. Without this, failures in other tests
        // would be hard to attribute to setup vs the code under test.
        Battle match = setupPlayableMatchWithBot();
        Player human = match.getPlayerOne();
        MatchStateDTO state = gameEngine.getMatchState(match.getId(), human.getId());

        assertThat(state).isNotNull();
        assertThat(state.getSelf()).isNotNull();
        assertThat(state.getOpponent()).isNotNull();
        assertThat(state.isFinished()).isFalse();
    }

    // ──────────────────────────────── helpers ─────────────────────────────────

    /**
     * Builds a match between a human and a bot, deals cards and selects an
     * active for both sides so the match is in a playable state. Returns the
     * underlying {@link Battle} so tests can mutate its internal state to
     * recreate edge cases.
     */
    private Battle setupPlayableMatchWithBot() {
        Player human = createHuman("human-" + UUID.randomUUID());
        Player bot = createBot("bot-easy-" + UUID.randomUUID());
        // The engine reads bot difficulty from the id prefix "bot-", so override
        // the id after construction to keep matchmaking unique while preserving
        // the prefix that BotDifficulty.fromString expects.
        bot.setId("bot-easy");
        Deck humanDeck = human.getDecks().get(0);
        Deck botDeck = bot.getDecks().get(0);

        String matchId = gameEngine.createMatch(human, bot, humanDeck, botDeck);
        gameEngine.dealCards(matchId);

        Battle match = battleRegistry.getBattleOrThrow(matchId);
        // createMatch picks the first turn player at random. The GameContext is
        // oriented around the current turn player, so we read each side's hand
        // dynamically rather than assuming the human is the attacker.
        Card humanActive = firstBasicInHand(handOf(match, human));
        gameEngine.selectActivePokemon(matchId, human.getId(), humanActive.getId());

        Card botActive = firstBasicInHand(handOf(match, bot));
        gameEngine.selectActivePokemon(matchId, bot.getId(), botActive.getId());

        gameEngine.confirmSetupReady(matchId, human.getId());
        gameEngine.confirmSetupReady(matchId, bot.getId());

        // Normalize: tests below assume the human is the current turn player so
        // they can read attacker/defender sides without dynamic resolution.
        if (!match.isCurrentTurnPlayer(human.getId())) {
            match.swapTurn();
        }
        return match;
    }

    /**
     * Causes the engine's bot-resolution loop to run. The cleanest reentry point
     * in the public API is {@code selectActivePokemon}, but that requires the
     * human's active slot to be empty. We clear it and re-flag the human as
     * pending active so the trigger call is valid. The bot's pending state is
     * the actual subject under test — the human's pending-active dance is just
     * the trip wire.
     *
     * <p>{@link GameContext}'s {@code attacker}/{@code defender} fields are
     * oriented from the current turn player's perspective. If the test forced
     * the bot to be the current turn player, then the human is on the defender
     * side and we need to clear/refill from there.
     */
    private void triggerBotResolutionLoop(Battle match) {
        Player human = match.getPlayerOne();
        GameContext ctx = match.getContext();
        boolean humanIsCurrentSide = match.isCurrentTurnPlayer(human.getId());
        if (humanIsCurrentSide) {
            ctx.setAttacker(null);
        } else {
            ctx.setDefender(null);
        }
        match.markPendingActiveSelection(human.getId());
        List<Card> humanHand = humanIsCurrentSide ? ctx.getAttackerHand() : ctx.getDefenderHand();
        Card basicInHand = firstBasicInHand(humanHand);
        gameEngine.selectActivePokemon(match.getId(), human.getId(), basicInHand.getId());
    }

    private void forceCurrentTurnPlayer(Battle match, String playerId) {
        // The engine alternates turns via TurnManager; for tests we sometimes
        // need a specific player as the current turn regardless of what the
        // engine decided.
        if (!playerId.equals(match.getCurrentTurnPlayerId())) {
            match.swapTurn();
        }
    }

    /**
     * Returns the in-context hand for the given player, picking the correct side
     * of the oriented {@link GameContext}. Use this instead of {@code getAttackerHand}
     * when the test cannot assume who is the current turn player.
     */
    private List<Card> handOf(Battle match, Player player) {
        GameContext ctx = match.getContext();
        return match.isCurrentTurnPlayer(player.getId())
                ? ctx.getAttackerHand()
                : ctx.getDefenderHand();
    }

    private Card firstBasicInHand(List<Card> hand) {
        return hand.stream()
                .filter(c -> c instanceof PokemonCard pc && pc.getStage() == PokemonStage.BASIC)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No basic in hand"));
    }

    private Player createHuman(String playerId) {
        Player player = new Player();
        player.setId(playerId);
        player.setUsername(playerId);
        player.setTrainerName(playerId);
        player.setEmail(playerId + "@test.dev");
        player.setPassword("secret");
        player.setBot(false);
        player.setDecks(List.of(playableDeck(playerId + "-deck")));
        return player;
    }

    private Player createBot(String playerId) {
        Player player = new Player();
        player.setId(playerId);
        player.setUsername(playerId);
        player.setTrainerName(playerId);
        player.setEmail(playerId + "@test.dev");
        player.setPassword("secret");
        player.setBot(true);
        player.setDecks(List.of(playableDeck(playerId + "-deck")));
        return player;
    }

    private Deck playableDeck(String deckId) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; cards.size() < PLAYABLE_DECK_SIZE; i++) {
            for (int copy = 0; copy < POKEMON_COPIES_PER_SLOT && cards.size() < PLAYABLE_DECK_SIZE; copy++) {
                cards.add(pokemonCard(
                        deckId + "-basic-" + i + "-copy-" + copy,
                        "Basic " + i,
                        60));
            }
        }
        Deck deck = new Deck();
        deck.setId(deckId);
        deck.setName("Playable Deck");
        deck.setCards(cards);
        return deck;
    }

    private PokemonCard pokemonCard(String id, String name, int hp) {
        return PokemonCard.builder()
                .id(id)
                .name(name)
                .hp(hp)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.COLORLESS)
                .attacks(List.of())
                .abilities(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .costToAttack(List.of())
                .retreatCost(0)
                .build();
    }

    private void injectBotDecisionService(BotDecisionService service) throws Exception {
        Field field = GameEngine.class.getDeclaredField("botDecisionService");
        field.setAccessible(true);
        field.set(gameEngine, service);
    }
}
