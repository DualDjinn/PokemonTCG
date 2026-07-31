package ar.edu.utn.frc.tup.piii.engine.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.ItemCard;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.SupporterCard;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LegalActionGenerator}. They exercise every branch by
 * building a minimal {@link GameContext} per scenario. No Spring context is
 * loaded; the tests run as pure JUnit so the suite stays fast.
 *
 * <p>Each test pins one rule from the engine's rulebook (first-turn lock,
 * energy attachment quota, supporter-per-turn limit, etc.) and asserts that
 * the action set produced for the bot reflects that rule. This mirrors the
 * validations enforced by {@code RuleValidator} and keeps the bot search space
 * aligned with what the engine will actually accept.
 */
class LegalActionGeneratorTest {

    // ───────────────────────────── phase handling ─────────────────────────────

    @Test
    void mainPhaseAlwaysIncludesAdvanceToAttackEvenWithEmptyHand() {
        GameContext ctx = mainPhaseContext();
        ctx.setAttackerHand(new ArrayList<>());

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, actions.size(), "Empty hand in MAIN should yield only the fallback action");
        assertEquals(GameActionType.ADVANCE_TO_ATTACK, actions.get(0).getType());
    }

    @Test
    void attackPhaseAlwaysIncludesEndTurnAsFallback() {
        GameContext ctx = attackPhaseContext(basic("Pikachu", List.of()));

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertTrue(actions.stream().anyMatch(a -> a.getType() == GameActionType.END_TURN),
                "Attack phase must always allow ending the turn without attacking");
    }

    @Test
    void nullPhaseDefaultsToMain() {
        GameContext ctx = mainPhaseContext();
        ctx.setCurrentPhase(null);
        ctx.setAttackerHand(new ArrayList<>());

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(GameActionType.ADVANCE_TO_ATTACK, actions.get(0).getType(),
                "When phase is null the generator must treat it as MAIN");
    }

    @Test
    void drawPhaseProducesNoActions() {
        GameContext ctx = mainPhaseContext();
        ctx.setCurrentPhase(TurnPhase.DRAW);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertTrue(actions.isEmpty(), "DRAW is transient — bots do not act on it");
    }

    @Test
    void betweenTurnsPhaseProducesNoActions() {
        GameContext ctx = mainPhaseContext();
        ctx.setCurrentPhase(TurnPhase.BETWEEN_TURNS);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertTrue(actions.isEmpty(), "BETWEEN_TURNS is transient — bots do not act on it");
    }

    @Test
    void nullHandDoesNotThrowAndStillEmitsAdvance() {
        GameContext ctx = mainPhaseContext();
        ctx.setAttackerHand(null);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, actions.size());
        assertEquals(GameActionType.ADVANCE_TO_ATTACK, actions.get(0).getType());
    }

    // ───────────────────────────── playing basics ─────────────────────────────

    @Test
    void basicPokemonInHandGeneratesPlayBasicWhenBenchHasRoom() {
        PokemonCard pikachu = basicWithId("xy1-001", "Pikachu", List.of());
        GameContext ctx = mainPhaseContext();
        ctx.setAttackerHand(new ArrayList<>(List.of(pikachu)));
        ctx.setAttackerBench(new ArrayList<>());

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, countByType(actions, GameActionType.PLAY_BASIC));
    }

    @Test
    void basicPokemonInHandIsRejectedWhenBenchIsFull() {
        PokemonCard pikachu = basicWithId("xy1-001", "Pikachu", List.of());
        GameContext ctx = mainPhaseContext();
        ctx.setAttackerHand(new ArrayList<>(List.of(pikachu)));
        // 5 benched pokemon means MAX_BENCH_SIZE reached — no more basics legal.
        List<InPlayPokemon> fullBench = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fullBench.add(new InPlayPokemon(basic("Filler", List.of())));
        }
        ctx.setAttackerBench(fullBench);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.PLAY_BASIC),
                "Bench cap is 5; basic plays must be blocked when full");
    }

    @Test
    void evolutionInHandIsNotConfusedForABasicPlay() {
        // A Stage 1 card sitting in hand must not produce PLAY_BASIC.
        PokemonCard charmeleon = evolution("Charmeleon", "Charmander");
        GameContext ctx = mainPhaseContext();
        ctx.setAttackerHand(new ArrayList<>(List.of(charmeleon)));
        ctx.setAttackerBench(new ArrayList<>());

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.PLAY_BASIC));
    }

    // ───────────────────────────── evolution rules ────────────────────────────

    @Test
    void evolutionIsBlockedDuringFirstTurn() {
        PokemonCard charmander = basic("Charmander", List.of());
        PokemonCard charmeleon = evolution("Charmeleon", "Charmander");
        InPlayPokemon active = new InPlayPokemon(charmander);
        GameContext ctx = mainPhaseContext();
        ctx.setFirstTurn(true);
        ctx.setAttacker(active);
        ctx.setAttackerHand(new ArrayList<>(List.of(charmeleon)));

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.EVOLVE),
                "Evolution is forbidden during the opening turn");
    }

    @Test
    void evolutionOnActivePokemonIsGeneratedWhenLegal() {
        PokemonCard charmander = basic("Charmander", List.of());
        PokemonCard charmeleon = evolutionWithId("xy1-002", "Charmeleon", "Charmander");
        InPlayPokemon active = new InPlayPokemon(charmander);
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(active);
        ctx.setAttackerHand(new ArrayList<>(List.of(charmeleon)));

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, countByType(actions, GameActionType.EVOLVE));
        GameAction evolve = firstByType(actions, GameActionType.EVOLVE);
        assertEquals(active.getInstanceId(), evolve.getTargetCardId());
    }

    @Test
    void evolutionTargetCanBeOnBench() {
        PokemonCard squirtle = basic("Squirtle", List.of());
        PokemonCard wartortle = evolution("Wartortle", "Squirtle");
        InPlayPokemon benched = new InPlayPokemon(squirtle);
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(new InPlayPokemon(basic("Bulbasaur", List.of()))); // unrelated active
        ctx.setAttackerBench(new ArrayList<>(List.of(benched)));
        ctx.setAttackerHand(new ArrayList<>(List.of(wartortle)));

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        GameAction evolve = firstByType(actions, GameActionType.EVOLVE);
        assertEquals(benched.getInstanceId(), evolve.getTargetCardId());
    }

    @Test
    void evolutionIsBlockedForPokemonPlayedThisTurn() {
        PokemonCard charmander = basic("Charmander", List.of());
        PokemonCard charmeleon = evolution("Charmeleon", "Charmander");
        InPlayPokemon active = new InPlayPokemon(charmander);
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(active);
        ctx.setAttackerHand(new ArrayList<>(List.of(charmeleon)));
        // Pokemon was put into play this turn → summoning sickness, cannot evolve yet.
        ctx.setAttackerPokemonPlayedThisTurn(new ArrayList<>(List.of(charmander)));

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.EVOLVE));
    }

    @Test
    void evolutionIsBlockedForPokemonThatAlreadyEvolvedThisTurn() {
        PokemonCard charmander = basic("Charmander", List.of());
        PokemonCard charmeleon = evolution("Charmeleon", "Charmander");
        InPlayPokemon active = new InPlayPokemon(charmander);
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(active);
        ctx.setAttackerHand(new ArrayList<>(List.of(charmeleon)));
        // Already evolved once this turn → cannot chain to Stage 2 in the same turn.
        ctx.getAttackerEvolvedThisTurn().add(active.getInstanceId());

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.EVOLVE));
    }

    @Test
    void evolutionWithoutEvolvesFromIsIgnored() {
        // Defensive coverage: an evolution card with no evolvesFrom (data bug)
        // should be silently skipped rather than crash or evolve anything.
        PokemonCard orphan = evolution("Orphan", null);
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(new InPlayPokemon(basic("Bulbasaur", List.of())));
        ctx.setAttackerHand(new ArrayList<>(List.of(orphan)));

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.EVOLVE));
    }

    @Test
    void evolutionWithoutMatchingBasicProducesNoAction() {
        PokemonCard charmander = basic("Charmander", List.of());
        PokemonCard wartortle = evolution("Wartortle", "Squirtle"); // looking for Squirtle, not here
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(new InPlayPokemon(charmander));
        ctx.setAttackerHand(new ArrayList<>(List.of(wartortle)));

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.EVOLVE));
    }

    // ─────────────────────────── energy attachments ───────────────────────────

    @Test
    void energyAttachIsOfferedForActiveAndEveryBenchedPokemon() {
        EnergyCard fire = EnergyCard.builder().id("energy-fire").energyType(EnergyType.FIRE).build();
        InPlayPokemon active = new InPlayPokemon(basic("Charmander", List.of()));
        InPlayPokemon benchedA = new InPlayPokemon(basic("Squirtle", List.of()));
        InPlayPokemon benchedB = new InPlayPokemon(basic("Bulbasaur", List.of()));
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(active);
        ctx.setAttackerBench(new ArrayList<>(List.of(benchedA, benchedB)));
        ctx.setAttackerHand(new ArrayList<>(List.of(fire)));

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        // 1 active + 2 benched → 3 ATTACH_ENERGY actions.
        assertEquals(3, countByType(actions, GameActionType.ATTACH_ENERGY));
    }

    @Test
    void energyAttachIsBlockedAfterTheOnePerTurnQuotaIsSpent() {
        EnergyCard fire = EnergyCard.builder().energyType(EnergyType.FIRE).build();
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(new InPlayPokemon(basic("Charmander", List.of())));
        ctx.setAttackerHand(new ArrayList<>(List.of(fire)));
        // Manual energy already attached this turn.
        ctx.setAttackerEnergyAttachmentsThisTurn(1);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.ATTACH_ENERGY));
    }

    @Test
    void additionalEnergyAttachmentsGrantedByEffectsAreHonoured() {
        EnergyCard fire = EnergyCard.builder().energyType(EnergyType.FIRE).build();
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(new InPlayPokemon(basic("Charmander", List.of())));
        ctx.setAttackerHand(new ArrayList<>(List.of(fire)));
        // Already attached once but a card grants +1 extra → still legal.
        ctx.setAttackerEnergyAttachmentsThisTurn(1);
        ctx.setAttackerAdditionalEnergyAttachments(1);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertTrue(countByType(actions, GameActionType.ATTACH_ENERGY) > 0,
                "Bonus attachments must be added to the per-turn quota");
    }

    @Test
    void negativeAdditionalAttachmentsAreClampedToZero() {
        // Math.max(0, …) branch — a corrupted negative bonus must not unlock extra plays.
        EnergyCard fire = EnergyCard.builder().energyType(EnergyType.FIRE).build();
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(new InPlayPokemon(basic("Charmander", List.of())));
        ctx.setAttackerHand(new ArrayList<>(List.of(fire)));
        ctx.setAttackerEnergyAttachmentsThisTurn(1);
        ctx.setAttackerAdditionalEnergyAttachments(-5);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.ATTACH_ENERGY));
    }

    @Test
    void energyAttachIgnoresNullBenchSlots() {
        EnergyCard fire = EnergyCard.builder().energyType(EnergyType.FIRE).build();
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(new InPlayPokemon(basic("Charmander", List.of())));
        // Bench with a null slot (mid-game vacancies are represented as nulls).
        List<InPlayPokemon> bench = new ArrayList<>();
        bench.add(null);
        bench.add(new InPlayPokemon(basic("Squirtle", List.of())));
        ctx.setAttackerBench(bench);
        ctx.setAttackerHand(new ArrayList<>(List.of(fire)));

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        // active + 1 real benched = 2 attachments, not 3.
        assertEquals(2, countByType(actions, GameActionType.ATTACH_ENERGY));
    }

    // ───────────────────────────── trainer cards ──────────────────────────────

    @Test
    void itemCardsAreAlwaysOfferedRegardlessOfSupporterLimit() {
        ItemCard potion = ItemCard.builder().id("xy1-potion").build();
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(new InPlayPokemon(basic("Pikachu", List.of())));
        ctx.setAttackerHand(new ArrayList<>(List.of(potion)));
        // Even with the supporter slot already used, items remain legal.
        ctx.setAttackerSupportersPlayedThisTurn(1);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, countByType(actions, GameActionType.PLAY_ITEM));
    }

    @Test
    void supporterIsOfferedOnceWhenNoneHasBeenPlayed() {
        SupporterCard sycamore = SupporterCard.builder().id("xy1-sycamore").build();
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(new InPlayPokemon(basic("Pikachu", List.of())));
        ctx.setAttackerHand(new ArrayList<>(List.of(sycamore)));

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, countByType(actions, GameActionType.PLAY_SUPPORTER));
    }

    @Test
    void supporterIsBlockedWhenTheTurnAlreadyUsedItsSupporter() {
        SupporterCard sycamore = SupporterCard.builder().build();
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(new InPlayPokemon(basic("Pikachu", List.of())));
        ctx.setAttackerHand(new ArrayList<>(List.of(sycamore)));
        ctx.setAttackerSupportersPlayedThisTurn(1);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.PLAY_SUPPORTER),
                "Only one Supporter per turn is allowed");
    }

    @Test
    void supporterIsBlockedWhenAnEffectForbidsSupporters() {
        SupporterCard sycamore = SupporterCard.builder().build();
        GameContext ctx = mainPhaseContext();
        ctx.setAttacker(new InPlayPokemon(basic("Pikachu", List.of())));
        ctx.setAttackerHand(new ArrayList<>(List.of(sycamore)));
        ctx.setAttackerCantPlaySupporters(true);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.PLAY_SUPPORTER));
    }

    // ───────────────────────────── attack phase ───────────────────────────────

    @Test
    void attackIsForbiddenDuringFirstTurn() {
        Attack ember = attackCosting("Ember", Map.of(EnergyType.FIRE, 1));
        InPlayPokemon active = new InPlayPokemon(basic("Charmander", List.of(ember)));
        attachEnergies(active, EnergyType.FIRE);
        GameContext ctx = attackPhaseContext(active.getCard());
        ctx.setAttacker(active);
        ctx.setFirstTurn(true);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.ATTACK),
                "Attacks are forbidden on the opening turn even with enough energy");
    }

    @Test
    void attackIsBlockedWhenActiveIsAsleep() {
        Attack ember = attackCosting("Ember", Map.of(EnergyType.FIRE, 1));
        InPlayPokemon active = new InPlayPokemon(basic("Charmander", List.of(ember)));
        attachEnergies(active, EnergyType.FIRE);
        active.applyStatus(StatusCondition.ASLEEP);
        GameContext ctx = attackPhaseContext(active.getCard());
        ctx.setAttacker(active);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.ATTACK));
    }

    @Test
    void attackIsBlockedWhenActiveIsParalyzed() {
        Attack ember = attackCosting("Ember", Map.of(EnergyType.FIRE, 1));
        InPlayPokemon active = new InPlayPokemon(basic("Charmander", List.of(ember)));
        attachEnergies(active, EnergyType.FIRE);
        active.applyStatus(StatusCondition.PARALYZED);
        GameContext ctx = attackPhaseContext(active.getCard());
        ctx.setAttacker(active);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.ATTACK));
    }

    @Test
    void attackIsBlockedWhenCantAttackFlagIsSet() {
        Attack ember = attackCosting("Ember", Map.of(EnergyType.FIRE, 1));
        InPlayPokemon active = new InPlayPokemon(basic("Charmander", List.of(ember)));
        attachEnergies(active, EnergyType.FIRE);
        active.setCantAttack(true);
        GameContext ctx = attackPhaseContext(active.getCard());
        ctx.setAttacker(active);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.ATTACK));
    }

    @Test
    void attackIsEmittedWhenAllEnergyRequirementsAreMet() {
        Attack ember = attackCosting("Ember", Map.of(EnergyType.FIRE, 1));
        InPlayPokemon active = new InPlayPokemon(basic("Charmander", List.of(ember)));
        attachEnergies(active, EnergyType.FIRE);
        GameContext ctx = attackPhaseContext(active.getCard());
        ctx.setAttacker(active);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, countByType(actions, GameActionType.ATTACK));
        assertEquals("Ember", firstByType(actions, GameActionType.ATTACK).getAttackName());
    }

    @Test
    void attackIsBlockedWhenSpecificEnergyTypeIsMissing() {
        // 1 Water energy attached but Ember requires 1 Fire — not enough by type.
        Attack ember = attackCosting("Ember", Map.of(EnergyType.FIRE, 1));
        InPlayPokemon active = new InPlayPokemon(basic("Charmander", List.of(ember)));
        attachEnergies(active, EnergyType.WATER);
        GameContext ctx = attackPhaseContext(active.getCard());
        ctx.setAttacker(active);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.ATTACK));
    }

    @Test
    void attackIsBlockedWhenColorlessSlotsExceedTotalAttached() {
        // Attack cost: 1 Fire + 1 Colorless = 2 total. Only 1 Fire attached → fails total count.
        Attack flamethrower = attackCosting("Flamethrower",
                Map.of(EnergyType.FIRE, 1, EnergyType.COLORLESS, 1));
        InPlayPokemon active = new InPlayPokemon(basic("Charmander", List.of(flamethrower)));
        attachEnergies(active, EnergyType.FIRE);
        GameContext ctx = attackPhaseContext(active.getCard());
        ctx.setAttacker(active);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(0, countByType(actions, GameActionType.ATTACK));
    }

    @Test
    void colorlessCostIsPayableByAnyEnergyType() {
        // Pure 2-Colorless attack can be paid with 2 of any type.
        Attack tackle = attackCosting("Tackle", Map.of(EnergyType.COLORLESS, 2));
        InPlayPokemon active = new InPlayPokemon(basic("Snorlax", List.of(tackle)));
        attachEnergies(active, EnergyType.PSYCHIC, EnergyType.PSYCHIC);
        GameContext ctx = attackPhaseContext(active.getCard());
        ctx.setAttacker(active);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, countByType(actions, GameActionType.ATTACK));
    }

    @Test
    void attackWithoutEnergyCostIsAlwaysPlayable() {
        Attack free = attackCosting("Struggle", Map.of());
        InPlayPokemon active = new InPlayPokemon(basic("Pikachu", List.of(free)));
        GameContext ctx = attackPhaseContext(active.getCard());
        ctx.setAttacker(active);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, countByType(actions, GameActionType.ATTACK));
    }

    @Test
    void attackByExactNameRestrictionIsExcluded() {
        // Pokemon has two attacks, one of them is currently restricted by name.
        Attack ember = attackCosting("Ember", Map.of(EnergyType.FIRE, 1));
        Attack flare = attackCosting("Flare", Map.of(EnergyType.FIRE, 1));
        InPlayPokemon active = new InPlayPokemon(basic("Charmander", List.of(ember, flare)));
        attachEnergies(active, EnergyType.FIRE, EnergyType.FIRE);
        active.setCantUseAttackName("Flare");
        GameContext ctx = attackPhaseContext(active.getCard());
        ctx.setAttacker(active);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, countByType(actions, GameActionType.ATTACK));
        assertEquals("Ember", firstByType(actions, GameActionType.ATTACK).getAttackName());
    }

    @Test
    void nullAttacksInTheListAreSkippedSafely() {
        Attack ember = attackCosting("Ember", Map.of(EnergyType.FIRE, 1));
        List<Attack> attacks = new ArrayList<>();
        attacks.add(null);
        attacks.add(ember);
        InPlayPokemon active = new InPlayPokemon(basic("Charmander", attacks));
        attachEnergies(active, EnergyType.FIRE);
        GameContext ctx = attackPhaseContext(active.getCard());
        ctx.setAttacker(active);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, countByType(actions, GameActionType.ATTACK));
    }

    @Test
    void activeWithoutAttackListProducesOnlyEndTurn() {
        // attacks=null means the card simply has no attacks defined (data edge case).
        PokemonCard attackless = PokemonCard.builder()
                .name("Pikachu")
                .hp(60)
                .stage(PokemonStage.BASIC)
                .attacks(null)
                .build();
        InPlayPokemon active = new InPlayPokemon(attackless);
        GameContext ctx = attackPhaseContext(attackless);
        ctx.setAttacker(active);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, actions.size());
        assertEquals(GameActionType.END_TURN, actions.get(0).getType());
    }

    @Test
    void nullActiveInAttackPhaseStillEmitsEndTurnFallback() {
        GameContext ctx = mainPhaseContext();
        ctx.setCurrentPhase(TurnPhase.ATTACK);
        ctx.setAttacker(null);

        List<GameAction> actions = LegalActionGenerator.generate(ctx);

        assertEquals(1, actions.size());
        assertEquals(GameActionType.END_TURN, actions.get(0).getType());
    }

    // ───────────────────────────── helpers ────────────────────────────────────

    private GameContext mainPhaseContext() {
        GameContext ctx = new GameContext();
        ctx.setCurrentPhase(TurnPhase.MAIN);
        // GameContext defaults firstTurn=true; flip it for the general-case scenarios.
        // Tests that exercise the opening-turn lock flip it back to true explicitly.
        ctx.setFirstTurn(false);
        ctx.setAttackerHand(new ArrayList<>());
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setAttackerEvolvedThisTurn(new HashSet<>());
        return ctx;
    }

    private GameContext attackPhaseContext(PokemonCard activeCard) {
        GameContext ctx = mainPhaseContext();
        ctx.setCurrentPhase(TurnPhase.ATTACK);
        ctx.setAttacker(new InPlayPokemon(activeCard));
        return ctx;
    }

    private PokemonCard basic(String name, List<Attack> attacks) {
        return basicWithId(null, name, attacks);
    }

    private PokemonCard basicWithId(String id, String name, List<Attack> attacks) {
        return PokemonCard.builder()
                .id(id)
                .name(name)
                .hp(60)
                .stage(PokemonStage.BASIC)
                .attacks(attacks)
                .build();
    }

    private PokemonCard evolution(String name, String evolvesFrom) {
        return evolutionWithId(null, name, evolvesFrom);
    }

    private PokemonCard evolutionWithId(String id, String name, String evolvesFrom) {
        return PokemonCard.builder()
                .id(id)
                .name(name)
                .hp(80)
                .stage(PokemonStage.STAGE_1)
                .attacks(List.of())
                .evolvesFrom(evolvesFrom)
                .build();
    }

    private Attack attackCosting(String name, Map<EnergyType, Integer> cost) {
        return Attack.builder()
                .name(name)
                .energyCost(cost)
                .baseDamage(10)
                .build();
    }

    private void attachEnergies(InPlayPokemon pokemon, EnergyType... types) {
        for (EnergyType t : types) {
            pokemon.getEnergies().add(EnergyCard.builder().energyType(t).build());
        }
    }

    private int countByType(List<GameAction> actions, GameActionType type) {
        return (int) actions.stream().filter(a -> a.getType() == type).count();
    }

    private GameAction firstByType(List<GameAction> actions, GameActionType type) {
        return actions.stream().filter(a -> a.getType() == type).findFirst().orElseThrow();
    }

    /** Defensive sanity: the helper above must actually filter, not return everything. */
    @Test
    void countByTypeHelperFiltersByActionType() {
        List<GameAction> mixed = List.of(GameAction.endTurn(), GameAction.advanceToAttack());
        assertEquals(1, countByType(mixed, GameActionType.END_TURN));
        assertFalse(countByType(mixed, GameActionType.ATTACK) > 0);
    }
}
