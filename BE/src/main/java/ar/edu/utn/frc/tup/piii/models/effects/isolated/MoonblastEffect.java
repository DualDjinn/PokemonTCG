package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Lunatone — Moonblast: during the opponent's next turn, damage from the defending
 * Pokémon's attacks is reduced by 20 (before W/R).
 */
public class MoonblastEffect implements AttackEffect {

    private static final int REDUCTION = 20;

    @Override
    public void apply(GameContext ctx) {
        ctx.getDefender().setIncomingDamageReduction(REDUCTION);
    }
}
