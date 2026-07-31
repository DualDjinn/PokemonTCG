package ar.edu.utn.frc.tup.piii.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frc.tup.piii.models.PlayerCardBack;

/**
 * Repositorio de dorsos de carta poseídos por jugadores.
 */
@Repository
public interface PlayerCardBackRepository extends JpaRepository<PlayerCardBack, Long> {

    boolean existsByPlayer_IdAndCardBack_Id(String playerId, String cardBackId);

    List<PlayerCardBack> findByPlayer_Id(String playerId);
}
