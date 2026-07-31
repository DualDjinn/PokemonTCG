package ar.edu.utn.frc.tup.piii.models;

/**
 * Battle phases that make up a single turn, modelled as an explicit finite-state machine.
 *
 * <p>A turn always advances in the fixed order {@link #DRAW} → {@link #MAIN} → {@link #ATTACK} →
 * {@link #BETWEEN_TURNS}. The {@link #DRAW} and {@link #BETWEEN_TURNS} phases are transient: they
 * perform their entry effects and immediately hand control to the next phase, so a player only ever
 * submits actions while the turn rests in {@link #MAIN} or {@link #ATTACK}.
 *
 * <p>This enum stays the serializable phase identifier exposed to the transport layer; the per-phase
 * behaviour lives in the {@code engine.turnstates} State objects keyed by these values.
 */
public enum TurnPhase {
    DRAW,
    MAIN,
    ATTACK,
    BETWEEN_TURNS
}
