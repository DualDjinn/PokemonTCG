package ar.edu.utn.frc.tup.piii.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frc.tup.piii.models.CardSkin;
import ar.edu.utn.frc.tup.piii.models.CardSkinOrigin;

/**
 * Repositorio de skins de cartas.
 */
@Repository
public interface CardSkinRepository extends JpaRepository<CardSkin, String> {

    List<CardSkin> findByBaseCard_Id(String baseCardId);

    List<CardSkin> findByOrigin(CardSkinOrigin origin);
}
