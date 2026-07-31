package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.choice.ChoiceType;
import ar.edu.utn.frc.tup.piii.engine.choice.PendingPlayerChoice;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

import java.util.List;

/**
 * Roller Skates (xy1-125): Flip a coin. If heads, draw 3 cards.
 *
 * <p>Asks the player to choose heads or tails, then flips the coin.
 * If the player guessed correctly, draws 3 cards.
 */
public class RollerSkatesEffect implements TrainerEffect {

    private static final int CARDS_TO_DRAW = 3;

    @Override
    public void apply(GameContext ctx) {
        ctx.setPendingPlayerChoice(new PendingPlayerChoice(
                ctx.getAttackerPlayerId(),
                ChoiceType.CHOOSE_COIN_SIDE,
                List.of(),
                1,
                1,
                "Elegí cara o cruz",
                null,
                chosen -> {
                    String playerSide = chosen.isEmpty() ? "" : chosen.get(0);
                    boolean heads = ctx.flipCoin("rollerSkates");
                    boolean playerChoseHeads = "heads".equalsIgnoreCase(playerSide);
                    if (playerChoseHeads == heads) {
                        for (int i = 0; i < CARDS_TO_DRAW; i++) {
                            ctx.attackerDrawCard();
                        }
                    }
                }
        ));
    }
}
