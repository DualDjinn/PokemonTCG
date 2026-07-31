package ar.edu.utn.frc.tup.piii.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frc.tup.piii.models.Player;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {
    Optional<Player> findByUsername(String username);
    Optional<Player> findByEmail(String email);
    Optional<Player> findByResetToken(String token);

    @Query("SELECT p FROM Player p LEFT JOIN FETCH p.decks WHERE p.id = :id")
    Optional<Player> findByIdWithDecks(@Param("id") String id);

    @EntityGraph(attributePaths = {"decks", "collection"})
    Optional<Player> findWithDecksAndCollectionById(String id);
}
