package ar.edu.utn.frc.tup.piii.models;

import ar.edu.utn.frc.tup.piii.models.converters.StringListConverter;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Abstract base for every Trainer card in the game (Item, Supporter, Stadium,
 * Pokémon Tool, ACE SPEC).
 *
 * <p>Extends {@link Card} so all trainer cards share common card identity
 * (id, name, set, number, rarity, image URLs). Subclasses must declare their
 * specific subtype and the play rule that the game engine enforces.
 */
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "trainer_card")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "trainer_subtype", discriminatorType = DiscriminatorType.STRING)
public abstract class TrainerCard extends Card {

    @Setter
    @Convert(converter = StringListConverter.class)
    @Column(name = "rules", columnDefinition = "TEXT")
    private List<String> rules;
    @Setter
    private String artist;

    /**
     * Returns the trainer subtype: "Item", "Supporter", "Stadium",
     * "Pokémon Tool", or "ACE SPEC".
     */
    public abstract String getSubtype();

    /**
     * Returns the human-readable play rule for this subtype, used for
     * validation messages and UI tooltips.
     */
    public abstract String getPlayRule();

    public List<String> getRules() {
        return rules != null ? Collections.unmodifiableList(rules) : Collections.emptyList();
    }

    public String getArtist() { return artist; }

    @Override
    public final Supertype getSupertype() { return Supertype.TRAINER; }

    @Override
    public boolean canBePlayed(GameContext ctx) {
        return ctx != null && ctx.getCurrentPhase() == TurnPhase.MAIN;
    }

    @Override
    public String toString() {
        return String.format("%s [Trainer - %s]", getName(), getSubtype());
    }
}
