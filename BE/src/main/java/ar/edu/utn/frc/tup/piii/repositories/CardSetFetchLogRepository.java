package ar.edu.utn.frc.tup.piii.repositories;

import java.util.Optional;

import ar.edu.utn.frc.tup.piii.models.CardSetFetchLog;
import ar.edu.utn.frc.tup.piii.models.FetchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for card set fetch log entries.
 */
@Repository
public interface CardSetFetchLogRepository extends JpaRepository<CardSetFetchLog, Long> {

    /**
     * Finds the most recent fetch log with the given status for a set.
     *
     * @param setId  the card set identifier
     * @param status the fetch status to filter by
     * @return the most recent matching log entry
     */
    Optional<CardSetFetchLog> findTopBySetIdAndStatusOrderByFetchedAtDesc(
            String setId, FetchStatus status);
}
