package ar.edu.utn.frc.tup.piii.dtos.quest;

import ar.edu.utn.frc.tup.piii.dtos.player.CardSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resultado de reclamar la recompensa de un NPC.
 *
 * <p>Si {@code alreadyClaimed} es {@code true} la recompensa ya había sido
 * otorgada en una visita anterior y {@code card} viene en {@code null};
 * el frontend no debe reproducir la animación de descubrimiento.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NpcRewardClaimResponse {

    private String npcId;
    private boolean alreadyClaimed;
    private CardSummaryDTO card;
}
