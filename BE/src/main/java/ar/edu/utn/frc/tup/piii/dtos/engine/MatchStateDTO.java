package ar.edu.utn.frc.tup.piii.dtos.engine;

import ar.edu.utn.frc.tup.piii.engine.log.MatchLogEntry;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.Supertype;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.Builder;

/**
 * Read model returned by {@code GameEngine} for one player's current match view.
 *
 * <p>The DTO reveals the requesting player's full hand while exposing only the
 * opponent hand size. Everything else needed by the client to render the shared
 * battlefield is included symmetrically.
 */
@Data
public class MatchStateDTO {

    private String matchId;
    private String requestedPlayerId;
    private String currentTurnPlayerId;
    private String winnerPlayerId;
    private TurnPhase currentPhase;
    private boolean currentTurn;
    private boolean waitingForActiveSelection;
    private boolean requiresActiveSelection;
    private boolean pendingPrizeSelection;
    private String pendingPrizeSelectionPlayerId;
    private boolean finished;
    private boolean firstTurn;
    private String openingPhase;
    private String openingChooserPlayerId;
    private String openingCoinChooserPlayerId;
    private String openingCoinChoice;
    private String openingCoinResult;
    private String openingStarterPlayerId;
    private int mulliganIteration;
    private boolean setupComplete;
    private List<MatchLogEntry> matchLog;
    private int currentTurnNumber;
    private String mulliganPlayerId;
    private String mulliganBeneficiaryPlayerId;
    private int mulliganBenefitCount;
    private boolean mulliganResolutionPending;
    private PlayerBattlefieldDTO self;
    private PlayerBattlefieldDTO opponent;
    private CardViewDTO activeStadium;
    private CardViewDTO lastPlayedCard;
    private String lastPlayedCardPlayerId;

    /**
     * View of one side of the battlefield from the facade response.
     */
    @Data
    public static class PlayerBattlefieldDTO {
        private String playerId;
        private String trainerName;
        private int handSize;
        private int remainingPrizes;
        private List<CardViewDTO> hand;
        private List<CardViewDTO> prizeCards;
        private InPlayPokemonViewDTO activePokemon;
        private List<InPlayPokemonViewDTO> bench;
        private int mulliganCount;
        private boolean setupReady;
        private long remainingTimeMs;
        private boolean hasAttachedEnergyThisTurn;
        private List<String> abilitiesUsedThisTurn;
        private PendingPlayerChoiceDTO pendingPlayerChoice;
        private String cardBackUrl;
        private List<CardViewDTO> discard;
    }

    /**
     * Minimal card summary suitable for hidden-hand match state responses.
     * For Pokemon cards, additional stats fields are populated.
     */
    @Data
    public static class CardViewDTO {
        private String id;
        private String name;
        private Supertype supertype;
        private String imageUrl;
        private String evolvesFrom;
        private List<String> subtypes;
        private Integer hp;
        private List<AttackViewDTO> attacks;
        private List<AbilityViewDTO> abilities;
        private List<WeaknessDTO> weaknesses;
        private List<ResistanceDTO> resistances;
        private Integer retreatCost;
        private String pokemonType;
        private String gifUrl;
        private List<String> rules;
    }

    /**
     * One attack of a Pokémon.
     */
    @Data
    @Builder
    public static class AttackViewDTO {
        private String name;
        private String damage;
        private List<String> cost;
        private String description;
    }

    /**
     * One ability of a Pokémon.
     */
    @Data
    @Builder
    public static class AbilityViewDTO {
        private String name;
        private String description;
        private String type;
    }

    /**
     * Weakness of a Pokémon card.
     */
    @Data
    @Builder
    public static class WeaknessDTO {
        private EnergyType type;
        private String value;
    }

    /**
     * Resistance of a Pokémon card.
     */
    @Data
    @Builder
    public static class ResistanceDTO {
        private EnergyType type;
        private int reduction;
    }

    /**
     * Battlefield snapshot of a Pokémon currently in play.
     */
    @Data
    public static class InPlayPokemonViewDTO {
        private String cardId;
        private String instanceId;
        private String name;
        private String imageUrl;
        private int maxHp;
        private int currentHp;
        private int damageTaken;
        private int attachedEnergyCount;
        private List<String> attachedEnergyTypes;
        private List<String> attachedEnergyCardIds;
        private List<String> attachedEnergyImageUrls;
        private int retreatCost;
        private String pokemonType;
        private boolean knockedOut;
        private Set<StatusCondition> statuses;
        private List<AttackViewDTO> attacks;
        private List<AbilityViewDTO> abilities;
        private List<WeaknessDTO> weaknesses;
        private List<ResistanceDTO> resistances;
        private String toolCardName;
        private String toolCardId;
        private String toolCardImageUrl;
        private List<String> toolCardRules;
        private String gifUrl;
    }
}
