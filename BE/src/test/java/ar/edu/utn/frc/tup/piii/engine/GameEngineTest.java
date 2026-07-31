package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.TrainerEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GameEngine}.
 */
class GameEngineTest {

    private static final int OPENING_HAND_SIZE = 7;
    private static final int PLAYABLE_DECK_SIZE = 60;
    private static final int POKEMON_COPIES_PER_SLOT = 4;

    private GameEngine gameEngine;

    @BeforeEach
    void setUp() {
        BattleRegistry matchService = new BattleRegistry();

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
        gameEngine = new GameEngine(matchService, turnManager);
    }

    @Test
    void createMatch_InitializesOpeningStateAndReturnsPerspectiveSafeView() {
        Player playerOne = createPlayer("player-one");
        Player playerTwo = createPlayer("player-two");
        Deck deckOne = playerOne.getDecks().get(0);
        Deck deckTwo = playerTwo.getDecks().get(0);

        String matchId = gameEngine.createMatch(playerOne, playerTwo, deckOne, deckTwo);
        MatchStateDTO beforeDeal = gameEngine.getMatchState(matchId, playerOne.getId());

        assertThat(matchId).isNotBlank();
        assertThat(beforeDeal.getMatchId()).isEqualTo(matchId);
        assertThat(beforeDeal.getCurrentTurnPlayerId()).isEqualTo(playerOne.getId());
        assertThat(beforeDeal.getCurrentPhase()).isEqualTo(TurnPhase.MAIN);
        assertThat(beforeDeal.isCurrentTurn()).isTrue();
        assertThat(beforeDeal.isWaitingForActiveSelection()).isTrue();
        assertThat(beforeDeal.isRequiresActiveSelection()).isTrue();
        assertThat(beforeDeal.isFinished()).isFalse();

        // After createMatch, hands and prizes are empty (dealt later via dealCards)
        assertThat(beforeDeal.getSelf().getHand()).isEmpty();
        assertThat(beforeDeal.getSelf().getHandSize()).isEqualTo(0);
        assertThat(beforeDeal.getSelf().getRemainingPrizes()).isEqualTo(0);
        assertThat(beforeDeal.getSelf().getActivePokemon()).isNull();
        assertThat(beforeDeal.getSelf().getBench()).isEmpty();

        assertThat(beforeDeal.getOpponent().getHand()).isEmpty();
        assertThat(beforeDeal.getOpponent().getHandSize()).isEqualTo(0);
        assertThat(beforeDeal.getOpponent().getRemainingPrizes()).isEqualTo(0);
        assertThat(beforeDeal.getOpponent().getActivePokemon()).isNull();
        assertThat(beforeDeal.getOpponent().getBench()).isEmpty();

        gameEngine.dealCards(matchId);
        MatchStateDTO afterDeal = gameEngine.getMatchState(matchId, playerOne.getId());

        assertThat(afterDeal.getSelf().getHand()).hasSize(OPENING_HAND_SIZE);
        assertThat(afterDeal.getSelf().getHandSize()).isEqualTo(OPENING_HAND_SIZE);
        assertThat(afterDeal.getSelf().getRemainingPrizes()).isEqualTo(0);
        assertThat(afterDeal.getOpponent().getHand()).isEmpty();
        assertThat(afterDeal.getOpponent().getHandSize()).isEqualTo(OPENING_HAND_SIZE);
        assertThat(afterDeal.getOpponent().getRemainingPrizes()).isEqualTo(0);
    }

    @Test
    void createMatch_WhenUsingFacadeConstructor_WiresPureEngineCollaboratorsSuccessfully() {
        GameEngine facadeWiredGameEngine = new GameEngine(new BattleRegistry());
        Player playerOne = createPlayer("player-one");
        Player playerTwo = createPlayer("player-two");
        Deck deckOne = playerOne.getDecks().get(0);
        Deck deckTwo = playerTwo.getDecks().get(0);

        String matchId = facadeWiredGameEngine.createMatch(playerOne, playerTwo, deckOne, deckTwo);
        MatchStateDTO beforeDeal = facadeWiredGameEngine.getMatchState(matchId, playerOne.getId());

        assertThat(matchId).isNotBlank();
        assertThat(beforeDeal.getCurrentTurnPlayerId()).isEqualTo(playerOne.getId());
        assertThat(beforeDeal.getSelf().getHandSize()).isEqualTo(0);
        assertThat(beforeDeal.getOpponent().getRemainingPrizes()).isEqualTo(0);

        facadeWiredGameEngine.dealCards(matchId);
        MatchStateDTO afterDeal = facadeWiredGameEngine.getMatchState(matchId, playerOne.getId());

        assertThat(afterDeal.getSelf().getHandSize()).isEqualTo(OPENING_HAND_SIZE);
        assertThat(afterDeal.getOpponent().getRemainingPrizes()).isEqualTo(0);
    }

    @Test
    void submitAction_WhenMatchDoesNotExist_ThrowsConsistentError() {
        String missingMatchId = "missing-match";
        String playerId = "player-one";
        GameAction action = GameAction.endTurn();

        assertThatThrownBy(() -> gameEngine.submitAction(missingMatchId, playerId, action))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Battle not found: missing-match");
    }

    @Test
    void submitAction_WhenPlayerActsOutOfTurn_RejectsTheAction() {
        Player playerOne = createPlayer("player-one");
        Player playerTwo = createPlayer("player-two");
        Deck deckOne = playerOne.getDecks().get(0);
        Deck deckTwo = playerTwo.getDecks().get(0);
        String matchId = gameEngine.createMatch(playerOne, playerTwo, deckOne, deckTwo);
        gameEngine.dealCards(matchId);
        gameEngine.selectActivePokemon(matchId, playerOne.getId(),
                gameEngine.getMatchState(matchId, playerOne.getId()).getSelf().getHand().getFirst().getId());
        gameEngine.selectActivePokemon(matchId, playerTwo.getId(),
                gameEngine.getMatchState(matchId, playerTwo.getId()).getSelf().getHand().getFirst().getId());
        gameEngine.confirmSetupReady(matchId, playerOne.getId());
        gameEngine.confirmSetupReady(matchId, playerTwo.getId());
        String actingPlayerId = playerTwo.getId();
        GameAction action = GameAction.endTurn();

        assertThatThrownBy(() -> gameEngine.submitAction(matchId, actingPlayerId, action))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("It is not the turn of player: " + playerTwo.getId());
    }

    @Test
    void selectActivePokemon_RevealsChosenActiveAndKeepsOpponentSelectionPending() {
        Player playerOne = createBattleReadyPlayer("player-one", "Spark", 30, 100);
        Player playerTwo = createBattleReadyPlayer("player-two", "Punch", 10, 120);
        Deck deckOne = playerOne.getDecks().get(0);
        Deck deckTwo = playerTwo.getDecks().get(0);
        String matchId = gameEngine.createMatch(playerOne, playerTwo, deckOne, deckTwo);
        gameEngine.dealCards(matchId);
        MatchStateDTO playerOneState = gameEngine.getMatchState(matchId, playerOne.getId());
        String activeCardId = playerOneState.getSelf().getHand().getFirst().getId();

        gameEngine.selectActivePokemon(
                matchId,
                playerOne.getId(),
                activeCardId
        );

        MatchStateDTO updatedPlayerOneState = gameEngine.getMatchState(matchId, playerOne.getId());
        MatchStateDTO updatedPlayerTwoState = gameEngine.getMatchState(matchId, playerTwo.getId());

        assertThat(updatedPlayerOneState.isWaitingForActiveSelection()).isTrue();
        assertThat(updatedPlayerOneState.isRequiresActiveSelection()).isFalse();
        assertThat(updatedPlayerOneState.getSelf().getActivePokemon()).isNotNull();
        assertThat(updatedPlayerOneState.getSelf().getHandSize()).isEqualTo(OPENING_HAND_SIZE - 1);
        assertThat(updatedPlayerTwoState.isRequiresActiveSelection()).isTrue();
    }

    @Test
    void submitAction_AttackAppliesDamageAndPassesTurnThroughFacade() {
        Player playerOne = createBattleReadyPlayer("player-one", "Spark", 30, 100);
        Player playerTwo = createBattleReadyPlayer("player-two", "Punch", 10, 120);
        Deck deckOne = playerOne.getDecks().get(0);
        Deck deckTwo = playerTwo.getDecks().get(0);
        String matchId = gameEngine.createMatch(playerOne, playerTwo, deckOne, deckTwo);
        gameEngine.dealCards(matchId);
        MatchStateDTO playerOneState = gameEngine.getMatchState(matchId, playerOne.getId());
        MatchStateDTO playerTwoState = gameEngine.getMatchState(matchId, playerTwo.getId());
        String playerOneActiveId = playerOneState.getSelf().getHand().getFirst().getId();
        String playerTwoActiveId = playerTwoState.getSelf().getHand().getFirst().getId();

        gameEngine.selectActivePokemon(
                matchId,
                playerOne.getId(),
                playerOneActiveId
        );
        gameEngine.selectActivePokemon(
                matchId,
                playerTwo.getId(),
                playerTwoActiveId
        );
        gameEngine.confirmSetupReady(matchId, playerOne.getId());
        gameEngine.confirmSetupReady(matchId, playerTwo.getId());

        // El primer jugador no puede atacar en su turno de apertura (regla XY: firstTurn).
        // Pasamos el turno de apertura y el del rival para que player-one ataque en un turno normal.
        gameEngine.submitAction(matchId, playerOne.getId(), GameAction.endTurn());
        gameEngine.submitAction(matchId, playerTwo.getId(), GameAction.endTurn());

        gameEngine.submitAction(matchId, playerOne.getId(), GameAction.advanceToAttack());

        gameEngine.submitAction(matchId, playerOne.getId(), GameAction.attack("Spark"));
        MatchStateDTO defenderPerspective = gameEngine.getMatchState(matchId, playerTwo.getId());

        assertThat(defenderPerspective.getCurrentTurnPlayerId()).isEqualTo(playerTwo.getId());
        assertThat(defenderPerspective.getCurrentPhase()).isEqualTo(TurnPhase.MAIN);
        assertThat(defenderPerspective.isCurrentTurn()).isTrue();
        assertThat(defenderPerspective.getSelf().getActivePokemon().getDamageTaken()).isEqualTo(30);
        assertThat(defenderPerspective.getSelf().getActivePokemon().getCurrentHp()).isEqualTo(90);
    }

    @Test
    void submitAction_AttackWithoutAdvancingToAttackPhase_RejectsAction() {
        Player playerOne = createBattleReadyPlayer("player-one", "Spark", 30, 100);
        Player playerTwo = createBattleReadyPlayer("player-two", "Punch", 10, 120);
        Deck deckOne = playerOne.getDecks().get(0);
        Deck deckTwo = playerTwo.getDecks().get(0);
        String matchId = gameEngine.createMatch(playerOne, playerTwo, deckOne, deckTwo);
        gameEngine.dealCards(matchId);
        MatchStateDTO playerOneState = gameEngine.getMatchState(matchId, playerOne.getId());
        MatchStateDTO playerTwoState = gameEngine.getMatchState(matchId, playerTwo.getId());
        String playerOneActiveId = playerOneState.getSelf().getHand().getFirst().getId();
        String playerTwoActiveId = playerTwoState.getSelf().getHand().getFirst().getId();

        gameEngine.selectActivePokemon(matchId, playerOne.getId(), playerOneActiveId);
        gameEngine.selectActivePokemon(matchId, playerTwo.getId(), playerTwoActiveId);
        gameEngine.confirmSetupReady(matchId, playerOne.getId());
        gameEngine.confirmSetupReady(matchId, playerTwo.getId());

        assertThatThrownBy(() -> gameEngine.submitAction(matchId, playerOne.getId(), GameAction.attack("Spark")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Attack cannot be declared before entering the attack phase");
    }

    private Player createPlayer(String playerId) {
        Player player = new Player();
        player.setId(playerId);
        player.setUsername(playerId);
        player.setTrainerName(playerId);
        player.setEmail(playerId + "@test.dev");
        player.setPassword("secret");
        player.setDecks(List.of(createPlayableDeck(playerId + "-deck")));
        return player;
    }

    private Player createBattleReadyPlayer(String playerId,
                                           String attackName,
                                           int baseDamage,
                                           int hp) {
        Player player = new Player();
        player.setId(playerId);
        player.setUsername(playerId);
        player.setTrainerName(playerId);
        player.setEmail(playerId + "@test.dev");
        player.setPassword("secret");
        player.setDecks(List.of(createBattleReadyDeck(
                playerId + "-deck",
                attackName,
                baseDamage,
                hp
        )));
        return player;
    }

    private ar.edu.utn.frc.tup.piii.models.Deck createPlayableDeck(String deckId) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; cards.size() < PLAYABLE_DECK_SIZE; i++) {
            addBasicPokemonCopies(cards, deckId, i);
        }

        ar.edu.utn.frc.tup.piii.models.Deck deck = new ar.edu.utn.frc.tup.piii.models.Deck();
        deck.setId(deckId);
        deck.setName("Playable Deck");
        deck.setCards(cards);
        return deck;
    }

    private ar.edu.utn.frc.tup.piii.models.Deck createBattleReadyDeck(String deckId,
                                                                       String attackName,
                                                                       int baseDamage,
                                                                       int hp) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; cards.size() < PLAYABLE_DECK_SIZE; i++) {
            cards.add(createBattlePokemon(
                    deckId + "-battle-" + i,
                    "Battle " + i,
                    hp,
                    attackName,
                    baseDamage
            ));
        }

        ar.edu.utn.frc.tup.piii.models.Deck deck = new ar.edu.utn.frc.tup.piii.models.Deck();
        deck.setId(deckId);
        deck.setName("Battle Ready Deck");
        deck.setCards(cards);
        return deck;
    }

    private void addBasicPokemonCopies(List<Card> cards, String deckId, int slotIndex) {
        for (int copyIndex = 0;
             copyIndex < POKEMON_COPIES_PER_SLOT && cards.size() < PLAYABLE_DECK_SIZE;
             copyIndex++) {
            cards.add(createDeckPokemon(deckId, slotIndex, copyIndex));
        }
    }

    private PokemonCard createDeckPokemon(String deckId, int slotIndex, int copyIndex) {
        return PokemonCard.builder()
                .id(deckId + "-basic-" + slotIndex + "-copy-" + copyIndex)
                .name("Basic " + slotIndex)
                .hp(60)
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

    private PokemonCard createBattlePokemon(String id, String name, int hp, String attackName, int baseDamage) {
        Attack attack = Attack.builder()
                .name(attackName)
                .energyCost(Map.of())
                .baseDamage(baseDamage)
                .build();

        return PokemonCard.builder()
                .id(id)
                .name(name)
                .hp(hp)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.LIGHTNING)
                .attacks(List.of(attack))
                .abilities(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .costToAttack(List.of())
                .retreatCost(0)
                .build();
    }
}
