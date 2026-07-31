package ar.edu.utn.frc.tup.piii.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Historial de un sobre comprado y abierto por un jugador.
 *
 * <p>Esta entidad es solo para registro/auditoría — no afecta el saldo de monedas
 * ni la colección. El descuento de monedas y el alta en la colección los hace
 * {@code PackService.openPack()} en la misma transacción que persiste esta fila.
 */
@Data
@Entity
@Table(name = "pack_purchase")
public class PackPurchase {

    @Id
    private String id;

    @Column(name = "player_id", nullable = false)
    private String playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pack_type", nullable = false, length = 16)
    private PackType packType;

    /** Precio cobrado en monedas. Snapshot al momento de la compra. */
    @Column(nullable = false)
    private int price;

    /** Lista de IDs de las 3 cartas obtenidas, serializada como CSV. */
    @Column(name = "card_ids", nullable = false, length = 512)
    private String cardIds;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** ID de la skin bonus obtenida (null si no cayó ninguna). */
    @Column(name = "skin_id")
    private String skinId;
}
