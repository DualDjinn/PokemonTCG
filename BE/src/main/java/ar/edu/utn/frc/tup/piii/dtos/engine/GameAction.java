package ar.edu.utn.frc.tup.piii.dtos.engine;

import java.util.List;
import lombok.Data;

/**
 * Command DTO consumed by {@code GameEngine} to advance a match.
 *
 * <p>The structure intentionally stays small so controllers only need to express
 * the player's intent while the engine keeps ownership of all rule enforcement
 * and state transitions.
 */
@Data
public class GameAction {

    private GameActionType type;
    private String attackName;

    /**
     * Identifier of the card the action operates on (hand card to play, energy to attach, etc.).
     */
    private String cardId;

    /**
     * Identifier of the in-play Pokémon targeted by the action (evolution target, energy
     * recipient). {@code null} for actions that need no target.
     */
    private String targetCardId;

    /**
     * IDs of energy cards the attacker wants to consume as part of the attack cost.
     */
    private List<String> energyCardIds;

    /**
     * Coin side chosen by the opening player. Used only during the opening flow.
     */
    private String coinSide;

    /**
     * Starter chosen after winning the opening coin toss.
     */
    private String starterPlayerId;

    /**
     * Cards the player picked (or ordered) in response to a pending player-choice prompt.
     * For reorder prompts this is the full ordered list; for pick prompts it is the picked subset.
     */
    private List<String> chosenCardIds;

    /**
     * Creates an action that puts a Basic Pokémon from the hand onto the bench.
     *
     * @param cardId hand card identifier of the Basic Pokémon to play
     * @return play-basic action DTO
     */
    public static GameAction playBasic(String cardId) {
        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_BASIC);
        action.setCardId(cardId);
        return action;
    }

    /**
     * Creates an action that evolves an in-play Pokémon using a card from the hand.
     *
     * @param cardId hand card identifier of the evolution card
     * @param targetCardId card identifier of the in-play Pokémon to evolve
     * @return evolve action DTO
     */
    public static GameAction evolve(String cardId, String targetCardId) {
        GameAction action = new GameAction();
        action.setType(GameActionType.EVOLVE);
        action.setCardId(cardId);
        action.setTargetCardId(targetCardId);
        return action;
    }

    /**
     * Creates an action that attaches an energy card from the hand to an in-play Pokémon.
     *
     * @param cardId hand card identifier of the energy card
     * @param targetCardId card identifier of the in-play Pokémon receiving the energy
     * @return attach-energy action DTO
     */
    public static GameAction attachEnergy(String cardId, String targetCardId) {
        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_ENERGY);
        action.setCardId(cardId);
        action.setTargetCardId(targetCardId);
        return action;
    }

    /**
     * Creates an action that plays an Item card from the hand.
     *
     * @param cardId hand card identifier of the Item card
     * @return play-item action DTO
     */
    public static GameAction playItem(String cardId) {
        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_ITEM);
        action.setCardId(cardId);
        return action;
    }

    /**
     * Creates an action that plays a Supporter card from the hand.
     *
     * @param cardId hand card identifier of the Supporter card
     * @return play-supporter action DTO
     */
    public static GameAction playSupporter(String cardId) {
        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_SUPPORTER);
        action.setCardId(cardId);
        return action;
    }

    /**
     * Creates an action that plays a Stadium card from the hand.
     *
     * @param cardId hand card identifier of the Stadium card
     * @return play-stadium action DTO
     */
    public static GameAction playStadium(String cardId) {
        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_STADIUM);
        action.setCardId(cardId);
        return action;
    }

    /**
     * Creates an action that attaches a Pokémon Tool from the hand to a target Pokémon.
     *
     * @param cardId hand card identifier of the Pokémon Tool
     * @param targetCardId instance ID of the target Pokémon
     * @return attach-pokemon-tool action DTO
     */
    public static GameAction attachPokemonTool(String cardId, String targetCardId) {
        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_TOOL);
        action.setCardId(cardId);
        action.setTargetCardId(targetCardId);
        return action;
    }

    /**
     * Creates an action that moves the current turn from the main phase into the attack phase.
     *
     * @return phase-transition action DTO
     */
    public static GameAction advanceToAttack() {
        GameAction action = new GameAction();
        action.setType(GameActionType.ADVANCE_TO_ATTACK);
        return action;
    }

    /**
     * Creates an attack action targeting the named attack on the current active Pokémon.
     *
     * @param attackName attack name to execute
     * @return attack action DTO
     */
    public static GameAction attack(String attackName) {
        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACK);
        action.setAttackName(attackName);
        return action;
    }

    /**
     * Creates an action that enters the attack phase and immediately resolves the named attack
     * using the specified energy cards to pay the cost.
     *
     * @param attackName    attack name to execute
     * @param energyCardIds IDs of attached energy cards to consume as cost
     * @return execute-attack action DTO
     */
    public static GameAction executeAttack(String attackName, List<String> energyCardIds) {
        GameAction action = new GameAction();
        action.setType(GameActionType.EXECUTE_ATTACK);
        action.setAttackName(attackName);
        action.setEnergyCardIds(energyCardIds);
        return action;
    }

    /**
     * Creates an action that finishes the current turn without attacking.
     *
     * @return end-turn action DTO
     */
    public static GameAction endTurn() {
        GameAction action = new GameAction();
        action.setType(GameActionType.END_TURN);
        return action;
    }

    /**
     * Creates an opening action that chooses heads or tails.
     */
    public static GameAction chooseCoinSide(String coinSide) {
        GameAction action = new GameAction();
        action.setType(GameActionType.CHOOSE_COIN_SIDE);
        action.setCoinSide(coinSide);
        return action;
    }

    /**
     * Creates an opening action that selects which player starts first.
     */
    public static GameAction chooseFirstTurn(String starterPlayerId) {
        GameAction action = new GameAction();
        action.setType(GameActionType.CHOOSE_FIRST_TURN);
        action.setStarterPlayerId(starterPlayerId);
        return action;
    }

    /**
     * Creates an action that resolves a pending player-choice prompt.
     *
     * @param chosenCardIds card identifiers picked (or ordered) by the player
     */
    public static GameAction resolvePlayerChoice(List<String> chosenCardIds) {
        GameAction action = new GameAction();
        action.setType(GameActionType.RESOLVE_PLAYER_CHOICE);
        action.setChosenCardIds(chosenCardIds);
        return action;
    }
}
