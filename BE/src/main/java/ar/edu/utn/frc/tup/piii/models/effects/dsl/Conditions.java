package ar.edu.utn.frc.tup.piii.models.effects.dsl;

import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;

/**
 * Factory of reusable {@link Condition}s used to gate effects via {@link Effects#when(Condition)}.
 */
public final class Conditions {

    private Conditions() {
        // Static factory; not instantiable.
    }

    /**
     * True when the defender currently has the given status condition.
     *
     * @param condition the status to test for
     * @return a condition over the defender's statuses
     */
    public static Condition defenderHasStatus(StatusCondition condition) {
        return ctx -> ctx.getDefender().hasStatus(condition);
    }

    /**
     * True when the attacker has taken any damage.
     *
     * @return a condition that holds while the attacker is damaged
     */
    public static Condition attackerDamaged() {
        return ctx -> ctx.getAttacker().getDamageTaken() > 0;
    }

    /**
     * True when the defender has taken any damage.
     *
     * @return a condition that holds while the defender is damaged
     */
    public static Condition defenderDamaged() {
        return ctx -> ctx.getDefender().getDamageTaken() > 0;
    }

    /**
     * True when the defender's card is a Pokémon-EX (or M-EX).
     *
     * @return a condition over the defender's EX status
     */
    public static Condition defenderIsEx() {
        return ctx -> {
            var card = ctx.getDefender().getCard();
            return card.isEx() || card.isMega();
        };
    }

    /**
     * True when the defender's Pokémon type matches {@code type}.
     *
     * @param type the energy type to match against the defender's Pokémon type
     * @return a condition over the defender's type
     */
    public static Condition defenderType(EnergyType type) {
        return ctx -> ctx.getDefender().getCard().getPokemonType() == type;
    }

    /**
     * True when the defender currently has any Special Condition applied.
     *
     * @return a condition that holds while the defender has at least one status
     */
    public static Condition defenderHasAnyStatus() {
        return ctx -> !ctx.getDefender().getStatuses().isEmpty();
    }

    /**
     * True when the attacker has at least one Energy of the given type attached.
     *
     * @param type the energy type to require
     * @return a condition that holds while the attacker has any of {@code type}
     */
    public static Condition attackerHasEnergyOf(EnergyType type) {
        return ctx -> ctx.getAttacker().countEnergies(type) > 0;
    }

    /**
     * True when any Pokémon on the attacker's bench is the card identified by {@code cardId}.
     *
     * @param cardId card identifier (e.g., {@code "xy1-25"})
     * @return a condition that holds when a bench Pokémon has that card id
     */
    public static Condition attackerBenchHasCardId(String cardId) {
        return ctx -> ctx.getAttackerBench().stream()
                .filter(java.util.Objects::nonNull)
                .anyMatch(p -> cardId.equals(p.getCard().getId()));
    }
}
