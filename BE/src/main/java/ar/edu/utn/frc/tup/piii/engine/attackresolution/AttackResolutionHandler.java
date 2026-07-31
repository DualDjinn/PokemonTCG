package ar.edu.utn.frc.tup.piii.engine.attackresolution;

/**
 * One step in the attack-resolution Chain of Responsibility.
 *
 * <p>Each handler performs a single resolution concern and may then delegate to the next handler
 * in the configured chain.
 */
public interface AttackResolutionHandler {

    /**
     * Executes this handler and, when appropriate, delegates to the next one.
     *
     * @param context mutable attack-resolution state
     */
    void handle(AttackResolutionContext context);
}
