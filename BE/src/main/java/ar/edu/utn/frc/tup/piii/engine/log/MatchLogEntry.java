package ar.edu.utn.frc.tup.piii.engine.log;

/**
 * One entry in the match event log, visible to both players during the session.
 *
 * @param turnNumber  1-based turn counter
 * @param playerId    identifier of the player who performed the action
 * @param actionType  short code describing the kind of action (PLAY_BASIC, EVOLVE, etc.)
 * @param description human-readable description in Spanish
 * @param timestampMs system clock when the action was logged
 */
public record MatchLogEntry(
        int turnNumber,
        String playerId,
        String actionType,
        String description,
        long timestampMs
) {}
