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
 * Registro de skins que posee un jugador.
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "player_skin", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_skin", columnNames = {"player_id", "skin_id"})
})
public class PlayerSkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "skin_id", nullable = false)
    private CardSkin skin;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private LocalDateTime acquiredAt;
}
