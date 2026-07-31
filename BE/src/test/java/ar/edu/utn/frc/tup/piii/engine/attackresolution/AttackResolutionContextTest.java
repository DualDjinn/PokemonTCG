package ar.edu.utn.frc.tup.piii.engine.attackresolution;

import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttackResolutionContextTest {

    private final GameContext gameContext = new GameContext();
    private final Attack attack = Attack.builder()
            .name("Tackle").energyCost(Map.of()).baseDamage(40).build();
    private final InPlayPokemon attacker = pokemon("attacker");
    private final InPlayPokemon defender = pokemon("defender");

    @Test
    void constructor_SetsAllFields() {
        AttackResolutionContext ctx = new AttackResolutionContext(
                gameContext, attack, attacker, defender);

        assertThat(ctx.getGameContext()).isSameAs(gameContext);
        assertThat(ctx.getAttack()).isSameAs(attack);
        assertThat(ctx.getAttacker()).isSameAs(attacker);
        assertThat(ctx.getDefender()).isSameAs(defender);
    }

    @Test
    void constructor_NullGameContext_Throws() {
        assertThatThrownBy(() -> new AttackResolutionContext(null, attack, attacker, defender))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_NullAttack_Throws() {
        assertThatThrownBy(() -> new AttackResolutionContext(gameContext, null, attacker, defender))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_NullAttacker_Throws() {
        assertThatThrownBy(() -> new AttackResolutionContext(gameContext, attack, null, defender))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_NullDefender_Throws() {
        assertThatThrownBy(() -> new AttackResolutionContext(gameContext, attack, attacker, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void setCalculatedDamage_getCalculatedDamage_Work() {
        AttackResolutionContext ctx = new AttackResolutionContext(
                gameContext, attack, attacker, defender);
        ctx.setCalculatedDamage(75);
        assertThat(ctx.getCalculatedDamage()).isEqualTo(75);
    }

    @Test
    void setFinalDamage_getFinalDamage_Work() {
        AttackResolutionContext ctx = new AttackResolutionContext(
                gameContext, attack, attacker, defender);
        ctx.setFinalDamage(120);
        assertThat(ctx.getFinalDamage()).isEqualTo(120);
    }

    @Test
    void finalDamage_DefaultsToZero() {
        AttackResolutionContext ctx = new AttackResolutionContext(
                gameContext, attack, attacker, defender);
        assertThat(ctx.getFinalDamage()).isZero();
    }

    @Test
    void calculatedDamage_DefaultsToZero() {
        AttackResolutionContext ctx = new AttackResolutionContext(
                gameContext, attack, attacker, defender);
        assertThat(ctx.getCalculatedDamage()).isZero();
    }

    private static InPlayPokemon pokemon(String id) {
        PokemonCard card = PokemonCard.builder()
                .id(id).name(id).hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE)
                .attacks(List.of()).abilities(List.of())
                .weaknesses(List.of()).resistances(List.of())
                .costToAttack(List.of()).retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }
}
