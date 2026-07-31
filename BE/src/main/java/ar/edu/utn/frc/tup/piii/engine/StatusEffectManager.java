package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import java.util.List;
import java.util.Objects;

/**
 * Resolves end-of-turn special conditions following the XY1 rulebook order.
 *
 * <p>This class has a single responsibility: process status effects already present on an
 * {@link InPlayPokemon} at the end of the turn. The resolution is deterministic except for the
 * coin flips delegated to {@link GameContext#flipCoin()}.
 *
 * <p>The resolution order is fixed:
 * <ol>
 *     <li>{@link StatusCondition#POISONED}</li>
 *     <li>{@link StatusCondition#BURNED}</li>
 *     <li>{@link StatusCondition#ASLEEP}</li>
 *     <li>{@link StatusCondition#PARALYZED}</li>
 * </ol>
 *
 * <p>If the Pokémon is knocked out while resolving a status, the remaining statuses are not
 * processed because the affected Pokémon would immediately leave play.
 */
public final class StatusEffectManager {

    private static final int POISON_DAMAGE = 10;
    private static final int BURN_DAMAGE = 20;
    private static final String CONTEXT_MUST_NOT_BE_NULL = "context must not be null";

    private static final List<StatusResolutionStep> XY1_END_OF_TURN_ORDER = List.of(
            new StatusResolutionStep(StatusCondition.POISONED, StatusEffectManager::resolvePoisoned),
            new StatusResolutionStep(StatusCondition.BURNED, StatusEffectManager::resolveBurned),
            new StatusResolutionStep(StatusCondition.ASLEEP, StatusEffectManager::resolveAsleep),
            new StatusResolutionStep(StatusCondition.PARALYZED, StatusEffectManager::resolveParalyzed)
    );

    /**
     * Processes the affected Pokémon's end-of-turn statuses in XY1 order.
     *
     * @param pokemon the Pokémon whose statuses must be resolved; must not be {@code null}
     * @param context the current game context used for deterministic coin flips; must not be
     *                {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public void processEndOfTurnEffects(InPlayPokemon pokemon, GameContext context) {
        Objects.requireNonNull(pokemon, "pokemon must not be null");
        Objects.requireNonNull(context, CONTEXT_MUST_NOT_BE_NULL);

        for (StatusResolutionStep step : XY1_END_OF_TURN_ORDER) {
            if (pokemon.isKnockedOut()) {
                return;
            }
            step.resolveIfPresent(pokemon, context);
        }
    }

    /**
     * Applies the XY1 poison check.
     *
     * <p>Poison always deals 10 damage. If that damage reduces the Pokémon to 0 HP, it is
     * knocked out and later statuses are not processed.
     *
     * @param pokemon affected Pokémon
     * @param context current game context
     */
    private static void resolvePoisoned(InPlayPokemon pokemon, GameContext context) {
        Objects.requireNonNull(context, CONTEXT_MUST_NOT_BE_NULL);
        applyDamage(pokemon, POISON_DAMAGE);
    }

    /**
     * Applies the XY1 burn check.
     *
     * <p>A heads coin flip prevents burn damage. A tails coin flip deals 20 damage. Burn is
     * always removed afterward, regardless of the flip result.
     *
     * @param pokemon affected Pokémon
     * @param context current game context
     */
    private static void resolveBurned(InPlayPokemon pokemon, GameContext context) {
        boolean heads = context.flipCoin("burn");
        if (!heads) {
            applyDamage(pokemon, BURN_DAMAGE);
        }
        removeStatus(pokemon, StatusCondition.BURNED);
    }

    /**
     * Applies the XY1 sleep check.
     *
     * <p>A heads coin flip wakes the Pokémon up. A tails coin flip leaves the condition in
     * place.
     *
     * @param pokemon affected Pokémon
     * @param context current game context
     */
    private static void resolveAsleep(InPlayPokemon pokemon, GameContext context) {
        boolean heads = context.flipCoin("sleep");
        if (heads) {
            removeStatus(pokemon, StatusCondition.ASLEEP);
        }
    }

    /**
     * Applies the XY1 paralysis cleanup.
     *
     * <p>Paralysis is always removed at the end of the turn.
     *
     * @param pokemon affected Pokémon
     * @param context current game context
     */
    private static void resolveParalyzed(InPlayPokemon pokemon, GameContext context) {
        Objects.requireNonNull(context, CONTEXT_MUST_NOT_BE_NULL);
        removeStatus(pokemon, StatusCondition.PARALYZED);
    }

    /**
     * Applies direct end-of-turn damage to the affected Pokémon.
     *
     * @param pokemon affected Pokémon
     * @param damage fixed damage to add
     */
    private static void applyDamage(InPlayPokemon pokemon, int damage) {
        pokemon.setDamageTaken(pokemon.getDamageTaken() + damage);
    }

    /**
     * Removes a resolved status from the affected Pokémon.
     *
     * @param pokemon affected Pokémon
     * @param status condition to remove
     */
    private static void removeStatus(InPlayPokemon pokemon, StatusCondition status) {
        Objects.requireNonNull(pokemon.getStatuses(), "pokemon statuses must not be null")
                .remove(status);
    }

    /**
     * Ordered status-resolution step.
     */
    private record StatusResolutionStep(StatusCondition status, StatusResolver resolver) {

        /**
         * Resolves the step only when the Pokémon currently has the corresponding status.
         *
         * @param pokemon affected Pokémon
         * @param context current game context
         */
        private void resolveIfPresent(InPlayPokemon pokemon, GameContext context) {
            if (pokemon.hasStatus(status)) {
                resolver.resolve(pokemon, context);
            }
        }
    }

    /**
     * Internal functional contract for a single status-resolution stage.
     */
    @FunctionalInterface
    private interface StatusResolver {

        /**
         * Resolves one end-of-turn status effect.
         *
         * @param pokemon affected Pokémon
         * @param context current game context
         */
        void resolve(InPlayPokemon pokemon, GameContext context);
    }
}
