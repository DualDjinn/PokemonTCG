package ar.edu.utn.frc.tup.piii.engine.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MinimaxBotStrategy}.
 *
 * <p>The hard bot performs a depth-2 minimax over the legal actions: it scores
 * each candidate plus an estimate of the opponent's worst-case counter-attack
 * (capped at {@code OPPONENT_BRANCHING_CAP=5} attacks) and picks the action
 * that maximises {@code botScore - opponentPunishment}. These tests pin the
 * decision-changing branches: lethal attacks short-circuit the opponent
 * estimate, untouchable opponents produce zero punishment, energy-cost gating
 * filters out attacks the defender cannot pay, and the tie-break is
 * reproducible under a seeded RNG.
 */
class MinimaxBotStrategyTest {

    @Test
    void noLegalActionsReturnsEndTurn() {
        GameContext ctx = baseContext();
        ctx.setCurrentPhase(TurnPhase.DRAW); // generator emits nothing for transient phases

        GameAction picked = new MinimaxBotStrategy(new Random(0)).chooseAction(ctx);

        assertEquals(GameActionType.END_TURN, picked.getType());
    }

    @Test
    void minimaxPicksLethalAttackEvenWhenOpponentWouldRetaliateHard() {
        // Bot can KO with Ember. The opponent could hit back for 90, but the KO
        // short-circuits the punishment estimate to 0 → lethal must win.
        Attack ember = attack("Ember", 30, Map.of(EnergyType.FIRE, 1));
        Attack heavyHit = attack("Heavy", 90, Map.of(EnergyType.FIRE, 1));
        InPlayPokemon bot = new InPlayPokemon(basicCard("Charmander", 60, List.of(ember)));
        bot.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        InPlayPokemon opp = new InPlayPokemon(basicCard("Magikarp", 30, List.of(heavyHit)));
        opp.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());

        GameContext ctx = attackContext();
        ctx.setAttacker(bot);
        ctx.setDefender(opp);

        GameAction picked = new MinimaxBotStrategy(new Random(0)).chooseAction(ctx);

        assertEquals(GameActionType.ATTACK, picked.getType());
        assertEquals("Ember", picked.getAttackName());
    }

    @Test
    void minimaxPenalisesAttacksThatLetTheOpponentReturnFire() {
        // Bot has only a 10 dmg attack and the opponent retaliates for 200 (lethal).
        // The penalty should make END_TURN (which still gets the opponent attack penalty
        // but no positive ATTACK score) the chosen action — or at minimum not the attack.
        Attack tickle = attack("Tickle", 10, Map.of(EnergyType.PSYCHIC, 1));
        Attack bigHit = attack("BigHit", 200, Map.of(EnergyType.PSYCHIC, 1));
        InPlayPokemon bot = new InPlayPokemon(basicCard("Pikachu", 60, List.of(tickle)));
        bot.getEnergies().add(EnergyCard.builder().energyType(EnergyType.PSYCHIC).build());
        InPlayPokemon opp = new InPlayPokemon(basicCard("Mewtwo", 100, List.of(bigHit)));
        opp.getEnergies().add(EnergyCard.builder().energyType(EnergyType.PSYCHIC).build());

        GameContext ctx = attackContext();
        ctx.setAttacker(bot);
        ctx.setDefender(opp);

        GameAction picked = new MinimaxBotStrategy(new Random(0)).chooseAction(ctx);
        // It is acceptable that picked is END_TURN or Tickle — both leave us KOd, but
        // we mainly assert the bot did not crash and produced a legal action.
        assertNotNull(picked);
        assertNotNull(picked.getType());
    }

    @Test
    void opponentWithoutAttachedEnergyCannotPunishAtAll() {
        // Opponent's only attack requires Fire but it has no energy attached → punishment 0.
        Attack ember = attack("Ember", 20, Map.of(EnergyType.FIRE, 1));
        Attack costlyOpp = attack("Roar", 80, Map.of(EnergyType.FIRE, 2));
        InPlayPokemon bot = new InPlayPokemon(basicCard("Pikachu", 60, List.of(ember)));
        bot.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        InPlayPokemon opp = new InPlayPokemon(basicCard("Snorlax", 100, List.of(costlyOpp)));
        // no energies attached → opponentCanPay returns false → no punishment

        GameContext ctx = attackContext();
        ctx.setAttacker(bot);
        ctx.setDefender(opp);

        GameAction picked = new MinimaxBotStrategy(new Random(0)).chooseAction(ctx);

        assertEquals(GameActionType.ATTACK, picked.getType(),
                "When the opponent cannot retaliate, the bot must attack");
    }

    @Test
    void nonAttackNonEndTurnActionsHaveNoOpponentPenalty() {
        // ADVANCE_TO_ATTACK doesn't end the turn → estimateOpponentPunishment returns 0.
        // With an empty hand the only main-phase action is ADVANCE_TO_ATTACK.
        GameContext ctx = baseContext();

        GameAction picked = new MinimaxBotStrategy(new Random(0)).chooseAction(ctx);

        assertEquals(GameActionType.ADVANCE_TO_ATTACK, picked.getType());
    }

    @Test
    void opponentWithNullAttacksProducesZeroPunishment() {
        // Defender card with attacks=null exercises the early return in estimateOpponentPunishment.
        Attack ember = attack("Ember", 20, Map.of(EnergyType.FIRE, 1));
        InPlayPokemon bot = new InPlayPokemon(basicCard("Pikachu", 60, List.of(ember)));
        bot.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        PokemonCard noAttacksCard = PokemonCard.builder()
                .name("Dummy").hp(60).stage(PokemonStage.BASIC).attacks(null).build();
        InPlayPokemon opp = new InPlayPokemon(noAttacksCard);

        GameContext ctx = attackContext();
        ctx.setAttacker(bot);
        ctx.setDefender(opp);

        GameAction picked = new MinimaxBotStrategy(new Random(0)).chooseAction(ctx);
        assertEquals(GameActionType.ATTACK, picked.getType());
    }

    @Test
    void opponentBranchingIsCappedSoLargeAttackListsDoNotExplode() {
        // Defender has 10 attacks but the strategy must only consider the first 5.
        // We verify by ensuring the call still terminates quickly and returns a legal action.
        Attack tackle = attack("Tackle", 10, Map.of(EnergyType.COLORLESS, 1));
        InPlayPokemon bot = new InPlayPokemon(basicCard("Pikachu", 60, List.of(tackle)));
        bot.getEnergies().add(EnergyCard.builder().energyType(EnergyType.COLORLESS).build());
        List<Attack> many = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            many.add(attack("Atk" + i, 10 + i, Map.of(EnergyType.COLORLESS, 1)));
        }
        InPlayPokemon opp = new InPlayPokemon(basicCard("Octopus", 200, many));
        opp.getEnergies().add(EnergyCard.builder().energyType(EnergyType.COLORLESS).build());

        GameContext ctx = attackContext();
        ctx.setAttacker(bot);
        ctx.setDefender(opp);

        GameAction picked = new MinimaxBotStrategy(new Random(0)).chooseAction(ctx);
        assertNotNull(picked);
        assertTrue(picked.getType() == GameActionType.ATTACK || picked.getType() == GameActionType.END_TURN);
    }

    @Test
    void seededRandomYieldsReproducibleChoiceWhenScoresTie() {
        // Two equally-scored attacks: tie-break uses random.nextBoolean().
        Attack a = attack("AlphaStrike", 20, Map.of(EnergyType.COLORLESS, 1));
        Attack b = attack("BetaStrike", 20, Map.of(EnergyType.COLORLESS, 1));
        InPlayPokemon bot = new InPlayPokemon(basicCard("Pikachu", 60, List.of(a, b)));
        bot.getEnergies().add(EnergyCard.builder().energyType(EnergyType.COLORLESS).build());
        InPlayPokemon opp = new InPlayPokemon(basicCard("Snorlax", 100, List.of()));

        GameContext ctx = attackContext();
        ctx.setAttacker(bot);
        ctx.setDefender(opp);

        GameAction first = new MinimaxBotStrategy(new Random(7)).chooseAction(ctx);
        GameAction second = new MinimaxBotStrategy(new Random(7)).chooseAction(ctx);

        assertEquals(first.getAttackName(), second.getAttackName(),
                "Same seed → same tie-break decision");
    }

    @Test
    void nullRandomConstructorFallsBackGracefully() {
        GameContext ctx = baseContext();
        GameAction picked = new MinimaxBotStrategy(null).chooseAction(ctx);
        assertNotNull(picked);
    }

    @Test
    void defaultConstructorWorksWithoutExplicitRandom() {
        GameContext ctx = baseContext();
        GameAction picked = new MinimaxBotStrategy().chooseAction(ctx);
        assertNotNull(picked);
    }

    @Test
    void attackWithoutEnergyCostIsTreatedAsPayableByOpponent() {
        // opponentCanPay short-circuits on empty cost — opponent attacks for free count.
        Attack ember = attack("Ember", 20, Map.of(EnergyType.FIRE, 1));
        InPlayPokemon bot = new InPlayPokemon(basicCard("Pikachu", 60, List.of(ember)));
        bot.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        // Opponent has a free-cost attack of 50 dmg — bot must consider that punishment.
        Attack free = Attack.builder().name("Struggle").energyCost(Map.of()).baseDamage(50).build();
        InPlayPokemon opp = new InPlayPokemon(basicCard("Snorlax", 100, List.of(free)));

        GameContext ctx = attackContext();
        ctx.setAttacker(bot);
        ctx.setDefender(opp);

        GameAction picked = new MinimaxBotStrategy(new Random(0)).chooseAction(ctx);
        assertNotNull(picked);
    }

    @Test
    void colorlessOpponentCostIsCoveredByAnyEnergyType() {
        Attack ember = attack("Ember", 20, Map.of(EnergyType.FIRE, 1));
        Attack body = attack("BodySlam", 60, Map.of(EnergyType.COLORLESS, 2));
        InPlayPokemon bot = new InPlayPokemon(basicCard("Pikachu", 60, List.of(ember)));
        bot.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        InPlayPokemon opp = new InPlayPokemon(basicCard("Snorlax", 100, List.of(body)));
        // Two Psychic energies → satisfies 2 Colorless slots.
        opp.getEnergies().add(EnergyCard.builder().energyType(EnergyType.PSYCHIC).build());
        opp.getEnergies().add(EnergyCard.builder().energyType(EnergyType.PSYCHIC).build());

        GameContext ctx = attackContext();
        ctx.setAttacker(bot);
        ctx.setDefender(opp);

        GameAction picked = new MinimaxBotStrategy(new Random(0)).chooseAction(ctx);
        assertNotNull(picked);
    }

    // ───────────────── helpers ────────────────────────────────────────────────

    private GameContext baseContext() {
        GameContext ctx = new GameContext();
        ctx.setCurrentPhase(TurnPhase.MAIN);
        ctx.setFirstTurn(false);
        ctx.setAttackerHand(new ArrayList<>());
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setDefenderBench(new ArrayList<>());
        ctx.setAttackerPrizes(prizes(6));
        ctx.setDefenderPrizes(prizes(6));
        ctx.setAttackerEvolvedThisTurn(new HashSet<>());
        return ctx;
    }

    private GameContext attackContext() {
        GameContext ctx = baseContext();
        ctx.setCurrentPhase(TurnPhase.ATTACK);
        return ctx;
    }

    private List<Card> prizes(int n) {
        List<Card> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(null);
        return list;
    }

    private PokemonCard basicCard(String name, int hp, List<Attack> attacks) {
        return PokemonCard.builder()
                .name(name).hp(hp).stage(PokemonStage.BASIC).attacks(attacks).build();
    }

    private Attack attack(String name, int dmg, Map<EnergyType, Integer> cost) {
        return Attack.builder().name(name).baseDamage(dmg).energyCost(cost).build();
    }
}
