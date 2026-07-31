package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.choice.ChoiceType;
import ar.edu.utn.frc.tup.piii.engine.choice.PendingPlayerChoice;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Great Ball (xy1-118): Look at the top 7 cards of your deck. You may
 * reveal a Pokémon you find there and put it into your hand. Shuffle
 * the other cards back into your deck.
 *
 * <p>Raises a {@link ChoiceType#PICK_FROM_DECK_TOP_N} prompt with the
 * Pokémon found among the top 7 as options. The player picks 0 or 1.
 */
public class GreatBallEffect implements TrainerEffect {

    private static final int CARDS_TO_LOOK = 7;

    @Override
    public void apply(GameContext ctx) {
        List<Card> deck = ctx.getAttackerDeck();
        int count = Math.min(CARDS_TO_LOOK, deck.size());
        List<Card> topCards = new ArrayList<>(deck.subList(0, count));

        List<Card> pokemonOptions = new ArrayList<>();
        for (Card card : topCards) {
            if (card instanceof PokemonCard) {
                pokemonOptions.add(card);
            }
        }

        if (pokemonOptions.isEmpty()) {
            ctx.shuffleAttackerDeck();
            return;
        }

        ctx.setPendingPlayerChoice(new PendingPlayerChoice(
                ctx.getAttackerPlayerId(),
                ChoiceType.PICK_FROM_DECK_TOP_N,
                pokemonOptions,
                0,
                1,
                "Great Ball: pick a Pokémon to take into your hand",
                null,
                chosen -> {
                    if (!chosen.isEmpty()) {
                        Card picked = pokemonOptions.stream()
                                .filter(c -> chosen.get(0).equals(c.getId()))
                                .findFirst().orElse(null);
                        if (picked != null) {
                            deck.remove(picked);
                            ctx.getAttackerHand().add(picked);
                        }
                    }
                    ctx.shuffleAttackerDeck();
                }
        ));
    }
}
