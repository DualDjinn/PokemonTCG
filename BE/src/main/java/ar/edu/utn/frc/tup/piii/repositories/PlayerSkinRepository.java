package ar.edu.utn.frc.tup.piii.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frc.tup.piii.models.PlayerSkin;

/**
 * Repositorio de skins de jugadores.
 */
@Repository
public interface PlayerSkinRepository extends JpaRepository<PlayerSkin, Long> {

    Optional<PlayerSkin> findByPlayer_IdAndSkin_Id(String playerId, String skinId);

    List<PlayerSkin> findByPlayer_Id(String playerId);
}
