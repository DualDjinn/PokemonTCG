package ar.edu.utn.frc.tup.piii.models;

/**
 * Lifecycle states of a match lobby aggregate.
 *
 * <p>The progression models the matchmaking and setup flow that surrounds the
 * battle engine:
 * <ul>
 *   <li>{@link #WAITING} — created by player one, waiting for an opponent to join.</li>
 *   <li>{@link #SETUP} — both players joined; the battle engine has dealt opening
 *       hands and is waiting for active Pokémon selection.</li>
 *   <li>{@link #ACTIVE} — both players chose their active Pokémon and turns are running.</li>
 *   <li>{@link #FINISHED} — a victory condition was met; {@code winnerId} is populated.</li>
 * </ul>
 */
public enum MatchStatus {
    WAITING,
    SETUP,
    ACTIVE,
    FINISHED
}
