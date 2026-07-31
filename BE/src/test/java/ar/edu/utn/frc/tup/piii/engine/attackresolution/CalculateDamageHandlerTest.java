package ar.edu.utn.frc.tup.piii.engine.attackresolution;

import ar.edu.utn.frc.tup.piii.engine.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.PassiveAbilityHandler;
import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.AbilityType;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.Weakness;
import ar.edu.utn.frc.tup.piii.models.effects.abilities.BoostDamageAbility;
import ar.edu.utn.frc.tup.piii.models.effects.abilities.ReduceDamageAbility;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CalculateDamageHandler} focusing on the integration
 * with {@link PassiveAbilityHandler}.
 */
class CalculateDamageHandlerTest {

    private final DamageCalculator damageCalculator = new DamageCalculator();
    private final PassiveAbilityHandler passiveAbilityHandler = new PassiveAbilityHandler();
    private final CalculateDamageHandler handler = new CalculateDamageHandler(
            damageCalculator, passiveAbilityHandler, null);

    @Test
    void handle_BoostPassiveOnAttacker_AddsBonusDamage() {
        InPlayPokemon attacker = pokemon(EnergyType.FIRE,
                List.of(passive(new BoostDamageAbility(20))));
        InPlayPokemon defender = pokemon(EnergyType.GRASS, List.of());
        Attack attack = attack(50);
        GameContext ctx = baseContext(attacker, defender);

        AttackResolutionContext result = invoke(attack, ctx);

        assertThat(result.getFinalDamage()).isEqualTo(70);
    }

    @Test
    void handle_ReducePassiveOnDefender_SubtractsDamageAfterWeakness() {
        InPlayPokemon attacker = pokemon(EnergyType.FIRE, List.of());
        InPlayPokemon defender = pokemon(EnergyType.GRASS,
                List.of(passive(new ReduceDamageAbility(15))));
        Attack attack = attack(40);
        GameContext ctx = baseContext(attacker, defender);

        AttackResolutionContext result = invoke(attack, ctx);

        assertThat(result.getFinalDamage()).isEqualTo(25);
    }

    @Test
    void handle_BoostAndReduce_AppliesBoth() {
        InPlayPokemon attacker = pokemon(EnergyType.FIRE,
                List.of(passive(new BoostDamageAbility(10))));
        InPlayPokemon defender = pokemon(EnergyType.GRASS,
                List.of(passive(new ReduceDamageAbility(20))));
        Attack attack = attack(60);
        GameContext ctx = baseContext(attacker, defender);

        AttackResolutionContext result = invoke(attack, ctx);

        assertThat(result.getFinalDamage()).isEqualTo(50);
    }

    @Test
    void handle_DefenderReduceLargerThanDamage_ClampsToZero() {
        InPlayPokemon attacker = pokemon(EnergyType.FIRE, List.of());
        InPlayPokemon defender = pokemon(EnergyType.GRASS,
                List.of(passive(new ReduceDamageAbility(80))));
        Attack attack = attack(30);
        GameContext ctx = baseContext(attacker, defender);

        AttackResolutionContext result = invoke(attack, ctx);

        assertThat(result.getFinalDamage()).isZero();
    }

    @Test
    void handle_AbilitiesDisabledOnAttacker_IgnoresBoost() {
        InPlayPokemon attacker = pokemon(EnergyType.FIRE,
                List.of(passive(new BoostDamageAbility(30))));
        attacker.setAbilitiesDisabled(true);
        InPlayPokemon defender = pokemon(EnergyType.GRASS, List.of());
        Attack attack = attack(20);
        GameContext ctx = baseContext(attacker, defender);

        AttackResolutionContext result = invoke(attack, ctx);

        assertThat(result.getFinalDamage()).isEqualTo(20);
    }

    @Test
    void handle_AbilitiesDisabledOnDefender_IgnoresReduce() {
        InPlayPokemon attacker = pokemon(EnergyType.FIRE, List.of());
        InPlayPokemon defender = pokemon(EnergyType.GRASS,
                List.of(passive(new ReduceDamageAbility(20))));
        defender.setAbilitiesDisabled(true);
        Attack attack = attack(40);
        GameContext ctx = baseContext(attacker, defender);

        AttackResolutionContext result = invoke(attack, ctx);

        assertThat(result.getFinalDamage()).isEqualTo(40);
    }

    @Test
    void handle_BoostInteractsWithWeakness() {
        InPlayPokemon attacker = pokemonWithPassive(EnergyType.FIRE, new BoostDamageAbility(20));
        InPlayPokemon defender = pokemonWithWeakness(EnergyType.GRASS,
                new Weakness(EnergyType.FIRE, "x2"));
        Attack attack = attack(30);
        GameContext ctx = baseContext(attacker, defender);

        AttackResolutionContext result = invoke(attack, ctx);

        // 30 + 20 boost = 50; 50 * 2 weakness = 100
        assertThat(result.getFinalDamage()).isEqualTo(100);
    }

    @Test
    void handle_NoPassives_BehaviourUnchanged() {
        InPlayPokemon attacker = pokemon(EnergyType.FIRE, List.of());
        InPlayPokemon defender = pokemon(EnergyType.GRASS, List.of());
        Attack attack = attack(40);
        GameContext ctx = baseContext(attacker, defender);

        AttackResolutionContext result = invoke(attack, ctx);

        assertThat(result.getFinalDamage()).isEqualTo(40);
    }

    private AttackResolutionContext invoke(Attack attack, GameContext ctx) {
        InPlayPokemon attacker = ctx.getAttacker();
        InPlayPokemon defender = ctx.getDefender();
        AttackResolutionContext resolution = new AttackResolutionContext(ctx, attack, attacker, defender);
        // PrepareAttackStateHandler in the real pipeline seeds pendingDamage
        // with the attack's baseDamage. The handler under test is intentionally
        // a smaller chain, so we replicate that one line here.
        ctx.setPendingDamage(attack.getBaseDamage());
        handler.handle(resolution);
        return resolution;
    }

    private GameContext baseContext(InPlayPokemon attacker, InPlayPokemon defender) {
        GameContext ctx = new GameContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);
        return ctx;
    }

    private Attack attack(int baseDamage) {
        return Attack.builder()
                .name("Test Attack")
                .energyCost(java.util.Map.of())
                .baseDamage(baseDamage)
                .damageModifier(ar.edu.utn.frc.tup.piii.models.DamageModifier.NONE)
                .build();
    }

    private InPlayPokemon pokemon(EnergyType type, List<Ability> abilities) {
        return pokemonBuilder(type, abilities, List.of(), List.of());
    }

    private InPlayPokemon pokemonWithPassive(EnergyType type, ar.edu.utn.frc.tup.piii.models.AbilityEffect effect) {
        return pokemonBuilder(type, List.of(passive(effect)), List.of(), List.of());
    }

    private InPlayPokemon pokemonWithWeakness(EnergyType type, Weakness weakness) {
        return pokemonBuilder(type, List.of(), List.of(weakness), List.of());
    }

    private InPlayPokemon pokemonBuilder(EnergyType type,
                                         List<Ability> abilities,
                                         List<Weakness> weaknesses,
                                         List<ar.edu.utn.frc.tup.piii.models.Resistance> resistances) {
        PokemonCard card = PokemonCard.builder()
                .id("test-" + type)
                .name("Pkmn-" + type)
                .hp(100)
                .stage(PokemonStage.BASIC)
                .pokemonType(type)
                .costToAttack(List.of())
                .attacks(List.of())
                .abilities(abilities)
                .weaknesses(weaknesses)
                .resistances(resistances)
                .retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }

    private Ability passive(ar.edu.utn.frc.tup.piii.models.AbilityEffect effect) {
        return Ability.builder()
                .name("Test")
                .description("Test passive")
                .type(AbilityType.PASSIVE)
                .effect(effect)
                .build();
    }
}
