package ar.edu.utn.frc.tup.piii.models;

import ar.edu.utn.frc.tup.piii.models.converters.StringListConverter;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
@Table(name = "collectible_card")
public class CollectibleCard extends Card {

    @Setter
    @Convert(converter = StringListConverter.class)
    @Column(name = "rules", columnDefinition = "TEXT")
    private List<String> rules;

    @Setter
    private String artist;

    @Override
    public Supertype getSupertype() {
        return Supertype.COLLECTIBLE;
    }

    @Override
    public boolean canBePlayed(GameContext ctx) {
        return false;
    }
}
