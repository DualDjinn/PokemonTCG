package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.*;
// import ar.edu.utn.frc.tup.piii.models.effects.common.DrawCardsEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.assertj.core.api.Assertions.assertThat;

class IsolatedCoinFlipEffectsTest {

    private static class FixedRandom extends Random {
        private final boolean[] values;
        private int idx;
        FixedRandom(boolean... values) { this.values = values; this.idx = 0; }
        @Override public boolean nextBoolean() { return values[idx++ % values.length]; }
        @Override public int nextInt(int bound) { return nextBoolean() ? bound - 1 : 0; }
        protected int next(int bits) { return nextBoolean() ? 1 : 0; }
    }

    private GameContext ctx;
    private InPlayPokemon attacker;
    private InPlayPokemon defender;
    private PokemonCard basePokemon;
    // It is used down below to test that the effects do not interact with the randomness of other effects like DrawCardsEffect, 
    // which also flips coins.
    @SuppressWarnings("unused")
    private PokemonCard lunatoneCard;
    // It is not used in these tests, but it is useful to have an EX card here to verify that the effects do not interact with 
    // the randomness of other effects like DrawCardsEffect, which also flips coins.
    @SuppressWarnings("unused")
    private PokemonCard exCard;

    @BeforeEach
    void setUp() {
        basePokemon = PokemonCard.builder()
                .id("p1").name("TestMon").hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS)
                .build();
        lunatoneCard = PokemonCard.builder()
                .id("p2").name("Lunatone").hp(70)
                .stage(PokemonStage.BASIC).build();
        exCard = PokemonCard.builder()
                .id("p3").name("TestEX").hp(120)
                .stage(PokemonStage.BASIC).isEx(true)
                .build();
        attacker = new InPlayPokemon(basePokemon);
        defender = new InPlayPokemon(basePokemon);
        ctx = new GameContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);
        // The default GameContext bench is 5 null slots; isolated effects assume a
        // compact bench (size == real count, get(0) == first real). Replace with empty
        // compact lists so bench-using effects behave as in the engine's compacted view.
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setDefenderBench(new ArrayList<>());
        ctx.setPendingDamage(50);
    }

    @Test
    void astonishEffect_WhenDefenderHasCards_TakesRandomAndShufflesIntoDeck() {
        ctx.setRandom(new FixedRandom(true));
        ctx.getDefenderHand().add(PokemonCard.builder().id("c1").name("Card1").build());
        ctx.getDefenderHand().add(PokemonCard.builder().id("c2").name("Card2").build());
        AttackEffect effect = new AstonishEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderHand()).hasSize(1);
        assertThat(ctx.getDefenderDeck()).hasSize(1);
    }

    @Test
    void astonishEffect_WhenDefenderHandIsEmpty_DoesNothing() {
        AttackEffect effect = new AstonishEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderHand()).isEmpty();
        assertThat(ctx.getDefenderDeck()).isEmpty();
    }

    // bounceEffect tests removed — BounceEffect class was dead code (never registered);
    // the actual xy1-39 (Froakie Bounce) card is registered via Effects.flip().onHeads(new SwitchSelfEffect())
    // in CardEffectRegistry#registerPhaseBCoinFlipPrimitives. SwitchSelfEffect's behaviour is covered
    // by existing tests in this file.

    @Test
    void botherEffect_OnHeads_SetsCantPlaySupporters() {
        ctx.setRandom(new FixedRandom(true));
        AttackEffect effect = new BotherEffect();
        effect.apply(ctx);
        assertThat(ctx.isAttackerCantPlaySupporters()).isTrue();
    }

    @Test
    void botherEffect_OnTails_DoesNothing() {
        ctx.setRandom(new FixedRandom(false));
        AttackEffect effect = new BotherEffect();
        effect.apply(ctx);
        assertThat(ctx.isAttackerCantPlaySupporters()).isFalse();
    }

    @Test
    void clampCrushEffect_OnHeads_ParalyzesAndDiscardsEnergy() {
        ctx.setRandom(new FixedRandom(true));
        defender.getEnergies().add(EnergyCard.builder()
                .id("e1").name("Water").energyType(EnergyType.WATER).build());
        AttackEffect effect = new ClampCrushEffect();
        effect.apply(ctx);
        assertThat(defender.hasStatus(StatusCondition.PARALYZED)).isTrue();
        assertThat(defender.getEnergies()).isEmpty();
        assertThat(ctx.getDefenderDiscard()).hasSize(1);
    }

    @Test
    void clampCrushEffect_OnTails_DoesNothing() {
        ctx.setRandom(new FixedRandom(false));
        defender.getEnergies().add(EnergyCard.builder()
                .id("e1").name("Water").energyType(EnergyType.WATER).build());
        AttackEffect effect = new ClampCrushEffect();
        effect.apply(ctx);
        assertThat(defender.hasStatus(StatusCondition.PARALYZED)).isFalse();
        assertThat(defender.getEnergies()).hasSize(1);
    }

    // Edge case: heads but the defender has no Energy to discard — still Paralyzes,
    // and the removed != null branch is not taken (nothing added to the discard pile).
    @Test
    void clampCrushEffect_OnHeads_NoEnergy_ParalyzesWithoutDiscard() {
        ctx.setRandom(new FixedRandom(true));
        AttackEffect effect = new ClampCrushEffect();
        effect.apply(ctx);
        assertThat(defender.hasStatus(StatusCondition.PARALYZED)).isTrue();
        assertThat(ctx.getDefenderDiscard()).isEmpty();
    }

    // darknessBladeEffect / digEffect tests moved to AttackEffectDslTest
    // (darknessBladeRecipe_* / digRecipe_*). See registerPhaseCMigratedFromIsolated.

    @Test
    void flashNeedleEffect_MultipliesDamageByHeads() {
        ctx.setRandom(new FixedRandom(true, true, false));
        ctx.setPendingDamage(40);
        AttackEffect effect = new FlashNeedleEffect();
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(80);
        assertThat(attacker.isPreventAllEffects()).isFalse();
    }

    @Test
    void flashNeedleEffect_AllHeads_PreventsAllEffects() {
        ctx.setRandom(new FixedRandom(true, true, true));
        ctx.setPendingDamage(40);
        AttackEffect effect = new FlashNeedleEffect();
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(120);
        assertThat(attacker.isPreventAllEffects()).isTrue();
    }

    @Test
    void flashNeedleEffect_ZeroHeads_SetsDamageToZero() {
        ctx.setRandom(new FixedRandom(false, false, false));
        ctx.setPendingDamage(40);
        AttackEffect effect = new FlashNeedleEffect();
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isZero();
    }

    @Test
    void madMountainEffect_TwoHeads_DiscardsPerDamageCounter() {
        attacker.setDamageTaken(30);
        ctx.setRandom(new FixedRandom(true, true));
        ctx.getDefenderDeck().addAll(List.of(
                PokemonCard.builder().id("c1").name("A").build(),
                PokemonCard.builder().id("c2").name("B").build(),
                PokemonCard.builder().id("c3").name("C").build()));
        AttackEffect effect = new MadMountainEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderDeck()).isEmpty();
        assertThat(ctx.getDefenderDiscard()).hasSize(3);
    }

    @Test
    void madMountainEffect_LessThanTwoHeads_DoesNothing() {
        attacker.setDamageTaken(30);
        ctx.setRandom(new FixedRandom(true, false));
        ctx.getDefenderDeck().add(PokemonCard.builder().id("c1").name("A").build());
        AttackEffect effect = new MadMountainEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderDeck()).hasSize(1);
    }

    // Edge case: both heads but the attacker has zero damage counters — the discard
    // loop body never executes, so nothing is discarded.
    @Test
    void madMountainEffect_TwoHeads_NoDamageCounters_DiscardsNothing() {
        attacker.setDamageTaken(0);
        ctx.setRandom(new FixedRandom(true, true));
        ctx.getDefenderDeck().add(PokemonCard.builder().id("c1").name("A").build());
        AttackEffect effect = new MadMountainEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderDeck()).hasSize(1);
        assertThat(ctx.getDefenderDiscard()).isEmpty();
    }

    @Test
    void mentalTrashEffect_FlipsCoins_DiscardsFromDefenderHand() {
        ctx.getDefenderHand().addAll(List.of(
                PokemonCard.builder().id("c1").name("A").build(),
                PokemonCard.builder().id("c2").name("B").build(),
                PokemonCard.builder().id("c3").name("C").build()));
        ctx.setRandom(new FixedRandom(false, true, false, true));
        AttackEffect effect = new MentalTrashEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderHand()).hasSize(1);
        assertThat(ctx.getDefenderDiscard()).hasSize(2);
    }

    @Test
    void mentalTrashEffect_AllHeads_DiscardsNothing() {
        ctx.getDefenderHand().add(PokemonCard.builder().id("c1").name("A").build());
        ctx.setRandom(new FixedRandom(true, true, true, true));
        AttackEffect effect = new MentalTrashEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderHand()).hasSize(1);
        assertThat(ctx.getDefenderDiscard()).isEmpty();
    }

    // Edge case: 4 tails but only 1 card in hand — Math.min clamps the discard count
    // to the hand size, so exactly one card is discarded (no IndexOutOfBounds).
    @Test
    void mentalTrashEffect_MoreTailsThanHand_DiscardsOnlyHand() {
        ctx.getDefenderHand().add(PokemonCard.builder().id("c1").name("A").build());
        ctx.setRandom(new FixedRandom(false, false, false, false));
        AttackEffect effect = new MentalTrashEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderHand()).isEmpty();
        assertThat(ctx.getDefenderDiscard()).hasSize(1);
    }

    @Test
    void mineEffect_WhenDeckNotEmpty_ShufflesDefenderDeck() {
        ctx.setRandom(new FixedRandom(true));
        ctx.getDefenderDeck().add(PokemonCard.builder().id("c1").name("A").build());
        AttackEffect effect = new MineEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderDeck()).hasSize(1);
    }

    @Test
    void mineEffect_WhenDeckEmpty_DoesNothing() {
        AttackEffect effect = new MineEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderDeck()).isEmpty();
    }

    @Test
    void nightClawEffect_OnHeads_DoesNotDiscard() {
        ctx.setRandom(new FixedRandom(true));
        attacker.getEnergies().addAll(List.of(
                EnergyCard.builder().id("e1").energyType(EnergyType.DARKNESS).build(),
                EnergyCard.builder().id("e2").energyType(EnergyType.DARKNESS).build()));
        AttackEffect effect = new NightClawEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(2);
    }

    @Test
    void nightClawEffect_OnTails_DiscardsTwoEnergies() {
        ctx.setRandom(new FixedRandom(false));
        attacker.getEnergies().addAll(List.of(
                EnergyCard.builder().id("e1").energyType(EnergyType.DARKNESS).build(),
                EnergyCard.builder().id("e2").energyType(EnergyType.DARKNESS).build(),
                EnergyCard.builder().id("e3").energyType(EnergyType.DARKNESS).build()));
        AttackEffect effect = new NightClawEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
        assertThat(ctx.getAttackerDiscard()).hasSize(2);
    }

    @Test
    void nightClawEffect_OnTailsWithNoEnergy_DoesNothing() {
        ctx.setRandom(new FixedRandom(false));
        AttackEffect effect = new NightClawEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerDiscard()).isEmpty();
    }

    // scrunchEffect tests moved to AttackEffectDslTest#scrunchRecipe_*
    // (migrated to flip().onHeads(attackerPreventAllIncomingDamage()) — see
    // CardEffectRegistry#registerPhaseCMigratedFromIsolated).

    @Test
    void seafaringEffect_AttachesWaterEnergyToBench() {
        ctx.setRandom(new FixedRandom(true, false, true));
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        ctx.getAttackerBench().add(benched);
        ctx.getAttackerDiscard().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.WATER).build());
        ctx.getAttackerDiscard().add(EnergyCard.builder()
                .id("e2").energyType(EnergyType.WATER).build());
        ctx.getAttackerDiscard().add(EnergyCard.builder()
                .id("e3").energyType(EnergyType.GRASS).build());
        AttackEffect effect = new SeafaringEffect();
        effect.apply(ctx);
        assertThat(benched.getEnergies()).hasSize(2);
        assertThat(ctx.getAttackerDiscard()).hasSize(1);
    }

    @Test
    void seafaringEffect_NoWaterEnergyInDiscard_AttachesNothing() {
        ctx.setRandom(new FixedRandom(true, true, true));
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        ctx.getAttackerBench().add(benched);
        ctx.getAttackerDiscard().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.GRASS).build());
        AttackEffect effect = new SeafaringEffect();
        effect.apply(ctx);
        assertThat(benched.getEnergies()).isEmpty();
    }

    @Test
    void seafaringEffect_NoBench_DoesNothing() {
        ctx.setRandom(new FixedRandom(true, true, true));
        ctx.getAttackerDiscard().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.WATER).build());
        AttackEffect effect = new SeafaringEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerDiscard()).hasSize(1);
    }

}
