package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class IsolatedEnergyEffectsTest {

    private GameContext ctx;
    private InPlayPokemon attacker;
    private InPlayPokemon defender;
    private PokemonCard basePokemon;

    @BeforeEach
    void setUp() {
        basePokemon = PokemonCard.builder()
                .id("p1").name("TestMon").hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS)
                .build();
        attacker = new InPlayPokemon(basePokemon);
        defender = new InPlayPokemon(basePokemon);
        ctx = new GameContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);
        // Replace the default 5-null bench with compact empty lists (see note in
        // IsolatedCoinFlipEffectsTest): isolated effects iterate/index the bench
        // assuming no null placeholder slots.
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setDefenderBench(new ArrayList<>());
        ctx.setPendingDamage(50);
    }

    @Test
    void digOutEffect_DiscardsTop_IfFightingAttaches() {
        ctx.getAttackerDeck().add(EnergyCard.builder()
                .id("e1").name("Fighting").energyType(EnergyType.FIGHTING).build());
        AttackEffect effect = new DigOutEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
        assertThat(attacker.getEnergies().get(0).getEnergyType()).isEqualTo(EnergyType.FIGHTING);
        assertThat(ctx.getAttackerDiscard()).isEmpty();
    }

    @Test
    void digOutEffect_DiscardsTop_IfNotFighting_DiscardsOnly() {
        ctx.getAttackerDeck().add(EnergyCard.builder()
                .id("e1").name("Water").energyType(EnergyType.WATER).build());
        AttackEffect effect = new DigOutEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).isEmpty();
        assertThat(ctx.getAttackerDiscard()).hasSize(1);
    }

    @Test
    void digOutEffect_EmptyDeck_DoesNothing() {
        AttackEffect effect = new DigOutEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerDiscard()).isEmpty();
    }

    @Test
    void electronCrushEffect_RemovesEnergyAndAddsBonus() {
        attacker.getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.LIGHTNING).build());
        ctx.setPendingDamage(20);
        AttackEffect effect = new ElectronCrushEffect();
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
        assertThat(attacker.getEnergies()).isEmpty();
        assertThat(ctx.getAttackerDiscard()).hasSize(1);
    }

    @Test
    void electronCrushEffect_NoEnergy_NoBonus() {
        ctx.setPendingDamage(20);
        AttackEffect effect = new ElectronCrushEffect();
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(20);
    }

    @Test
    void energyGlideEffect_SearchesLightning_AttachesAndSwitches() {
        ctx.getAttackerDeck().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.LIGHTNING).build());
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        ctx.getAttackerBench().add(benched);
        AttackEffect effect = new EnergyGlideEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
        assertThat(ctx.getAttacker()).isSameAs(benched);
        assertThat(ctx.getAttackerBench()).hasSize(1);
    }

    @Test
    void energyGlideEffect_NoLightning_SearchesAndShuffles() {
        ctx.getAttackerDeck().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.WATER).build());
        AttackEffect effect = new EnergyGlideEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).isEmpty();
    }

    @Test
    void energyGlideEffect_NoBench_SkipsSwitch() {
        ctx.getAttackerDeck().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.LIGHTNING).build());
        AttackEffect effect = new EnergyGlideEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
        assertThat(ctx.getAttacker()).isSameAs(attacker);
    }

    // Edge case: a non-Energy card precedes the Lightning Energy in the deck, exercising
    // the instanceof-false branch of the search predicate before the match is found.
    @Test
    void energyGlideEffect_SkipsNonEnergyCardsWhenSearching() {
        ctx.getAttackerDeck().add(PokemonCard.builder().id("c1").name("Pikachu").hp(60)
                .stage(PokemonStage.BASIC).build());
        ctx.getAttackerDeck().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.LIGHTNING).build());
        AttackEffect effect = new EnergyGlideEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
        assertThat(attacker.getEnergies().get(0).getEnergyType()).isEqualTo(EnergyType.LIGHTNING);
    }

    @Test
    void energySalonEffect_SearchesThreeDifferentTypes() {
        ctx.getAttackerDeck().addAll(List.of(
                EnergyCard.builder().id("e1").energyType(EnergyType.GRASS).build(),
                EnergyCard.builder().id("e2").energyType(EnergyType.FIRE).build(),
                EnergyCard.builder().id("e3").energyType(EnergyType.WATER).build(),
                EnergyCard.builder().id("e4").energyType(EnergyType.GRASS).build()));
        AttackEffect effect = new EnergySalonEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).hasSize(3);
        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }

    @Test
    void energySalonEffect_FewerThanThreeTypes_FindsAvailable() {
        ctx.getAttackerDeck().addAll(List.of(
                EnergyCard.builder().id("e1").energyType(EnergyType.GRASS).build(),
                EnergyCard.builder().id("e2").energyType(EnergyType.GRASS).build()));
        AttackEffect effect = new EnergySalonEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).hasSize(1);
    }

    @Test
    void energySalonEffect_NoEnergies_AddsNothing() {
        ctx.getAttackerDeck().add(PokemonCard.builder().id("c1").name("Pikachu").hp(60)
                .stage(PokemonStage.BASIC).build());
        AttackEffect effect = new EnergySalonEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).isEmpty();
    }

    // Edge case: a non-Energy card precedes the Energy in the deck, exercising the
    // instanceof-false branch of the search predicate during a successful search.
    @Test
    void energySalonEffect_SkipsNonEnergyCardsWhenSearching() {
        ctx.getAttackerDeck().add(PokemonCard.builder().id("c1").name("Pikachu").hp(60)
                .stage(PokemonStage.BASIC).build());
        ctx.getAttackerDeck().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.GRASS).build());
        AttackEffect effect = new EnergySalonEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).hasSize(1);
        assertThat(((EnergyCard) ctx.getAttackerHand().get(0)).getEnergyType())
                .isEqualTo(EnergyType.GRASS);
    }

    // Edge case: COLORLESS and null-typed Energies are not "basic Energy types" and must be
    // skipped by findNextDistinctType; only the single GRASS Energy is taken (and on the
    // second pass GRASS is already-found, so the loop breaks).
    @Test
    void energySalonEffect_SkipsColorlessAndNullTypedEnergies() {
        ctx.getAttackerDeck().addAll(List.of(
                EnergyCard.builder().id("e1").energyType(EnergyType.COLORLESS).build(),
                EnergyCard.builder().id("e2").build(),
                EnergyCard.builder().id("e3").energyType(EnergyType.GRASS).build()));
        AttackEffect effect = new EnergySalonEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).hasSize(1);
        assertThat(((EnergyCard) ctx.getAttackerHand().get(0)).getEnergyType())
                .isEqualTo(EnergyType.GRASS);
    }

    // gatherEnergyEffect tests removed — class deleted in favour of the existing
    // common SearchDeckAttachEnergyEffect(null, true). That class is already tested
    // via its other consumers; behaviour is identical (search-any-energy-and-attach-to-attacker).

    @Test
    void geomancyEffect_AttachesFairyToBenched() {
        InPlayPokemon b1 = new InPlayPokemon(basePokemon);
        InPlayPokemon b2 = new InPlayPokemon(basePokemon);
        ctx.getAttackerBench().addAll(List.of(b1, b2));
        ctx.getAttackerDeck().addAll(List.of(
                EnergyCard.builder().id("e1").energyType(EnergyType.FAIRY).build(),
                EnergyCard.builder().id("e2").energyType(EnergyType.FAIRY).build()));
        AttackEffect effect = new GeomancyEffect();
        effect.apply(ctx);
        assertThat(b1.getEnergies()).hasSize(1);
        assertThat(b2.getEnergies()).hasSize(1);
    }

    @Test
    void geomancyEffect_LessThanTwoBench_AttachesToAvailable() {
        InPlayPokemon b1 = new InPlayPokemon(basePokemon);
        ctx.getAttackerBench().add(b1);
        ctx.getAttackerDeck().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.FAIRY).build());
        AttackEffect effect = new GeomancyEffect();
        effect.apply(ctx);
        assertThat(b1.getEnergies()).hasSize(1);
    }

    @Test
    void geomancyEffect_NoFairyEnergy_AttachesNothing() {
        InPlayPokemon b1 = new InPlayPokemon(basePokemon);
        ctx.getAttackerBench().add(b1);
        ctx.getAttackerDeck().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.WATER).build());
        AttackEffect effect = new GeomancyEffect();
        effect.apply(ctx);
        assertThat(b1.getEnergies()).isEmpty();
    }

    // Edge case: a non-Energy card precedes the Fairy Energy in the deck, exercising the
    // instanceof-false branch of the search predicate before the Fairy match is found.
    @Test
    void geomancyEffect_SkipsNonEnergyCardsWhenSearching() {
        InPlayPokemon b1 = new InPlayPokemon(basePokemon);
        ctx.getAttackerBench().add(b1);
        ctx.getAttackerDeck().add(PokemonCard.builder().id("c1").name("Pikachu").hp(60)
                .stage(PokemonStage.BASIC).build());
        ctx.getAttackerDeck().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.FAIRY).build());
        AttackEffect effect = new GeomancyEffect();
        effect.apply(ctx);
        assertThat(b1.getEnergies()).hasSize(1);
        assertThat(b1.getEnergies().get(0).getEnergyType()).isEqualTo(EnergyType.FAIRY);
    }

    @Test
    void heartfeltSongEffect_DiscardsDarknessFromDefender() {
        defender.getEnergies().addAll(List.of(
                EnergyCard.builder().id("e1").energyType(EnergyType.DARKNESS).build(),
                EnergyCard.builder().id("e2").energyType(EnergyType.FIRE).build()));
        AttackEffect effect = new HeartfeltSongEffect();
        effect.apply(ctx);
        assertThat(defender.getEnergies()).hasSize(1);
        assertThat(defender.getEnergies().get(0).getEnergyType()).isEqualTo(EnergyType.FIRE);
        assertThat(ctx.getDefenderDiscard()).hasSize(1);
    }

    @Test
    void heartfeltSongEffect_NoDarknessEnergy_DoesNothing() {
        defender.getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.FIRE).build());
        AttackEffect effect = new HeartfeltSongEffect();
        effect.apply(ctx);
        assertThat(defender.getEnergies()).hasSize(1);
        assertThat(ctx.getDefenderDiscard()).isEmpty();
    }

    @Test
    void magmaMantleEffect_DiscardsTop_IfFireAttachesAndBonuses() {
        ctx.getAttackerDeck().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.FIRE).build());
        ctx.setPendingDamage(30);
        AttackEffect effect = new MagmaMantleEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
        assertThat(ctx.getPendingDamage()).isEqualTo(80);
        assertThat(ctx.getAttackerDiscard()).isEmpty();
    }

    @Test
    void magmaMantleEffect_DiscardsTop_IfNotFire_DiscardsOnly() {
        ctx.getAttackerDeck().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.WATER).build());
        ctx.setPendingDamage(30);
        AttackEffect effect = new MagmaMantleEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).isEmpty();
        assertThat(ctx.getPendingDamage()).isEqualTo(30);
        assertThat(ctx.getAttackerDiscard()).hasSize(1);
    }

    // Edge case: empty deck — attackerDiscardTopCard() returns null, so the instanceof
    // guard is false and neither the attach nor the +50 damage bonus is applied.
    @Test
    void magmaMantleEffect_EmptyDeck_DoesNothing() {
        ctx.setPendingDamage(30);
        AttackEffect effect = new MagmaMantleEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).isEmpty();
        assertThat(ctx.getPendingDamage()).isEqualTo(30);
        assertThat(ctx.getAttackerDiscard()).isEmpty();
    }

    @Test
    void recoverEffect_DiscardsEnergyAndHealsAll() {
        attacker.getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.WATER).build());
        attacker.setDamageTaken(80);
        AttackEffect effect = new RecoverEffect();
        effect.apply(ctx);
        assertThat(attacker.getDamageTaken()).isZero();
        assertThat(attacker.getEnergies()).isEmpty();
        assertThat(ctx.getAttackerDiscard()).hasSize(1);
    }

    @Test
    void recoverEffect_NoEnergy_DoesNotHeal() {
        attacker.setDamageTaken(80);
        AttackEffect effect = new RecoverEffect();
        effect.apply(ctx);
        assertThat(attacker.getDamageTaken()).isEqualTo(80);
    }

    @Test
    void recoverEffect_NoDamage_StillDiscardsEnergy() {
        attacker.getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.WATER).build());
        AttackEffect effect = new RecoverEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).isEmpty();
        assertThat(ctx.getAttackerDiscard()).hasSize(1);
    }

    @Test
    void trickyStepsEffect_MovesEnergyFromDefenderToBench() {
        defender.getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.FIRE).build());
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        ctx.getDefenderBench().add(benched);
        AttackEffect effect = new TrickyStepsEffect();
        effect.apply(ctx);
        assertThat(defender.getEnergies()).isEmpty();
        assertThat(benched.getEnergies()).hasSize(1);
    }

    @Test
    void trickyStepsEffect_NoBench_DoesNothing() {
        defender.getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.FIRE).build());
        AttackEffect effect = new TrickyStepsEffect();
        effect.apply(ctx);
        assertThat(defender.getEnergies()).hasSize(1);
    }

    @Test
    void trickyStepsEffect_NoEnergyOnDefender_DoesNothing() {
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        ctx.getDefenderBench().add(benched);
        AttackEffect effect = new TrickyStepsEffect();
        effect.apply(ctx);
        assertThat(benched.getEnergies()).isEmpty();
    }

    @Test
    void yCycloneEffect_MovesEnergyFromAttackerToBench() {
        attacker.getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.DARKNESS).build());
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        ctx.getAttackerBench().add(benched);
        AttackEffect effect = new YCycloneEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).isEmpty();
        assertThat(benched.getEnergies()).hasSize(1);
    }

    @Test
    void yCycloneEffect_NoBench_DoesNothing() {
        attacker.getEnergies().add(EnergyCard.builder()
                .id("e1").energyType(EnergyType.DARKNESS).build());
        AttackEffect effect = new YCycloneEffect();
        effect.apply(ctx);
        assertThat(attacker.getEnergies()).hasSize(1);
    }

    @Test
    void yCycloneEffect_NoEnergy_DoesNothing() {
        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        ctx.getAttackerBench().add(benched);
        AttackEffect effect = new YCycloneEffect();
        effect.apply(ctx);
        assertThat(benched.getEnergies()).isEmpty();
    }
}
