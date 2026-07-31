package ar.edu.utn.frc.tup.piii.engine.attackresolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;

/**
 * Isolated tests for {@link ApplyAttackEffectHandler} covering the defensive branches that the
 * full-pipeline tests cannot reach (attacker card with a {@code null} attack list, and the
 * defender preventing all effects).
 */
class ApplyAttackEffectHandlerTest {

    private final ApplyAttackEffectHandler handler =
            new ApplyAttackEffectHandler(new CardEffectRegistry(), null);

    private InPlayPokemon pokemon(String id, List<Attack> attacks) {
        PokemonCard card = PokemonCard.builder()
                .id(id).name(id).hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE)
                .attacks(attacks).abilities(List.of())
                .weaknesses(List.of()).resistances(List.of())
                .costToAttack(List.of()).retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }

    private Attack attack() {
        return Attack.builder().name("Tackle").energyCost(Map.of()).baseDamage(10).effect(null).build();
    }

    @Test
    void handle_attackerCardHasNullAttackList_resolvesNoEffect() {
        InPlayPokemon attacker = pokemon("card-null-attacks", null);
        InPlayPokemon defender = pokemon("defender", List.of());
        GameContext gc = new GameContext();
        gc.setAttacker(attacker);
        gc.setDefender(defender);
        AttackResolutionContext context = new AttackResolutionContext(gc, attack(), attacker, defender);

        assertThatCode(() -> handler.handle(context)).doesNotThrowAnyException();
        // The handler resolves no effect and leaves the damage fields untouched.
        assertThat(context.getFinalDamage()).isZero();
    }

    @Test
    void handle_defenderPreventsAllEffects_returnsEarly() {
        InPlayPokemon attacker = pokemon("attacker", List.of(attack()));
        InPlayPokemon defender = pokemon("defender", List.of());
        defender.setPreventAllEffects(true);
        GameContext gc = new GameContext();
        gc.setAttacker(attacker);
        gc.setDefender(defender);
        AttackResolutionContext context = new AttackResolutionContext(gc, attack(), attacker, defender);

        assertThatCode(() -> handler.handle(context)).doesNotThrowAnyException();
    }
}
