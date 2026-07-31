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
 * Registro de recompensas de NPCs ya reclamadas por un jugador.
 *
 * <p>Cada fila marca que el jugador completó la "sidequest" de un NPC
 * concreto y recibió su carta. La unicidad (jugador, NPC) hace el reclamo
 * idempotente: hablar de nuevo con el NPC no vuelve a otorgar la carta.
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "player_npc_reward", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_npc_reward", columnNames = {"player_id", "npc_id"})
})
public class PlayerNpcReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /** Identificador del NPC tal como figura en la configuración del mapa. */
    @Column(name = "npc_id", nullable = false)
    private String npcId;

    /** Id de la carta otorgada (referencia al registro de cartas). */
    @Column(name = "card_id", nullable = false)
    private String cardId;

    @Column(nullable = false)
    private LocalDateTime claimedAt;
}
