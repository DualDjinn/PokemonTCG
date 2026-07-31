package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IsolatedStatusEffectsTest {

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
    }

    // poisonRingEffect, conversionPowderEffect (both variants), hypnoHeadbuttEffect, xBlastEffect
    // tests all moved to AttackEffectDslTest (atoms: applyStatus + PreventRetreatEffect / applyStatus
    // / hypnoHeadbuttRecipe / attackerCantUseAttackName). See registerPhaseCMigratedFromIsolated.

    @Test
    void gastroAcidEffect_DisablesDefenderAbilities() {
        AttackEffect effect = new GastroAcidEffect();
        effect.apply(ctx);
        assertThat(defender.isAbilitiesDisabled()).isTrue();
    }

    @Test
    void hocusPinkusEffect_DefenderCantAttack() {
        AttackEffect effect = new HocusPinkusEffect();
        effect.apply(ctx);
        assertThat(defender.isCantAttack()).isTrue();
    }

    @Test
    void mentalPanicEffect_SetsIfAttacksFlipCoin() {
        AttackEffect effect = new MentalPanicEffect();
        effect.apply(ctx);
        assertThat(defender.isIfAttacksFlipCoin()).isTrue();
    }

    // kingsShieldEffect / rockWreckerEffect tests moved to AttackEffectDslTest
    // (kingsShieldRecipe_*, rockWreckerRecipe_*). See registerPhaseCMigratedFromIsolated.
}
