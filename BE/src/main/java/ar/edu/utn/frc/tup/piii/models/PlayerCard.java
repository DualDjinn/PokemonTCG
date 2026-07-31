package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cantidad de copias de una carta que posee un jugador.
 *
 * <p>Esta tabla es la fuente de verdad para los recuentos de la colección de un jugador.
 * Existe en paralelo con la tabla {@code player_collection} (Player.collection), que
 * almacena el conjunto de cartas únicas y se usa para validar mazos.
 *
 * <p>Restricción única (player_id, card_id) garantiza una sola fila por par y un
 * contador {@code quantity} ≥ 1.
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "player_card", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_card", columnNames = {"player_id", "card_id"})
})
public class PlayerCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(nullable = false)
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_skin_id")
    private CardSkin activeSkin;
}
