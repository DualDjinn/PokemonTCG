package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A Pokémon ability — a non-attack effect printed on a Pokémon card.
 *
 * <p>Abilities are categorised by {@link AbilityType}:
 * <ul>
 *     <li>{@link AbilityType#PASSIVE} — always active while the holder is in play
 *     (e.g. damage reduction, damage boost).</li>
 *     <li>{@link AbilityType#ACTIVATED} — the player may use it manually,
 *     typically once per turn.</li>
 *     <li>{@link AbilityType#TRIGGERED} — fires on a game event such as
 *     "when this Pokémon enters play".</li>
 * </ul>
 *
 * <p>The {@code effect} field is a {@link Transient} runtime-only reference to
 * the {@link AbilityEffect} strategy resolved by
 * {@code models.effects.AbilityRegistry} at mapping time; it is not persisted.
 */
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "ability")
public class Ability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    private AbilityType type;

    /**
     * Strategy resolved by the {@code AbilityRegistry} at mapping time.
     * Not persisted; ignored by JPA.
     */
    @Transient
    private AbilityEffect effect;

    /**
     * Creates an ability with the rulebook-printed fields. The {@code effect}
     * strategy is set later by the mapper via the builder.
     *
     * @param name        printed ability name (e.g. "Adaptability")
     * @param description full rulebook text of the ability
     * @param type        ability category from {@link AbilityType}
     */
    public Ability(String name, String description, AbilityType type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }
}
