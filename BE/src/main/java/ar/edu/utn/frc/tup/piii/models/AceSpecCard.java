package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Carta ACE SPEC. Mecánicamente es un Item pero con la restricción adicional
 * de "solo 1 carta ACE SPEC por mazo". Por eso hereda de ItemCard y
 * complementa su regla de juego.
 *
 * Nota: el JSON de la expansión XY no incluye ACE SPEC, pero la clase queda
 * preparada para sets como Boundaries Crossed, Plasma Blast, etc.
 */
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@DiscriminatorValue("ACE_SPEC")
public class AceSpecCard extends ItemCard {

    @Override
    public String getSubtype() {
        return "Item (ACE SPEC)";
    }

    @Override
    public String getPlayRule() {
        return super.getPlayRule()
             + " You can have only 1 ACE SPEC card in your deck.";
    }
}
