package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
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
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integración end-to-end del motor de juego para el escenario "Mulligan".
 *
 * <p>Un mulligan ocurre cuando un jugador no tiene ningún Pokémon Básico en su mano
 * inicial de 7 cartas. Debe devolver la mano, barajar, y robar 7 nuevas. Por cada
 * mulligan realizado, el oponente (beneficiario) tiene derecho a robar cartas extra.
 *
 * <p>Para forzar un mulligan determinista, el deck del jugador que debe hacer
 * mulligan se llena completamente con Pokémon Stage 1 (no Básicos). Esto garantiza
 * que nunca encontrará un Básico y el loop de mulligan correrá hasta el safety limit,
 * generando un {@code mulliganBenefitCount} alto y previsible.
 *
 * <p>Escenarios cubiertos:
 * <ol>
 *   <li>Mulligan de un jugador: el beneficiario acepta robar cartas extra.</li>
 *   <li>Mulligan de un jugador: el beneficiario rechaza las cartas extra (elige 0).</li>
 *   <li>Validación: solo el beneficiario puede resolver el mulligan.</li>
 *   <li>Validación: no se puede robar más cartas que las permitidas.</li>
 * </ol>
 */
class MulliganIntegrationTest {

    private static final int DECK_SIZE = 60;
    private static final int OPENING_HAND_SIZE = 7;
    private static final String P1_ID = "player-one";
    private static final String P2_ID = "player-two";

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
     * Mulligan — el beneficiario acepta robar cartas extra.
     *
     * <p>P1 tiene un deck de 60 Básicos (siempre tiene Básico en mano). P2 tiene un
     * deck con solo 1 Básico y 59 Energías (alta probabilidad de mulligan). Se repite
     * hasta 5 veces para garantizar que al menos una ejecución active el mulligan.
     *
     * <p>El DTO solo expone {@code mulliganResolutionPending} cuando ambos jugadores
     * confirmaron setup. Al resolver con {@code count=2}, P1 roba 2 cartas extra.
     */
    @RepeatedTest(5)
    void mulligan_BeneficiaryDrawsExtraCards() {
        Player p1 = buildPlayer(P1_ID);
        Player p2 = buildPlayer(P2_ID);
        ar.edu.utn.frc.tup.piii.models.Deck deckBasics = buildAllBasicsDeck("basics-deck");
        ar.edu.utn.frc.tup.piii.models.Deck deckFewBasics = buildFewBasicsDeck("few-basics-deck");

        String matchId = gameEngine.createMatch(p1, p2, deckBasics, deckFewBasics);
        gameEngine.dealCards(matchId);

        MatchStateDTO preDeal = gameEngine.getMatchState(matchId, P1_ID);
        assumeTrue(preDeal.getMulliganBenefitCount() > 0,
                "No hubo mulligan en esta iteración (shuffle aleatorio)");

        assertThat(preDeal.getMulliganBeneficiaryPlayerId()).isEqualTo(P1_ID);
        assertThat(preDeal.getMulliganPlayerId()).isEqualTo(P2_ID);

        completeSetup(matchId);

        MatchStateDTO stateP1 = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(stateP1.isMulliganResolutionPending()).isTrue();
        // Premios aún no colocados
        assertThat(stateP1.getSelf().getRemainingPrizes()).isEqualTo(0);
        // Mano: 7 iniciales menos el activo seleccionado = 6
        int handBeforeResolve = stateP1.getSelf().getHandSize();

        int extraCards = 2;
        gameEngine.resolveMulliganDraw(matchId, P1_ID, extraCards);

        MatchStateDTO afterResolve = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(afterResolve.getSelf().getHandSize())
                .isEqualTo(handBeforeResolve + extraCards);
        assertThat(afterResolve.isMulliganResolutionPending()).isFalse();
        assertThat(afterResolve.getSelf().getRemainingPrizes()).isEqualTo(6);
    }

    /**
     * Mulligan — el beneficiario rechaza las cartas extra.
     *
     * <p>El beneficiario elige {@code count=0}, renunciando a su ventaja. Su mano
     * no crece. Los premios se colocan igualmente y el juego puede continuar.
     */
    @RepeatedTest(5)
    void mulligan_BeneficiaryDeclinesExtraCards() {
        Player p1 = buildPlayer(P1_ID);
        Player p2 = buildPlayer(P2_ID);
        ar.edu.utn.frc.tup.piii.models.Deck deckBasics = buildAllBasicsDeck("basics-deck");
        ar.edu.utn.frc.tup.piii.models.Deck deckFewBasics = buildFewBasicsDeck("few-basics-deck");

        String matchId = gameEngine.createMatch(p1, p2, deckBasics, deckFewBasics);
        gameEngine.dealCards(matchId);

        assumeTrue(gameEngine.getMatchState(matchId, P1_ID).getMulliganBenefitCount() > 0,
                "No hubo mulligan en esta iteración");

        completeSetup(matchId);

        MatchStateDTO stateP1 = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(stateP1.isMulliganResolutionPending()).isTrue();
        int handBeforeResolve = stateP1.getSelf().getHandSize();

        gameEngine.resolveMulliganDraw(matchId, P1_ID, 0);

        MatchStateDTO afterResolve = gameEngine.getMatchState(matchId, P1_ID);
        assertThat(afterResolve.getSelf().getHandSize()).isEqualTo(handBeforeResolve);
        assertThat(afterResolve.isMulliganResolutionPending()).isFalse();
        assertThat(afterResolve.getSelf().getRemainingPrizes()).isEqualTo(6);
    }

    /**
     * Validación — solo el beneficiario puede resolver el mulligan.
     */
    @RepeatedTest(5)
    void mulligan_NonBeneficiaryCannotResolve() {
        Player p1 = buildPlayer(P1_ID);
        Player p2 = buildPlayer(P2_ID);
        ar.edu.utn.frc.tup.piii.models.Deck deckBasics = buildAllBasicsDeck("basics-deck");
        ar.edu.utn.frc.tup.piii.models.Deck deckFewBasics = buildFewBasicsDeck("few-basics-deck");

        String matchId = gameEngine.createMatch(p1, p2, deckBasics, deckFewBasics);
        gameEngine.dealCards(matchId);

        assumeTrue(gameEngine.getMatchState(matchId, P1_ID).getMulliganBenefitCount() > 0,
                "No hubo mulligan en esta iteración");

        completeSetup(matchId);

        assertThatThrownBy(() -> gameEngine.resolveMulliganDraw(matchId, P2_ID, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only the beneficiary can resolve mulligan draws");
    }

    /**
     * Validación — no se pueden robar más cartas que las permitidas por el mulligan.
     */
    @RepeatedTest(5)
    void mulligan_CannotDrawMoreThanBenefitCount() {
        Player p1 = buildPlayer(P1_ID);
        Player p2 = buildPlayer(P2_ID);
        ar.edu.utn.frc.tup.piii.models.Deck deckBasics = buildAllBasicsDeck("basics-deck");
        ar.edu.utn.frc.tup.piii.models.Deck deckFewBasics = buildFewBasicsDeck("few-basics-deck");

        String matchId = gameEngine.createMatch(p1, p2, deckBasics, deckFewBasics);
        gameEngine.dealCards(matchId);

        int maxAllowed = gameEngine.getMatchState(matchId, P1_ID).getMulliganBenefitCount();
        assumeTrue(maxAllowed > 0, "No hubo mulligan en esta iteración");

        assertThatThrownBy(
                () -> gameEngine.resolveMulliganDraw(matchId, P1_ID, maxAllowed + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Count must be between 0 and " + maxAllowed);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void completeSetup(String matchId) {
        String p1Active = firstBasicInHand(matchId, P1_ID);
        String p2Active = firstBasicInHand(matchId, P2_ID);
        gameEngine.selectActivePokemon(matchId, P1_ID, p1Active);
        gameEngine.selectActivePokemon(matchId, P2_ID, p2Active);
        gameEngine.confirmSetupReady(matchId, P1_ID);
        gameEngine.confirmSetupReady(matchId, P2_ID);
    }

    private String firstBasicInHand(String matchId, String playerId) {
        return gameEngine.getMatchState(matchId, playerId).getSelf().getHand().stream()
                .filter(c -> c.getHp() != null)
                .map(MatchStateDTO.CardViewDTO::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay Pokémon Básico en la mano de " + playerId));
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

    private ar.edu.utn.frc.tup.piii.models.Deck buildAllBasicsDeck(String deckId) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < DECK_SIZE; i++) {
            cards.add(buildBasicPokemon(deckId + "-basic-" + i, "Basic-" + i));
        }
        ar.edu.utn.frc.tup.piii.models.Deck deck = new ar.edu.utn.frc.tup.piii.models.Deck();
        deck.setId(deckId);
        deck.setName("All Basics Deck");
        deck.setCards(cards);
        return deck;
    }

    private ar.edu.utn.frc.tup.piii.models.Deck buildFewBasicsDeck(String deckId) {
        List<Card> cards = new ArrayList<>();
        cards.add(buildBasicPokemon(deckId + "-basic-0", "Lone-Basic"));
        for (int i = 1; i < DECK_SIZE; i++) {
            cards.add(EnergyCard.builder()
                    .id(deckId + "-energy-" + i)
                    .name("Energy-" + i)
                    .energyType(EnergyType.COLORLESS)
                    .build());
        }
        ar.edu.utn.frc.tup.piii.models.Deck deck = new ar.edu.utn.frc.tup.piii.models.Deck();
        deck.setId(deckId);
        deck.setName("Few Basics Deck");
        deck.setCards(cards);
        return deck;
    }

    private PokemonCard buildBasicPokemon(String id, String name) {
        Attack attack = Attack.builder()
                .name("Tackle")
                .energyCost(Map.of())
                .baseDamage(20)
                .build();
        return PokemonCard.builder()
                .id(id)
                .name(name)
                .hp(60)
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
