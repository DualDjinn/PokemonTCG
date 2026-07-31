package ar.edu.utn.frc.tup.piii.models.effects.common;

import ar.edu.utn.frc.tup.piii.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CommonAttackEffectsTest {

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
        // GameContext seeds benches with 5 null slots; these effects expect a compact bench.
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setDefenderBench(new ArrayList<>());
    }

    // ──────────────────────────────────────────────
    // DrawCardsEffect
    // ──────────────────────────────────────────────

    @Test
    void drawCardsEffect_DrawsSpecifiedCount() {
        ctx.getAttackerDeck().addAll(List.of(
                PokemonCard.builder().id("c1").name("A").build(),
                PokemonCard.builder().id("c2").name("B").build(),
                PokemonCard.builder().id("c3").name("C").build()
        ));
        AttackEffect effect = new DrawCardsEffect(2);
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).hasSize(2);
        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }

    @Test
    void drawCardsEffect_WhenDeckHasFewerCards_DrawsAvailable() {
        ctx.getAttackerDeck().add(PokemonCard.builder().id("c1").name("A").build());
        AttackEffect effect = new DrawCardsEffect(5);
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).hasSize(1);
        assertThat(ctx.getAttackerDeck()).isEmpty();
    }

    @Test
    void drawCardsEffect_WhenDeckEmpty_DoesNothing() {
        AttackEffect effect = new DrawCardsEffect(1);
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).isEmpty();
    }

    // ──────────────────────────────────────────────
    // SearchDeckPokemonToHandEffect
    // ──────────────────────────────────────────────

    @Test
    void searchDeckPokemonToHandEffect_FindsPokemonOfType_AddsToHand() {
        ctx.getAttackerDeck().add(
                PokemonCard.builder().id("p2").name("Bulbasaur")
                        .pokemonType(EnergyType.GRASS).hp(60)
                        .stage(PokemonStage.BASIC).build()
        );
        ctx.getAttackerDeck().add(
                PokemonCard.builder().id("p3").name("Charmander")
                        .pokemonType(EnergyType.FIRE).hp(60)
                        .stage(PokemonStage.BASIC).build()
        );
        AttackEffect effect = new SearchDeckPokemonToHandEffect(EnergyType.GRASS);
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).hasSize(1);
        assertThat(ctx.getAttackerHand().get(0).getName()).isEqualTo("Bulbasaur");
        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }

    @Test
    void searchDeckPokemonToHandEffect_NoMatch_AddsNothing() {
        ctx.getAttackerDeck().add(
                PokemonCard.builder().id("p3").name("Charmander")
                        .pokemonType(EnergyType.FIRE).hp(60)
                        .stage(PokemonStage.BASIC).build()
        );
        AttackEffect effect = new SearchDeckPokemonToHandEffect(EnergyType.WATER);
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).isEmpty();
    }

    // ──────────────────────────────────────────────
    // CompositeAttackEffect
    // ──────────────────────────────────────────────

    @Test
    void compositeAttackEffect_AppliesAllInOrder() {
        attacker.setDamageTaken(30);
        AttackEffect heal = c -> c.getAttacker().setDamageTaken(
                Math.max(0, c.getAttacker().getDamageTaken() - 20));
        AttackEffect poison = c -> c.getDefender().applyStatus(StatusCondition.POISONED);
        AttackEffect composite = new CompositeAttackEffect(heal, poison);
        composite.apply(ctx);
        assertThat(attacker.getDamageTaken()).isEqualTo(10);
        assertThat(defender.hasStatus(StatusCondition.POISONED)).isTrue();
    }

    @Test
    void compositeAttackEffect_WithListConstructor_AppliesAll() {
        AttackEffect paralyze = c -> c.getDefender().applyStatus(StatusCondition.PARALYZED);
        AttackEffect composite = new CompositeAttackEffect(List.of(paralyze));
        composite.apply(ctx);
        assertThat(defender.hasStatus(StatusCondition.PARALYZED)).isTrue();
    }

    @Test
    void compositeAttackEffect_WhenEmpty_DoesNothing() {
        AttackEffect composite = new CompositeAttackEffect(List.of());
        assertDoesNotThrow(() -> composite.apply(ctx));
    }

    // ──────────────────────────────────────────────
    // HealBenchedEffect
    // ──────────────────────────────────────────────

    @Test
    void healBenchedEffect_HealsFirstBenchedPokemon() {
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        benched.setDamageTaken(60);
        ctx.getAttackerBench().add(benched);
        AttackEffect effect = new HealBenchedEffect(60);
        effect.apply(ctx);
        assertThat(benched.getDamageTaken()).isZero();
    }

    @Test
    void healBenchedEffect_WhenNoBench_DoesNothing() {
        AttackEffect effect = new HealBenchedEffect(30);
        assertDoesNotThrow(() -> effect.apply(ctx));
    }

    @Test
    void healBenchedEffect_ClampsAtZero() {
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        benched.setDamageTaken(10);
        ctx.getAttackerBench().add(benched);
        AttackEffect effect = new HealBenchedEffect(30);
        effect.apply(ctx);
        assertThat(benched.getDamageTaken()).isZero();
    }

    // ──────────────────────────────────────────────
    // CoinFlipDiscardOpponentEnergyEffect
    // ──────────────────────────────────────────────

    @Test
    void coinFlipDiscardOpponentEnergyEffect_OnHeads_DiscardsEnergy() {
        ctx.setRandom(new FixedRandom(true));
        defender.getEnergies().add(EnergyCard.builder()
                .id("e1").name("Fire Energy").energyType(EnergyType.FIRE).build());
        AttackEffect effect = new CoinFlipDiscardOpponentEnergyEffect();
        effect.apply(ctx);
        assertThat(defender.getEnergies()).isEmpty();
        assertThat(ctx.getDefenderDiscard()).hasSize(1);
    }

    @Test
    void coinFlipDiscardOpponentEnergyEffect_OnTails_DoesNothing() {
        ctx.setRandom(new FixedRandom(false));
        defender.getEnergies().add(EnergyCard.builder()
                .id("e1").name("Fire Energy").energyType(EnergyType.FIRE).build());
        AttackEffect effect = new CoinFlipDiscardOpponentEnergyEffect();
        effect.apply(ctx);
        assertThat(defender.getEnergies()).hasSize(1);
        assertThat(ctx.getDefenderDiscard()).isEmpty();
    }

    // ──────────────────────────────────────────────
    // DiscardSelfEnergyEffect
    // ──────────────────────────────────────────────

    @Test
    void discardSelfEnergyEffect_RemovesOneEnergyOfType() {
        attacker.getEnergies().addAll(List.of(
                EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build(),
                EnergyCard.builder().id("e2").energyType(EnergyType.GRASS).build()
        ));
        AttackEffect effect = new DiscardSelfEnergyEffect(EnergyType.FIRE, false);
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
        assertThat(attacker.getEnergies().get(0).getEnergyType()).isEqualTo(EnergyType.GRASS);
        assertThat(ctx.getAttackerDiscard()).hasSize(1);
    }

    @Test
    void discardSelfEnergyEffect_RemovesAllEnergies() {
        attacker.getEnergies().addAll(List.of(
                EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build(),
                EnergyCard.builder().id("e2").energyType(EnergyType.FIRE).build(),
                EnergyCard.builder().id("e3").energyType(EnergyType.GRASS).build()
        ));
        AttackEffect effect = new DiscardSelfEnergyEffect(EnergyType.FIRE, true);
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
        assertThat(attacker.getEnergies().get(0).getEnergyType()).isEqualTo(EnergyType.GRASS);
        assertThat(ctx.getAttackerDiscard()).hasSize(2);
    }

    @Test
    void discardSelfEnergyEffect_NoEnergyOfType_DoesNothing() {
        attacker.getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.GRASS).build());
        AttackEffect effect = new DiscardSelfEnergyEffect(EnergyType.FIRE, false);
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
        assertThat(ctx.getAttackerDiscard()).isEmpty();
    }

    // ──────────────────────────────────────────────
    // SwitchOpponentEffect
    // ──────────────────────────────────────────────

    @Test
    void switchOpponentEffect_SwapsActiveWithBenched() {
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        ctx.getDefenderBench().add(benched);
        AttackEffect effect = new SwitchOpponentEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefender()).isSameAs(benched);
        assertThat(ctx.getDefenderBench()).hasSize(1);
        assertThat(ctx.getDefenderBench().get(0)).isSameAs(defender);
    }

    @Test
    void switchOpponentEffect_WhenNoBench_DoesNothing() {
        AttackEffect effect = new SwitchOpponentEffect();
        assertDoesNotThrow(() -> effect.apply(ctx));
        assertThat(ctx.getDefender()).isSameAs(defender);
    }

    // ──────────────────────────────────────────────
    // SwitchSelfEffect
    // ──────────────────────────────────────────────

    @Test
    void switchSelfEffect_SwapsActiveWithBenched() {
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        ctx.getAttackerBench().add(benched);
        AttackEffect effect = new SwitchSelfEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttacker()).isSameAs(benched);
        assertThat(ctx.getAttackerBench()).hasSize(1);
        assertThat(ctx.getAttackerBench().get(0)).isSameAs(attacker);
    }

    @Test
    void switchSelfEffect_WhenNoBench_DoesNothing() {
        AttackEffect effect = new SwitchSelfEffect();
        assertDoesNotThrow(() -> effect.apply(ctx));
        assertThat(ctx.getAttacker()).isSameAs(attacker);
    }

    // ──────────────────────────────────────────────
    // SpreadBenchDamageEffect
    // ──────────────────────────────────────────────

    @Test
    void spreadBenchDamageEffect_DealsDamageToMultipleTargets() {
        InPlayPokemon b1 = new InPlayPokemon(basePokemon);
        InPlayPokemon b2 = new InPlayPokemon(basePokemon);
        InPlayPokemon b3 = new InPlayPokemon(basePokemon);
        ctx.getDefenderBench().addAll(List.of(b1, b2, b3));
        AttackEffect effect = new SpreadBenchDamageEffect(20, 2);
        effect.apply(ctx);
        assertThat(b1.getDamageTaken()).isEqualTo(20);
        assertThat(b2.getDamageTaken()).isEqualTo(20);
        assertThat(b3.getDamageTaken()).isZero();
    }

    @Test
    void spreadBenchDamageEffect_WhenBenchHasFewerThanTargets() {
        InPlayPokemon b1 = new InPlayPokemon(basePokemon);
        ctx.getDefenderBench().add(b1);
        AttackEffect effect = new SpreadBenchDamageEffect(20, 3);
        effect.apply(ctx);
        assertThat(b1.getDamageTaken()).isEqualTo(20);
    }

    @Test
    void spreadBenchDamageEffect_WhenNoBench_DoesNothing() {
        AttackEffect effect = new SpreadBenchDamageEffect(20, 1);
        assertDoesNotThrow(() -> effect.apply(ctx));
    }

    // ──────────────────────────────────────────────
    // PreventRetreatEffect
    // ──────────────────────────────────────────────

    @Test
    void preventRetreatEffect_SetsCantRetreatOnDefender() {
        AttackEffect effect = new PreventRetreatEffect();
        effect.apply(ctx);
        assertThat(defender.isCantRetreat()).isTrue();
    }

    // ──────────────────────────────────────────────
    // SearchDeckSupporterToHandEffect
    // ──────────────────────────────────────────────

    @Test
    void searchDeckSupporterToHandEffect_FindsSupporters_AddsToHand() {
        ctx.getAttackerDeck().addAll(List.of(
                SupporterCard.builder().id("s1").name("Professor Sycamore").build(),
                PokemonCard.builder().id("p1").name("Pikachu").hp(60)
                        .stage(PokemonStage.BASIC).build(),
                SupporterCard.builder().id("s2").name("Shauna").build()
        ));
        AttackEffect effect = new SearchDeckSupporterToHandEffect(2);
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).hasSize(2);
        assertThat(ctx.getAttackerHand().get(0).getId()).isEqualTo("s1");
        assertThat(ctx.getAttackerHand().get(1).getId()).isEqualTo("s2");
        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }

    @Test
    void searchDeckSupporterToHandEffect_NoSupporters_AddsNothing() {
        ctx.getAttackerDeck().add(
                PokemonCard.builder().id("p1").name("Pikachu").hp(60)
                        .stage(PokemonStage.BASIC).build()
        );
        AttackEffect effect = new SearchDeckSupporterToHandEffect(1);
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).isEmpty();
    }

    // ──────────────────────────────────────────────
    // SearchDeckAttachEnergyEffect
    // ──────────────────────────────────────────────

    @Test
    void searchDeckAttachEnergyEffect_FindsEnergy_AttachesToSelf() {
        ctx.getAttackerDeck().add(
                EnergyCard.builder().id("e1").name("Fire Energy")
                        .energyType(EnergyType.FIRE).build()
        );
        AttackEffect effect = new SearchDeckAttachEnergyEffect(EnergyType.FIRE, true);
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
        assertThat(attacker.getEnergies().get(0).getEnergyType()).isEqualTo(EnergyType.FIRE);
    }

    @Test
    void searchDeckAttachEnergyEffect_NoMatchingEnergy_AttachesNothing() {
        ctx.getAttackerDeck().add(
                EnergyCard.builder().id("e1").name("Water Energy")
                        .energyType(EnergyType.WATER).build()
        );
        AttackEffect effect = new SearchDeckAttachEnergyEffect(EnergyType.FIRE, true);
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).isEmpty();
    }

    // ──────────────────────────────────────────────
    // IgnoreResistanceEffect
    // ──────────────────────────────────────────────

    @Test
    void ignoreResistanceEffect_SetsIgnoreResistanceFlag() {
        AttackEffect effect = new IgnoreResistanceEffect();
        effect.apply(ctx);
        assertThat(ctx.isIgnoreResistance()).isTrue();
        assertThat(ctx.isIgnoreWeakness()).isFalse();
    }

    // ──────────────────────────────────────────────
    // IgnoreWeaknessAndResistanceEffect
    // ──────────────────────────────────────────────

    @Test
    void ignoreWeaknessAndResistanceEffect_SetsBothFlags() {
        AttackEffect effect = new IgnoreWeaknessAndResistanceEffect();
        effect.apply(ctx);
        assertThat(ctx.isIgnoreResistance()).isTrue();
        assertThat(ctx.isIgnoreWeakness()).isTrue();
    }

    // ──────────────────────────────────────────────
    // AttachFromDiscardEffect
    // ──────────────────────────────────────────────

    @Test
    void attachFromDiscardEffect_TakesEnergyFromDiscard_AttachesToSelf() {
        ctx.getAttackerDiscard().add(
                EnergyCard.builder().id("e1").name("Darkness Energy")
                        .energyType(EnergyType.DARKNESS).build()
        );
        AttackEffect effect = new AttachFromDiscardEffect(EnergyType.DARKNESS, false);
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
        assertThat(attacker.getEnergies().get(0).getEnergyType()).isEqualTo(EnergyType.DARKNESS);
        assertThat(ctx.getAttackerDiscard()).isEmpty();
    }

    @Test
    void attachFromDiscardEffect_NoEnergyInDiscard_DoesNothing() {
        AttackEffect effect = new AttachFromDiscardEffect(EnergyType.DARKNESS, false);
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).isEmpty();
    }

    @Test
    void attachFromDiscardEffect_AttachToBench_WhenNoBench_EnergyLost() {
        ctx.getAttackerDiscard().add(
                EnergyCard.builder().id("e1").name("Darkness Energy")
                        .energyType(EnergyType.DARKNESS).build()
        );
        AttackEffect effect = new AttachFromDiscardEffect(EnergyType.DARKNESS, true);
        effect.apply(ctx);
        assertThat(ctx.getAttackerBench()).isEmpty();
        assertThat(ctx.getAttackerDiscard()).isEmpty();
        assertThat(attacker.getEnergies()).isEmpty();
    }

    @Test
    void attachFromDiscardEffect_AttachToBench_AttachesToFirstBenched() {
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        ctx.getAttackerBench().add(benched);
        ctx.getAttackerDiscard().add(
                EnergyCard.builder().id("e1").name("Darkness Energy")
                        .energyType(EnergyType.DARKNESS).build()
        );
        AttackEffect effect = new AttachFromDiscardEffect(EnergyType.DARKNESS, true);
        effect.apply(ctx);
        assertThat(benched.getEnergies()).hasSize(1);
        assertThat(ctx.getAttackerDiscard()).isEmpty();
    }

    // ──────────────────────────────────────────────
    // Edge cases: AttackEffect as FunctionalInterface
    // ──────────────────────────────────────────────

    @Test
    void attackEffect_Lambda_WorksAsFunctionalInterface() {
        AttackEffect lambda = ctx -> ctx.setPendingDamage(999);
        lambda.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(999);
    }

    // ──────────────────────────────────────────────
    // Extra branch coverage
    // ──────────────────────────────────────────────

    @Test
    void searchDeckPokemonToHandEffect_NullTypeFilter_MatchesAnyPokemonAndSkipsNonPokemon() {
        ctx.getAttackerDeck().add(EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build());
        ctx.getAttackerDeck().add(PokemonCard.builder().id("p9").name("Anymon")
                .pokemonType(EnergyType.WATER).hp(60).stage(PokemonStage.BASIC).build());
        AttackEffect effect = new SearchDeckPokemonToHandEffect(null);
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).hasSize(1);
        assertThat(ctx.getAttackerHand().get(0).getId()).isEqualTo("p9");
    }

    @Test
    void searchDeckAttachEnergyEffect_NullType_AttachesAnyEnergy() {
        ctx.getAttackerDeck().add(EnergyCard.builder().id("e1").name("Grass Energy")
                .energyType(EnergyType.GRASS).build());
        AttackEffect effect = new SearchDeckAttachEnergyEffect(null, true);
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
    }

    @Test
    void searchDeckAttachEnergyEffect_AttachToSelfFalse_StillAttaches() {
        ctx.getAttackerDeck().add(EnergyCard.builder().id("e1").name("Fire Energy")
                .energyType(EnergyType.FIRE).build());
        AttackEffect effect = new SearchDeckAttachEnergyEffect(EnergyType.FIRE, false);
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
    }

    @Test
    void searchDeckAttachEnergyEffect_NonEnergyCardInDeck_Ignored() {
        ctx.getAttackerDeck().add(PokemonCard.builder().id("p9").name("Mon").hp(60)
                .stage(PokemonStage.BASIC).build());
        ctx.getAttackerDeck().add(EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build());
        AttackEffect effect = new SearchDeckAttachEnergyEffect(EnergyType.FIRE, true);
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
    }

    @Test
    void attachFromDiscardEffect_SkipsNonMatchingCards_FindsMatch() {
        ctx.getAttackerDiscard().add(PokemonCard.builder().id("p9").name("Mon").hp(60)
                .stage(PokemonStage.BASIC).build());
        ctx.getAttackerDiscard().add(EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build());
        ctx.getAttackerDiscard().add(EnergyCard.builder().id("e2").energyType(EnergyType.DARKNESS).build());
        AttackEffect effect = new AttachFromDiscardEffect(EnergyType.DARKNESS, false);
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
        assertThat(attacker.getEnergies().get(0).getEnergyType()).isEqualTo(EnergyType.DARKNESS);
        assertThat(ctx.getAttackerDiscard()).hasSize(2);
    }

    @Test
    void coinFlipDiscardOpponentEnergyEffect_OnHeadsButNoEnergy_DiscardsNothing() {
        ctx.setRandom(new FixedRandom(true));
        AttackEffect effect = new CoinFlipDiscardOpponentEnergyEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderDiscard()).isEmpty();
    }
}
