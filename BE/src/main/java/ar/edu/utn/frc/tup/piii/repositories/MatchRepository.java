package ar.edu.utn.frc.tup.piii.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.MatchStatus;

/**
 * Data access for persisted {@link Match} lobby aggregates.
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, String> {

    /** Partidas ganadas por el jugador (para desbloqueos por batallas). */
    long countByWinnerId(String winnerId);

    /**
     * Looks up a match by its shareable join code.
     *
     * @param joinCode code generated for the host to share with an opponent
     * @return the matching lobby, if any
     */
    Optional<Match> findByJoinCode(String joinCode);

    /**
     * Returns all matches with the given status, ordered by creation date descending.
     *
     * @param status the lifecycle status to filter by
     * @return the matching lobbies
     */
    List<Match> findByStatusOrderByCreatedAtDesc(MatchStatus status);

    /**
     * Returns all matches whose status is in the given set, ordered by creation
     * date descending.
     *
     * @param statuses the lifecycle statuses to filter by
     * @return the matching lobbies
     */
    List<Match> findByStatusInOrderByCreatedAtDesc(List<MatchStatus> statuses);

    List<Match> findByStatus(MatchStatus status);

    List<Match> findByPlayerOneIdAndStatus(String playerOneId, MatchStatus status);

    List<Match> findByPlayerTwoIdAndStatus(String playerTwoId, MatchStatus status);

    List<Match> findByPlayerOneIdAndStatusIn(String playerOneId, List<MatchStatus> statuses);

    List<Match> findByPlayerTwoIdAndStatusIn(String playerTwoId, List<MatchStatus> statuses);
}
