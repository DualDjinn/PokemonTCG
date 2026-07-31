package ar.edu.utn.frc.tup.piii.dtos.pack;

import java.util.List;

import ar.edu.utn.frc.tup.piii.dtos.cardback.CardBackDTO;
import ar.edu.utn.frc.tup.piii.dtos.player.CardSummaryDTO;
import ar.edu.utn.frc.tup.piii.dtos.skin.CardSkinDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta al abrir un sobre. El frontend usa {@code cards} para alimentar la
 * animación de revelado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenedPackResponse {
    private String packType;
    /** Las 3 cartas obtenidas, ya guardadas en la colección del jugador. */
    private List<CardSummaryDTO> cards;
    /** Saldo de monedas del jugador después del descuento. */
    private int remainingCoins;
    /** Skin bonus obtenida (null si no cayó ninguna). */
    private CardSkinDTO bonusSkin;
    /** Dorso de carta bonus obtenido (null si no cayó ninguno). */
    private CardBackDTO bonusCardBack;

    public OpenedPackResponse(String packType, List<CardSummaryDTO> cards, int remainingCoins) {
        this.packType = packType;
        this.cards = cards;
        this.remainingCoins = remainingCoins;
    }
}
