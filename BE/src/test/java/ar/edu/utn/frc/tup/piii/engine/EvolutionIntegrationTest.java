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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integración end-to-end del motor de juego para el escenario "Evolución".
 *
 * <p>La evolución permite reemplazar un Pokémon en juego por su forma superior,
 * conservando el daño acumulado, las energías adjuntas y el Tool, pero removiendo
 * condiciones de estado especiales.
 *
 * <p>Restricciones del reglamento XY cubiertas:
 * <ul>
 *   <li>No se puede evolucionar en el primer turno de la partida.</li>
 *   <li>No se puede evolucionar un Pokémon que entró a jugar en el mismo turno.</li>
 *   <li>La carta de evolución debe declarar {@code evolvesFrom} igual al nombre del
 *       Pokémon objetivo.</li>
 * </ul>
 *
 * <p>Escenarios cubiertos:
 * <ol>
 *   <li>Evolución feliz del Pokémon activo en turno 2.</li>
 *   <li>Evolución feliz de un Pokémon en el bench en turno 2.</li>
 *   <li>Validación: error al intentar evolucionar en el primer turno.</li>
 *   <li>Validación: error al intentar evolucionar un Pokémon jugado en este turno.</li>
 *   <li>Validación: error cuando la cadena evolutiva no coincide.</li>
 * </ol>
 *
 * <p>Estrategia de deck: P1 usa un deck mixto con 30 Básicos ("Bulb", 60 HP) y 30
 * Stage 1 ("Ivy", 90 HP, evolvesFrom = "Bulb"). Con 30 de cada 60 cartas la
 * probabilidad de no encontrar al menos uno de cada tipo en la mano inicial es
 * inferior al 0.1 %, eliminando la necesidad de {@code @RepeatedTest}.
 */
class EvolutionIntegrationTest {

    private static final int DECK_SIZE = 60;
    private static final String P1_ID = "player-one";
    private static final String P2_ID = "player-two";
    private static final String BASIC_NAME = "Bulb";
    private static final String STAGE1_NAME = "Ivy";
    private static final int BASIC_HP = 60;
    private static final int STAGE1_HP = 90;

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
     * Evolución del Pokémon activo — camino feliz.
     *
     * <p>P1 selecciona un "Bulb" (60 HP) como activo. En el segundo turno (cuando la
     * regla ya lo permite), juega "Ivy" (90 HP, evolvesFrom = "Bulb") sobre el activo.
     * El motor debe reemplazar el activo: el nuevo nombre es "Ivy", el HP máximo es 90
     * y la carta "Ivy" desaparece de la mano.
     */
    @Test
    void evolution_ActivePokemonEvolvesOnTurn2() {
        String matchId = createAndDeal();

        String basicActiveId = firstBasicInHand(matchId, P1_ID);
        gameEngine.selectActivePokemon(matchId, P1_ID, basicActiveId);
        gameEngine.selectActivePokemon(matchId, P2_ID, firstBasicInHand(matchId, P2_ID));
        gameEngine.confirmSetupReady(matchId, P1_ID);
        gameEngine.confirmSetupReady(matchId, P2_ID);

        // Turno 1: P1 no puede atacar (regla XY primer turno)
        gameEngine.submitAction(matchId, P1_ID, GameAction.endTurn());
        gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());

        // Turno 2 — P1 puede evolucionar
        String stage1CardId = firstStage1InHand(matchId, P1_ID);
        String activeInstanceId = gameEngine.getMatchState(matchId, P1_ID)
                .getSelf().getActivePokemon().getInstanceId();

        int handSizeBefore = gameEngine.getMatchState(matchId, P1_ID).getSelf().getHandSize();

        gameEngine.submitAction(matchId, P1_ID, GameAction.evolve(stage1CardId, activeInstanceId));

        MatchStateDTO afterEvo = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(afterEvo.getSelf().getActivePokemon().getName()).isEqualTo(STAGE1_NAME);
        assertThat(afterEvo.getSelf().getActivePokemon().getMaxHp()).isEqualTo(STAGE1_HP);
        assertThat(afterEvo.getSelf().getHandSize()).isEqualTo(handSizeBefore - 1);
        assertThat(afterEvo.isFinished()).isFalse();
    }

    /**
     * Evolución de un Pokémon en el bench — camino feliz.
     *
     * <p>Durante el setup, P1 coloca un segundo "Bulb" en el bench. En el turno 2,
     * P1 evoluciona al Pokémon del bench (no al activo). El bench debe reflejar el
     * Stage 1; el activo permanece como "Bulb".
     */
    @Test
    void evolution_BenchPokemonEvolvesOnTurn2() {
        String matchId = createAndDeal();

        String basicActiveId = firstBasicInHand(matchId, P1_ID);
        gameEngine.selectActivePokemon(matchId, P1_ID, basicActiveId);

        // Coloca un segundo Bulb en el bench durante el setup
        String benchBasicId = firstBasicInHand(matchId, P1_ID);
        gameEngine.placeBenchPokemon(matchId, P1_ID, benchBasicId);

        gameEngine.selectActivePokemon(matchId, P2_ID, firstBasicInHand(matchId, P2_ID));
        gameEngine.confirmSetupReady(matchId, P1_ID);
        gameEngine.confirmSetupReady(matchId, P2_ID);

        // Turno 1: ambos pasan
        gameEngine.submitAction(matchId, P1_ID, GameAction.endTurn());
        gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());

        // Turno 2 — P1 evoluciona el bench
        MatchStateDTO stateBeforeEvo = gameEngine.getMatchState(matchId, P1_ID);
        String benchInstanceId = stateBeforeEvo.getSelf().getBench().stream()
                .filter(b -> b != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay Pokémon en el bench de P1"))
                .getInstanceId();

        String stage1CardId = firstStage1InHand(matchId, P1_ID);
        gameEngine.submitAction(matchId, P1_ID, GameAction.evolve(stage1CardId, benchInstanceId));

        MatchStateDTO afterEvo = gameEngine.getMatchState(matchId, P1_ID);
        // El activo sigue siendo el Bulb original
        assertThat(afterEvo.getSelf().getActivePokemon().getName()).isEqualTo(BASIC_NAME);
        // El bench ahora muestra "Ivy"
        MatchStateDTO.InPlayPokemonViewDTO evolvedBench = afterEvo.getSelf().getBench().stream()
                .filter(b -> b != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Bench vacío tras evolución"));
        assertThat(evolvedBench.getName()).isEqualTo(STAGE1_NAME);
        assertThat(evolvedBench.getMaxHp()).isEqualTo(STAGE1_HP);
    }

    /**
     * Validación — no se puede evolucionar en el primer turno.
     *
     * <p>La regla XY prohíbe evolucionar durante el primer turno de la partida
     * (ni siquiera hay un turno anterior en el que el Pokémon base haya podido
     * "estar listo"). El motor debe lanzar {@link IllegalArgumentException}.
     */
    @Test
    void evolution_CannotEvolveOnFirstTurn() {
        String matchId = createAndDeal();

        String basicActiveId = firstBasicInHand(matchId, P1_ID);
        gameEngine.selectActivePokemon(matchId, P1_ID, basicActiveId);
        gameEngine.selectActivePokemon(matchId, P2_ID, firstBasicInHand(matchId, P2_ID));
        gameEngine.confirmSetupReady(matchId, P1_ID);
        gameEngine.confirmSetupReady(matchId, P2_ID);

        // Primer turno — intentar evolucionar debe fallar
        String stage1CardId = firstStage1InHand(matchId, P1_ID);
        String activeInstanceId = gameEngine.getMatchState(matchId, P1_ID)
                .getSelf().getActivePokemon().getInstanceId();

        assertThatThrownBy(() ->
                gameEngine.submitAction(matchId, P1_ID,
                        GameAction.evolve(stage1CardId, activeInstanceId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot evolve on the first turn");
    }

    /**
     * Validación — no se puede evolucionar un Pokémon jugado en el mismo turno.
     *
     * <p>P1 juega un "Bulb" al bench durante su turno 2 (con {@code PLAY_BASIC}) e
     * inmediatamente intenta evolucionar ese mismo Pokémon. El motor debe rechazarlo.
     */
    @Test
    void evolution_CannotEvolvePlayedThisTurn() {
        String matchId = createAndDeal();

        String basicActiveId = firstBasicInHand(matchId, P1_ID);
        gameEngine.selectActivePokemon(matchId, P1_ID, basicActiveId);
        gameEngine.selectActivePokemon(matchId, P2_ID, firstBasicInHand(matchId, P2_ID));
        gameEngine.confirmSetupReady(matchId, P1_ID);
        gameEngine.confirmSetupReady(matchId, P2_ID);

        // Turno 1: ambos pasan
        gameEngine.submitAction(matchId, P1_ID, GameAction.endTurn());
        gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());

        // Turno 2 — jugar un Bulb al bench este mismo turno
        String newBulbId = firstBasicInHand(matchId, P1_ID);
        gameEngine.submitAction(matchId, P1_ID, GameAction.playBasic(newBulbId));

        // Obtener el instanceId del Bulb recién jugado (último slot no-null del bench)
        String newBenchInstanceId = gameEngine.getMatchState(matchId, P1_ID).getSelf().getBench()
                .stream()
                .filter(b -> b != null)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("No hay Pokémon en bench"))
                .getInstanceId();

        String stage1CardId = firstStage1InHand(matchId, P1_ID);

        assertThatThrownBy(() ->
                gameEngine.submitAction(matchId, P1_ID,
                        GameAction.evolve(stage1CardId, newBenchInstanceId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("was played this turn");
    }

    /**
     * Validación — la cadena evolutiva debe coincidir.
     *
     * <p>P1 usa un deck con "WrongBase" (Básico) e "Ivy" (Stage 1, evolvesFrom="Bulb").
     * Como el activo es "WrongBase" y no "Bulb", la evolución debe ser rechazada.
     */
    @Test
    void evolution_CannotEvolveWrongSpecies() {
        Player p1 = buildPlayer(P1_ID);
        Player p2 = buildPlayerAllBasics(P2_ID);
        // P1: 30 "WrongBase" + 30 "Ivy" (evolvesFrom="Bulb") — especie incorrecta
        ar.edu.utn.frc.tup.piii.models.Deck deck1 = buildMismatchedDeck("mismatch-deck");
        ar.edu.utn.frc.tup.piii.models.Deck deck2 = buildAllBasicsDeck("p2-deck", "Simple", 60);

        String matchId = gameEngine.createMatch(p1, p2, deck1, deck2);
        gameEngine.dealCards(matchId);

        // P1 selecciona un "WrongBase" como activo
        String wrongBaseId = firstBasicInHand(matchId, P1_ID);
        gameEngine.selectActivePokemon(matchId, P1_ID, wrongBaseId);
        gameEngine.selectActivePokemon(matchId, P2_ID,
                gameEngine.getMatchState(matchId, P2_ID).getSelf().getHand().stream()
                        .filter(c -> c.getHp() != null)
                        .map(MatchStateDTO.CardViewDTO::getId)
                        .findFirst().orElseThrow());
        gameEngine.confirmSetupReady(matchId, P1_ID);
        gameEngine.confirmSetupReady(matchId, P2_ID);

        // Turno 1: ambos pasan
        gameEngine.submitAction(matchId, P1_ID, GameAction.endTurn());
        gameEngine.submitAction(matchId, P2_ID, GameAction.endTurn());

        // Turno 2: "Ivy" (evolvesFrom="Bulb") sobre "WrongBase" → especie no coincide
        String ivyId = firstStage1InHand(matchId, P1_ID);
        String activeInstanceId = gameEngine.getMatchState(matchId, P1_ID)
                .getSelf().getActivePokemon().getInstanceId();

        assertThatThrownBy(() ->
                gameEngine.submitAction(matchId, P1_ID,
                        GameAction.evolve(ivyId, activeInstanceId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not evolve from");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String createAndDeal() {
        Player p1 = buildPlayer(P1_ID);
        Player p2 = buildPlayerAllBasics(P2_ID);
        ar.edu.utn.frc.tup.piii.models.Deck deck1 = buildMixedDeck("p1-deck");
        ar.edu.utn.frc.tup.piii.models.Deck deck2 = buildAllBasicsDeck("p2-deck", BASIC_NAME, BASIC_HP);
        String matchId = gameEngine.createMatch(p1, p2, deck1, deck2);
        gameEngine.dealCards(matchId);
        return matchId;
    }

    private String firstBasicInHand(String matchId, String playerId) {
        return gameEngine.getMatchState(matchId, playerId).getSelf().getHand().stream()
                .filter(c -> c.getHp() != null && c.getEvolvesFrom() == null)
                .map(MatchStateDTO.CardViewDTO::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay Básico en mano de " + playerId));
    }

    private String firstStage1InHand(String matchId, String playerId) {
        return gameEngine.getMatchState(matchId, playerId).getSelf().getHand().stream()
                .filter(c -> c.getEvolvesFrom() != null)
                .map(MatchStateDTO.CardViewDTO::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay Stage 1 en mano de " + playerId));
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

    private Player buildPlayerAllBasics(String id) {
        return buildPlayer(id);
    }

    /** 30 Básicos "Bulb" + 30 Stage 1 "Ivy" (evolvesFrom = "Bulb"). */
    private ar.edu.utn.frc.tup.piii.models.Deck buildMixedDeck(String deckId) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < DECK_SIZE / 2; i++) {
            cards.add(buildBasic(deckId + "-basic-" + i, BASIC_NAME));
        }
        for (int i = 0; i < DECK_SIZE / 2; i++) {
            cards.add(buildStage1(deckId + "-stage1-" + i, STAGE1_NAME, BASIC_NAME));
        }
        ar.edu.utn.frc.tup.piii.models.Deck deck = new ar.edu.utn.frc.tup.piii.models.Deck();
        deck.setId(deckId);
        deck.setName("Mixed Evolution Deck");
        deck.setCards(cards);
        return deck;
    }

    /**
     * 30 "WrongBase" (Básico) + 30 "Ivy" (Stage1, evolvesFrom="Bulb").
     * Permite probar el error cuando el activo no coincide con la cadena evolutiva.
     */
    private ar.edu.utn.frc.tup.piii.models.Deck buildMismatchedDeck(String deckId) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < DECK_SIZE / 2; i++) {
            cards.add(buildBasic(deckId + "-wrong-" + i, "WrongBase"));
        }
        for (int i = 0; i < DECK_SIZE / 2; i++) {
            cards.add(buildStage1(deckId + "-ivy-" + i, STAGE1_NAME, BASIC_NAME));
        }
        ar.edu.utn.frc.tup.piii.models.Deck deck = new ar.edu.utn.frc.tup.piii.models.Deck();
        deck.setId(deckId);
        deck.setName("Mismatched Deck");
        deck.setCards(cards);
        return deck;
    }

    private ar.edu.utn.frc.tup.piii.models.Deck buildAllBasicsDeck(
            String deckId, String pokemonName, int hp) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < DECK_SIZE; i++) {
            cards.add(buildBasicWithHp(deckId + "-basic-" + i, pokemonName, hp));
        }
        ar.edu.utn.frc.tup.piii.models.Deck deck = new ar.edu.utn.frc.tup.piii.models.Deck();
        deck.setId(deckId);
        deck.setName("All Basics Deck");
        deck.setCards(cards);
        return deck;
    }

    private PokemonCard buildBasic(String id, String name) {
        return buildBasicWithHp(id, name, BASIC_HP);
    }

    private PokemonCard buildBasicWithHp(String id, String name, int hp) {
        Attack attack = Attack.builder()
                .name("Tackle")
                .energyCost(Map.of())
                .baseDamage(20)
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

    private PokemonCard buildStage1(String id, String name, String evolvesFrom) {
        Attack attack = Attack.builder()
                .name("Vine Whip")
                .energyCost(Map.of())
                .baseDamage(30)
                .build();
        return PokemonCard.builder()
                .id(id)
                .name(name)
                .hp(STAGE1_HP)
                .stage(PokemonStage.STAGE_1)
                .evolvesFrom(evolvesFrom)
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
