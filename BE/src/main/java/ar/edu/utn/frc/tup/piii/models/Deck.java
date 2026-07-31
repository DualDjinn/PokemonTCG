package ar.edu.utn.frc.tup.piii.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "deck")
public class Deck {

    @Id
    private String id;
    private String name;
    private boolean favorite;

    @ManyToMany
    @JoinTable(
        name = "deck_card",
        joinColumns = @JoinColumn(name = "deck_id"),
        inverseJoinColumns = @JoinColumn(name = "card_id")
    )
    private List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
    }

    public Deck(String id, String name, List<Card> cards) {
        this.id = id;
        this.name = name;
        this.cards = cards != null ? cards : new ArrayList<>();
    }

    public List<String> validate(Player owner, Set<String> bannedCardIds) {
        List<String> errors = new ArrayList<>();

        if (cards == null || cards.size() != 60) {
            errors.add("Deck must have exactly 60 cards (has "
                    + (cards == null ? 0 : cards.size()) + ")");
            return errors;
        }

        boolean hasBasicPokemon = cards.stream()
                .anyMatch(c -> c instanceof PokemonCard pk && pk.isBasic());
        if (!hasBasicPokemon) {
            errors.add("Deck must contain at least 1 Basic Pokémon");
        }

        Map<String, Long> countByCardId = cards.stream()
                .collect(Collectors.groupingBy(Card::getId, Collectors.counting()));
        for (var entry : countByCardId.entrySet()) {
            String cardId = entry.getKey();
            long count = entry.getValue();
            Card sample = cards.stream()
                    .filter(c -> c.getId().equals(cardId))
                    .findFirst().orElse(null);
            if (sample != null && !(sample instanceof EnergyCard) && count > 4) {
                errors.add("Max 4 copies per card allowed for non-Energy cards ("
                        + sample.getName() + " has " + count + ")");
            }
        }

        if (bannedCardIds != null && !bannedCardIds.isEmpty()) {
            boolean hasBanned = cards.stream()
                    .anyMatch(c -> bannedCardIds.contains(c.getId()));
            if (hasBanned) {
                errors.add("Deck contains banned or illegal cards");
            }
        }

        if (owner != null && owner.getCollection() != null) {
            boolean allOwned = cards.stream()
                    .allMatch(c -> owner.getCollection().contains(c));
            if (!allOwned) {
                errors.add("Deck contains cards not owned by the player");
            }
        }

        return errors;
    }

    public boolean isValid(Player owner, Set<String> bannedCardIds) {
        return validate(owner, bannedCardIds).isEmpty();
    }
}
