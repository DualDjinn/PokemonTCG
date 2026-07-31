package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Krookodile — Bother: flip a coin; if heads the opponent cannot play any Supporter
 * cards from their hand during their next turn.
 */
public class BotherEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        if (ctx.flipCoin("bother")) {
            ctx.setAttackerCantPlaySupporters(true);
        }
    }
}
