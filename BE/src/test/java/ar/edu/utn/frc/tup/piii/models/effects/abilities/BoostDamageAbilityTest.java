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
 * Unit tests for {@link BoostDamageAbility}.
 */
class BoostDamageAbilityTest {

    @Test
    void apply_AddsConfiguredAmountToPendingDamage() {
        BoostDamageAbility ability = new BoostDamageAbility(20);
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(30);

        ability.apply(ctx, createInPlayPokemon(), createAbility());

        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    @Test
    void apply_FromZero_AddsConfiguredAmount() {
        BoostDamageAbility ability = new BoostDamageAbility(10);
        GameContext ctx = new GameContext();

        ability.apply(ctx, createInPlayPokemon(), createAbility());

        assertThat(ctx.getPendingDamage()).isEqualTo(10);
    }

    @Test
    void getAmount_ReturnsConfiguredValue() {
        assertThat(new BoostDamageAbility(15).getAmount()).isEqualTo(15);
        assertThat(new BoostDamageAbility(0).getAmount()).isZero();
    }

    @Test
    void apply_NullHolder_Throws() {
        BoostDamageAbility ability = new BoostDamageAbility(10);
        assertThatThrownBy(() -> ability.apply(new GameContext(), null, createAbility()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("holder");
    }

    private InPlayPokemon createInPlayPokemon() {
        PokemonCard card = PokemonCard.builder()
                .id("boost-1")
                .name("Booster")
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
