package ar.edu.utn.frc.tup.piii.models.effects.isolated;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;

/**
 * Talonflame — Devastating Wind: the opponent shuffles their hand into their deck
 * and draws 4 cards.
 */
public class DevastatingWindEffect implements AttackEffect {

    private static final int DRAW_COUNT = 4;

    @Override
    public void apply(GameContext ctx) {
        ctx.getDefenderDeck().addAll(ctx.getDefenderHand());
        ctx.getDefenderHand().clear();
        ctx.shuffleDefenderDeck();
        List<Card> drawn = new ArrayList<>();
        for (int i = 0; i < DRAW_COUNT && !ctx.getDefenderDeck().isEmpty(); i++) {
            drawn.add(ctx.getDefenderDeck().remove(0));
        }
        ctx.getDefenderHand().addAll(drawn);
    }
}
