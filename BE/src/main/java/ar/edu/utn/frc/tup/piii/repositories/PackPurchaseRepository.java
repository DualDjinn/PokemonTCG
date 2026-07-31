package ar.edu.utn.frc.tup.piii.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.utn.frc.tup.piii.models.PackPurchase;

@Repository
public interface PackPurchaseRepository extends JpaRepository<PackPurchase, String> {
    List<PackPurchase> findByPlayerIdOrderByCreatedAtDesc(String playerId);
}
