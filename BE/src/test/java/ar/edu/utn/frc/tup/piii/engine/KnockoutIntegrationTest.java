package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.TrainerEffectRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integración end-to-end del motor de juego para el escenario "Knockout".
 *
 * <p>Un Knockout (KO) ocurre cuando el daño acumulado de un Pokémon supera
 * o iguala su HP máximo. El motor debe:
 * <ul>
 *   <li>Remover al Pokémon derribado del campo y colocarlo en el descarte.</li>
 *   <li>Activar la selección de premio para el atacante.</li>
 *   <li>Exigir al defensor que promueva un Pokémon del bench (si tiene).</li>
 *   <li>Terminar la partida inmediatamente si el defensor no tiene bench (Knockout Total).</li>
 * </ul>
 *
 * <p>Escenarios cubiertos:
 * <ol>
 *   <li>Estado del tablero inmediatamente después de un KO con bench disponible.</li>
 *   <li>Flujo completo KO → selección de premio → promoción de bench → turno continúa.</li>
 *   <li>El contador de premios restantes decrementa al tomar un premio.</li>
 *   <li>Validación: el jugador equivocado no puede seleccionar el premio.</li>
 *   <li>Validación: un índice de premio inválido es rechazado.</li>
 * </ol>
 */
class KnockoutIntegrationTest {

    private static final int DECK_SIZE = 60;
    private static final int PRIZE_COUNT = 6;
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
     * Estado del tablero inmediatamente después de un KO.
     *
     * <p>Tras el ataque que derriba al activo de P2:
     * <ul>
     *   <li>{@code pendingPrizeSelection = true}, seleccionador = P1.</li>
     *   <li>El activo de P2 es {@code null} (descarté automático).</li>
     *   <li>{@code requiresActiveSelection} del defensor (P2) = {@code true}.</li>
     *   <li>La partida no ha terminado porque P2 aún tiene bench.</li>
     * </ul>
     */
    @Test
    void knockout_BoardStateAfterKo_WhenOpponentHasBench() {
        String matchId = setupWithP2Bench(1);

        executeFirstAttack(matchId);

        MatchStateDTO stateP1 = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(stateP1.isFinished()).isFalse();
        assertThat(stateP1.isPendingPrizeSelection()).isTrue();
        assertThat(stateP1.getPendingPrizeSelectionPlayerId()).isEqualTo(P1_ID);
        assertThat(stateP1.getOpponent().getActivePokemon()).isNull();

        MatchStateDTO stateP2 = gameEngine.getMatchState(matchId, P2_ID);
        assertThat(stateP2.isRequiresActiveSelection()).isTrue();
    }

    /**
     * Flujo completo: KO → selección de premio → promoción de bench → turno continúa.
     *
     * <p>Tras un KO con bench disponible, P1 selecciona un premio, P2 promueve su
     * bench. El juego avanza al turno de P2 con todos los estados limpios.
     */
    @Test
    void knockout_FullFlow_PrizeAndPromote_TurnContinues() {
        String matchId = setupWithP2Bench(1);

        executeFirstAttack(matchId);

        assertThat(gameEngine.getMatchState(matchId, P1_ID).isPendingPrizeSelection()).isTrue();

        gameEngine.selectPrize(matchId, P1_ID, 0);

        MatchStateDTO afterPrize = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(afterPrize.isPendingPrizeSelection()).isFalse();

        int benchSlot = firstOccupiedBenchSlot(matchId, P2_ID);
        gameEngine.promoteFromBench(matchId, P2_ID, benchSlot);

        MatchStateDTO afterPromote = gameEngine.getMatchState(matchId, P2_ID);
        assertThat(afterPromote.isFinished()).isFalse();
        assertThat(afterPromote.getOpponent().getActivePokemon()).isNotNull();
        assertThat(afterPromote.isCurrentTurn()).isTrue();
        assertThat(afterPromote.getCurrentPhase()).isEqualTo(TurnPhase.MAIN);
    }

    /**
     * Contador de premios restantes decrementa al tomar un premio.
     *
     * <p>P1 comienza con {@value PRIZE_COUNT} premios. Tras el KO y la selección
     * del primer premio, P1 debe tener {@code PRIZE_COUNT - 1} premios restantes.
     */
    @Test
    void knockout_RemainingPrizesDecrements_AfterSelectPrize() {
        String matchId = setupWithP2Bench(1);

        MatchStateDTO before = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(before.getSelf().getRemainingPrizes()).isEqualTo(PRIZE_COUNT);

        executeFirstAttack(matchId);
        gameEngine.selectPrize(matchId, P1_ID, 0);

        MatchStateDTO after = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(after.getSelf().getRemainingPrizes()).isEqualTo(PRIZE_COUNT - 1);
    }

    /**
     * Validación — el jugador equivocado no puede seleccionar el premio.
     *
     * <p>Tras el KO, P1 es quien debe seleccionar su premio. Si P2 intenta
     * llamar a {@code selectPrize}, el motor debe rechazarlo.
     */
    @Test
    void knockout_WrongPlayer_CannotSelectPrize() {
        String matchId = setupWithP2Bench(1);

        executeFirstAttack(matchId);

        assertThat(gameEngine.getMatchState(matchId, P1_ID).isPendingPrizeSelection()).isTrue();

        assertThatThrownBy(() -> gameEngine.selectPrize(matchId, P2_ID, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not pending prize selection");
    }

    /**
     * Validación — un índice de premio fuera de rango es rechazado.
     *
     * <p>Si el jugador proporciona un índice negativo o mayor o igual al número
     * de premios disponibles, el motor debe lanzar {@link IllegalArgumentException}.
     */
    @Test
    void knockout_InvalidPrizeIndex_IsRejected() {
        String matchId = setupWithP2Bench(1);

        executeFirstAttack(matchId);

        assertThatThrownBy(() -> gameEngine.selectPrize(matchId, P1_ID, PRIZE_COUNT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid prize index");

        assertThatThrownBy(() -> gameEngine.selectPrize(matchId, P1_ID, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid prize index");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Crea una partida donde P2 empieza con 1 activo + {@code benchCount} bench.
     * P1 tiene daño suficiente para noquear al activo de P2 en un solo ataque.
     * Devuelve el matchId con el primer turno ya pasado (listo para atacar en turno 2).
     */
    private String setupWithP2Bench(int benchCount) {
        Player p1 = buildPlayer(P1_ID);
        Player p2 = buildPlayer(P2_ID);
        ar.edu.utn.frc.tup.piii.models.Deck deck1 = buildDeck("p1-deck", ATTACK_NAME, 100, 200);
        ar.edu.utn.frc.tup.piii.models.Deck deck2 = buildDeck("p2-deck", "Tackle", 20, 30);

        String matchId = gameEngine.createMatch(p1, p2, deck1, deck2);
        gameEngine.dealCards(matchId);

        String p1ActiveId = gameEngine.getMatchState(matchId, P1_ID)
                .getSelf().getHand().getFirst().getId();
        gameEngine.selectActivePokemon(matchId, P1_ID, p1ActiveId);

        List<MatchStateDTO.CardViewDTO> p2Hand = gameEngine.getMatchState(matchId, P2_ID)
                .getSelf().getHand();
        gameEngine.selectActivePokemon(matchId, P2_ID, p2Hand.get(0).getId());

        for (int i = 0; i < benchCount && i + 1 < p2Hand.size(); i++) {
            gameEngine.placeBenchPokemon(matchId, P2_ID, p2Hand.get(i + 1).getId());
        }

        gameEngine.confirmSetupReady(matchId, P1_ID);
        gameEngine.confirmSetupReady(matchId, P2_ID);

        // Turno 1: P1 no puede atacar (regla XY)
        gameEngine.submitAction(matchId, P1_ID, GameAction.endTurn());
        gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());

        return matchId;
    }

    /** Avanza al ataque y ataca en el turno 2 de P1. */
    private void executeFirstAttack(String matchId) {
        gameEngine.submitAction(matchId, P1_ID, GameAction.advanceToAttack());
        gameEngine.submitAction(matchId, P1_ID, GameAction.attack(ATTACK_NAME));
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
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < DECK_SIZE; i++) {
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
