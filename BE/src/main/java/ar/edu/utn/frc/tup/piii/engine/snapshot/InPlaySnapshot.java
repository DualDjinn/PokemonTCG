package ar.edu.utn.frc.tup.piii.engine.snapshot;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.services.CardRegistryService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializable snapshot of an {@link InPlayPokemon}.
 * Card references are stored by ID and reloaded from the registry.
 */
public class InPlaySnapshot {

    private String cardId;
    private String instanceId;
    private int damageTaken;
    private List<String> energyCardIds;
    private String toolCardId;
    private List<String> underlyingCardIds;
    private boolean preventAllEffects;
    private boolean preventAllIncomingDamage;
    private int preventIncomingDamageThreshold;
    private int incomingDamageReduction;
    private boolean cantRetreat;
    private boolean cantAttack;
    private String cantUseAttackName;
    private boolean ifAttacksFlipCoin;
    private boolean abilitiesDisabled;
    private int metalWallopBonus;

    public InPlaySnapshot() {}

    public static InPlaySnapshot from(InPlayPokemon p) {
        InPlaySnapshot s = new InPlaySnapshot();
        s.cardId = p.getCard() != null ? p.getCard().getId() : null;
        s.instanceId = p.getInstanceId();
        s.damageTaken = p.getDamageTaken();
        s.energyCardIds = p.getEnergies() != null
                ? p.getEnergies().stream().map(Card::getId).collect(Collectors.toList())
                : new ArrayList<>();
        s.toolCardId = p.getTool() != null ? p.getTool().getId() : null;
        s.underlyingCardIds = p.getUnderlyingCards() != null
                ? p.getUnderlyingCards().stream().map(Card::getId).collect(Collectors.toList())
                : new ArrayList<>();
        s.preventAllEffects = p.isPreventAllEffects();
        s.preventAllIncomingDamage = p.isPreventAllIncomingDamage();
        s.preventIncomingDamageThreshold = p.getPreventIncomingDamageThreshold();
        s.incomingDamageReduction = p.getIncomingDamageReduction();
        s.cantRetreat = p.isCantRetreat();
        s.cantAttack = p.isCantAttack();
        s.cantUseAttackName = p.getCantUseAttackName();
        s.ifAttacksFlipCoin = p.isIfAttacksFlipCoin();
        s.abilitiesDisabled = p.isAbilitiesDisabled();
        s.metalWallopBonus = p.getMetalWallopBonus();
        return s;
    }

    public InPlayPokemon toInPlayPokemon(CardRegistryService cardRegistry) {
        Card cardData = cardRegistry.getById(cardId);
        if (!(cardData instanceof PokemonCard pokemonCard)) {
            throw new IllegalStateException("Cannot restore InPlayPokemon: card not found for id " + cardId);
        }
        InPlayPokemon p = new InPlayPokemon(pokemonCard);
        // instanceId is generated in constructor, override it
        p.setInstanceId(instanceId);
        p.setDamageTaken(damageTaken);

        if (energyCardIds != null) {
            List<EnergyCard> energies = new ArrayList<>();
            for (String eid : energyCardIds) {
                Card c = cardRegistry.getById(eid);
                if (c instanceof EnergyCard ec) energies.add(ec);
            }
            p.setEnergies(energies);
        }

        if (toolCardId != null) {
            p.setTool(cardRegistry.getById(toolCardId));
        }

        if (underlyingCardIds != null && !underlyingCardIds.isEmpty()) {
            List<PokemonCard> underlying = new ArrayList<>();
            for (String uid : underlyingCardIds) {
                Card c = cardRegistry.getById(uid);
                if (c instanceof PokemonCard pc) underlying.add(pc);
            }
            p.setUnderlyingCards(underlying);
        }

        p.setPreventAllEffects(preventAllEffects);
        p.setPreventAllIncomingDamage(preventAllIncomingDamage);
        p.setPreventIncomingDamageThreshold(preventIncomingDamageThreshold);
        p.setIncomingDamageReduction(incomingDamageReduction);
        p.setCantRetreat(cantRetreat);
        p.setCantAttack(cantAttack);
        p.setCantUseAttackName(cantUseAttackName);
        p.setIfAttacksFlipCoin(ifAttacksFlipCoin);
        p.setAbilitiesDisabled(abilitiesDisabled);
        p.setMetalWallopBonus(metalWallopBonus);

        return p;
    }

    // ---- Getters / Setters ----

    public String getCardId() { return cardId; }
    public void setCardId(String v) { cardId = v; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String v) { instanceId = v; }
    public int getDamageTaken() { return damageTaken; }
    public void setDamageTaken(int v) { damageTaken = v; }
    public List<String> getEnergyCardIds() { return energyCardIds; }
    public void setEnergyCardIds(List<String> v) { energyCardIds = v; }
    public String getToolCardId() { return toolCardId; }
    public void setToolCardId(String v) { toolCardId = v; }
    public List<String> getUnderlyingCardIds() { return underlyingCardIds; }
    public void setUnderlyingCardIds(List<String> v) { underlyingCardIds = v; }
    public boolean isPreventAllEffects() { return preventAllEffects; }
    public void setPreventAllEffects(boolean v) { preventAllEffects = v; }
    public boolean isPreventAllIncomingDamage() { return preventAllIncomingDamage; }
    public void setPreventAllIncomingDamage(boolean v) { preventAllIncomingDamage = v; }
    public int getPreventIncomingDamageThreshold() { return preventIncomingDamageThreshold; }
    public void setPreventIncomingDamageThreshold(int v) { preventIncomingDamageThreshold = v; }
    public int getIncomingDamageReduction() { return incomingDamageReduction; }
    public void setIncomingDamageReduction(int v) { incomingDamageReduction = v; }
    public boolean isCantRetreat() { return cantRetreat; }
    public void setCantRetreat(boolean v) { cantRetreat = v; }
    public boolean isCantAttack() { return cantAttack; }
    public void setCantAttack(boolean v) { cantAttack = v; }
    public String getCantUseAttackName() { return cantUseAttackName; }
    public void setCantUseAttackName(String v) { cantUseAttackName = v; }
    public boolean isIfAttacksFlipCoin() { return ifAttacksFlipCoin; }
    public void setIfAttacksFlipCoin(boolean v) { ifAttacksFlipCoin = v; }
    public boolean isAbilitiesDisabled() { return abilitiesDisabled; }
    public void setAbilitiesDisabled(boolean v) { abilitiesDisabled = v; }
    public int getMetalWallopBonus() { return metalWallopBonus; }
    public void setMetalWallopBonus(int v) { metalWallopBonus = v; }
}
