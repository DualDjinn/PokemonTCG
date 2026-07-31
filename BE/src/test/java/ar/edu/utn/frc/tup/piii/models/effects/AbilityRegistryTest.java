package ar.edu.utn.frc.tup.piii.models.effects;

import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.AbilityEffect;
import ar.edu.utn.frc.tup.piii.models.AbilityEffect.TriggerContext;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AbilityRegistry}: lookup/registration semantics plus a full pass over each
 * registered ability body (PASSIVE and TRIGGERED entry points) covering their internal branches.
 */
class AbilityRegistryTest {

    private static final class FixedRandom extends Random {
        private final boolean[] values;
        private int idx;

        FixedRandom(boolean... values) {
            this.values = values.clone();
        }

        @Override
        public boolean nextBoolean() {
            return values[idx++ % values.length];
        }
    }

    private AbilityRegistry registry;
    private GameContext ctx;
    private InPlayPokemon attacker;
    private InPlayPokemon defender;
    private PokemonCard base;

    @BeforeEach
    void setUp() {
        registry = new AbilityRegistry();
        base = PokemonCard.builder().id("p1").name("Mon").hp(100).stage(PokemonStage.BASIC).build();
        attacker = new InPlayPokemon(base);
        defender = new InPlayPokemon(base);
        ctx = new GameContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setDefenderBench(new ArrayList<>());
    }

    private AbilityEffect effect(String cardId) {
        return registry.getAbilityEffect(cardId, 0);
    }

    private List<Card> dummyPrizes(int n) {
        List<Card> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(PokemonCard.builder().id("prize-" + i).name("Prize").build());
        }
        return list;
    }

    // ───────── lookup / registration ─────────

    @Test
    void getAbilityEffect_UnregisteredKey_ReturnsNoOp() {
        AbilityEffect effect = registry.getAbilityEffect("xy1-999", 0);
        assertThat(effect).isSameAs(AbilityRegistry.NO_OP);
    }

    @Test
    void register_AndGetAbilityEffect_ReturnsRegisteredStrategy() {
        AbilityEffect marker = (GameContext c, InPlayPokemon holder, Ability ability) -> { };
        registry.register("custom-1", 0, marker);
        assertThat(registry.getAbilityEffect("custom-1", 0)).isSameAs(marker);
    }

    @Test
    void register_SameKey_OverwritesPreviousEntry() {
        AbilityEffect first = (GameContext c, InPlayPokemon holder, Ability ability) -> { };
        AbilityEffect second = (GameContext c, InPlayPokemon holder, Ability ability) -> { };
        registry.register("custom-2", 0, first);
        registry.register("custom-2", 0, second);
        assertThat(registry.getAbilityEffect("custom-2", 0)).isSameAs(second);
    }

    @Test
    void getAbilityEffect_DifferentAbilityIndex_ReturnsDistinctEntries() {
        AbilityEffect first = (GameContext c, InPlayPokemon holder, Ability ability) -> { };
        AbilityEffect second = (GameContext c, InPlayPokemon holder, Ability ability) -> { };
        registry.register("custom-3", 0, first);
        registry.register("custom-3", 1, second);
        assertThat(registry.getAbilityEffect("custom-3", 0)).isSameAs(first);
        assertThat(registry.getAbilityEffect("custom-3", 1)).isSameAs(second);
    }

    @Test
    void noOp_AppliesWithoutSideEffects() {
        assertThatCode(() -> AbilityRegistry.NO_OP.apply(null, null, null))
                .doesNotThrowAnyException();
    }

    // ───────── xy1-14 Spiky Shield (TRIGGERED) ─────────

    @Test
    void spikyShield_passiveApply_isNoOp() {
        assertThatCode(() -> effect("xy1-14").apply(ctx, defender, null)).doesNotThrowAnyException();
    }

    @Test
    void spikyShield_triggeredWithAttacker_addsDamageToAttacker() {
        effect("xy1-14").apply(ctx, defender, null, new TriggerContext(attacker));
        assertThat(attacker.getDamageTaken()).isEqualTo(30);
    }

    @Test
    void spikyShield_triggeredWithoutAttacker_noChange() {
        effect("xy1-14").apply(ctx, defender, null, new TriggerContext(null));
        assertThat(attacker.getDamageTaken()).isZero();
    }

    // ───────── xy1-44 Destiny Burst (TRIGGERED) ─────────

    @Test
    void destinyBurst_passiveApply_isNoOp() {
        assertThatCode(() -> effect("xy1-44").apply(ctx, defender, null)).doesNotThrowAnyException();
    }

    @Test
    void destinyBurst_triggeredHeadsWithAttacker_adds50() {
        ctx.setRandom(new FixedRandom(true));
        effect("xy1-44").apply(ctx, defender, null, new TriggerContext(attacker));
        assertThat(attacker.getDamageTaken()).isEqualTo(50);
    }

    @Test
    void destinyBurst_triggeredTails_noChange() {
        ctx.setRandom(new FixedRandom(false));
        effect("xy1-44").apply(ctx, defender, null, new TriggerContext(attacker));
        assertThat(attacker.getDamageTaken()).isZero();
    }

    @Test
    void destinyBurst_triggeredHeadsWithoutAttacker_noChange() {
        ctx.setRandom(new FixedRandom(true));
        effect("xy1-44").apply(ctx, defender, null, new TriggerContext(null));
        assertThat(attacker.getDamageTaken()).isZero();
    }

    // ───────── xy1-114 Fur Coat (PASSIVE) ─────────

    @Test
    void furCoat_whenHolderIsDefender_reducesIncomingDamage() {
        ctx.setPendingDamage(50);
        effect("xy1-114").apply(ctx, defender, null);
        assertThat(ctx.getPendingDamage()).isEqualTo(30);
    }

    @Test
    void furCoat_whenHolderIsNotDefender_noChange() {
        ctx.setPendingDamage(50);
        effect("xy1-114").apply(ctx, attacker, null);
        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    // ───────── xy1-74 Upside-Down Evolution (ACTIVATED) ─────────

    @Test
    void upsideDownEvolution_opponentHasNotTakenMorePrizes_throws() {
        ctx.setAttackerPrizes(dummyPrizes(6));
        ctx.setDefenderPrizes(dummyPrizes(6));
        assertThatThrownBy(() -> effect("xy1-74").apply(ctx, attacker, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has not taken more prizes");
    }

    @Test
    void upsideDownEvolution_noEvolutionInDeck_throws() {
        ctx.setAttackerPrizes(dummyPrizes(6));
        ctx.setDefenderPrizes(dummyPrizes(4));
        // A non-Pokemon card and a Pokemon that does not evolve from Inkay: filter never matches.
        ctx.getAttackerDeck().add(EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build());
        ctx.getAttackerDeck().add(PokemonCard.builder()
                .id("xy1-2").name("Other").hp(50).stage(PokemonStage.STAGE_1).evolvesFrom("Pikachu").build());
        assertThatThrownBy(() -> effect("xy1-74").apply(ctx, attacker, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no card that evolves from Inkay");
    }

    @Test
    void upsideDownEvolution_nullPrizes_treatedAsSixLeft_throws() {
        ctx.setAttackerPrizes(null);
        ctx.setDefenderPrizes(null);
        assertThatThrownBy(() -> effect("xy1-74").apply(ctx, attacker, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("has not taken more prizes");
    }

    @Test
    void upsideDownEvolution_holderNeitherActiveNorBenched_evolvesWithoutBenchUpdate() {
        ctx.setAttackerPrizes(dummyPrizes(6));
        ctx.setDefenderPrizes(dummyPrizes(4));
        InPlayPokemon orphan = new InPlayPokemon(base);
        PokemonCard malamar = PokemonCard.builder()
                .id("xy1-75").name("Malamar").hp(90).stage(PokemonStage.STAGE_1)
                .evolvesFrom("Inkay").build();
        ctx.getAttackerDeck().add(malamar);

        effect("xy1-74").apply(ctx, orphan, null);

        assertThat(ctx.getAttackerDeck()).isEmpty();
        assertThat(ctx.getAttacker()).isSameAs(attacker);
    }

    @Test
    void upsideDownEvolution_evolvesActiveHolder() {
        ctx.setAttackerPrizes(dummyPrizes(6));
        ctx.setDefenderPrizes(dummyPrizes(4));
        PokemonCard malamar = PokemonCard.builder()
                .id("xy1-75").name("Malamar").hp(90).stage(PokemonStage.STAGE_1)
                .evolvesFrom("Inkay").build();
        ctx.getAttackerDeck().add(malamar);

        effect("xy1-74").apply(ctx, attacker, null);

        assertThat(ctx.getAttacker().getCard().getId()).isEqualTo("xy1-75");
        assertThat(ctx.getAttackerDeck()).isEmpty();
    }

    @Test
    void upsideDownEvolution_evolvesBenchedHolder() {
        ctx.setAttackerPrizes(dummyPrizes(6));
        ctx.setDefenderPrizes(dummyPrizes(4));
        InPlayPokemon benched = new InPlayPokemon(base);
        ctx.getAttackerBench().add(benched);
        PokemonCard malamar = PokemonCard.builder()
                .id("xy1-75").name("Malamar").hp(90).stage(PokemonStage.STAGE_1)
                .evolvesFrom("Inkay").build();
        ctx.getAttackerDeck().add(malamar);

        effect("xy1-74").apply(ctx, benched, null);

        assertThat(ctx.getAttackerBench().get(0).getCard().getId()).isEqualTo("xy1-75");
    }

    // ───────── not-yet-implemented guards ─────────

    @Test
    void stanceChange85_throwsNotImplemented() {
        assertThatThrownBy(() -> effect("xy1-85").apply(ctx, attacker, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not yet implemented");
    }

    @Test
    void stanceChange86_throwsNotImplemented() {
        assertThatThrownBy(() -> effect("xy1-86").apply(ctx, attacker, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not yet implemented");
    }

    @Test
    void fairyTransfer93_throwsNotImplemented() {
        assertThatThrownBy(() -> effect("xy1-93").apply(ctx, attacker, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not yet implemented");
    }

    // ───────── xy1-26 Delphox — Mystical Fire ─────────

    @Test
    void delphox_drawsUntilSixCards() {
        for (int i = 0; i < 10; i++) {
            ctx.getAttackerDeck().add(PokemonCard.builder().id("d" + i).name("D").build());
        }
        effect("xy1-26").apply(ctx, attacker, null);
        assertThat(ctx.getAttackerHand()).hasSize(6);
    }

    @Test
    void delphox_emptyDeck_stopsWithoutError() {
        effect("xy1-26").apply(ctx, attacker, null);
        assertThat(ctx.getAttackerHand()).isEmpty();
    }

    // ───────── xy1-41 Greninja — Water Shuriken ─────────

    @Test
    void greninja_noWaterEnergyInHand_throws() {
        assertThatThrownBy(() -> effect("xy1-41").apply(ctx, attacker, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No Water Energy");
    }

    @Test
    void greninja_withWaterEnergy_discardsAndDamagesDefender() {
        ctx.getAttackerHand().add(EnergyCard.builder().id("e1").energyType(EnergyType.WATER).build());
        effect("xy1-41").apply(ctx, attacker, null);
        assertThat(ctx.getAttackerHand()).isEmpty();
        assertThat(ctx.getAttackerDiscard()).hasSize(1);
        assertThat(defender.getDamageTaken()).isEqualTo(30);
    }

    @Test
    void greninja_withWaterEnergyButNoDefender_discardsWithoutDamage() {
        ctx.getAttackerHand().add(EnergyCard.builder().id("e1").energyType(EnergyType.WATER).build());
        ctx.setDefender(null);
        effect("xy1-41").apply(ctx, attacker, null);
        assertThat(ctx.getAttackerDiscard()).hasSize(1);
    }

    // ───────── xy1-103 Swellow — Drive Off ─────────

    @Test
    void swellow_noBench_doesNothing() {
        assertThatCode(() -> effect("xy1-103").apply(ctx, attacker, null)).doesNotThrowAnyException();
        assertThat(ctx.getDefender()).isSameAs(defender);
    }

    @Test
    void swellow_benchPresent_swapsDefenderWithBenched() {
        InPlayPokemon benched = new InPlayPokemon(base);
        ctx.getDefenderBench().add(benched);
        effect("xy1-103").apply(ctx, attacker, null);
        assertThat(ctx.getDefender()).isSameAs(benched);
        assertThat(ctx.getDefenderBench().get(0)).isSameAs(defender);
    }

    @Test
    void swellow_benchWithNullSlot_skipsNullAndSwapsFirstReal() {
        InPlayPokemon benched = new InPlayPokemon(base);
        ctx.getDefenderBench().add(null);
        ctx.getDefenderBench().add(benched);
        effect("xy1-103").apply(ctx, attacker, null);
        assertThat(ctx.getDefender()).isSameAs(benched);
    }
}
