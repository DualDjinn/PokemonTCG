package ar.edu.utn.frc.tup.piii.engine.bot;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.TrainerCard;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates the set of {@link GameAction}s that a bot can legally submit for the
 * current {@link GameContext}. Mirrors the validations enforced by
 * {@code RuleValidator} and the {@code MainPhaseState}/{@code AttackPhaseState}
 * handlers in the engine.
 *
 * <p>Used by every bot strategy to bound the search space.
 */
public final class LegalActionGenerator {

    private static final int MAX_BENCH_SIZE = 5;

    private LegalActionGenerator() {
        // utility class
    }

    /**
     * Returns every legal action that the active player (the bot) can submit right now.
     *
     * @param context current battle context oriented from the bot's perspective
     * @return list of legal actions (always non-empty unless the bot has no legal move)
     */
    public static List<GameAction> generate(GameContext context) {
        List<GameAction> actions = new ArrayList<>();
        TurnPhase phase = context.getCurrentPhase();
        if (phase == null) {
            phase = TurnPhase.MAIN;
        }

        switch (phase) {
            case MAIN -> generateMainPhaseActions(context, actions);
            case ATTACK -> generateAttackPhaseActions(context, actions);
            default -> {
                // DRAW and BETWEEN_TURNS are transient; the bot does not act on them.
            }
        }

        return actions;
    }

    private static void generateMainPhaseActions(GameContext context, List<GameAction> actions) {
        List<Card> hand = context.getAttackerHand();
        InPlayPokemon active = context.getAttacker();
        List<InPlayPokemon> bench = context.getAttackerBench();

        if (hand != null) {
            for (Card card : hand) {
                if (card instanceof PokemonCard pokemon && pokemon.getStage() == PokemonStage.BASIC) {
                    int benchSize = bench != null ? bench.size() : 0;
                    if (benchSize < MAX_BENCH_SIZE) {
                        actions.add(GameAction.playBasic(card.getId()));
                    }
                }
            }

            if (!context.isFirstTurn()) {
                for (Card card : hand) {
                    if (card instanceof PokemonCard evolution && evolution.getStage() != PokemonStage.BASIC) {
                        InPlayPokemon target = findEvolutionTarget(evolution, active, bench);
                        if (target != null && target.getCard() != null
                                && canEvolveTarget(target, context)) {
                            actions.add(GameAction.evolve(card.getId(), target.getInstanceId()));
                        }
                    }
                }
            }

            if (canAttachEnergy(context)) {
                for (Card card : hand) {
                    if (!(card instanceof EnergyCard)) {
                        continue;
                    }
                    if (active != null && active.getCard() != null) {
                        actions.add(GameAction.attachEnergy(card.getId(), active.getInstanceId()));
                    }
                    if (bench != null) {
                        for (InPlayPokemon benched : bench) {
                            if (benched != null && benched.getCard() != null) {
                                actions.add(GameAction.attachEnergy(card.getId(), benched.getInstanceId()));
                            }
                        }
                    }
                }
            }

            for (Card card : hand) {
                if (card instanceof TrainerCard trainer) {
                    if (isItem(trainer)) {
                        actions.add(GameAction.playItem(card.getId()));
                    } else if (isSupporter(trainer)
                            && context.getAttackerSupportersPlayedThisTurn() < 1
                            && !context.isAttackerCantPlaySupporters()) {
                        actions.add(GameAction.playSupporter(card.getId()));
                    }
                }
            }
        }

        // Always allow advancing to the attack phase as a fallback option.
        actions.add(GameAction.advanceToAttack());
    }

    private static void generateAttackPhaseActions(GameContext context, List<GameAction> actions) {
        InPlayPokemon active = context.getAttacker();
        if (!context.isFirstTurn()
                && active != null && active.getCard() != null && active.getCard().getAttacks() != null
                && !active.hasStatus(ar.edu.utn.frc.tup.piii.models.StatusCondition.ASLEEP)
                && !active.hasStatus(ar.edu.utn.frc.tup.piii.models.StatusCondition.PARALYZED)
                && !active.isCantAttack()) {
            for (Attack attack : active.getCard().getAttacks()) {
                if (attack == null) {
                    continue;
                }
                if (canPayCost(active, attack) && !isAttackRestricted(active, attack)) {
                    actions.add(GameAction.attack(attack.getName()));
                }
            }
        }

        // Always allow ending the turn without attacking.
        actions.add(GameAction.endTurn());
    }

    private static boolean isAttackRestricted(InPlayPokemon active, Attack attack) {
        return attack.getName() != null
                && attack.getName().equals(active.getCantUseAttackName());
    }

    private static boolean canEvolveTarget(InPlayPokemon target, GameContext context) {
        if (context.getAttackerEvolvedThisTurn().contains(target.getInstanceId())) {
            return false;
        }
        List<PokemonCard> playedThisTurn = context.getAttackerPokemonPlayedThisTurn();
        return playedThisTurn == null || !playedThisTurn.contains(target.getCard());
    }

    private static boolean canAttachEnergy(GameContext context) {
        int allowance = 1 + Math.max(0, context.getAttackerAdditionalEnergyAttachments());
        return context.getAttackerEnergyAttachmentsThisTurn() < allowance;
    }

    private static InPlayPokemon findEvolutionTarget(PokemonCard evolution,
                                                     InPlayPokemon active,
                                                     List<InPlayPokemon> bench) {
        if (evolution.getEvolvesFrom() == null) {
            return null;
        }
        if (active != null && active.getCard() != null
                && evolution.getEvolvesFrom().equals(active.getCard().getName())) {
            return active;
        }
        if (bench != null) {
            for (InPlayPokemon benched : bench) {
                if (benched != null && benched.getCard() != null
                        && evolution.getEvolvesFrom().equals(benched.getCard().getName())) {
                    return benched;
                }
            }
        }
        return null;
    }

    private static boolean canPayCost(InPlayPokemon active, Attack attack) {
        Map<EnergyType, Integer> cost = attack.getEnergyCost();
        if (cost == null || cost.isEmpty()) {
            return true;
        }
        int totalRequired = 0;
        int totalAttached = active.countEnergies(null);
        for (Map.Entry<EnergyType, Integer> entry : cost.entrySet()) {
            EnergyType type = entry.getKey();
            int required = entry.getValue() != null ? entry.getValue() : 0;
            totalRequired += required;
            if (type != EnergyType.COLORLESS && active.countEnergies(type) < required) {
                return false;
            }
        }
        return totalAttached >= totalRequired;
    }

    private static boolean isItem(TrainerCard trainer) {
        String subtype = trainer.getSubtype();
        return subtype != null && subtype.equalsIgnoreCase("Item");
    }

    private static boolean isSupporter(TrainerCard trainer) {
        String subtype = trainer.getSubtype();
        return subtype != null && subtype.equalsIgnoreCase("Supporter");
    }
}
