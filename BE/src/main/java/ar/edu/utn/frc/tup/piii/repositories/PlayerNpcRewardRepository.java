package ar.edu.utn.frc.tup.piii.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frc.tup.piii.models.PlayerNpcReward;

/**
 * Repositorio de recompensas de NPC reclamadas por jugadores.
 */
@Repository
public interface PlayerNpcRewardRepository extends JpaRepository<PlayerNpcReward, Long> {

    boolean existsByPlayer_IdAndNpcId(String playerId, String npcId);

    List<PlayerNpcReward> findByPlayer_Id(String playerId);
}
