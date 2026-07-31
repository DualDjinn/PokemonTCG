package ar.edu.utn.frc.tup.piii.models.effects.dsl;

import ar.edu.utn.frc.tup.piii.models.EnergyType;

/**
 * Factory of reusable {@link CountSource}s — the variable inputs to scaling damage.
 *
 * <p>Each method names a quantity drawn from the live battle state (coin heads, bench
 * size, attached energy, damage counters) so that one {@link Effects#addDamagePer} or
 * {@link Effects#multiplyDamageBy} call expresses what used to be a bespoke effect class.
 */
public final class Counts {

    private Counts() {
        // Static factory; not instantiable.
    }

    /**
     * Number of heads from flipping {@code coins} coins.
     *
     * @param coins number of coins to flip
     * @return a source yielding the heads count
     */
    public static CountSource coinHeads(int coins) {
        return ctx -> ctx.flipCoins(coins);
    }

    /**
     * Number of heads obtained while flipping until the first tails.
     *
     * @return a source yielding the heads-before-tails count
     */
    public static CountSource coinHeadsUntilTails() {
        return ctx -> ctx.flipUntilTails();
    }

    /**
     * Number of Pokémon on the attacker's bench.
     *
     * @return a source yielding the attacker's bench size
     */
    public static CountSource attackerBenchCount() {
        return ctx -> (int) ctx.getAttackerBench().stream().filter(java.util.Objects::nonNull).count();
    }

    /**
     * Number of Pokémon on the defender's bench.
     *
     * @return a source yielding the defender's bench size
     */
    public static CountSource defenderBenchCount() {
        return ctx -> (int) ctx.getDefenderBench().stream().filter(java.util.Objects::nonNull).count();
    }

    /**
     * Energies of {@code type} attached to the attacker (all energies when {@code null}).
     *
     * @param type energy type to count, or {@code null} for all
     * @return a source yielding the attached-energy count
     */
    public static CountSource attackerEnergy(EnergyType type) {
        return ctx -> ctx.getAttacker().countEnergies(type);
    }

    /**
     * Damage counters on the defender's active Pokémon.
     *
     * @return a source yielding the defender's damage-counter count
     */
    public static CountSource defenderDamageCounters() {
        return ctx -> ctx.defenderDamageCounters();
    }

    /**
     * Damage counters on the attacker's active Pokémon.
     *
     * @return a source yielding the attacker's damage-counter count
     */
    public static CountSource attackerDamageCounters() {
        return ctx -> ctx.attackerDamageCounters();
    }

    /**
     * Number of distinct (non-Colorless) basic Energy types attached to the attacker.
     *
     * @return a source yielding the count of distinct attached energy types
     */
    public static CountSource attackerDistinctEnergyTypes() {
        return ctx -> (int) ctx.getAttacker().countDistinctEnergyTypes();
    }

    /**
     * Total energies attached across both Active Pokémon (attacker + defender).
     *
     * @return a source yielding the sum of attached energies on both Actives
     */
    public static CountSource bothActiveEnergyTotal() {
        return ctx -> ctx.getAttacker().countEnergies(null)
                + ctx.getDefender().countEnergies(null);
    }

    /**
     * Flips one coin per unit of {@code coinsSource} and yields the heads count.
     *
     * <p>Lets a recipe express "flip N coins where N depends on board state" — e.g.
     * Rock Black (one coin per attached Fighting Energy) or Seething Anger (one coin
     * per damage counter on self).
     *
     * @param coinsSource quantity supplying the number of coins to flip
     * @return a source yielding the heads count of that flip batch
     */
    public static CountSource coinHeadsOf(CountSource coinsSource) {
        return ctx -> ctx.flipCoins(coinsSource.count(ctx));
    }
}
