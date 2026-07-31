package ar.edu.utn.frc.tup.piii.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frc.tup.piii.models.PlayerCard;

@Repository
public interface PlayerCardRepository extends JpaRepository<PlayerCard, Long> {

    Optional<PlayerCard> findByPlayer_IdAndCard_Id(String playerId, String cardId);

    List<PlayerCard> findByPlayer_Id(String playerId);
}
