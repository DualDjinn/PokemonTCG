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
 * Max Revive (xy1-120): Put a Pokémon from your discard pile on top of your deck.
 *
 * <p>Raises a {@link ChoiceType#PICK_FROM_DISCARD} prompt so the player
 * chooses which Pokémon to revive.
 */
public class MaxReviveEffect implements TrainerEffect {

    @Override
    public void apply(GameContext ctx) {
        List<Card> discard = ctx.getAttackerDiscard();
        List<Card> pokemonOptions = new ArrayList<>();
        for (Card card : discard) {
            if (card instanceof PokemonCard) {
                pokemonOptions.add(card);
            }
        }

        if (pokemonOptions.isEmpty()) {
            return;
        }

        ctx.setPendingPlayerChoice(new PendingPlayerChoice(
                ctx.getAttackerPlayerId(),
                ChoiceType.PICK_FROM_DISCARD,
                pokemonOptions,
                1,
                1,
                "Max Revive: elegí un Pokémon del descarte para poner en la cima de tu mazo",
                null,
                chosen -> {
                    if (!chosen.isEmpty()) {
                        Card picked = pokemonOptions.stream()
                                .filter(c -> chosen.get(0).equals(c.getId()))
                                .findFirst().orElse(null);
                        if (picked != null) {
                            discard.remove(picked);
                            ctx.getAttackerDeck().addFirst(picked);
                        }
                    }
                }
        ));
    }
}
