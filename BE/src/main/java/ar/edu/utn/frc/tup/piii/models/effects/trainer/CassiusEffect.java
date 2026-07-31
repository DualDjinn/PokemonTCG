package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.choice.ChoiceType;
import ar.edu.utn.frc.tup.piii.engine.choice.PendingPlayerChoice;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Cassius (xy1-115): Shuffle 1 of your Pokémon and all cards attached
 * to it into your deck.
 *
 * <p>Raises a {@link ChoiceType#PICK_BENCHED_POKEMON} prompt with all the
 * attacker's benched Pokémon as options. The picked Pokémon, its energies,
 * underlying cards, and tool are shuffled into the deck.
 *
 * <p>The active is intentionally excluded: shuffling it back would leave
 * no active Pokémon, which is not a legal mid-turn state in v1.
 */
public class CassiusEffect implements TrainerEffect {

    @Override
    public void apply(GameContext ctx) {
        List<InPlayPokemon> bench = ctx.getAttackerBench();
        List<Card> options = new ArrayList<>();
        for (InPlayPokemon pokemon : bench) {
            if (pokemon != null) {
                options.add(pokemon.getCard());
            }
        }
        if (options.isEmpty()) {
            return;
        }

        ctx.setPendingPlayerChoice(new PendingPlayerChoice(
                ctx.getAttackerPlayerId(),
                ChoiceType.PICK_BENCHED_POKEMON,
                options,
                // minPick = 0 so the player can back out of the supporter; if they skip,
                // Cassius is consumed without effect (matches "play it but pick nothing").
                0,
                1,
                "Cassius: pick a benched Pokémon to shuffle into your deck",
                null,
                chosen -> {
                    if (chosen.isEmpty()) return;
                    String pickedId = chosen.get(0);
                    for (int i = 0; i < bench.size(); i++) {
                        InPlayPokemon p = bench.get(i);
                        if (p != null && pickedId.equals(p.getCard().getId())) {
                            ctx.getAttackerDeck().add(p.getCard());
                            ctx.getAttackerDeck().addAll(p.getEnergies());
                            ctx.getAttackerDeck().addAll(p.getUnderlyingCards());
                            if (p.getTool() != null) {
                                ctx.getAttackerDeck().add(p.getTool());
                            }
                            bench.set(i, null);
                            break;
                        }
                    }
                    ctx.shuffleAttackerDeck();
                }
        ));
    }
}
