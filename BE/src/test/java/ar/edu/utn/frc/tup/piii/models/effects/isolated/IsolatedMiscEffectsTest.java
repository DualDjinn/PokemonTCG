package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IsolatedMiscEffectsTest {

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
        exCard = PokemonCard.builder()
                .id("p2").name("TestEX").hp(120)
                .stage(PokemonStage.BASIC).isEx(true)
                .build();
        lunatoneCard = PokemonCard.builder()
                .id("p3").name("Lunatone").hp(70)
                .stage(PokemonStage.BASIC).build();
        attacker = new InPlayPokemon(basePokemon);
        defender = new InPlayPokemon(basePokemon);
        ctx = new GameContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);
        ctx.setPendingDamage(50);
    }

    // chargeDashEffect tests moved to AttackEffectDslTest#chargeDashRecipe_*
    // (migrated to seq(addDamage(20), selfDamage(20)) — see registerPhaseCMigratedFromIsolated).

    @Test
    void hardenEffect_SetsPreventThreshold() {
        AttackEffect effect = new HardenEffect();
        effect.apply(ctx);
        assertThat(attacker.getPreventIncomingDamageThreshold()).isEqualTo(60);
    }


    @Test
    void joustEffect_DiscardsToolFromDefender() {
        defender.setTool(PokemonToolCard.builder()
                .id("t1").name("Fairy Garden").build());
        AttackEffect effect = new JoustEffect();
        effect.apply(ctx);
        assertThat(defender.getTool()).isNull();
        assertThat(ctx.getDefenderDiscard()).hasSize(1);
    }

    @Test
    void joustEffect_NoTool_DoesNothing() {
        AttackEffect effect = new JoustEffect();
        effect.apply(ctx);
        assertThat(ctx.getDefenderDiscard()).isEmpty();
    }

    @Test
    void metalWallopEffect_AppliesExistingBonusAndStacks() {
        attacker.setMetalWallopBonus(40);
        ctx.setPendingDamage(30);
        AttackEffect effect = new MetalWallopEffect();
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(70);
        assertThat(attacker.getMetalWallopBonus()).isEqualTo(80);
    }

    @Test
    void metalWallopEffect_NoExistingBonus_StillSetsBonus() {
        ctx.setPendingDamage(30);
        AttackEffect effect = new MetalWallopEffect();
        effect.apply(ctx);
        assertThat(ctx.getPendingDamage()).isEqualTo(30);
        assertThat(attacker.getMetalWallopBonus()).isEqualTo(40);
    }

    @Test
    void moonblastEffect_SetsDamageReductionOnDefender() {
        AttackEffect effect = new MoonblastEffect();
        effect.apply(ctx);
        assertThat(defender.getIncomingDamageReduction()).isEqualTo(20);
    }

    // rapidSpinEffect tests moved to AttackEffectDslTest#rapidSpinRecipe_*
    // (migrated to seq(SwitchSelfEffect, SwitchOpponentEffect) — see registerPhaseCMigratedFromIsolated).
    // The empty-bench / partial-bench safety is provided by SwitchSelfEffect / SwitchOpponentEffect
    // themselves; their own tests cover those edge cases.

    @Test
    void sweetScentEffect_HealsAttacker() {
        attacker.setDamageTaken(30);
        AttackEffect effect = new SweetScentEffect();
        effect.apply(ctx);
        assertThat(attacker.getDamageTaken()).isEqualTo(10);
    }

    @Test
    void sweetScentEffect_ClampsAtZero() {
        attacker.setDamageTaken(10);
        AttackEffect effect = new SweetScentEffect();
        effect.apply(ctx);
        assertThat(attacker.getDamageTaken()).isZero();
    }

}
