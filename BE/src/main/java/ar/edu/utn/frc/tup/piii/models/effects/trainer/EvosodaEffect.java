package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import ar.edu.utn.frc.tup.piii.engine.choice.ChoiceType;
import ar.edu.utn.frc.tup.piii.engine.choice.PendingPlayerChoice;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.TrainerEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Evosoda (xy1-116): Search your deck for a card that evolves from 1 of
 * your Pokémon and put it onto that Pokémon. (This counts as evolving
 * that Pokémon.) Shuffle your deck afterward.
 *
 * <p>Raises a {@link ChoiceType#PICK_FROM_DECK_TOP_N} prompt when there
 * are multiple valid evolution targets, letting the player choose.
 */
public class EvosodaEffect implements TrainerEffect {

    @Override
    public void apply(GameContext ctx) {
        // Card rule: can't use on the first turn.
        if (ctx.isFirstTurn()) {
            return;
        }

        List<Card> deck = ctx.getAttackerDeck();
        List<InPlayPokemon> candidates = new ArrayList<>();
        if (ctx.getAttacker() != null
                && isEligible(ctx.getAttacker(), ctx)) {
            candidates.add(ctx.getAttacker());
        }
        for (InPlayPokemon benched : ctx.getAttackerBench()) {
            if (benched != null && isEligible(benched, ctx)) {
                candidates.add(benched);
            }
        }
        if (candidates.isEmpty()) {
            ctx.shuffleAttackerDeck();
            return;
        }

        // Find all evolution cards in the deck that can evolve any candidate.
        List<PokemonCard> evolutionOptions = new ArrayList<>();
        for (Card card : deck) {
            if (card instanceof PokemonCard evoCard) {
                for (InPlayPokemon candidate : candidates) {
                    if (canEvolveInto(candidate.getCard(), evoCard)) {
                        evolutionOptions.add(evoCard);
                        break;
                    }
                }
            }
        }
        if (evolutionOptions.isEmpty()) {
            ctx.shuffleAttackerDeck();
            return;
        }

        // Single candidate + single evolution → auto-apply.
        if (evolutionOptions.size() == 1 && candidates.size() == 1) {
            deck.remove(evolutionOptions.get(0));
            doEvolve(ctx, candidates.get(0), evolutionOptions.get(0));
            ctx.shuffleAttackerDeck();
            return;
        }

        // Multiple options → let the player choose the evolution card.
        ctx.setPendingPlayerChoice(new PendingPlayerChoice(
                ctx.getAttackerPlayerId(),
                ChoiceType.PICK_FROM_DECK_TOP_N,
                new ArrayList<>(evolutionOptions),
                1,
                1,
                "Evosoda: elegí una evolución del mazo",
                null,
                chosen -> {
                    if (!chosen.isEmpty()) {
                        Card picked = evolutionOptions.stream()
                                .filter(c -> chosen.get(0).equals(c.getId()))
                                .findFirst().orElse(null);
                        if (picked instanceof PokemonCard evoCard) {
                            for (InPlayPokemon candidate : candidates) {
                                if (canEvolveInto(candidate.getCard(), evoCard)) {
                                    deck.remove(picked);
                                    doEvolve(ctx, candidate, evoCard);
                                    break;
                                }
                            }
                        }
                    }
                    ctx.shuffleAttackerDeck();
                }
        ));
    }

    private boolean isEligible(InPlayPokemon pokemon, GameContext ctx) {
        return !wasPlayedThisTurn(pokemon.getCard(), ctx)
                && !ctx.getAttackerEvolvedThisTurn().contains(pokemon.getInstanceId());
    }

    private boolean wasPlayedThisTurn(PokemonCard base, GameContext ctx) {
        List<PokemonCard> playedThisTurn = ctx.getAttackerPokemonPlayedThisTurn();
        return playedThisTurn != null && playedThisTurn.contains(base);
    }

    private boolean canEvolveInto(PokemonCard base, PokemonCard evolution) {
        String evolvesFrom = evolution.getEvolvesFrom();
        return evolvesFrom != null && evolvesFrom.equals(base.getName());
    }

    private void doEvolve(GameContext ctx, InPlayPokemon target, PokemonCard evolutionCard) {
        InPlayPokemon evolved = evolve(target, evolutionCard);
        replaceInPlay(ctx, target, evolved);
        ctx.getAttackerEvolvedThisTurn().add(target.getInstanceId());
    }

    private InPlayPokemon evolve(InPlayPokemon target, PokemonCard evolutionCard) {
        InPlayPokemon evolved = new InPlayPokemon(evolutionCard);
        evolved.setDamageTaken(target.getDamageTaken());
        evolved.setEnergies(target.getEnergies());
        evolved.setTool(target.getTool());
        evolved.setUnderlyingCards(target.getUnderlyingCards());
        evolved.getUnderlyingCards().add(target.getCard());
        evolved.clearStatuses();
        return evolved;
    }

    private void replaceInPlay(GameContext ctx, InPlayPokemon target, InPlayPokemon replacement) {
        if (ctx.getAttacker() == target) {
            ctx.setAttacker(replacement);
            return;
        }
        List<InPlayPokemon> bench = ctx.getAttackerBench();
        int index = bench.indexOf(target);
        if (index >= 0) {
            bench.set(index, replacement);
        }
    }
}
