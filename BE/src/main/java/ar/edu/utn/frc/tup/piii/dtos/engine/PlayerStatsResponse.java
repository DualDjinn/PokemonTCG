package ar.edu.utn.frc.tup.piii.dtos.engine;

/**
 * Response for the player statistics endpoint.
 *
 * @param matchesPlayed    total finished matches where the player participated
 * @param matchesWon       matches won
 * @param matchesLost      matches lost
 * @param matchesTied      matches tied (no winner)
 * @param totalMatchTimeMs accumulated time spent inside matches
 * @param mostPlayedCard   name of the card played most often, or {@code null}
 * @param mostUsedDeck     name of the deck used most often, or {@code null}
 */
public record PlayerStatsResponse(
        long matchesPlayed,
        long matchesWon,
        long matchesLost,
        long matchesTied,
        long totalMatchTimeMs,
        String mostPlayedCard,
        String mostUsedDeck) {
}