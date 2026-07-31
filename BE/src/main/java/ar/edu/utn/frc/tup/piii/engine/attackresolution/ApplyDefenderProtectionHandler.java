package ar.edu.utn.frc.tup.piii.engine.attackresolution;

/**
 * Applies defender-side prevention and reduction flags after base damage has been calculated.
 */
public final class ApplyDefenderProtectionHandler extends AbstractAttackResolutionHandler {

    private static final int MINIMUM_DAMAGE = 0;

    /**
     * Creates the handler.
     *
     * @param next next step in the chain, or {@code null} for the last handler
     */
    public ApplyDefenderProtectionHandler(AttackResolutionHandler next) {
        super(next);
    }

    @Override
    protected void doHandle(AttackResolutionContext context) {
        if (context.getDefender().isPreventAllEffects() || context.getDefender().isPreventAllIncomingDamage()) {
            context.setFinalDamage(MINIMUM_DAMAGE);
            return;
        }
        if (isPreventedByThreshold(context)) {
            context.setFinalDamage(MINIMUM_DAMAGE);
            return;
        }
        context.setFinalDamage(reduceIncomingDamage(context));
    }

    private boolean isPreventedByThreshold(AttackResolutionContext context) {
        return context.getDefender().getPreventIncomingDamageThreshold() > MINIMUM_DAMAGE
                && context.getCalculatedDamage() <= context.getDefender().getPreventIncomingDamageThreshold();
    }

    private int reduceIncomingDamage(AttackResolutionContext context) {
        return Math.max(
                MINIMUM_DAMAGE,
                context.getCalculatedDamage() - context.getDefender().getIncomingDamageReduction()
        );
    }
}

