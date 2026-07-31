package ar.edu.utn.frc.tup.piii.engine.attackresolution;

import java.util.Objects;

/**
 * Base implementation for linked attack-resolution handlers.
 *
 * <p>Concrete subclasses implement one deterministic step and this base class guarantees the next
 * handler is invoked afterward.
 */
public abstract class AbstractAttackResolutionHandler implements AttackResolutionHandler {

    private final AttackResolutionHandler next;

    protected AbstractAttackResolutionHandler(AttackResolutionHandler next) {
        this.next = next;
    }

    @Override
    public final void handle(AttackResolutionContext context) {
        doHandle(Objects.requireNonNull(context, "context must not be null"));
        if (next != null) {
            next.handle(context);
        }
    }

    /**
     * Performs the concrete work of this resolution step.
     *
     * @param context mutable attack-resolution state
     */
    protected abstract void doHandle(AttackResolutionContext context);
}
