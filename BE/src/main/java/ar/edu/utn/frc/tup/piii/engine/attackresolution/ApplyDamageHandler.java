package ar.edu.utn.frc.tup.piii.engine.attackresolution;

import ar.edu.utn.frc.tup.piii.engine.TriggeredAbilityDispatcher;
import java.util.Objects;

/**
 * Commits the final damage value to the defending Pokémon, then fires any
 * TRIGGERED abilities on the defender that react to incoming damage (e.g.
 * Chesnaught's Spiky Shield).
 */
public final class ApplyDamageHandler extends AbstractAttackResolutionHandler {

    private static final int MINIMUM_DAMAGE = 0;

    private final TriggeredAbilityDispatcher triggeredAbilityDispatcher;

    /**
     * Creates the handler.
     *
     * @param triggeredAbilityDispatcher dispatcher for on-damaged TRIGGERED abilities
     * @param next next step in the chain, or {@code null} for the last handler
     */
    public ApplyDamageHandler(TriggeredAbilityDispatcher triggeredAbilityDispatcher,
                               AttackResolutionHandler next) {
        super(next);
        this.triggeredAbilityDispatcher = Objects.requireNonNull(
                triggeredAbilityDispatcher, "triggeredAbilityDispatcher must not be null");
    }

    @Override
    protected void doHandle(AttackResolutionContext context) {
        if (context.getFinalDamage() <= MINIMUM_DAMAGE) {
            return;
        }
        context.getDefender().setDamageTaken(context.getDefender().getDamageTaken() + context.getFinalDamage());
        triggeredAbilityDispatcher.onDamaged(
                context.getGameContext(), context.getDefender(), context.getAttacker());
    }
}

