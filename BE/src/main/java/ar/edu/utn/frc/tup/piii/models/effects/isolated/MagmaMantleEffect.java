package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Magcargo — Magma Mantle: discard the top card of the attacker's deck;
 * if that card is a Fire Energy, attach it to this Pokémon and this attack does 50 more damage.
 */
public class MagmaMantleEffect implements AttackEffect {

    private static final int BONUS = 50;

    @Override
    public void apply(GameContext ctx) {
        Card top = ctx.attackerDiscardTopCard();
        if (top instanceof EnergyCard energy && energy.getEnergyType() == EnergyType.FIRE) {
            ctx.getAttackerDiscard().remove(energy);
            ctx.getAttacker().getEnergies().add(energy);
            ctx.setPendingDamage(ctx.getPendingDamage() + BONUS);
        }
    }
}
