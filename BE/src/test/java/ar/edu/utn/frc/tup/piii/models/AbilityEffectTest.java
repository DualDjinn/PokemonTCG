package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link AbilityEffect} functional interface.
 *
 * <p>Verifica que el método default de 4 argumentos
 * {@link AbilityEffect#apply(GameContext, InPlayPokemon, Ability, AbilityEffect.TriggerContext)}
 * delegue en el método abstracto de 3 argumentos, y el accesor del record
 * {@link AbilityEffect.TriggerContext}.
 */
class AbilityEffectTest {

    private InPlayPokemon newPokemon() {
        PokemonCard card = PokemonCard.builder()
                .id("p1").name("TestMon").hp(60)
                .stage(PokemonStage.BASIC)
                .build();
        return new InPlayPokemon(card);
    }

    @Test
    void defaultApply_DelegatesToThreeArgApply() {
        GameContext ctx = new GameContext();
        InPlayPokemon holder = newPokemon();
        AtomicInteger calls = new AtomicInteger();

        AbilityEffect effect = (c, h, a) -> calls.incrementAndGet();
        effect.apply(ctx, holder, null, new AbilityEffect.TriggerContext(holder));

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void defaultApply_PassesThroughSameArguments() {
        GameContext ctx = new GameContext();
        InPlayPokemon holder = newPokemon();
        Object[] captured = new Object[2];

        AbilityEffect effect = (c, h, a) -> {
            captured[0] = c;
            captured[1] = h;
        };
        effect.apply(ctx, holder, null, new AbilityEffect.TriggerContext(null));

        assertThat(captured[0]).isSameAs(ctx);
        assertThat(captured[1]).isSameAs(holder);
    }

    @Test
    void triggerContext_ExposesAttacker() {
        InPlayPokemon attacker = newPokemon();
        AbilityEffect.TriggerContext trigger = new AbilityEffect.TriggerContext(attacker);
        assertThat(trigger.attacker()).isSameAs(attacker);
    }

    @Test
    void triggerContext_AllowsNullAttacker() {
        AbilityEffect.TriggerContext trigger = new AbilityEffect.TriggerContext(null);
        assertThat(trigger.attacker()).isNull();
    }
}
