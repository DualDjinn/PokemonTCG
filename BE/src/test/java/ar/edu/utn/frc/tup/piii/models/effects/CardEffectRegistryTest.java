package ar.edu.utn.frc.tup.piii.models.effects;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.effects.common.SearchDeckPokemonToHandEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.assertj.core.api.Assertions.assertThat;

class CardEffectRegistryTest {

    private CardEffectRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new CardEffectRegistry();
    }

    @Test
    void getAttackEffect_VenusaurPoisonPowder_PoisonsDefender() {
        AttackEffect effect = registry.getAttackEffect("xy1-1", 0);
        GameContext ctx = ctxWith(false);
        effect.apply(ctx);
        assertThat(ctx.getDefender().hasStatus(StatusCondition.POISONED)).isTrue();
    }

    @Test
    void getAttackEffect_VenusaurJungleHammer_Heals30FromSelf() {
        AttackEffect effect = registry.getAttackEffect("xy1-1", 1);
        GameContext ctx = ctxWith(false);
        ctx.getAttacker().setDamageTaken(50);
        effect.apply(ctx);
        assertThat(ctx.getAttacker().getDamageTaken()).isEqualTo(20);
    }

    @Test
    void getAttackEffect_RegisteredSearchEffect_ReturnsSearchDeckPokemonToHandEffect() {
        AttackEffect effect = registry.getAttackEffect("xy1-9", 0);
        assertThat(effect).isInstanceOf(SearchDeckPokemonToHandEffect.class);
    }

    @Test
    void getAttackEffect_RegisteredCoinFlipBonus_OnHeads_AddsBonusDamage() {
        AttackEffect effect = registry.getAttackEffect("xy1-9", 1);
        GameContext ctx = ctxWith(true);
        ctx.setPendingDamage(30);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    @Test
    void getAttackEffect_RegisteredCoinFlipBonus_OnTails_AddsNothing() {
        AttackEffect effect = registry.getAttackEffect("xy1-9", 1);
        GameContext ctx = ctxWith(false);
        ctx.setPendingDamage(30);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(30);
    }

    @Test
    void getAttackEffect_ConkeldurrDynamicPunch_OnHeads_AddsBonusAndConfuses() {
        AttackEffect effect = registry.getAttackEffect("xy1-67", 1);
        GameContext ctx = ctxWith(true);
        ctx.setPendingDamage(60);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(100);
        assertThat(ctx.getDefender().hasStatus(StatusCondition.CONFUSED)).isTrue();
    }

    @Test
    void getAttackEffect_MalamarDistortionBeam_OnHeads_AppliesAsleep() {
        AttackEffect effect = registry.getAttackEffect("xy1-76", 1);
        GameContext ctx = ctxWith(true);
        effect.apply(ctx);
        assertThat(ctx.getDefender().hasStatus(StatusCondition.ASLEEP)).isTrue();
    }

    @Test
    void getAttackEffect_MalamarDistortionBeam_OnTails_AppliesConfused() {
        AttackEffect effect = registry.getAttackEffect("xy1-76", 1);
        GameContext ctx = ctxWith(false);
        effect.apply(ctx);
        assertThat(ctx.getDefender().hasStatus(StatusCondition.CONFUSED)).isTrue();
    }

    @Test
    void getAttackEffect_BlastoiseSplashBomb_OnTails_DealsSelfDamage() {
        AttackEffect effect = registry.getAttackEffect("xy1-29", 1);
        GameContext ctx = ctxWith(false);
        effect.apply(ctx);
        assertThat(ctx.getAttacker().getDamageTaken()).isEqualTo(30);
    }

    @Test
    void getAttackEffect_BlastoiseSplashBombSecondPrinting_OnTails_DealsSelfDamage() {
        AttackEffect effect = registry.getAttackEffect("xy1-142", 1);
        GameContext ctx = ctxWith(false);
        effect.apply(ctx);
        assertThat(ctx.getAttacker().getDamageTaken()).isEqualTo(30);
    }

    @Test
    void getAttackEffect_WhirlipedeContinuousTumble_MultipliesDamageByHeads() {
        AttackEffect effect = registry.getAttackEffect("xy1-52", 0);
        GameContext ctx = ctxWith(true, true, false);
        ctx.setPendingDamage(30);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(60);
    }

    @Test
    void getAttackEffect_CorsolaSpinyRush_AddsBonusPerHead() {
        AttackEffect effect = registry.getAttackEffect("xy1-36", 1);
        GameContext ctx = ctxWith(true, true, true, false);
        ctx.setPendingDamage(30);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(90);
    }

    private GameContext ctxWith(boolean... coins) {
        PokemonCard card = PokemonCard.builder()
                .id("p1").name("TestMon").hp(100).stage(PokemonStage.BASIC).build();
        GameContext ctx = new GameContext();
        ctx.setAttacker(new InPlayPokemon(card));
        ctx.setDefender(new InPlayPokemon(card));
        ctx.setRandom(new FixedRandom(coins));
        return ctx;
    }

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

    @Test
    void getAttackEffect_UnknownCard_ReturnsNull() {
        AttackEffect effect = registry.getAttackEffect("xy1-999", 0);
        assertThat(effect).isNull();
    }

    @Test
    void getAttackEffect_UnknownAttackIndex_ReturnsNull() {
        AttackEffect effect = registry.getAttackEffect("xy1-1", 99);
        assertThat(effect).isNull();
    }

    @Test
    void getAttackEffect_BeedrillPoisonJab_PoisonsDefender() {
        AttackEffect effect = registry.getAttackEffect("xy1-5", 0);
        GameContext ctx = ctxWith(false);
        effect.apply(ctx);
        assertThat(ctx.getDefender().hasStatus(StatusCondition.POISONED)).isTrue();
    }

    @Test
    void getAttackEffect_PansageLeechSeed_Heals10FromSelf() {
        AttackEffect effect = registry.getAttackEffect("xy1-10", 1);
        GameContext ctx = ctxWith(false);
        ctx.getAttacker().setDamageTaken(30);
        effect.apply(ctx);
        assertThat(ctx.getAttacker().getDamageTaken()).isEqualTo(20);
    }

    @Test
    void getAttackEffect_ChesnaughtTouchdown_Heals20FromSelf() {
        AttackEffect effect = registry.getAttackEffect("xy1-14", 0);
        GameContext ctx = ctxWith(false);
        ctx.getAttacker().setDamageTaken(50);
        effect.apply(ctx);
        assertThat(ctx.getAttacker().getDamageTaken()).isEqualTo(30);
    }

    // ── Bucket A registrations ────────────────────────────────────────

    @Test
    void getAttackEffect_MVenusaurCrisisVine_ParalyzesAndPoisons() {
        AttackEffect effect = registry.getAttackEffect("xy1-2", 0);
        GameContext ctx = ctxWith(false);
        effect.apply(ctx);
        assertThat(ctx.getDefender().hasStatus(StatusCondition.PARALYZED)).isTrue();
        assertThat(ctx.getDefender().hasStatus(StatusCondition.POISONED)).isTrue();
    }

    @Test
    void getAttackEffect_WeedleLeafMunch_GrassDefender_AddsBonus() {
        AttackEffect effect = registry.getAttackEffect("xy1-3", 0);
        PokemonCard grass = PokemonCard.builder()
                .id("g1").name("Grasso").hp(60).stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.GRASS).build();
        GameContext ctx = ctxWith(false);
        ctx.setDefender(new InPlayPokemon(grass));
        ctx.setPendingDamage(30);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    @Test
    void getAttackEffect_VivillonColorfulWind_AddsBonusPerDistinctEnergyType() {
        AttackEffect effect = registry.getAttackEffect("xy1-17", 1);
        GameContext ctx = ctxWith(false);
        ctx.getAttacker().getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.FIRE).build());
        ctx.getAttacker().getEnergies().add(EnergyCard.builder()
                .id("e2").energyType(EnergyType.WATER).build());
        ctx.setPendingDamage(0);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(60);
    }

    @Test
    void getAttackEffect_CorsolaRefresh_HealsAndClearsStatuses() {
        AttackEffect effect = registry.getAttackEffect("xy1-36", 0);
        GameContext ctx = ctxWith(false);
        ctx.getAttacker().setDamageTaken(50);
        ctx.getAttacker().applyStatus(StatusCondition.POISONED);
        effect.apply(ctx);
        assertThat(ctx.getAttacker().getDamageTaken()).isEqualTo(20);
        assertThat(ctx.getAttacker().getStatuses()).isEmpty();
    }

    @Test
    void getAttackEffect_RaichuCircleCircuit_MultipliesByBenchCount() {
        AttackEffect effect = registry.getAttackEffect("xy1-43", 0);
        GameContext ctx = ctxWith(false);
        PokemonCard card = PokemonCard.builder()
                .id("p1").name("Bench").hp(60).stage(PokemonStage.BASIC).build();
        ctx.getAttackerBench().add(new InPlayPokemon(card));
        ctx.getAttackerBench().add(new InPlayPokemon(card));
        ctx.setPendingDamage(20);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(40);
    }

    @Test
    void getAttackEffect_DugtrioEarthquake_Damages10ToEachBench() {
        AttackEffect effect = registry.getAttackEffect("xy1-59", 0);
        GameContext ctx = ctxWith(false);
        PokemonCard card = PokemonCard.builder()
                .id("p1").name("Bench").hp(60).stage(PokemonStage.BASIC).build();
        var b1 = new InPlayPokemon(card);
        var b2 = new InPlayPokemon(card);
        ctx.getAttackerBench().add(b1);
        ctx.getAttackerBench().add(b2);
        effect.apply(ctx);
        assertThat(b1.getDamageTaken()).isEqualTo(10);
        assertThat(b2.getDamageTaken()).isEqualTo(10);
    }

    @Test
    void getAttackEffect_RhyperiorRockBlack_MultipliesByFightingHeads() {
        AttackEffect effect = registry.getAttackEffect("xy1-62", 0);
        GameContext ctx = ctxWith(true, true, false);   // 3 coins → 2 heads
        ctx.getAttacker().getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.FIGHTING).build());
        ctx.getAttacker().getEnergies().add(EnergyCard.builder()
                .id("e2").energyType(EnergyType.FIGHTING).build());
        ctx.getAttacker().getEnergies().add(EnergyCard.builder()
                .id("e3").energyType(EnergyType.FIGHTING).build());
        ctx.setPendingDamage(50);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(100);
    }

    @Test
    void getAttackEffect_SolrockCosmicSpin_WithLunatoneOnBench_AddsBonus() {
        AttackEffect effect = registry.getAttackEffect("xy1-64", 0);
        GameContext ctx = ctxWith(false);
        PokemonCard lunatone = PokemonCard.builder()
                .id("xy1-44").name("Lunatone").hp(70).stage(PokemonStage.BASIC).build();
        ctx.getAttackerBench().add(new InPlayPokemon(lunatone));
        ctx.setPendingDamage(50);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(80);
    }

    @Test
    void getAttackEffect_ConkeldurrWakeUpSlap_DefenderHasStatus_AddsBonusAndClears() {
        AttackEffect effect = registry.getAttackEffect("xy1-67", 0);
        GameContext ctx = ctxWith(false);
        ctx.getDefender().applyStatus(StatusCondition.ASLEEP);
        ctx.setPendingDamage(40);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(100);
        assertThat(ctx.getDefender().getStatuses()).isEmpty();
    }

    @Test
    void getAttackEffect_InkayConfusionWave_ConfusesBothActives() {
        AttackEffect effect = registry.getAttackEffect("xy1-74", 0);
        GameContext ctx = ctxWith(false);
        effect.apply(ctx);
        assertThat(ctx.getDefender().hasStatus(StatusCondition.CONFUSED)).isTrue();
        assertThat(ctx.getAttacker().hasStatus(StatusCondition.CONFUSED)).isTrue();
    }

    @Test
    void getAttackEffect_YveltalEvilBall_AddsPerBothActiveEnergy() {
        AttackEffect effect = registry.getAttackEffect("xy1-79", 0);
        GameContext ctx = ctxWith(false);
        ctx.getAttacker().getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.DARKNESS).build());
        ctx.getDefender().getEnergies().add(EnergyCard.builder()
                .id("e2").energyType(EnergyType.WATER).build());
        ctx.getDefender().getEnergies().add(EnergyCard.builder()
                .id("e3").energyType(EnergyType.WATER).build());
        ctx.setPendingDamage(20);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(80);
    }

    @Test
    void getAttackEffect_SkarmoryTailspinPiledriver_DefenderDamaged_AddsBonus() {
        AttackEffect effect = registry.getAttackEffect("xy1-80", 1);
        GameContext ctx = ctxWith(false);
        ctx.getDefender().setDamageTaken(20);
        ctx.setPendingDamage(60);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(100);
    }

    @Test
    void getAttackEffect_WigglytuffBalloonBarrage_MultipliesByAttachedEnergy() {
        AttackEffect effect = registry.getAttackEffect("xy1-90", 0);
        GameContext ctx = ctxWith(false);
        ctx.getAttacker().getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.FAIRY).build());
        ctx.getAttacker().getEnergies().add(EnergyCard.builder()
                .id("e2").energyType(EnergyType.COLORLESS).build());
        ctx.setPendingDamage(20);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(40);
    }

    @Test
    void getAttackEffect_TaurosSeethingAnger_MultipliesByDamageCounterHeads() {
        AttackEffect effect = registry.getAttackEffect("xy1-100", 1);
        GameContext ctx = ctxWith(true, true, true, false);
        ctx.getAttacker().setDamageTaken(40);   // 4 damage counters → 4 coins → 3 heads
        ctx.setPendingDamage(30);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(90);
    }

    @Test
    void getAttackEffect_SkittyHealBell_HealsEveryAttackerPokemon() {
        AttackEffect effect = registry.getAttackEffect("xy1-104", 0);
        GameContext ctx = ctxWith(false);
        ctx.getAttacker().setDamageTaken(20);
        PokemonCard card = PokemonCard.builder()
                .id("p1").name("Bench").hp(60).stage(PokemonStage.BASIC).build();
        var b1 = new InPlayPokemon(card);
        b1.setDamageTaken(15);
        ctx.getAttackerBench().add(b1);
        effect.apply(ctx);
        assertThat(ctx.getAttacker().getDamageTaken()).isEqualTo(10);
        assertThat(b1.getDamageTaken()).isEqualTo(5);
    }

    @Test
    void getAttackEffect_StoutlandBiteOff_DefenderIsEx_AddsBonus() {
        AttackEffect effect = registry.getAttackEffect("xy1-110", 0);
        GameContext ctx = ctxWith(false);
        PokemonCard exCard = PokemonCard.builder()
                .id("e1").name("ExMon").hp(180).stage(PokemonStage.BASIC).isEx(true).build();
        ctx.setDefender(new InPlayPokemon(exCard));
        ctx.setPendingDamage(50);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(110);
    }

    @Test
    void getAttackEffect_YveltalEvilBallSecondPrinting_SameAsFirst() {
        AttackEffect effect = registry.getAttackEffect("xy1-144", 0);
        assertThat(effect).isNotNull();
        GameContext ctx = ctxWith(false);
        ctx.getAttacker().getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.DARKNESS).build());
        ctx.setPendingDamage(20);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(40);
    }

    @Test
    void getAttackEffect_SkarmoryTailspinPiledriverSecondPrinting_SameAsFirst() {
        AttackEffect effect = registry.getAttackEffect("xy1-145", 1);
        assertThat(effect).isNotNull();
        GameContext ctx = ctxWith(false);
        ctx.getDefender().setDamageTaken(10);
        ctx.setPendingDamage(60);
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(100);
    }
}
