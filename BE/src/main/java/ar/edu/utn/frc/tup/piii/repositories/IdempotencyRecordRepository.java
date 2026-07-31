package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.models.IdempotencyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/**
 * Spring Data repository for {@link IdempotencyRecord}.
 *
 * <p>Reuses the pessimistic-lock pattern from the MercadoPago webhook
 * ({@code PurchaseRepository.findByIdForUpdate}) to serialize concurrent
 * re-deliveries that carry the same idempotency key.
 */
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {

    /**
     * Looks up a record by its key taking a pessimistic write lock
     * ({@code SELECT ... FOR UPDATE}). Must run inside an active transaction.
     *
     * @param key idempotency key from the {@code Idempotency-Key} header
     * @return the row locked for writing, or empty if the key is new
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM IdempotencyRecord r WHERE r.idempotencyKey = :key")
    Optional<IdempotencyRecord> findByIdForUpdate(@Param("key") String key);
}
