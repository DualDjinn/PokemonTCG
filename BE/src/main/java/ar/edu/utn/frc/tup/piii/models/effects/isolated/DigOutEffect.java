package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Rhyhorn — Dig Out: discard the top card of the attacker's deck;
 * if it is a Fighting Energy, attach it to this Pokémon.
 */
public class DigOutEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        Card top = ctx.attackerDiscardTopCard();
        if (top instanceof EnergyCard energy && energy.getEnergyType() == EnergyType.FIGHTING) {
            ctx.getAttackerDiscard().remove(energy);
            ctx.getAttacker().getEnergies().add(energy);
        }
    }
}
