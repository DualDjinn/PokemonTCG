package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.TrainerEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
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

/**
 * Integración end-to-end del motor de juego para el escenario "Partida Completa".
 *
 * <p>Ejercita el flujo real sin mocks: {@link GameEngine} cablea internamente sus
 * colaboradores ({@link TurnManager}, {@link RuleValidator}, {@link DamageCalculator},
 * {@link VictoryConditionChecker}, etc.) igual que en producción.
 *
 * <p>Escenarios cubiertos:
 * <ol>
 *   <li>Victoria por Knockout Total sin bench — el camino más corto al fin de partida.</li>
 *   <li>Victoria por Knockout Total tras múltiples rondas con selección de premios y
 *       promoción de bench — verifica el ciclo completo turno → ataque → KO → premio →
 *       promote → turno del rival → turno propio.</li>
 * </ol>
 */
class FullGameIntegrationTest {

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
     * Partida completa — victoria por Knockout Total sin bench.
     *
     * <p>P1 no puede atacar en su primer turno (regla XY). En el segundo turno
     * inflige suficiente daño para noquear al activo de P2. Como P2 no tiene bench,
     * el motor detecta Knockout Total y declara a P1 ganador de inmediato, sin
     * requerir selección de premios ni promoción.
     */
    @Test
    void fullGame_P1WinsOnFirstAttack_WhenP2HasNoBench() {
        Player p1 = buildPlayer(P1_ID, ATTACK_NAME, 100, 200);
        Player p2 = buildPlayer(P2_ID, "Rival", 10, 30);
        ar.edu.utn.frc.tup.piii.models.Deck deck1 = p1.getDecks().get(0);
        ar.edu.utn.frc.tup.piii.models.Deck deck2 = p2.getDecks().get(0);

        String matchId = gameEngine.createMatch(p1, p2, deck1, deck2);
        gameEngine.dealCards(matchId);

        // Ambos eligen su Pokémon activo de la mano (sin colocar bench)
        String p1ActiveId = gameEngine.getMatchState(matchId, P1_ID)
                .getSelf().getHand().getFirst().getId();
        String p2ActiveId = gameEngine.getMatchState(matchId, P2_ID)
                .getSelf().getHand().getFirst().getId();
        gameEngine.selectActivePokemon(matchId, P1_ID, p1ActiveId);
        gameEngine.selectActivePokemon(matchId, P2_ID, p2ActiveId);
        gameEngine.confirmSetupReady(matchId, P1_ID);
        gameEngine.confirmSetupReady(matchId, P2_ID);

        // Turno 1: P1 no puede atacar en el primer turno (regla XY), ambos pasan
        gameEngine.submitAction(matchId, P1_ID, GameAction.endTurn());
        gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());

        // Turno 2: P1 ataca — 100 de daño vs 30 HP → KO, P2 sin bench → victoria inmediata
        MatchStateDTO beforeAttack = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(beforeAttack.isCurrentTurn()).isTrue();
        assertThat(beforeAttack.getCurrentPhase()).isEqualTo(TurnPhase.MAIN);
        assertThat(beforeAttack.isFinished()).isFalse();

        gameEngine.submitAction(matchId, P1_ID, GameAction.advanceToAttack());
        gameEngine.submitAction(matchId, P1_ID, GameAction.attack(ATTACK_NAME));

        MatchStateDTO finalState = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(finalState.isFinished()).isTrue();
        assertThat(finalState.getWinnerPlayerId()).isEqualTo(P1_ID);
    }

    /**
     * Partida completa — victoria por Knockout Total tras múltiples rondas.
     *
     * <p>P2 comienza con activo + 2 bench (3 Pokémon totales). P1 los va noqueando
     * uno a uno; tras cada KO con bench disponible, P1 selecciona un premio y P2
     * promueve un bench al activo. En el tercer KO P2 ya no tiene bench y el motor
     * declara a P1 ganador sin requerir selección de premios adicional.
     *
     * <p>Este escenario verifica el ciclo completo:
     * setup → turno → ataque → KO → premio → promote → turno rival → turno propio.
     */
    @Test
    void fullGame_P1WinsAfterThreeKnockoutsAndTwoPrizeSelections() {
        Player p1 = buildPlayer(P1_ID, ATTACK_NAME, 100, 200);
        Player p2 = buildPlayer(P2_ID, "Rival", 10, 30);
        ar.edu.utn.frc.tup.piii.models.Deck deck1 = p1.getDecks().get(0);
        ar.edu.utn.frc.tup.piii.models.Deck deck2 = p2.getDecks().get(0);

        String matchId = gameEngine.createMatch(p1, p2, deck1, deck2);
        gameEngine.dealCards(matchId);

        // ── Setup ─────────────────────────────────────────────────────────────
        // P1: elige activo de la mano, sin bench
        String p1ActiveId = gameEngine.getMatchState(matchId, P1_ID)
                .getSelf().getHand().getFirst().getId();
        gameEngine.selectActivePokemon(matchId, P1_ID, p1ActiveId);

        // P2: elige activo, luego coloca 2 Pokémon en el bench
        String p2ActiveId = gameEngine.getMatchState(matchId, P2_ID)
                .getSelf().getHand().getFirst().getId();
        gameEngine.selectActivePokemon(matchId, P2_ID, p2ActiveId);

        List<MatchStateDTO.CardViewDTO> p2HandAfterActive =
                gameEngine.getMatchState(matchId, P2_ID).getSelf().getHand();
        gameEngine.placeBenchPokemon(matchId, P2_ID, p2HandAfterActive.get(0).getId());
        gameEngine.placeBenchPokemon(matchId, P2_ID, p2HandAfterActive.get(1).getId());

        gameEngine.confirmSetupReady(matchId, P1_ID);
        gameEngine.confirmSetupReady(matchId, P2_ID);

        // Verificar setup: P2 tiene activo + 2 bench (bench incluye slots null, se filtra)
        MatchStateDTO setupState = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(setupState.isFinished()).isFalse();
        assertThat(setupState.getOpponent().getActivePokemon()).isNotNull();
        assertThat(setupState.getOpponent().getBench())
                .filteredOn(b -> b != null).hasSize(2);

        // ── Turno 1: ambos pasan (P1 no puede atacar en primer turno) ─────────
        gameEngine.submitAction(matchId, P1_ID, GameAction.endTurn());
        gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());

        // ── KO #1: P1 ataca → KO → P1 selecciona premio → P2 promueve bench ──
        gameEngine.submitAction(matchId, P1_ID, GameAction.advanceToAttack());
        gameEngine.submitAction(matchId, P1_ID, GameAction.attack(ATTACK_NAME));

        MatchStateDTO afterKo1 = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(afterKo1.isFinished()).isFalse();
        assertThat(afterKo1.isPendingPrizeSelection()).isTrue();
        assertThat(afterKo1.getPendingPrizeSelectionPlayerId()).isEqualTo(P1_ID);

        // P1 toma su primer premio; P2 promueve el primer Pokémon del bench
        gameEngine.selectPrize(matchId, P1_ID, 0);
        gameEngine.promoteFromBench(matchId, P2_ID, firstOccupiedBenchSlot(matchId, P2_ID));

        // P2 ahora tiene activo + 1 bench
        MatchStateDTO afterPromote1 = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(afterPromote1.isFinished()).isFalse();
        assertThat(afterPromote1.getOpponent().getActivePokemon()).isNotNull();
        assertThat(afterPromote1.getOpponent().getBench())
                .filteredOn(b -> b != null).hasSize(1);

        // Turno de P2: pasa sin atacar
        gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());

        // ── KO #2: P1 ataca → KO → P1 selecciona premio → P2 promueve bench ──
        gameEngine.submitAction(matchId, P1_ID, GameAction.advanceToAttack());
        gameEngine.submitAction(matchId, P1_ID, GameAction.attack(ATTACK_NAME));

        MatchStateDTO afterKo2 = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(afterKo2.isFinished()).isFalse();
        assertThat(afterKo2.isPendingPrizeSelection()).isTrue();

        // P1 toma su segundo premio; P2 promueve el último bench (ahora sin bench)
        gameEngine.selectPrize(matchId, P1_ID, 0);
        gameEngine.promoteFromBench(matchId, P2_ID, firstOccupiedBenchSlot(matchId, P2_ID));

        // P2 ahora tiene solo activo, sin bench
        MatchStateDTO afterPromote2 = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(afterPromote2.isFinished()).isFalse();
        assertThat(afterPromote2.getOpponent().getActivePokemon()).isNotNull();
        assertThat(afterPromote2.getOpponent().getBench())
                .filteredOn(b -> b != null).isEmpty();

        // Turno de P2: pasa sin atacar
        gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());

        // ── KO #3: P1 ataca → KO → P2 sin bench → victoria por Knockout Total ─
        gameEngine.submitAction(matchId, P1_ID, GameAction.advanceToAttack());
        gameEngine.submitAction(matchId, P1_ID, GameAction.attack(ATTACK_NAME));

        MatchStateDTO finalState = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(finalState.isFinished()).isTrue();
        assertThat(finalState.getWinnerPlayerId()).isEqualTo(P1_ID);
        // Partida ganada por Knockout Total: P1 aún tenía 4 premios restantes (tomó 2 de 6)
        assertThat(finalState.isPendingPrizeSelection()).isFalse();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Player buildPlayer(String id, String attackName, int damage, int hp) {
        Player player = new Player();
        player.setId(id);
        player.setUsername(id);
        player.setTrainerName(id);
        player.setEmail(id + "@test.dev");
        player.setPassword("secret");
        player.setDecks(List.of(buildDeck(id + "-deck", attackName, damage, hp)));
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
