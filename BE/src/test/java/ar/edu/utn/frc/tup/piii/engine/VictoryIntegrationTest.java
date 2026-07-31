package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.TrainerEffectRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integración end-to-end del motor de juego para el escenario "Victoria".
 *
 * <p>El reglamento XY establece tres condiciones de victoria mutuamente excluyentes
 * que el motor debe detectar y declarar automáticamente:
 * <ol>
 *   <li><b>Premios completos</b> — un jugador toma sus 6 cartas de premio tomando KOs.</li>
 *   <li><b>Knockout Total</b> — el rival no tiene Pokémon activo ni en el bench.</li>
 *   <li><b>Deck Out</b> — el rival comienza su turno sin cartas en el deck.</li>
 * </ol>
 *
 * <p>Escenarios cubiertos:
 * <ol>
 *   <li>Victoria por premios: P1 noquea 6 Pokémon y toma sus 6 premios.</li>
 *   <li>Victoria por Knockout Total: P2 queda sin activo ni bench tras un KO.</li>
 *   <li>Victoria por Deck Out: P2 inicia su turno con el deck vacío.</li>
 * </ol>
 */
class VictoryIntegrationTest {

    /**
     * Deck mínimo para activar Deck Out: 7 (mano) + 6 (premios) + 1 restante = 14.
     * Tras el primer turno de P2, el único restante se roba y el deck queda vacío.
     */
    private static final int DECK_OUT_DECK_SIZE = 14;
    private static final int FULL_DECK_SIZE = 60;
    private static final int PRIZE_COUNT = 6;
    private static final int MAX_BENCH = 5;
    private static final String P1_ID = "player-one";
    private static final String P2_ID = "player-two";
    private static final String ATTACK_NAME = "Golpe";

    private GameEngine gameEngine;

    @BeforeEach
    void setUp() {
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
        gameEngine = new GameEngine(new BattleRegistry(), turnManager);
    }

    /**
     * Victoria por premios — P1 noquea 6 Pokémon y toma los 6 premios.
     *
     * <p>P2 comienza con 1 activo + 5 bench (total 6 Pokémon). P1 los derriba uno
     * a uno. Los primeros 5 KOs exigen selección de premio y promoción de bench.
     * El 6.º KO completa los premios de P1: el motor declara la victoria sin
     * pasar por la selección interactiva del último premio.
     */
    @Test
    void victory_ByAllPrizes_WhenSixKnockoutsScored() {
        String matchId = createAndDeal(
                buildDeck("p1-deck", ATTACK_NAME, 100, 200),
                buildDeck("p2-deck", "Tackle", 10, 10));

        // Setup — P2 llena el bench con 5 Pokémon
        String p1ActiveId = firstCardId(matchId, P1_ID);
        gameEngine.selectActivePokemon(matchId, P1_ID, p1ActiveId);

        List<MatchStateDTO.CardViewDTO> p2Hand = hand(matchId, P2_ID);
        gameEngine.selectActivePokemon(matchId, P2_ID, p2Hand.get(0).getId());
        for (int i = 1; i <= MAX_BENCH; i++) {
            gameEngine.placeBenchPokemon(matchId, P2_ID, p2Hand.get(i).getId());
        }
        gameEngine.confirmSetupReady(matchId, P1_ID);
        gameEngine.confirmSetupReady(matchId, P2_ID);

        // Turno 1: P1 no puede atacar (regla XY primer turno)
        gameEngine.submitAction(matchId, P1_ID, GameAction.endTurn());
        gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());

        // KOs #1 al #5: ataque → selectPrize → promoteFromBench → P2 endTurn
        for (int ko = 1; ko <= PRIZE_COUNT - 1; ko++) {
            attack(matchId);
            assertThat(state(matchId).isPendingPrizeSelection()).isTrue();
            gameEngine.selectPrize(matchId, P1_ID, 0);
            gameEngine.promoteFromBench(matchId, P2_ID, firstOccupiedBenchSlot(matchId, P2_ID));
            gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());
        }

        // KO #6: el motor detecta que P1 tomó su último premio → fin automático
        MatchStateDTO before = state(matchId);
        assertThat(before.isFinished()).isFalse();
        assertThat(before.getSelf().getRemainingPrizes()).isEqualTo(1);

        attack(matchId);

        MatchStateDTO finalState = state(matchId);
        assertThat(finalState.isFinished()).isTrue();
        assertThat(finalState.getWinnerPlayerId()).isEqualTo(P1_ID);
        assertThat(finalState.isPendingPrizeSelection()).isFalse();
    }

    /**
     * Victoria por Knockout Total — P2 queda sin Pokémon en juego.
     *
     * <p>P2 comienza solo con su activo (sin bench). Tras el primer KO de P1,
     * P2 no tiene ningún Pokémon con el que continuar: el motor declara la
     * victoria sin requerir selección de premio ni promoción de bench.
     */
    @Test
    void victory_ByKnockoutTotal_WhenOpponentHasNoBench() {
        String matchId = createAndDeal(
                buildDeck("p1-deck", ATTACK_NAME, 100, 200),
                buildDeck("p2-deck", "Tackle", 10, 10));

        String p1ActiveId = firstCardId(matchId, P1_ID);
        String p2ActiveId = firstCardId(matchId, P2_ID);
        gameEngine.selectActivePokemon(matchId, P1_ID, p1ActiveId);
        gameEngine.selectActivePokemon(matchId, P2_ID, p2ActiveId);
        gameEngine.confirmSetupReady(matchId, P1_ID);
        gameEngine.confirmSetupReady(matchId, P2_ID);

        // Turno 1: ambos pasan
        gameEngine.submitAction(matchId, P1_ID, GameAction.endTurn());
        gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());

        // Turno 2: P1 ataca → KO → P2 sin bench → victoria instantánea
        attack(matchId);

        MatchStateDTO finalState = state(matchId);
        assertThat(finalState.isFinished()).isTrue();
        assertThat(finalState.getWinnerPlayerId()).isEqualTo(P1_ID);
        assertThat(finalState.isPendingPrizeSelection()).isFalse();
    }

    /**
     * Victoria por Deck Out — P2 intenta iniciar su turno con el deck vacío.
     *
     * <p>P2 usa un deck de {@value DECK_OUT_DECK_SIZE} cartas: 7 para la mano
     * inicial, 6 para los premios y 1 única carta restante en el deck.
     * Esa carta es robada por P2 en su primer turno. En el tercer turno de P1
     * (cuando el control vuelve a P2 por segunda vez), el motor detecta que el
     * deck de P2 está vacío y declara vencedor a P1.
     */
    @Test
    void victory_ByDeckOut_WhenOpponentDeckIsExhausted() {
        String matchId = createAndDeal(
                buildDeck("p1-deck", ATTACK_NAME, 20, 200),
                buildDeck("p2-deck", "Tackle", 10, 200, DECK_OUT_DECK_SIZE));

        String p1ActiveId = firstCardId(matchId, P1_ID);
        String p2ActiveId = firstCardId(matchId, P2_ID);
        gameEngine.selectActivePokemon(matchId, P1_ID, p1ActiveId);
        gameEngine.selectActivePokemon(matchId, P2_ID, p2ActiveId);
        gameEngine.confirmSetupReady(matchId, P1_ID);
        gameEngine.confirmSetupReady(matchId, P2_ID);

        // Turno 1 de P1: pasa (sin ataque en primer turno)
        // Al finalizar el turno de P1, empieza el turno de P2 → P2 roba su única carta restante
        gameEngine.submitAction(matchId, P1_ID, GameAction.endTurn());

        assertThat(state(matchId).isFinished()).isFalse();

        // Turno 1 de P2: pasa. Al terminar, empieza turno de P1 → deck de P1 tiene cartas, OK.
        gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());

        assertThat(state(matchId).isFinished()).isFalse();

        // Turno 2 de P1: al pasar, el motor intenta iniciar el turno de P2 y detecta deck vacío → P1 gana.
        gameEngine.submitAction(matchId, P1_ID, GameAction.endTurn());

        MatchStateDTO finalState = state(matchId);
        assertThat(finalState.isFinished()).isTrue();
        assertThat(finalState.getWinnerPlayerId()).isEqualTo(P1_ID);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String createAndDeal(ar.edu.utn.frc.tup.piii.models.Deck deck1,
                                  ar.edu.utn.frc.tup.piii.models.Deck deck2) {
        Player p1 = buildPlayer(P1_ID);
        Player p2 = buildPlayer(P2_ID);
        String matchId = gameEngine.createMatch(p1, p2, deck1, deck2);
        gameEngine.dealCards(matchId);
        return matchId;
    }

    private void attack(String matchId) {
        gameEngine.submitAction(matchId, P1_ID, GameAction.advanceToAttack());
        gameEngine.submitAction(matchId, P1_ID, GameAction.attack(ATTACK_NAME));
    }

    private MatchStateDTO state(String matchId) {
        return gameEngine.getMatchState(matchId, P1_ID);
    }

    private List<MatchStateDTO.CardViewDTO> hand(String matchId, String playerId) {
        return gameEngine.getMatchState(matchId, playerId).getSelf().getHand();
    }

    private String firstCardId(String matchId, String playerId) {
        return hand(matchId, playerId).getFirst().getId();
    }

    private int firstOccupiedBenchSlot(String matchId, String playerId) {
        List<MatchStateDTO.InPlayPokemonViewDTO> bench =
                gameEngine.getMatchState(matchId, playerId).getSelf().getBench();
        for (int i = 0; i < bench.size(); i++) {
            if (bench.get(i) != null) {
                return i;
            }
        }
        throw new IllegalStateException("No hay Pokémon en el bench de " + playerId);
    }

    private Player buildPlayer(String id) {
        Player player = new Player();
        player.setId(id);
        player.setUsername(id);
        player.setTrainerName(id);
        player.setEmail(id + "@test.dev");
        player.setPassword("secret");
        player.setDecks(List.of());
        return player;
    }

    private ar.edu.utn.frc.tup.piii.models.Deck buildDeck(
            String deckId, String attackName, int damage, int hp) {
        return buildDeck(deckId, attackName, damage, hp, FULL_DECK_SIZE);
    }

    private ar.edu.utn.frc.tup.piii.models.Deck buildDeck(
            String deckId, String attackName, int damage, int hp, int size) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            cards.add(buildPokemon(deckId + "-mon-" + i, "Mon-" + i, hp, attackName, damage));
        }
        ar.edu.utn.frc.tup.piii.models.Deck deck = new ar.edu.utn.frc.tup.piii.models.Deck();
        deck.setId(deckId);
        deck.setName("Test Deck");
        deck.setCards(cards);
        return deck;
    }

    private PokemonCard buildPokemon(String id, String name, int hp,
                                     String attackName, int damage) {
        Attack attack = Attack.builder()
                .name(attackName)
                .energyCost(Map.of())
                .baseDamage(damage)
                .build();
        return PokemonCard.builder()
                .id(id)
                .name(name)
                .hp(hp)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.COLORLESS)
                .attacks(List.of(attack))
                .abilities(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .costToAttack(List.of())
                .retreatCost(0)
                .build();
    }
}
