package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.models.Purchase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/**
 * Repositorio Spring Data para la entidad {@link Purchase}.
 *
 * <p>Además de las operaciones CRUD heredadas de {@link JpaRepository},
 * expone {@link #findByPreferenceId} que usa el handler del webhook para
 * vincular la notificación entrante de MercadoPago con el registro de compra
 * original (MP no devuelve el {@code purchaseId} directamente en la notificación,
 * sino el {@code preference_id} o el {@code external_reference}).
 */
public interface PurchaseRepository extends JpaRepository<Purchase, String> {

    /**
     * Busca una compra por el ID de preferencia de MercadoPago.
     *
     * @param preferenceId ID de la preferencia asignado por MP al crear el checkout
     * @return la compra asociada, o vacío si no existe
     */
    Optional<Purchase> findByPreferenceId(String preferenceId);

    /**
     * Busca una compra por su ID tomando un lock pesimista de escritura
     * ({@code SELECT ... FOR UPDATE}). Lo usa el handler del webhook para
     * serializar reentregas concurrentes del IPN de MercadoPago y así evitar
     * la doble acreditación de monedas (hallazgo BQ-01). Requiere ejecutarse
     * dentro de una transacción activa.
     *
     * @param id ID interno de la compra
     * @return la compra bloqueada para escritura, o vacío si no existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Purchase p WHERE p.id = :id")
    Optional<Purchase> findByIdForUpdate(@Param("id") String id);
}
