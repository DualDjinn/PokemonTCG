package ar.edu.utn.frc.tup.piii.engine;

/**
 * Enumerates the possible outcomes of a victory-condition evaluation.
 *
 * <p>The values are expressed relative to the immutable player slots stored in
 * {@link Match}: player one, player two, or no winner yet.
 */
public enum MatchResult {

    /** Player one has satisfied at least one victory condition. */
    PLAYER_ONE_WINS,

    /** Player two has satisfied at least one victory condition. */
    PLAYER_TWO_WINS,

    /** No victory condition is currently satisfied. */
    NO_WINNER_YET
}
