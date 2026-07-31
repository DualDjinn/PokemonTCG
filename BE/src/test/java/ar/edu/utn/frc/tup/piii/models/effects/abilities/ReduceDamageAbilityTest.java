package ar.edu.utn.frc.tup.piii.models.effects.abilities;

import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.AbilityType;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ReduceDamageAbility}.
 */
class ReduceDamageAbilityTest {

    @Test
    void apply_SubtractsConfiguredAmountFromPendingDamage() {
        ReduceDamageAbility ability = new ReduceDamageAbility(20);
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(50);

        ability.apply(ctx, createInPlayPokemon(), createAbility());

        assertThat(ctx.getPendingDamage()).isEqualTo(30);
    }

    @Test
    void apply_ClampsResultToZeroWhenReductionExceedsDamage() {
        ReduceDamageAbility ability = new ReduceDamageAbility(20);
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(10);

        ability.apply(ctx, createInPlayPokemon(), createAbility());

        assertThat(ctx.getPendingDamage()).isZero();
    }

    @Test
    void apply_FromZero_StaysAtZero() {
        ReduceDamageAbility ability = new ReduceDamageAbility(20);
        GameContext ctx = new GameContext();

        ability.apply(ctx, createInPlayPokemon(), createAbility());

        assertThat(ctx.getPendingDamage()).isZero();
    }

    @Test
    void getAmount_ReturnsConfiguredValue() {
        assertThat(new ReduceDamageAbility(15).getAmount()).isEqualTo(15);
        assertThat(new ReduceDamageAbility(0).getAmount()).isZero();
    }

    @Test
    void apply_NullHolder_Throws() {
        ReduceDamageAbility ability = new ReduceDamageAbility(10);
        assertThatThrownBy(() -> ability.apply(new GameContext(), null, createAbility()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("holder");
    }

    private InPlayPokemon createInPlayPokemon() {
        PokemonCard card = PokemonCard.builder()
                .id("reduce-1")
                .name("Reducer")
                .hp(100)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.FIRE)
                .costToAttack(List.of())
                .attacks(List.of())
                .abilities(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }

    private Ability createAbility() {
        return new Ability("Test", "Test ability", AbilityType.PASSIVE);
    }
}
