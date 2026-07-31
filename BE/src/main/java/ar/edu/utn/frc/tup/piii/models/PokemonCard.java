package ar.edu.utn.frc.tup.piii.models;

import ar.edu.utn.frc.tup.piii.models.converters.EnergyTypeListConverter;
import ar.edu.utn.frc.tup.piii.models.converters.ResistanceListConverter;
import ar.edu.utn.frc.tup.piii.models.converters.StringListConverter;
import ar.edu.utn.frc.tup.piii.models.converters.WeaknessListConverter;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.NotImplementedException;

@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "pokemon_card")
public class PokemonCard extends Card {

    @Setter
    private int hp;
    @Setter
    @Enumerated(EnumType.STRING)
    private PokemonStage stage;
    @Setter
    @Column(name = "is_ex")
    private boolean isEx;
    @Setter
    @Column(name = "is_mega")
    private boolean isMega;
    @Setter
    @Convert(converter = EnergyTypeListConverter.class)
    @Column(name = "cost_to_attack", columnDefinition = "TEXT")
    private List<EnergyType> costToAttack;
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "pokemon_type")
    private EnergyType pokemonType;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "pokemon_card_id")
    private List<Attack> attacks;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "pokemon_card_id")
    private List<Ability> abilities;
    @Setter
    @Convert(converter = WeaknessListConverter.class)
    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private List<Weakness> weaknesses;
    @Setter
    @Convert(converter = ResistanceListConverter.class)
    @Column(name = "resistances", columnDefinition = "TEXT")
    private List<Resistance> resistances;
    @Setter
    @Column(name = "retreat_cost")
    private int retreatCost;
    @Setter
    @Column(name = "evolves_from")
    private String evolvesFrom;
    @Setter
    @Convert(converter = StringListConverter.class)
    @Column(name = "evolves_to", columnDefinition = "TEXT")
    private List<String> evolvesTo;
    @Setter
    @Convert(converter = StringListConverter.class)
    @Column(name = "rules", columnDefinition = "TEXT")
    private List<String> rules;
    @Setter
    @Column(name = "gif_url")
    private String gifUrl;

    @Override
    public Supertype getSupertype() { return Supertype.POKEMON; }

    public boolean isBasic() { return stage == PokemonStage.BASIC; }

    public boolean canEvolveFrom(PokemonCard other) {
        return evolvesFrom != null && evolvesFrom.equals(other.getName());
    }

    public int getPrizeValue() {
        return (isEx || isMega) ? 2 : 1;
    }

    @Override
    public boolean canBePlayed(GameContext ctx) {
        throw new NotImplementedException("Not implemented yet");
    }
}
