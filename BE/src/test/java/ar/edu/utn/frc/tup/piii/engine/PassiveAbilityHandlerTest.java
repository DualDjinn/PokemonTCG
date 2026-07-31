package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.AbilityEffect;
import ar.edu.utn.frc.tup.piii.models.AbilityType;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.effects.abilities.BoostDamageAbility;
import ar.edu.utn.frc.tup.piii.models.effects.abilities.ReduceDamageAbility;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PassiveAbilityHandler}.
 */
class PassiveAbilityHandlerTest {

    private final PassiveAbilityHandler handler = new PassiveAbilityHandler();

    @Test
    void applyOffensivePassives_BoostAbility_AddsToPendingDamage() {
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(30);
        InPlayPokemon attacker = pokemonWithPassive(new BoostDamageAbility(20));

        handler.applyOffensivePassives(ctx, attacker);

        assertThat(ctx.getPendingDamage()).isEqualTo(50);
    }

    @Test
    void applyDefensivePassives_ReduceAbility_SubtractsFromPendingDamage() {
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(60);
        InPlayPokemon defender = pokemonWithPassive(new ReduceDamageAbility(20));

        handler.applyDefensivePassives(ctx, defender);

        assertThat(ctx.getPendingDamage()).isEqualTo(40);
    }

    @Test
    void applyDefensivePassives_ClampsDamageToZero() {
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(10);
        InPlayPokemon defender = pokemonWithPassive(new ReduceDamageAbility(30));

        handler.applyDefensivePassives(ctx, defender);

        assertThat(ctx.getPendingDamage()).isZero();
    }

    @Test
    void applyOffensivePassives_AbilitiesDisabled_Skips() {
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(10);
        InPlayPokemon attacker = pokemonWithPassive(new BoostDamageAbility(50));
        attacker.setAbilitiesDisabled(true);

        handler.applyOffensivePassives(ctx, attacker);

        assertThat(ctx.getPendingDamage()).isEqualTo(10);
    }

    @Test
    void applyDefensivePassives_AbilitiesDisabled_Skips() {
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(60);
        InPlayPokemon defender = pokemonWithPassive(new ReduceDamageAbility(20));
        defender.setAbilitiesDisabled(true);

        handler.applyDefensivePassives(ctx, defender);

        assertThat(ctx.getPendingDamage()).isEqualTo(60);
    }

    @Test
    void applyPassives_ActivatedAbility_IsIgnored() {
        AbilityEffect never = (c, h, a) -> { throw new AssertionError("activated ability must not run"); };
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(10);
        InPlayPokemon pokemon = pokemonWith(Ability.builder()
                .name("Activated")
                .description("d")
                .type(AbilityType.ACTIVATED)
                .effect(never)
                .build());

        handler.applyOffensivePassives(ctx, pokemon);

        assertThat(ctx.getPendingDamage()).isEqualTo(10);
    }

    @Test
    void applyPassives_TriggeredAbility_IsIgnored() {
        AbilityEffect never = (c, h, a) -> { throw new AssertionError("triggered ability must not run"); };
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(10);
        InPlayPokemon pokemon = pokemonWith(Ability.builder()
                .name("Triggered")
                .description("d")
                .type(AbilityType.TRIGGERED)
                .effect(never)
                .build());

        handler.applyDefensivePassives(ctx, pokemon);

        assertThat(ctx.getPendingDamage()).isEqualTo(10);
    }

    @Test
    void applyPassives_NullEffect_SkipsAbility() {
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(10);
        InPlayPokemon pokemon = pokemonWith(Ability.builder()
                .name("NoEffect")
                .description("d")
                .type(AbilityType.PASSIVE)
                .effect(null)
                .build());

        handler.applyOffensivePassives(ctx, pokemon);

        assertThat(ctx.getPendingDamage()).isEqualTo(10);
    }

    @Test
    void applyPassives_NoAbilities_LeavesDamageUnchanged() {
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(40);
        InPlayPokemon pokemon = pokemonWith(new ArrayList<>());

        handler.applyOffensivePassives(ctx, pokemon);
        handler.applyDefensivePassives(ctx, pokemon);

        assertThat(ctx.getPendingDamage()).isEqualTo(40);
    }

    @Test
    void applyPassives_MultiplePassives_AppliesAll() {
        AtomicInteger calls = new AtomicInteger(0);
        AbilityEffect tracker = (c, h, a) -> calls.incrementAndGet();
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(0);
        InPlayPokemon pokemon = pokemonWith(
                Ability.builder().name("A").description("d").type(AbilityType.PASSIVE).effect(tracker).build(),
                Ability.builder().name("B").description("d").type(AbilityType.PASSIVE).effect(tracker).build(),
                Ability.builder().name("C").description("d").type(AbilityType.PASSIVE).effect(tracker).build()
        );

        handler.applyOffensivePassives(ctx, pokemon);

        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void applyOffensivePassives_NullCtx_Throws() {
        InPlayPokemon pokemon = pokemonWithPassive(new BoostDamageAbility(10));
        assertThatThrownBy(() -> handler.applyOffensivePassives(null, pokemon))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ctx");
    }

    @Test
    void applyOffensivePassives_NullAttacker_Throws() {
        assertThatThrownBy(() -> handler.applyOffensivePassives(new GameContext(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("attacker");
    }

    @Test
    void applyDefensivePassives_NullCtx_Throws() {
        InPlayPokemon pokemon = pokemonWithPassive(new ReduceDamageAbility(10));
        assertThatThrownBy(() -> handler.applyDefensivePassives(null, pokemon))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ctx");
    }

    @Test
    void applyDefensivePassives_NullDefender_Throws() {
        assertThatThrownBy(() -> handler.applyDefensivePassives(new GameContext(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("defender");
    }

    @Test
    void applyPassives_NullCardOnHolder_LeavesDamageUnchanged() {
        InPlayPokemon pokemon = new InPlayPokemon(null);
        GameContext ctx = new GameContext();
        ctx.setPendingDamage(25);

        handler.applyOffensivePassives(ctx, pokemon);
        handler.applyDefensivePassives(ctx, pokemon);

        assertThat(ctx.getPendingDamage()).isEqualTo(25);
    }

    private InPlayPokemon pokemonWithPassive(AbilityEffect effect) {
        return pokemonWith(Ability.builder()
                .name("Test")
                .description("d")
                .type(AbilityType.PASSIVE)
                .effect(effect)
                .build());
    }

    private InPlayPokemon pokemonWith(Ability... abilities) {
        return pokemonWith(List.of(abilities));
    }

    private InPlayPokemon pokemonWith(List<Ability> abilities) {
        PokemonCard card = PokemonCard.builder()
                .id("card-with-abilities")
                .name("Holder")
                .hp(100)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.FIRE)
                .costToAttack(List.of())
                .attacks(List.of())
                .abilities(abilities)
                .weaknesses(List.of())
                .resistances(List.of())
                .retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }
}
