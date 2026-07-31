package ar.edu.utn.frc.tup.piii.models;

import java.time.LocalDateTime;

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
 * Dorsos de carta que posee un jugador. La unicidad (jugador, dorso) hace
 * idempotente cualquier otorgamiento repetido (compra, sobre, recompensa).
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "player_card_back", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_card_back", columnNames = {"player_id", "card_back_id"})
})
public class PlayerCardBack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "card_back_id", nullable = false)
    private CardBack cardBack;

    @Column(nullable = false)
    private LocalDateTime acquiredAt;
}
