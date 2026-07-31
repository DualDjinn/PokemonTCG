package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "card")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Card {

    @Id
    private String id;
    @Setter
    private String name;
    @Setter
    @Column(name = "set_code")
    private String setCode;
    private String number;
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CardRarity rarity;
    @Setter
    @Column(name = "image_url")
    private String imageUrl;
    @Setter
    @Column(name = "image_url_large")
    private String imageUrlLarge;

    public abstract Supertype getSupertype();

    public abstract boolean canBePlayed(GameContext ctx);

    public boolean isFullArt() {
        return rarity == CardRarity.RARE_ULTRA;
    }
}
