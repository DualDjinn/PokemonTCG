package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dorso cosmético de carta. El jugador equipa uno como dorso global: es lo
 * que ve el rival durante la partida (mano, mazo, premios) y lo que se
 * muestra en las cartas boca abajo al abrir sobres.
 *
 * <p>El dorso clásico de la app no es una fila de esta tabla: un jugador con
 * {@code activeCardBackId == null} usa el dorso por defecto.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "card_back")
public class CardBack {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardBackOrigin origin;

    /** Precio en monedas; solo aplica cuando origin == SHOP. */
    @Column
    private Integer price;

    /**
     * Umbral de desbloqueo según el origen: nivel requerido (LEVEL) o
     * batallas ganadas (BATTLE). Null para SHOP y PACK.
     */
    @Column(name = "unlock_value")
    private Integer unlockValue;
}
