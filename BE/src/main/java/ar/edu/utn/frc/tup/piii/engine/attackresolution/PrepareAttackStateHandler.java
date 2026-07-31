package ar.edu.utn.frc.tup.piii.engine.attackresolution;

/**
 * Initializes the transient attack state for a new resolution.
 */
public final class PrepareAttackStateHandler extends AbstractAttackResolutionHandler {

    /**
     * Creates the handler.
     *
     * @param next next step in the chain, or {@code null} for the last handler
     */
    public PrepareAttackStateHandler(AttackResolutionHandler next) {
        super(next);
    }

    @Override
    protected void doHandle(AttackResolutionContext context) {
        context.getGameContext().setPendingDamage(context.getAttack().getBaseDamage());
        context.getGameContext().setIgnoreWeakness(false);
        context.getGameContext().setIgnoreResistance(false);
        context.getGameContext().clearDamageCalculationModifiers();
        context.setCalculatedDamage(0);
        context.setFinalDamage(0);
    }
}

