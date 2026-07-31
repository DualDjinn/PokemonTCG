package ar.edu.utn.frc.tup.piii.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frc.tup.piii.models.CardBack;

/**
 * Repositorio del catálogo de dorsos de carta.
 */
@Repository
public interface CardBackRepository extends JpaRepository<CardBack, String> {
}
