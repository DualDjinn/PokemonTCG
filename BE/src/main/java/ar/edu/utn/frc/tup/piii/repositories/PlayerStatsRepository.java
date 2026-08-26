package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.models.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access for per-player accumulated match statistics.
 */
@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, String> {
}