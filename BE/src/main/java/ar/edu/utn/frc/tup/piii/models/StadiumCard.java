package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Carta de Entrenador del subtipo Stadium.
 * Ejemplos del JSON: Fairy Garden, Shadow Circle.
 */
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@DiscriminatorValue("STADIUM")
public class StadiumCard extends TrainerCard {

    @Override
    public String getSubtype() {
        return "Stadium";
    }

    @Override
    public String getPlayRule() {
        return "This card stays in play when you play it. Discard this card if another Stadium card "
             + "comes into play. If another card with the same name is in play, you can't play this card.";
    }

    @Override
    public boolean canBePlayed(GameContext ctx) {
        if (!super.canBePlayed(ctx)) {
            return false;
        }
        StadiumCard active = ctx.getActiveStadium();
        return active == null || !active.getName().equals(getName());
    }
}
