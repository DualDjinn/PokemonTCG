package ar.edu.utn.frc.tup.piii.dtos.cardback;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dorso de carta del catálogo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardBackDTO {

    private String id;
    private String name;
    private String imageUrl;
    /** SHOP, PACK, BATTLE o LEVEL. */
    private String origin;
    /** Precio en monedas; null si no se vende en la tienda. */
    private Integer price;
    /** Nivel o batallas ganadas requeridas; null para SHOP/PACK. */
    private Integer unlockValue;
}
