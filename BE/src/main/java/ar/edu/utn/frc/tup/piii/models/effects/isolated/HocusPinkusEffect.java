package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Wigglytuff — Hocus Pinkus: the defending Pokémon cannot attack during the opponent's next turn.
 */
public class HocusPinkusEffect implements AttackEffect {

    @Override
    public void apply(GameContext ctx) {
        ctx.getDefender().setCantAttack(true);
    }
}
