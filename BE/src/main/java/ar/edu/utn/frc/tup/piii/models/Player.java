package ar.edu.utn.frc.tup.piii.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"collection", "decks", "version"})
@Entity
@Table(name = "player")
public class Player {

    private static final int BASE_XP = 100;

    @Id
    @EqualsAndHashCode.Include
    private String id;
    @Column(unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private String username;
    @Column(nullable = false)
    @EqualsAndHashCode.Include
    private String trainerName;
    @Column(unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private String email;
    @Column(nullable = false)
    private String password;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @EqualsAndHashCode.Include
    private int experience;
    @Column(name = "reset_token")
    private String resetToken;
    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "active_deck_id")
    private String activeDeckId;

    /** Dorso de carta equipado; null = dorso clásico por defecto. */
    @Column(name = "active_card_back_id")
    private String activeCardBackId;

    @Column(name = "character_id")
    private String character;

    @Column(name = "character_color")
    private String color;

    @Column(nullable = false)
    private int coins;

    /**
     * Version field for optimistic locking. Prevents lost updates on {@code coins} when two
     * purchases by the same player run in parallel: the second transaction to commit detects
     * the changed version and fails with {@code ObjectOptimisticLockingFailureException}
     * (mapped to HTTP 409), instead of overwriting the first one's deduction.
     */
    @Version
    private long version;

    /** {@code true} when this player is an AI bot rather than a human. */
    @Column(name = "is_bot", nullable = false)
    private boolean isBot;

    @ManyToMany
    @JoinTable(
        name = "player_collection",
        joinColumns = @JoinColumn(name = "player_id"),
        inverseJoinColumns = @JoinColumn(name = "card_id")
    )
    private List<Card> collection;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "player_id")
    private List<Deck> decks;

    public Player() {
        this.collection = new ArrayList<>();
        this.decks = new ArrayList<>();
    }

    public Player(String id, String username, String trainerName, String email, String password,
                  LocalDate dateOfBirth, int experience, int coins,
                  List<Card> collection, List<Deck> decks) {
        this.id = id;
        this.username = username;
        this.trainerName = trainerName;
        this.email = email;
        this.password = password;
        this.dateOfBirth = dateOfBirth;
        this.experience = experience;
        this.coins = coins;
        this.collection = collection != null ? collection : new ArrayList<>();
        this.decks = decks != null ? decks : new ArrayList<>();
    }

    public void addExperience(int xp) {
        this.experience += xp;
    }

    public int getLevel() {
        int level = 1;
        int xpNeeded = BASE_XP;
        int remaining = experience;
        while (remaining >= xpNeeded) {
            remaining -= xpNeeded;
            level++;
            xpNeeded *= 2;
        }
        return level;
    }

    public int getXpForNextLevel() {
        return BASE_XP * (int) Math.pow(2, getLevel() - 1);
    }

    public int getXpIntoCurrentLevel() {
        int level = getLevel();
        int xpNeeded = BASE_XP;
        int remaining = experience;
        for (int i = 1; i < level; i++) {
            remaining -= xpNeeded;
            xpNeeded *= 2;
        }
        return Math.max(0, remaining);
    }

    public void addToCollection(Card card) {
        this.collection.add(card);
    }

    public void addDeck(Deck deck) {
        this.decks.add(deck);
    }
}
