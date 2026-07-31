package ar.edu.utn.frc.tup.piii.engine.snapshot;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import ar.edu.utn.frc.tup.piii.services.CardRegistryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Serializable snapshot of the match state, used for persistence and reconnection.
 * Cards are stored by their ID strings and reloaded from {@link CardRegistryService}.
 */
public class GameSnapshot {

    private int turnNumber;
    private String currentPhase;
    private boolean firstTurn;
    private int attackerEnergyAttachmentsThisTurn;
    private int attackerAdditionalEnergyAttachments;
    private int attackerSupportersPlayedThisTurn;
    private boolean attackerCantPlaySupporters;
    private boolean prizesDealt;

    private InPlaySnapshot attacker;
    private InPlaySnapshot defender;
    private List<InPlaySnapshot> attackerBench;
    private List<InPlaySnapshot> defenderBench;

    private List<String> attackerHand;
    private List<String> defenderHand;
    private List<String> attackerDeck;
    private List<String> defenderDeck;
    private List<String> attackerDiscard;
    private List<String> defenderDiscard;
    private List<String> attackerPrizes;
    private List<String> defenderPrizes;

    private List<String> attackerPokemonPlayedThisTurn;
    // attackerEvolvedThisTurn stores instanceIds of evolved Pokemon
    private List<String> attackerEvolvedThisTurn;
    // Match log entries
    private List<MatchLogEntryData> matchLog;

    // Disconnect tracking
    private String disconnectedPlayerId;
    private long disconnectedAtMs;

    public GameSnapshot() {}

    public static GameSnapshot from(GameContext ctx) {
        GameSnapshot s = new GameSnapshot();
        s.turnNumber = ctx.getTurnNumber();
        s.currentPhase = ctx.getCurrentPhase() != null ? ctx.getCurrentPhase().name() : null;
        s.firstTurn = ctx.isFirstTurn();
        s.attackerEnergyAttachmentsThisTurn = ctx.getAttackerEnergyAttachmentsThisTurn();
        s.attackerAdditionalEnergyAttachments = ctx.getAttackerAdditionalEnergyAttachments();
        s.attackerSupportersPlayedThisTurn = ctx.getAttackerSupportersPlayedThisTurn();
        s.attackerCantPlaySupporters = ctx.isAttackerCantPlaySupporters();
        s.prizesDealt = ctx.isPrizesDealt();

        s.attacker = ctx.getAttacker() != null ? InPlaySnapshot.from(ctx.getAttacker()) : null;
        s.defender = ctx.getDefender() != null ? InPlaySnapshot.from(ctx.getDefender()) : null;
        s.attackerBench = mapBench(ctx.getAttackerBench());
        s.defenderBench = mapBench(ctx.getDefenderBench());

        s.attackerHand = mapCards(ctx.getAttackerHand());
        s.defenderHand = mapCards(ctx.getDefenderHand());
        s.attackerDeck = mapCards(ctx.getAttackerDeck());
        s.defenderDeck = mapCards(ctx.getDefenderDeck());
        s.attackerDiscard = mapCards(ctx.getAttackerDiscard());
        s.defenderDiscard = mapCards(ctx.getDefenderDiscard());
        s.attackerPrizes = mapCards(ctx.getAttackerPrizes());
        s.defenderPrizes = mapCards(ctx.getDefenderPrizes());

        s.attackerPokemonPlayedThisTurn = ctx.getAttackerPokemonPlayedThisTurn() != null
                ? ctx.getAttackerPokemonPlayedThisTurn().stream().map(Card::getId).collect(Collectors.toList())
                : new ArrayList<>();
        s.attackerEvolvedThisTurn = ctx.getAttackerEvolvedThisTurn() != null
                ? new ArrayList<>(ctx.getAttackerEvolvedThisTurn())
                : new ArrayList<>();

        if (ctx.getMatchLog() != null) {
            s.matchLog = ctx.getMatchLog().stream()
                    .map(e -> new MatchLogEntryData(e.turnNumber(), e.playerId(),
                            e.actionType(), e.description(), e.timestampMs()))
                    .collect(Collectors.toList());
        }
        return s;
    }

    public GameContext toGameContext(CardRegistryService cardRegistry) {
        GameContext ctx = new GameContext();
        ctx.setTurnNumber(turnNumber);
        ctx.setCurrentPhase(currentPhase != null ? TurnPhase.valueOf(currentPhase) : TurnPhase.MAIN);
        ctx.setFirstTurn(firstTurn);
        ctx.setAttackerEnergyAttachmentsThisTurn(attackerEnergyAttachmentsThisTurn);
        ctx.setAttackerAdditionalEnergyAttachments(attackerAdditionalEnergyAttachments);
        ctx.setAttackerSupportersPlayedThisTurn(attackerSupportersPlayedThisTurn);
        ctx.setAttackerCantPlaySupporters(attackerCantPlaySupporters);
        ctx.setPrizesDealt(prizesDealt);

        ctx.setAttacker(attacker != null ? attacker.toInPlayPokemon(cardRegistry) : null);
        ctx.setDefender(defender != null ? defender.toInPlayPokemon(cardRegistry) : null);
        ctx.setAttackerBench(restoreBench(attackerBench, cardRegistry));
        ctx.setDefenderBench(restoreBench(defenderBench, cardRegistry));

        ctx.setAttackerHand(restoreCardList(attackerHand, cardRegistry));
        ctx.setDefenderHand(restoreCardList(defenderHand, cardRegistry));
        ctx.setAttackerDeck(restoreCardList(attackerDeck, cardRegistry));
        ctx.setDefenderDeck(restoreCardList(defenderDeck, cardRegistry));
        ctx.setAttackerDiscard(restoreCardList(attackerDiscard, cardRegistry));
        ctx.setDefenderDiscard(restoreCardList(defenderDiscard, cardRegistry));
        ctx.setAttackerPrizes(restoreCardList(attackerPrizes, cardRegistry));
        ctx.setDefenderPrizes(restoreCardList(defenderPrizes, cardRegistry));

        if (attackerPokemonPlayedThisTurn != null) {
            List<PokemonCard> played = new ArrayList<>();
            for (String id : attackerPokemonPlayedThisTurn) {
                Card c = cardRegistry.getById(id);
                if (c instanceof PokemonCard p) played.add(p);
            }
            ctx.setAttackerPokemonPlayedThisTurn(played);
        }
        if (attackerEvolvedThisTurn != null) {
            ctx.setAttackerEvolvedThisTurn(new java.util.HashSet<>(attackerEvolvedThisTurn));
        }

        // Restore match log if we had it (it's also persisted separately)
        // The context starts fresh; we can add log entries back during replay
        return ctx;
    }

    // ---- Getters / Setters for Jackson ----

    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int v) { turnNumber = v; }
    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String v) { currentPhase = v; }
    public boolean isFirstTurn() { return firstTurn; }
    public void setFirstTurn(boolean v) { firstTurn = v; }
    public int getAttackerEnergyAttachmentsThisTurn() { return attackerEnergyAttachmentsThisTurn; }
    public void setAttackerEnergyAttachmentsThisTurn(int v) { attackerEnergyAttachmentsThisTurn = v; }
    public int getAttackerAdditionalEnergyAttachments() { return attackerAdditionalEnergyAttachments; }
    public void setAttackerAdditionalEnergyAttachments(int v) { attackerAdditionalEnergyAttachments = v; }
    public int getAttackerSupportersPlayedThisTurn() { return attackerSupportersPlayedThisTurn; }
    public void setAttackerSupportersPlayedThisTurn(int v) { attackerSupportersPlayedThisTurn = v; }
    public boolean isAttackerCantPlaySupporters() { return attackerCantPlaySupporters; }
    public void setAttackerCantPlaySupporters(boolean v) { attackerCantPlaySupporters = v; }
    public boolean isPrizesDealt() { return prizesDealt; }
    public void setPrizesDealt(boolean v) { prizesDealt = v; }
    public InPlaySnapshot getAttacker() { return attacker; }
    public void setAttacker(InPlaySnapshot v) { attacker = v; }
    public InPlaySnapshot getDefender() { return defender; }
    public void setDefender(InPlaySnapshot v) { defender = v; }
    public List<InPlaySnapshot> getAttackerBench() { return attackerBench; }
    public void setAttackerBench(List<InPlaySnapshot> v) { attackerBench = v; }
    public List<InPlaySnapshot> getDefenderBench() { return defenderBench; }
    public void setDefenderBench(List<InPlaySnapshot> v) { defenderBench = v; }
    public List<String> getAttackerHand() { return attackerHand; }
    public void setAttackerHand(List<String> v) { attackerHand = v; }
    public List<String> getDefenderHand() { return defenderHand; }
    public void setDefenderHand(List<String> v) { defenderHand = v; }
    public List<String> getAttackerDeck() { return attackerDeck; }
    public void setAttackerDeck(List<String> v) { attackerDeck = v; }
    public List<String> getDefenderDeck() { return defenderDeck; }
    public void setDefenderDeck(List<String> v) { defenderDeck = v; }
    public List<String> getAttackerDiscard() { return attackerDiscard; }
    public void setAttackerDiscard(List<String> v) { attackerDiscard = v; }
    public List<String> getDefenderDiscard() { return defenderDiscard; }
    public void setDefenderDiscard(List<String> v) { defenderDiscard = v; }
    public List<String> getAttackerPrizes() { return attackerPrizes; }
    public void setAttackerPrizes(List<String> v) { attackerPrizes = v; }
    public List<String> getDefenderPrizes() { return defenderPrizes; }
    public void setDefenderPrizes(List<String> v) { defenderPrizes = v; }
    public List<String> getAttackerPokemonPlayedThisTurn() { return attackerPokemonPlayedThisTurn; }
    public void setAttackerPokemonPlayedThisTurn(List<String> v) { attackerPokemonPlayedThisTurn = v; }
    public List<String> getAttackerEvolvedThisTurn() { return attackerEvolvedThisTurn; }
    public void setAttackerEvolvedThisTurn(List<String> v) { attackerEvolvedThisTurn = v; }
    public List<MatchLogEntryData> getMatchLog() { return matchLog; }
    public void setMatchLog(List<MatchLogEntryData> v) { matchLog = v; }
    public String getDisconnectedPlayerId() { return disconnectedPlayerId; }
    public void setDisconnectedPlayerId(String v) { disconnectedPlayerId = v; }
    public long getDisconnectedAtMs() { return disconnectedAtMs; }
    public void setDisconnectedAtMs(long v) { disconnectedAtMs = v; }

    // ---- Helpers ----

    private static List<InPlaySnapshot> mapBench(List<InPlayPokemon> bench) {
        if (bench == null) return new ArrayList<>();
        List<InPlaySnapshot> result = new ArrayList<>();
        for (InPlayPokemon p : bench) {
            result.add(p != null ? InPlaySnapshot.from(p) : null);
        }
        return result;
    }

    private static List<String> mapCards(List<? extends Card> cards) {
        if (cards == null) return new ArrayList<>();
        return cards.stream().filter(Objects::nonNull).map(Card::getId).collect(Collectors.toList());
    }

    private static List<InPlayPokemon> restoreBench(List<InPlaySnapshot> bench, CardRegistryService reg) {
        if (bench == null) return new ArrayList<>();
        List<InPlayPokemon> result = new ArrayList<>();
        for (InPlaySnapshot s : bench) {
            result.add(s != null ? s.toInPlayPokemon(reg) : null);
        }
        return result;
    }

    private static List<Card> restoreCardList(List<String> ids, CardRegistryService reg) {
        if (ids == null) return new ArrayList<>();
        return ids.stream().map(reg::getById).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Lightweight DTO for match log entries inside the snapshot.
     */
    public static class MatchLogEntryData {
        private int turnNumber;
        private String playerId;
        private String actionType;
        private String description;
        private long timestampMs;

        public MatchLogEntryData() {}
        public MatchLogEntryData(int turnNumber, String playerId, String actionType, String description, long timestampMs) {
            this.turnNumber = turnNumber;
            this.playerId = playerId;
            this.actionType = actionType;
            this.description = description;
            this.timestampMs = timestampMs;
        }
        public int getTurnNumber() { return turnNumber; }
        public void setTurnNumber(int v) { turnNumber = v; }
        public String getPlayerId() { return playerId; }
        public void setPlayerId(String v) { playerId = v; }
        public String getActionType() { return actionType; }
        public void setActionType(String v) { actionType = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { description = v; }
        public long getTimestampMs() { return timestampMs; }
        public void setTimestampMs(long v) { timestampMs = v; }
    }
}
