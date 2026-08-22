package ar.edu.utn.frc.tup.piii.dtos.engine;

/**
 * Response for the abandoned-result endpoint.
 *
 * <p>Indicates whether the authenticated player recently forfeited a match by
 * disconnecting and failing to reconnect within the allowed window.
 *
 * @param matchId   identifier of the abandoned match, or {@code null} if none
 * @param abandoned whether the player lost a recent match by abandonment
 */
public record AbandonedResultResponse(String matchId, boolean abandoned) {
}
