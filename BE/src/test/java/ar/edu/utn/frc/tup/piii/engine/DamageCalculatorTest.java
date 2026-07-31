package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.Resistance;
import ar.edu.utn.frc.tup.piii.models.StadiumCard;
import ar.edu.utn.frc.tup.piii.models.Weakness;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DamageCalculator}.
 */
class DamageCalculatorTest {

    private final DamageCalculator damageCalculator = new DamageCalculator();

    @Test
    void shouldReturnZeroWhenBaseDamageIsZero() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(new Weakness(EnergyType.FIRE, "x2")),
                List.of());
        GameContext context = createContext(attacker, defender, 0);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isZero();
    }

    @Test
    void shouldClampResistanceResultToZeroWhenReductionWouldMakeDamageNegative() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(),
                List.of(new Resistance(EnergyType.FIRE, 20)));
        GameContext context = createContext(attacker, defender, 10);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isZero();
    }

    @Test
    void shouldUseFixedXy1ResistanceReductionEvenWhenStoredResistanceValueIsNegative() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(),
                List.of(new Resistance(EnergyType.FIRE, -20)));
        GameContext context = createContext(attacker, defender, 50);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(30);
    }

    @Test
    void shouldApplyWeaknessBeforeAdditionalContextModifier() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(new Weakness(EnergyType.FIRE, "x2")),
                List.of());
        GameContext context = createContext(attacker, defender, 30);
        context.addDamageCalculationModifier(currentDamage -> currentDamage + 10);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(70);
    }

    @Test
    void shouldApplyMultipleAdditionalModifiersInRegistrationOrder() {
        InPlayPokemon attacker = createPokemon(EnergyType.WATER, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.FIRE, List.of(), List.of());
        GameContext context = createContext(attacker, defender, 40);
        context.addDamageCalculationModifier(currentDamage -> currentDamage + 10);
        context.addDamageCalculationModifier(currentDamage -> currentDamage * 2);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(100);
    }

    @Test
    void shouldApplyWeaknessAndResistanceInRulebookOrder() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(new Weakness(EnergyType.FIRE, "x2")),
                List.of(new Resistance(EnergyType.FIRE, 20)));
        GameContext context = createContext(attacker, defender, 30);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(40);
    }

    @Test
    void shouldKeepBaseDamageWhenDefenderHasNoWeaknessOrResistanceAgainstAttacker() {
        InPlayPokemon attacker = createPokemon(EnergyType.PSYCHIC, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.FIGHTING,
                List.of(new Weakness(EnergyType.GRASS, "x2")),
                List.of(new Resistance(EnergyType.METAL, 20)));
        GameContext context = createContext(attacker, defender, 50);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(50);
    }

    @Test
    void shouldIgnoreWeaknessAndResistanceWhenContextFlagsDisableThem() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(new Weakness(EnergyType.FIRE, "x2")),
                List.of(new Resistance(EnergyType.FIRE, 20)));
        GameContext context = createContext(attacker, defender, 40);
        context.setIgnoreWeakness(true);
        context.setIgnoreResistance(true);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(40);
    }

    @Test
    void shouldClampAdditionalModifierResultsToZero() {
        InPlayPokemon attacker = createPokemon(EnergyType.WATER, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.FIRE, List.of(), List.of());
        GameContext context = createContext(attacker, defender, 15);
        context.addDamageCalculationModifier(currentDamage -> currentDamage - 30);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isZero();
    }

    @Test
    void shouldApplyWeaknessAndThenAllContextModifiersInOrder() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(new Weakness(EnergyType.FIRE, "x2")),
                List.of());
        GameContext context = createContext(attacker, defender, 20);
        context.addDamageCalculationModifier(currentDamage -> currentDamage + 10);
        context.addDamageCalculationModifier(currentDamage -> currentDamage + 5);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(55);
    }

    @Test
    void shouldKeepBaseDamageWhenDefenderHasNoWeaknessesOrResistances() {
        InPlayPokemon attacker = createPokemon(EnergyType.WATER, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.FIRE, null, null);
        GameContext context = createContext(attacker, defender, 60);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(60);
    }

    @Test
    void shouldClampNegativeBaseDamageToZeroBeforeApplyingRulebookStages() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(new Weakness(EnergyType.FIRE, "x2")),
                List.of(new Resistance(EnergyType.FIRE, 20)));
        GameContext context = createContext(attacker, defender, -10);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isZero();
    }

    @Test
    void shouldThrowWhenAttackerIsNull() {
        InPlayPokemon defender = createPokemon(EnergyType.GRASS, List.of(), List.of());
        GameContext context = createContext(null, defender, 20);

        assertThatThrownBy(() -> damageCalculator.calculateDamage(null, defender, context))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("attacker must not be null");
    }

    @Test
    void shouldThrowWhenDefenderIsNull() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        GameContext context = createContext(attacker, null, 20);

        assertThatThrownBy(() -> damageCalculator.calculateDamage(attacker, null, context))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("defender must not be null");
    }

    @Test
    void shouldThrowWhenContextIsNull() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS, List.of(), List.of());

        assertThatThrownBy(() -> damageCalculator.calculateDamage(attacker, defender, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("context must not be null");
    }

    @Test
    void shouldDoubleDamageWhenOnlyWeaknessApplies_NoResistanceOrModifiers() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(new Weakness(EnergyType.FIRE, "x2")),
                List.of());
        GameContext context = createContext(attacker, defender, 50);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(100);
    }

    @Test
    void shouldSubtractResistanceWhenOnlyResistanceApplies_NoWeaknessOrModifiers() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(),
                List.of(new Resistance(EnergyType.FIRE, 20)));
        GameContext context = createContext(attacker, defender, 50);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(30);
    }

    @Test
    void shouldSkipWeaknessWhenShadowCircleIsActiveAndDefenderHasDarknessEnergy() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(new Weakness(EnergyType.FIRE, "x2")),
                List.of());
        defender.getEnergies().add(EnergyCard.builder()
                .id("energy-1")
                .energyType(EnergyType.DARKNESS)
                .build());
        GameContext context = createContext(attacker, defender, 50);
        context.setActiveStadium(StadiumCard.builder()
                .id("xy1-126")
                .name("Shadow Circle")
                .build());

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(50);
    }

    @Test
    void shouldApplyWeaknessWhenShadowCircleIsActiveButDefenderHasNoDarknessEnergy() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(new Weakness(EnergyType.FIRE, "x2")),
                List.of());
        GameContext context = createContext(attacker, defender, 50);
        context.setActiveStadium(StadiumCard.builder()
                .id("xy1-126")
                .name("Shadow Circle")
                .build());

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(100);
    }

    @Test
    void shouldApplyWeaknessWhenShadowCircleIsNotActiveAndDefenderHasDarknessEnergy() {
        InPlayPokemon attacker = createPokemon(EnergyType.FIRE, List.of(), List.of());
        InPlayPokemon defender = createPokemon(EnergyType.GRASS,
                List.of(new Weakness(EnergyType.FIRE, "x2")),
                List.of());
        defender.getEnergies().add(EnergyCard.builder()
                .id("energy-1")
                .energyType(EnergyType.DARKNESS)
                .build());
        GameContext context = createContext(attacker, defender, 50);

        int finalDamage = damageCalculator.calculateDamage(attacker, defender, context);

        assertThat(finalDamage).isEqualTo(100);
    }

    private GameContext createContext(InPlayPokemon attacker, InPlayPokemon defender, int pendingDamage) {
        GameContext context = new GameContext();
        context.setAttacker(attacker);
        context.setDefender(defender);
        context.setPendingDamage(pendingDamage);
        return context;
    }

    private InPlayPokemon createPokemon(EnergyType type,
                                        List<Weakness> weaknesses,
                                        List<Resistance> resistances) {
        int weaknessCount = weaknesses == null ? 0 : weaknesses.size();
        int resistanceCount = resistances == null ? 0 : resistances.size();
        PokemonCard card = PokemonCard.builder()
                .id("card-" + type + "-" + weaknessCount + "-" + resistanceCount)
                .name("Pokemon-" + type)
                .hp(100)
                .stage(PokemonStage.BASIC)
                .pokemonType(type)
                .costToAttack(List.of())
                .attacks(List.of())
                .abilities(List.of())
                .weaknesses(weaknesses)
                .resistances(resistances)
                .retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }
}
