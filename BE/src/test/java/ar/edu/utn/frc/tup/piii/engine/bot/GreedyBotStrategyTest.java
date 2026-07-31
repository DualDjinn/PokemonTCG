package ar.edu.utn.frc.tup.piii.engine.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GreedyBotStrategy}.
 *
 * <p>The greedy bot has three behavioural contracts that matter for gameplay
 * and that we exercise here:
 * <ol>
 *   <li>It never returns {@code null} — when it has no legal moves it must
 *       still propose {@code END_TURN} so the engine can advance.</li>
 *   <li>It picks the highest-scoring action according to
 *       {@link GameStateEvaluator} (no random choice when a clear winner
 *       exists).</li>
 *   <li>When several actions tie, it picks one of them at random; with a
 *       seeded RNG the choice is reproducible.</li>
 * </ol>
 */
class GreedyBotStrategyTest {

    @Test
    void emptyLegalActionsFallBackToEndTurn() {
        // BETWEEN_TURNS is a transient phase where LegalActionGenerator returns no actions.
        GameContext ctx = baseContext();
        ctx.setCurrentPhase(TurnPhase.BETWEEN_TURNS);

        GameAction action = new GreedyBotStrategy().chooseAction(ctx);

        assertEquals(GameActionType.END_TURN, action.getType(),
                "Greedy must never return null — END_TURN is the safe fallback");
    }

    @Test
    void greedyPicksAttackThatKnocksTheDefenderOut() {
        // Active charmander with 1 fire energy and Ember (10 dmg) facing a 10-HP defender.
        // The KO bonus inside scoreAction should make ATTACK dominate END_TURN.
        Attack ember = attack("Ember", Map.of(EnergyType.FIRE, 1), 10);
        InPlayPokemon attacker = new InPlayPokemon(basic("Charmander", List.of(ember)));
        attacker.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        InPlayPokemon defender = new InPlayPokemon(basic("Caterpie", List.of()));
        // Pre-damage the defender so a 10-dmg Ember actually KOs it deterministically.
        defender.setDamageTaken(defender.getCard().getHp() - 5);

        GameContext ctx = attackPhaseContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);

        GameAction picked = new GreedyBotStrategy(new Random(0)).chooseAction(ctx);

        assertEquals(GameActionType.ATTACK, picked.getType(),
                "When a knockout is available the greedy bot must take it");
        assertEquals("Ember", picked.getAttackName());
    }

    @Test
    void greedyPrefersAdvancingToAttackOverPassingTheTurn() {
        // Main phase, empty hand. The only options are ADVANCE_TO_ATTACK (score base+1)
        // — END_TURN is not even on the table because we are not in ATTACK.
        GameContext ctx = baseContext();

        GameAction picked = new GreedyBotStrategy(new Random(0)).chooseAction(ctx);

        assertEquals(GameActionType.ADVANCE_TO_ATTACK, picked.getType());
    }

    @Test
    void tieBreakIsReproducibleUnderSeededRandom() {
        // Two attacks with the same damage and identical cost → identical scores.
        // The reservoir-sampling tie-break must be deterministic under a fixed seed.
        Attack a = attack("Spark", Map.of(EnergyType.LIGHTNING, 1), 20);
        Attack b = attack("Bolt", Map.of(EnergyType.LIGHTNING, 1), 20);
        InPlayPokemon attacker = new InPlayPokemon(basic("Pikachu", List.of(a, b)));
        attacker.getEnergies().add(EnergyCard.builder().energyType(EnergyType.LIGHTNING).build());
        GameContext ctx = attackPhaseContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(new InPlayPokemon(basic("Magikarp", List.of())));

        GameAction firstRun = new GreedyBotStrategy(new Random(42)).chooseAction(ctx);
        GameAction secondRun = new GreedyBotStrategy(new Random(42)).chooseAction(ctx);

        assertEquals(firstRun.getAttackName(), secondRun.getAttackName(),
                "Same seed must yield the same tie-break choice");
    }

    @Test
    void nullRandomIsToleratedByFallingBackToFreshRandom() {
        // Defensive: the constructor explicitly tolerates null random, so the bot
        // must still produce a legal action without throwing NPE.
        GameContext ctx = baseContext();

        GameAction picked = new GreedyBotStrategy(null).chooseAction(ctx);

        assertNotNull(picked);
    }

    @Test
    void noArgConstructorUsesAFreshRandomAndStillProducesAction() {
        GameContext ctx = baseContext();

        GameAction picked = new GreedyBotStrategy().chooseAction(ctx);

        assertNotNull(picked);
        // The only legal action in the base context is ADVANCE_TO_ATTACK; the no-arg
        // ctor wires a real Random but does not change the highest-scoring choice.
        assertEquals(GameActionType.ADVANCE_TO_ATTACK, picked.getType());
    }

    @Test
    void singleActionIsReturnedWithoutInvokingRandom() {
        // If there is exactly one legal action there is nothing to tie-break.
        // We pass a random whose nextInt would explode if it were ever called.
        GameContext ctx = baseContext();
        GameAction picked = new GreedyBotStrategy(new ExplodingRandom()).chooseAction(ctx);
        assertEquals(GameActionType.ADVANCE_TO_ATTACK, picked.getType());
    }

    @Test
    void laterCandidateReplacesBestWhenItsScoreIsStrictlyHigher() {
        // Two basics in hand, the high-HP one comes second.
        // LegalActionGenerator iterates hand in order, so the low-HP basic is the
        // initial best and the loop must overwrite it when the high-HP candidate
        // appears with a higher scorePlayBasic delta. Covers the "score > bestScore" branch.
        PokemonCard lowHp = PokemonCard.builder()
                .id("low").name("Caterpie").hp(40).stage(PokemonStage.BASIC).attacks(List.of()).build();
        PokemonCard highHp = PokemonCard.builder()
                .id("high").name("Wailord").hp(200).stage(PokemonStage.BASIC).attacks(List.of()).build();
        GameContext ctx = baseContext();
        ctx.setAttackerHand(List.of(lowHp, highHp));

        GameAction picked = new GreedyBotStrategy(new Random(0)).chooseAction(ctx);

        assertEquals(GameActionType.PLAY_BASIC, picked.getType());
        assertEquals("high", picked.getCardId(),
                "Greedy must replace the initial best when a strictly better candidate appears later");
    }

    @Test
    void tieBreakActuallySwapsBestWhenReservoirSamplingHits() {
        // Two basics with identical HP → identical scores → reservoir sampling kicks in.
        // We feed a Random whose nextInt(ties) returns 0 every time so the swap happens.
        PokemonCard a = PokemonCard.builder()
                .id("a").name("Voltorb").hp(60).stage(PokemonStage.BASIC).attacks(List.of()).build();
        PokemonCard b = PokemonCard.builder()
                .id("b").name("Magnemite").hp(60).stage(PokemonStage.BASIC).attacks(List.of()).build();
        GameContext ctx = baseContext();
        ctx.setAttackerHand(List.of(a, b));

        GameAction picked = new GreedyBotStrategy(new AlwaysZeroRandom()).chooseAction(ctx);

        assertEquals(GameActionType.PLAY_BASIC, picked.getType());
        assertEquals("b", picked.getCardId(),
                "When random.nextInt(ties)==0 the new tied candidate must overwrite the current best");
    }

    @Test
    void attackThatDoesNotKnockoutStillBeatsEndTurn() {
        // 10 dmg vs full-HP defender → no KO bonus, but ATTACK is still better than END_TURN.
        Attack ember = attack("Ember", Map.of(EnergyType.FIRE, 1), 10);
        InPlayPokemon attacker = new InPlayPokemon(basic("Charmander", List.of(ember)));
        attacker.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        InPlayPokemon defender = new InPlayPokemon(basic("Snorlax", List.of()));
        GameContext ctx = attackPhaseContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);

        GameAction picked = new GreedyBotStrategy(new Random(0)).chooseAction(ctx);

        assertTrue(picked.getType() == GameActionType.ATTACK,
                "Even without a KO, attacking should outscore the −5 END_TURN penalty");
    }

    // ───────────────────────────── helpers ────────────────────────────────────

    private GameContext baseContext() {
        GameContext ctx = new GameContext();
        ctx.setCurrentPhase(TurnPhase.MAIN);
        ctx.setFirstTurn(false);
        ctx.setAttackerHand(new ArrayList<>());
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setDefenderBench(new ArrayList<>());
        ctx.setAttackerPrizes(prizes(6));
        ctx.setDefenderPrizes(prizes(6));
        ctx.setAttackerEvolvedThisTurn(new HashSet<>());
        return ctx;
    }

    private GameContext attackPhaseContext() {
        GameContext ctx = baseContext();
        ctx.setCurrentPhase(TurnPhase.ATTACK);
        return ctx;
    }

    private List<ar.edu.utn.frc.tup.piii.models.Card> prizes(int n) {
        List<ar.edu.utn.frc.tup.piii.models.Card> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(null);
        return list;
    }

    private PokemonCard basic(String name, List<Attack> attacks) {
        return PokemonCard.builder()
                .name(name)
                .hp(60)
                .stage(PokemonStage.BASIC)
                .attacks(attacks)
                .build();
    }

    private Attack attack(String name, Map<EnergyType, Integer> cost, int dmg) {
        return Attack.builder().name(name).energyCost(cost).baseDamage(dmg).build();
    }

    /** Random whose {@code nextInt} throws — proves no tie-break path was taken. */
    private static final class ExplodingRandom extends Random {
        private static final long serialVersionUID = 1L;

        @Override
        public int nextInt(int bound) {
            throw new AssertionError("Random.nextInt was called when no tie existed");
        }
    }

    /** Random whose {@code nextInt} always returns 0 — forces reservoir swap on every tie. */
    private static final class AlwaysZeroRandom extends Random {
        private static final long serialVersionUID = 1L;

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }
}
