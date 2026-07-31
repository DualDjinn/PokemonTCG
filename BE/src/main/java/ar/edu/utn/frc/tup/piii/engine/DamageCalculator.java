package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.Resistance;
import ar.edu.utn.frc.tup.piii.models.Weakness;
import java.util.List;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * Calculates final attack damage according to the XY1 rulebook.
 *
 * <p>This class has a single responsibility: transform the incoming base damage stored in
 * {@link GameContext#getPendingDamage()} into the final damage that must be applied to the
 * defending Pokémon. The calculation is deterministic, framework-free and split into explicit
 * stages so it remains easy to test, reason about and extend.
 *
 * <p>The resolution order is fixed:
 * <ol>
 *     <li>Start from the base damage.</li>
 *     <li>Apply Weakness ({@code x2}) if the defender is weak to the attacker's type.</li>
 *     <li>Apply Resistance ({@code -20}, minimum {@code 0}) if the defender resists the
 *     attacker's type.</li>
 *     <li>Apply additional modifiers registered in the {@link GameContext} in registration
 *     order.</li>
 * </ol>
 *
 * <p>The calculator never mutates the battle state. It only returns the resulting damage value.
 */
public final class DamageCalculator {

    private static final int MINIMUM_DAMAGE = 0;
    private static final int XY1_WEAKNESS_MULTIPLIER = 2;
    private static final int XY1_RESISTANCE_REDUCTION = 20;

    /**
     * Calculates the final damage for the current attack resolution.
     *
     * <p>The attacker's primary Pokémon type is used to evaluate the defender's Weaknesses
     * and Resistances. The input base damage is read from
     * {@link GameContext#getPendingDamage()}. Additional modifiers are then applied in
     * registration order after the mandatory XY1 Weakness/Resistance steps.
     *
     * @param attacker the attacking Pokémon; must not be {@code null}
     * @param defender the defending Pokémon; must not be {@code null}
     * @param context the battle context containing the incoming damage and extra modifiers;
     *                must not be {@code null}
     * @return the non-negative final damage after applying all rulebook stages
     * @throws NullPointerException if any required argument is {@code null}
     */
    public int calculateDamage(InPlayPokemon attacker, InPlayPokemon defender, GameContext context) {
        Objects.requireNonNull(attacker, "attacker must not be null");
        Objects.requireNonNull(defender, "defender must not be null");
        Objects.requireNonNull(context, "context must not be null");

        // XY1 requires a fixed sequence: base damage -> weakness -> resistance -> extra modifiers.
        int damage = resolveBaseDamage(context);
        damage = applyWeaknessStage(attacker, defender, context, damage);
        damage = applyResistanceStage(attacker, defender, context, damage);
        return applyAdditionalModifierStage(context, damage);
    }

    /**
     * Reads the incoming base damage from the context and normalizes it to a non-negative value.
     *
     * @param context current attack-resolution context
     * @return base damage clamped to {@code 0} or more
     */
    private int resolveBaseDamage(GameContext context) {
        return clampToMinimumDamage(context.getPendingDamage());
    }

    /**
     * Applies the XY1 Weakness step for the defender against the attacker's type.
     *
     * @param attacker attacking Pokémon
     * @param defender defending Pokémon
     * @param context current attack-resolution context
     * @param damage current damage before Weakness
     * @return damage after applying Weakness if it matches and is not ignored
     */
    private int applyWeaknessStage(InPlayPokemon attacker,
                                   InPlayPokemon defender,
                                   GameContext context,
                                   int damage) {
        if (context.isIgnoreWeakness() || !hasMatchingWeakness(attacker, defender)) {
            return damage;
        }
        // Shadow Circle (xy1-126): any Pokémon with Darkness Energy attached has no Weakness.
        if (isShadowCircleActive(context) && defender.countEnergies(EnergyType.DARKNESS) > 0) {
            return damage;
        }
        return damage * XY1_WEAKNESS_MULTIPLIER;
    }

    private static boolean isShadowCircleActive(GameContext context) {
        return context.getActiveStadium() != null
                && "xy1-126".equals(context.getActiveStadium().getId());
    }

    /**
     * Applies the XY1 Resistance step for the defender against the attacker's type.
     *
     * @param attacker attacking Pokémon
     * @param defender defending Pokémon
     * @param context current attack-resolution context
     * @param damage current damage before Resistance
     * @return damage after subtracting the fixed XY1 resistance reduction, never below {@code 0}
     */
    private int applyResistanceStage(InPlayPokemon attacker,
                                     InPlayPokemon defender,
                                     GameContext context,
                                     int damage) {
        if (context.isIgnoreResistance() || !hasMatchingResistance(attacker, defender)) {
            return damage;
        }
        return clampToMinimumDamage(damage - XY1_RESISTANCE_REDUCTION);
    }

    /**
     * Applies all extra deterministic damage modifiers registered in the context.
     *
     * @param context current attack-resolution context
     * @param damage damage after Weakness and Resistance
     * @return damage after applying all registered modifiers in order
     */
    private int applyAdditionalModifierStage(GameContext context, int damage) {
        int modifiedDamage = damage;
        for (IntUnaryOperator modifier : context.getDamageCalculationModifiers()) {
            modifiedDamage = applySingleModifier(modifier, modifiedDamage);
        }
        return modifiedDamage;
    }

    /**
     * Applies a single registered modifier and clamps the result to a non-negative value.
     *
     * @param modifier modifier to execute
     * @param currentDamage damage entering the modifier
     * @return modifier result clamped to {@code 0} or more
     */
    private int applySingleModifier(IntUnaryOperator modifier, int currentDamage) {
        Objects.requireNonNull(modifier, "damageCalculationModifier must not be null");
        return clampToMinimumDamage(modifier.applyAsInt(currentDamage));
    }

    /**
     * Determines whether the defender has a Weakness matching the attacker's primary type.
     *
     * @param attacker attacking Pokémon
     * @param defender defending Pokémon
     * @return {@code true} when XY1 Weakness applies
     */
    private boolean hasMatchingWeakness(InPlayPokemon attacker, InPlayPokemon defender) {
        return containsWeaknessType(attacker, defender.getCard().getWeaknesses());
    }

    /**
     * Determines whether the defender has a Resistance matching the attacker's primary type.
     *
     * @param attacker attacking Pokémon
     * @param defender defending Pokémon
     * @return {@code true} when XY1 Resistance applies
     */
    private boolean hasMatchingResistance(InPlayPokemon attacker, InPlayPokemon defender) {
        return containsResistanceType(attacker, defender.getCard().getResistances());
    }

    /**
     * Searches the defender's Weakness list for one matching the attacker's type.
     *
     * @param attacker attacking Pokémon
     * @param weaknesses defender weakness list, possibly {@code null}
     * @return {@code true} if a matching Weakness exists
     */
    private boolean containsWeaknessType(InPlayPokemon attacker, List<Weakness> weaknesses) {
        if (weaknesses == null) {
            return false;
        }
        for (Weakness weakness : weaknesses) {
            if (weakness != null && weakness.getType() == attacker.getCard().getPokemonType()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Searches the defender's Resistance list for one matching the attacker's type.
     *
     * @param attacker attacking Pokémon
     * @param resistances defender resistance list, possibly {@code null}
     * @return {@code true} if a matching Resistance exists
     */
    private boolean containsResistanceType(InPlayPokemon attacker, List<Resistance> resistances) {
        if (resistances == null) {
            return false;
        }
        for (Resistance resistance : resistances) {
            if (resistance != null && resistance.getType() == attacker.getCard().getPokemonType()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ensures the calculated damage never drops below the minimum legal value.
     *
     * @param damage raw calculated damage
     * @return {@code damage} clamped to at least {@code 0}
     */
    private int clampToMinimumDamage(int damage) {
        return Math.max(MINIMUM_DAMAGE, damage);
    }
}
