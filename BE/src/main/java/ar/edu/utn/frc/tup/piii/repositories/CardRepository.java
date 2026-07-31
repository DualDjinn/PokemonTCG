package ar.edu.utn.frc.tup.piii.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frc.tup.piii.models.Card;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
    List<Card> findByName(String name);
}
