package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Carta de Entrenador del subtipo Supporter.
 * Ejemplos del JSON: Cassius, Professor Sycamore, Shauna, Team Flare Grunt.
 */
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@DiscriminatorValue("SUPPORTER")
public class SupporterCard extends TrainerCard {

    @Override
    public String getSubtype() {
        return "Supporter";
    }

    @Override
    public String getPlayRule() {
        return "You may play only 1 Supporter card during your turn (before your attack).";
    }

    @Override
    public boolean canBePlayed(GameContext ctx) {
        return super.canBePlayed(ctx)
                && !ctx.isAttackerCantPlaySupporters()
                && ctx.getAttackerSupportersPlayedThisTurn() < 1;
    }
}
