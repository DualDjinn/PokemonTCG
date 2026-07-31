package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class IsolatedDeckHandEffectsTest {

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
        // Replace the default 5-null bench with compact empty lists (see note in
        // IsolatedCoinFlipEffectsTest): isolated effects iterate/index the bench
        // assuming no null placeholder slots.
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setDefenderBench(new ArrayList<>());
        ctx.setPendingDamage(50);
    }

    @Test
    void devastatingWindEffect_ShufflesHandIntoDeckAndDrawsFour() {
        ctx.getDefenderHand().addAll(List.of(
                PokemonCard.builder().id("c1").name("A").build(),
                PokemonCard.builder().id("c2").name("B").build()));
        ctx.getDefenderDeck().addAll(List.of(
                PokemonCard.builder().id("c3").name("C").build(),
                PokemonCard.builder().id("c4").name("D").build(),
                PokemonCard.builder().id("c5").name("E").build(),
                PokemonCard.builder().id("c6").name("F").build()));
        AttackEffect effect = new DevastatingWindEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderHand()).hasSize(4);
        assertThat(ctx.getDefenderDeck()).hasSize(2);
    }

    @Test
    void devastatingWindEffect_WhenDeckHasFewerThanFour_DrawsAvailable() {
        ctx.getDefenderHand().add(PokemonCard.builder().id("c1").name("A").build());
        ctx.getDefenderDeck().add(PokemonCard.builder().id("c2").name("B").build());
        AttackEffect effect = new DevastatingWindEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderHand()).hasSize(2);
        assertThat(ctx.getDefenderDeck()).isEmpty();
    }

    @Test
    void eerieVoiceEffect_DealsDamageToAllDefenderPokemon() {
        InPlayPokemon benched1 = new InPlayPokemon(basePokemon);
        InPlayPokemon benched2 = new InPlayPokemon(basePokemon);
        ctx.getDefenderBench().addAll(List.of(benched1, benched2));
        AttackEffect effect = new EerieVoiceEffect();
        effect.apply(ctx);
        assertThat(defender.getDamageTaken()).isEqualTo(20);
        assertThat(benched1.getDamageTaken()).isEqualTo(20);
        assertThat(benched2.getDamageTaken()).isEqualTo(20);
    }

    @Test
    void eerieVoiceEffect_AccumulatesExistingDamage() {
        defender.setDamageTaken(10);
        AttackEffect effect = new EerieVoiceEffect();
        effect.apply(ctx);
        assertThat(defender.getDamageTaken()).isEqualTo(30);
    }

    @Test
    void eerieVoiceEffect_NoBench_DamagesOnlyActive() {
        AttackEffect effect = new EerieVoiceEffect();
        effect.apply(ctx);
        assertThat(defender.getDamageTaken()).isEqualTo(20);
    }

    // hammerArmEffect tests moved to AttackEffectDslTest#discardDefenderDeckTop_*
    // (migrated to DSL atom — see CardEffectRegistry#registerPhaseCMigratedFromIsolated).

    @Test
    void nastyPlotEffect_TakesFirstCardFromDeckToHand() {
        ctx.getAttackerDeck().addAll(List.of(
                PokemonCard.builder().id("c1").name("A").build(),
                PokemonCard.builder().id("c2").name("B").build()));
        AttackEffect effect = new NastyPlotEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).hasSize(1);
        assertThat(ctx.getAttackerHand().get(0).getName()).isEqualTo("A");
        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }

    @Test
    void nastyPlotEffect_EmptyDeck_DoesNothing() {
        AttackEffect effect = new NastyPlotEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).isEmpty();
    }

    @Test
    void pickupEffect_RecoversItemCardsFromDiscard() {
        ctx.getAttackerDiscard().addAll(List.of(
                ItemCard.builder().id("i1").name("Potion").build(),
                PokemonCard.builder().id("c1").name("Pikachu").hp(60)
                        .stage(PokemonStage.BASIC).build(),
                ItemCard.builder().id("i2").name("Switch").build(),
                ItemCard.builder().id("i3").name("Pok\u00e9 Ball").build()));
        AttackEffect effect = new PickupEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).hasSize(2);
        assertThat(ctx.getAttackerDiscard()).hasSize(2);
        assertThat(ctx.getAttackerDiscard().get(0).getName()).isEqualTo("Potion");
        assertThat(ctx.getAttackerDiscard().get(1).getName()).isEqualTo("Pikachu");
    }

    @Test
    void pickupEffect_LessThanTwoItems_RecoversAvailable() {
        ctx.getAttackerDiscard().add(ItemCard.builder().id("i1").name("Potion").build());
        AttackEffect effect = new PickupEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).hasSize(1);
    }

    @Test
    void pickupEffect_NoItemCards_RecoversNothing() {
        ctx.getAttackerDiscard().add(PokemonCard.builder().id("c1").name("Pikachu").hp(60)
                .stage(PokemonStage.BASIC).build());
        AttackEffect effect = new PickupEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerHand()).isEmpty();
    }

    @Test
    void recycleEffect_MovesLastDiscardToTopOfDeck() {
        ctx.getAttackerDiscard().addAll(List.of(
                PokemonCard.builder().id("c1").name("A").build(),
                PokemonCard.builder().id("c2").name("B").build()));
        AttackEffect effect = new RecycleEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerDeck()).hasSize(1);
        assertThat(ctx.getAttackerDeck().get(0).getName()).isEqualTo("B");
        assertThat(ctx.getAttackerDiscard()).hasSize(1);
    }

    @Test
    void recycleEffect_EmptyDiscard_DoesNothing() {
        AttackEffect effect = new RecycleEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerDeck()).isEmpty();
    }

    @Test
    void spiritScreamEffect_SetsBothActiveTo10Hp() {
        PokemonCard highHp = PokemonCard.builder()
                .id("p2").name("HighHP").hp(180)
                .stage(PokemonStage.BASIC).build();
        attacker = new InPlayPokemon(highHp);
        attacker.setDamageTaken(50);
        ctx.setAttacker(attacker);
        defender.setDamageTaken(120);
        AttackEffect effect = new SpiritScreamEffect();
        effect.apply(ctx);
        assertThat(attacker.getDamageTaken()).isEqualTo(170);
        assertThat(defender.getDamageTaken()).isEqualTo(120);
        assertThat(ctx.getPendingDamage()).isZero();
    }

    @Test
    void spiritScreamEffect_WhenAlreadyBelow10Hp_DoesNotIncreaseDamage() {
        defender.setDamageTaken(95);
        AttackEffect effect = new SpiritScreamEffect();
        effect.apply(ctx);
        assertThat(defender.getDamageTaken()).isEqualTo(95);
    }

    @Test
    void clairvoyantEyeEffect_WhenDeckHasEnoughCards_DoesNothingStructural() {
        ctx.getAttackerDeck().addAll(List.of(
                PokemonCard.builder().id("c1").name("A").build(),
                PokemonCard.builder().id("c2").name("B").build(),
                PokemonCard.builder().id("c3").name("C").build()));
        AttackEffect effect = new ClairvoyantEyeEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerDeck()).hasSize(3);
    }

    @Test
    void clairvoyantEyeEffect_WhenDeckHasFewerThanThree_Returns() {
        ctx.getAttackerDeck().add(PokemonCard.builder().id("c1").name("A").build());
        AttackEffect effect = new ClairvoyantEyeEffect();
        effect.apply(ctx);
        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }
}
