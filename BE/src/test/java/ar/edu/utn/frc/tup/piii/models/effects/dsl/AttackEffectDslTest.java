package ar.edu.utn.frc.tup.piii.models.effects.dsl;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Conditions.attackerBenchHasCardId;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Conditions.attackerDamaged;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Conditions.attackerHasEnergyOf;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Conditions.defenderDamaged;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Conditions.defenderHasAnyStatus;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Conditions.defenderHasStatus;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Conditions.defenderIsEx;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Conditions.defenderType;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Counts.attackerBenchCount;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Counts.attackerDistinctEnergyTypes;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Counts.attackerEnergy;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Counts.bothActiveEnergyTotal;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Counts.coinHeads;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Counts.coinHeadsOf;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Counts.coinHeadsUntilTails;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Counts.defenderDamageCounters;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.addDamage;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.addDamagePer;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.applyStatus;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.applyStatusToAttacker;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.attackerCantAttack;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.attackerCantUseAttackName;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.attackerPreventAllEffects;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.attackerPreventAllIncomingDamage;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.cancelDamage;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.clearAttackerStatuses;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.clearDefenderStatuses;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.damageEachAttackerBench;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.discardDefenderDeckTop;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.flip;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.heal;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.healAllAttackerPokemon;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.multiplyDamageBy;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.selfDamage;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.seq;
import static ar.edu.utn.frc.tup.piii.models.effects.dsl.Effects.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Proves that DSL recipes reproduce the behaviour of the hand-written effect classes
 * they are intended to replace. Each test mirrors a scenario from
 * {@code CommonAttackEffectsTest} / the isolated effect tests, expressed as a recipe.
 */
class AttackEffectDslTest {

    /** Deterministic coin source: {@code nextBoolean()} cycles through the given values. */
    private static final class FixedRandom extends Random {
        private final boolean[] values;
        private int idx;

        FixedRandom(boolean... values) {
            this.values = values.clone();
            this.idx = 0;
        }

        @Override
        public boolean nextBoolean() {
            return values[idx++ % values.length];
        }
    }

    private GameContext ctx;
    private InPlayPokemon attacker;
    private InPlayPokemon defender;
    private PokemonCard basePokemon;

    @BeforeEach
    void setUp() {
        basePokemon = PokemonCard.builder()
                .id("p1").name("TestMon").hp(100)
                .stage(PokemonStage.BASIC)
                .build();
        attacker = new InPlayPokemon(basePokemon);
        defender = new InPlayPokemon(basePokemon);
        ctx = new GameContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);
        ctx.setPendingDamage(50);
        // GameContext seeds benches with 5 null slots; the DSL counters/effects expect a compact bench.
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setDefenderBench(new ArrayList<>());
    }

    // ── Atomic leaves ────────────────────────────────────────────────

    @Test
    void addDamage_RaisesPendingDamage() {
        ctx.setPendingDamage(30);
        addDamage(20).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    @Test
    void heal_FloorsAtZero() {
        attacker.setDamageTaken(10);
        heal(30).apply(ctx);
        assertThat(attacker.getDamageTaken()).isZero();
    }

    @Test
    void selfDamage_AccumulatesRecoil() {
        attacker.setDamageTaken(20);
        selfDamage(10).apply(ctx);
        assertThat(attacker.getDamageTaken()).isEqualTo(30);
    }

    // ── flip() gate == DynamicPunch / coin-flip family ───────────────

    @Test
    void flipGate_OnHeads_AppliesDamageAndStatus() {
        ctx.setPendingDamage(50);
        ctx.setRandom(new FixedRandom(true));
        AttackEffect dynamicPunch = flip().onHeads(addDamage(40), applyStatus(StatusCondition.CONFUSED));
        dynamicPunch.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(90);
        assertThat(defender.hasStatus(StatusCondition.CONFUSED)).isTrue();
    }

    @Test
    void flipGate_OnTails_DoesNothing() {
        ctx.setPendingDamage(50);
        ctx.setRandom(new FixedRandom(false));
        AttackEffect dynamicPunch = flip().onHeads(addDamage(40), applyStatus(StatusCondition.CONFUSED));
        dynamicPunch.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
        assertThat(defender.hasStatus(StatusCondition.CONFUSED)).isFalse();
    }

    @Test
    void flipGate_WithBothBranches_OnTails_RunsTailsBranch() {
        // Distortion Beam: heads → ASLEEP, tails → CONFUSED.
        ctx.setRandom(new FixedRandom(false));
        AttackEffect distortionBeam = flip()
                .onHeads(applyStatus(StatusCondition.ASLEEP))
                .onTails(applyStatus(StatusCondition.CONFUSED));
        distortionBeam.apply(ctx);
        assertThat(defender.hasStatus(StatusCondition.CONFUSED)).isTrue();
        assertThat(defender.hasStatus(StatusCondition.ASLEEP)).isFalse();
    }

    // ── addDamagePer / multiplyDamageBy == the ×N / +per N family ────

    @Test
    void multiplyDamageBy_BenchCount_EqualsCircleCircuit() {
        ctx.setPendingDamage(20);
        ctx.getAttackerBench().addAll(List.of(
                new InPlayPokemon(basePokemon),
                new InPlayPokemon(basePokemon),
                new InPlayPokemon(basePokemon)));
        multiplyDamageBy(attackerBenchCount()).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(60);
    }

    @Test
    void multiplyDamageBy_EmptyBench_YieldsZero() {
        ctx.setPendingDamage(20);
        multiplyDamageBy(attackerBenchCount()).apply(ctx);
        assertThat(ctx.getPendingDamage()).isZero();
    }

    @Test
    void addDamagePer_CoinHeads_EqualsMultiCoinBonus() {
        ctx.setPendingDamage(20);
        ctx.setRandom(new FixedRandom(true, true));
        addDamagePer(coinHeads(2), 20).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(60);
    }

    @Test
    void multiplyDamageBy_CoinHeads_EqualsMultiCoinDamage() {
        ctx.setPendingDamage(10);
        ctx.setRandom(new FixedRandom(true, true, true, false));
        multiplyDamageBy(coinHeads(4)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(30);
    }

    @Test
    void addDamagePer_AttackerEnergy_EqualsEnergyCountBonus() {
        ctx.setPendingDamage(40);
        attacker.getEnergies().addAll(List.of(
                EnergyCard.builder().id("e1").energyType(EnergyType.WATER).build(),
                EnergyCard.builder().id("e2").energyType(EnergyType.WATER).build()));
        addDamagePer(attackerEnergy(EnergyType.WATER), 20).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(80);
    }

    @Test
    void addDamagePer_DefenderDamageCounters_EqualsDamageCounterBonusOpponent() {
        ctx.setPendingDamage(20);
        defender.setDamageTaken(40);
        addDamagePer(defenderDamageCounters(), 10).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(60);
    }

    @Test
    void addDamagePer_CoinHeadsUntilTails_EqualsFlipUntilTailsAdditive() {
        ctx.setPendingDamage(20);
        ctx.setRandom(new FixedRandom(true, true, false));
        addDamagePer(coinHeadsUntilTails(), 20).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(60);
    }

    @Test
    void multiplyDamageBy_CoinHeadsUntilTails_EqualsFlipUntilTailsReplace() {
        ctx.setPendingDamage(30);
        ctx.setRandom(new FixedRandom(true, true, false));
        multiplyDamageBy(coinHeadsUntilTails()).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(60);
    }

    // ── when() conditional ───────────────────────────────────────────

    @Test
    void when_ConditionTrue_RunsThenBranch() {
        ctx.setPendingDamage(50);
        defender.applyStatus(StatusCondition.POISONED);
        when(defenderHasStatus(StatusCondition.POISONED))
                .then(addDamage(30))
                .otherwise(addDamage(5))
                .apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(80);
    }

    @Test
    void when_ConditionFalse_RunsOtherwiseBranch() {
        ctx.setPendingDamage(50);
        when(defenderHasStatus(StatusCondition.POISONED))
                .then(addDamage(30))
                .otherwise(addDamage(5))
                .apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(55);
    }

    // ── seq() composition ────────────────────────────────────────────

    @Test
    void seq_AppliesAllInOrder() {
        attacker.setDamageTaken(30);
        seq(heal(20), applyStatus(StatusCondition.POISONED)).apply(ctx);
        assertThat(attacker.getDamageTaken()).isEqualTo(10);
        assertThat(defender.hasStatus(StatusCondition.POISONED)).isTrue();
    }

    @Test
    void seq_Empty_DoesNothing() {
        assertDoesNotThrow(() -> seq().apply(ctx));
    }

    // ── New conditions (Bucket A) ────────────────────────────────────

    @Test
    void defenderIsEx_TrueForExCard_AddsBonus() {
        PokemonCard ex = PokemonCard.builder()
                .id("p2").name("ExMon").hp(180).stage(PokemonStage.BASIC).isEx(true).build();
        ctx.setDefender(new InPlayPokemon(ex));
        ctx.setPendingDamage(50);
        when(defenderIsEx()).then(addDamage(60)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(110);
    }

    @Test
    void defenderIsEx_FalseForNonExCard_NoBonus() {
        ctx.setPendingDamage(50);
        when(defenderIsEx()).then(addDamage(60)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    @Test
    void defenderType_MatchesType_AddsBonus() {
        PokemonCard grassMon = PokemonCard.builder()
                .id("p3").name("Grasso").hp(80).stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.GRASS).build();
        ctx.setDefender(new InPlayPokemon(grassMon));
        ctx.setPendingDamage(30);
        when(defenderType(EnergyType.GRASS)).then(addDamage(20)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    @Test
    void defenderType_MismatchedType_NoBonus() {
        PokemonCard fireMon = PokemonCard.builder()
                .id("p4").name("Fyro").hp(80).stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.FIRE).build();
        ctx.setDefender(new InPlayPokemon(fireMon));
        ctx.setPendingDamage(30);
        when(defenderType(EnergyType.GRASS)).then(addDamage(20)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(30);
    }

    @Test
    void defenderHasAnyStatus_True_TriggersBranch() {
        defender.applyStatus(StatusCondition.POISONED);
        ctx.setPendingDamage(40);
        when(defenderHasAnyStatus()).then(addDamage(60)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(100);
    }

    @Test
    void defenderHasAnyStatus_False_NoBonus() {
        ctx.setPendingDamage(40);
        when(defenderHasAnyStatus()).then(addDamage(60)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(40);
    }

    @Test
    void attackerBenchHasCardId_BenchContainsCard_TriggersBranch() {
        PokemonCard lunatone = PokemonCard.builder()
                .id("xy1-44").name("Lunatone").hp(70).stage(PokemonStage.BASIC).build();
        ctx.getAttackerBench().add(new InPlayPokemon(lunatone));
        ctx.setPendingDamage(50);
        when(attackerBenchHasCardId("xy1-44")).then(addDamage(30)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(80);
    }

    @Test
    void attackerBenchHasCardId_BenchEmpty_NoBonus() {
        ctx.setPendingDamage(50);
        when(attackerBenchHasCardId("xy1-44")).then(addDamage(30)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    // ── New counters (Bucket A) ──────────────────────────────────────

    @Test
    void attackerDistinctEnergyTypes_CountsDistinctNonColorless() {
        attacker.getEnergies().addAll(List.of(
                EnergyCard.builder().id("e1").energyType(EnergyType.GRASS).build(),
                EnergyCard.builder().id("e2").energyType(EnergyType.GRASS).build(),
                EnergyCard.builder().id("e3").energyType(EnergyType.FIRE).build(),
                EnergyCard.builder().id("e4").energyType(EnergyType.WATER).build()));
        ctx.setPendingDamage(0);
        addDamagePer(attackerDistinctEnergyTypes(), 30).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(90);
    }

    @Test
    void bothActiveEnergyTotal_SumsBothSides() {
        attacker.getEnergies().add(EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build());
        defender.getEnergies().addAll(List.of(
                EnergyCard.builder().id("e2").energyType(EnergyType.WATER).build(),
                EnergyCard.builder().id("e3").energyType(EnergyType.WATER).build()));
        ctx.setPendingDamage(0);
        addDamagePer(bothActiveEnergyTotal(), 20).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(60);
    }

    @Test
    void coinHeadsOf_FlipsAsManyCoinsAsInnerSource() {
        // Rock Black shape: 50 × heads, one coin per Fighting Energy attached.
        attacker.getEnergies().addAll(List.of(
                EnergyCard.builder().id("e1").energyType(EnergyType.FIGHTING).build(),
                EnergyCard.builder().id("e2").energyType(EnergyType.FIGHTING).build(),
                EnergyCard.builder().id("e3").energyType(EnergyType.FIGHTING).build()));
        ctx.setRandom(new FixedRandom(true, false, true));   // 3 coins → 2 heads
        ctx.setPendingDamage(50);
        multiplyDamageBy(coinHeadsOf(attackerEnergy(EnergyType.FIGHTING))).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(100);
    }

    // ── New effect verbs (Bucket A) ──────────────────────────────────

    @Test
    void applyStatusToAttacker_AppliesStatusToSelf() {
        applyStatusToAttacker(StatusCondition.CONFUSED).apply(ctx);
        assertThat(attacker.hasStatus(StatusCondition.CONFUSED)).isTrue();
    }

    @Test
    void clearAttackerStatuses_RemovesAllSelfStatuses() {
        attacker.applyStatus(StatusCondition.POISONED);
        clearAttackerStatuses().apply(ctx);
        assertThat(attacker.getStatuses()).isEmpty();
    }

    @Test
    void clearDefenderStatuses_RemovesAllDefenderStatuses() {
        defender.applyStatus(StatusCondition.ASLEEP);
        clearDefenderStatuses().apply(ctx);
        assertThat(defender.getStatuses()).isEmpty();
    }

    @Test
    void healAllAttackerPokemon_HealsActiveAndEachBench() {
        attacker.setDamageTaken(50);
        InPlayPokemon b1 = new InPlayPokemon(basePokemon);
        b1.setDamageTaken(20);
        InPlayPokemon b2 = new InPlayPokemon(basePokemon);
        b2.setDamageTaken(5);
        ctx.getAttackerBench().addAll(List.of(b1, b2));
        healAllAttackerPokemon(10).apply(ctx);
        assertThat(attacker.getDamageTaken()).isEqualTo(40);
        assertThat(b1.getDamageTaken()).isEqualTo(10);
        assertThat(b2.getDamageTaken()).isZero();    // clamped at 0
    }

    @Test
    void addStatus_DoesNotClearExistingStatuses() {
        defender.addStatus(StatusCondition.PARALYZED);
        Effects.addStatus(StatusCondition.POISONED).apply(ctx);
        assertThat(defender.hasStatus(StatusCondition.PARALYZED)).isTrue();
        assertThat(defender.hasStatus(StatusCondition.POISONED)).isTrue();
    }

    @Test
    void damageEachAttackerBench_DamagesEveryBenchSlot() {
        InPlayPokemon b1 = new InPlayPokemon(basePokemon);
        InPlayPokemon b2 = new InPlayPokemon(basePokemon);
        b2.setDamageTaken(30);
        ctx.getAttackerBench().addAll(List.of(b1, b2));
        damageEachAttackerBench(10).apply(ctx);
        assertThat(b1.getDamageTaken()).isEqualTo(10);
        assertThat(b2.getDamageTaken()).isEqualTo(40);
        // attacker (active) is unaffected
        assertThat(attacker.getDamageTaken()).isZero();
    }

    // ── Self-cooldown / shield / mill atoms (pilot migration) ────────

    @Test
    void attackerCantUseAttackName_SetsName() {
        attackerCantUseAttackName("X Blast").apply(ctx);
        assertThat(attacker.getCantUseAttackName()).isEqualTo("X Blast");
    }

    @Test
    void attackerPreventAllIncomingDamage_SetsFlag() {
        attackerPreventAllIncomingDamage().apply(ctx);
        assertThat(attacker.isPreventAllIncomingDamage()).isTrue();
    }

    @Test
    void scrunchRecipe_OnHeads_PreventsIncomingDamage() {
        // Scrunch: flip; if heads, prevent all incoming damage during opponent's next turn.
        ctx.setRandom(new FixedRandom(true));
        AttackEffect scrunch = flip().onHeads(attackerPreventAllIncomingDamage());
        scrunch.apply(ctx);
        assertThat(attacker.isPreventAllIncomingDamage()).isTrue();
    }

    @Test
    void scrunchRecipe_OnTails_DoesNothing() {
        ctx.setRandom(new FixedRandom(false));
        AttackEffect scrunch = flip().onHeads(attackerPreventAllIncomingDamage());
        scrunch.apply(ctx);
        assertThat(attacker.isPreventAllIncomingDamage()).isFalse();
    }

    @Test
    void discardDefenderDeckTop_MovesTopCardToDiscard() {
        ctx.getDefenderDeck().add(PokemonCard.builder().id("c1").name("A").build());
        discardDefenderDeckTop().apply(ctx);
        assertThat(ctx.getDefenderDeck()).isEmpty();
        assertThat(ctx.getDefenderDiscard()).hasSize(1);
    }

    @Test
    void discardDefenderDeckTop_EmptyDeck_DoesNothing() {
        discardDefenderDeckTop().apply(ctx);
        assertThat(ctx.getDefenderDiscard()).isEmpty();
    }

    @Test
    void hypnoHeadbuttRecipe_AddsDamageAndPutsSelfToSleep() {
        // Hypno Headbutt: +30 damage and the attacker is now Asleep.
        ctx.setPendingDamage(30);
        AttackEffect hypnoHeadbutt = seq(addDamage(30), applyStatusToAttacker(StatusCondition.ASLEEP));
        hypnoHeadbutt.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(60);
        assertThat(attacker.hasStatus(StatusCondition.ASLEEP)).isTrue();
    }

    // ── Self-disable atoms (Phase C round 2) ─────────────────────────

    @Test
    void attackerCantAttack_SetsFlag() {
        attackerCantAttack().apply(ctx);
        assertThat(attacker.isCantAttack()).isTrue();
    }

    @Test
    void attackerPreventAllEffects_SetsFlag() {
        attackerPreventAllEffects().apply(ctx);
        assertThat(attacker.isPreventAllEffects()).isTrue();
    }

    @Test
    void darknessBladeRecipe_OnTails_LocksAttackerOut() {
        ctx.setRandom(new FixedRandom(false));
        AttackEffect darknessBlade = flip().onTails(attackerCantAttack());
        darknessBlade.apply(ctx);
        assertThat(attacker.isCantAttack()).isTrue();
    }

    @Test
    void darknessBladeRecipe_OnHeads_DoesNothing() {
        ctx.setRandom(new FixedRandom(true));
        AttackEffect darknessBlade = flip().onTails(attackerCantAttack());
        darknessBlade.apply(ctx);
        assertThat(attacker.isCantAttack()).isFalse();
    }

    @Test
    void digRecipe_OnHeads_PreventsAllEffects() {
        ctx.setRandom(new FixedRandom(true));
        AttackEffect dig = flip().onHeads(attackerPreventAllEffects());
        dig.apply(ctx);
        assertThat(attacker.isPreventAllEffects()).isTrue();
    }

    @Test
    void digRecipe_OnTails_DoesNothing() {
        ctx.setRandom(new FixedRandom(false));
        AttackEffect dig = flip().onHeads(attackerPreventAllEffects());
        dig.apply(ctx);
        assertThat(attacker.isPreventAllEffects()).isFalse();
    }

    @Test
    void kingsShieldRecipe_PreventsIncomingDamageAndLocksOutNamedAttack() {
        // King's Shield: prevent all damage next turn + cannot use King's Shield next turn.
        AttackEffect kingsShield = seq(
                attackerPreventAllIncomingDamage(),
                attackerCantUseAttackName("King's Shield"));
        kingsShield.apply(ctx);
        assertThat(attacker.isPreventAllIncomingDamage()).isTrue();
        assertThat(attacker.getCantUseAttackName()).isEqualTo("King's Shield");
    }

    @Test
    void chargeDashRecipe_AddsBonusDamageAndSelfDamage() {
        // Charge Dash: +20 damage; this Pokémon does 20 damage to itself.
        ctx.setPendingDamage(40);
        AttackEffect chargeDash = seq(addDamage(20), selfDamage(20));
        chargeDash.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(60);
        assertThat(attacker.getDamageTaken()).isEqualTo(20);
    }

    @Test
    void poisonRingRecipe_PoisonsAndPreventsRetreat() {
        AttackEffect poisonRing = seq(
                applyStatus(StatusCondition.POISONED),
                new ar.edu.utn.frc.tup.piii.models.effects.common.PreventRetreatEffect());
        poisonRing.apply(ctx);
        assertThat(defender.hasStatus(StatusCondition.POISONED)).isTrue();
        assertThat(defender.isCantRetreat()).isTrue();
    }

    @Test
    void rapidSpinRecipe_SwitchesBothActives() {
        // Rapid Spin: switch self with bench[0], then opponent switches with their bench[0].
        InPlayPokemon attackerBench = new InPlayPokemon(basePokemon);
        InPlayPokemon defenderBench = new InPlayPokemon(basePokemon);
        ctx.getAttackerBench().add(attackerBench);
        ctx.getDefenderBench().add(defenderBench);
        AttackEffect rapidSpin = seq(
                new ar.edu.utn.frc.tup.piii.models.effects.common.SwitchSelfEffect(),
                new ar.edu.utn.frc.tup.piii.models.effects.common.SwitchOpponentEffect());
        rapidSpin.apply(ctx);
        assertThat(ctx.getAttacker()).isSameAs(attackerBench);
        assertThat(ctx.getDefender()).isSameAs(defenderBench);
    }

    // ── Additional condition / effect branch coverage ────────────────

    @Test
    void attackerDamaged_True_AddsBonus() {
        attacker.setDamageTaken(10);
        ctx.setPendingDamage(20);
        when(attackerDamaged()).then(addDamage(30)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    @Test
    void attackerDamaged_False_NoBonus() {
        ctx.setPendingDamage(20);
        when(attackerDamaged()).then(addDamage(30)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(20);
    }

    @Test
    void defenderDamaged_True_AddsBonus() {
        defender.setDamageTaken(10);
        ctx.setPendingDamage(20);
        when(defenderDamaged()).then(addDamage(30)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    @Test
    void defenderDamaged_False_NoBonus() {
        ctx.setPendingDamage(20);
        when(defenderDamaged()).then(addDamage(30)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(20);
    }

    @Test
    void attackerHasEnergyOf_True_AddsBonus() {
        attacker.getEnergies().add(EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build());
        ctx.setPendingDamage(20);
        when(attackerHasEnergyOf(EnergyType.FIRE)).then(addDamage(30)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    @Test
    void attackerHasEnergyOf_False_NoBonus() {
        ctx.setPendingDamage(20);
        when(attackerHasEnergyOf(EnergyType.FIRE)).then(addDamage(30)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(20);
    }

    @Test
    void defenderIsEx_MegaCard_AddsBonus() {
        PokemonCard mega = PokemonCard.builder()
                .id("m1").name("MMon").hp(200).stage(PokemonStage.MEGA).isMega(true).build();
        ctx.setDefender(new InPlayPokemon(mega));
        ctx.setPendingDamage(20);
        when(defenderIsEx()).then(addDamage(40)).apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(60);
    }

    @Test
    void cancelDamage_ZeroesPendingDamage() {
        ctx.setPendingDamage(80);
        cancelDamage().apply(ctx);
        assertThat(ctx.getPendingDamage()).isZero();
    }

    @Test
    void rockWreckerRecipe_IgnoresWeaknessAndResistanceAndLocksAttacker() {
        AttackEffect rockWrecker = seq(
                new ar.edu.utn.frc.tup.piii.models.effects.common.IgnoreWeaknessAndResistanceEffect(),
                attackerCantAttack());
        rockWrecker.apply(ctx);
        assertThat(ctx.isIgnoreWeakness()).isTrue();
        assertThat(ctx.isIgnoreResistance()).isTrue();
        assertThat(attacker.isCantAttack()).isTrue();
    }
}
