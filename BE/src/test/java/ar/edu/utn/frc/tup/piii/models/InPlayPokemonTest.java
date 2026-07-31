package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class InPlayPokemonTest {

    private PokemonCard baseCard;
    private InPlayPokemon inPlay;

    @BeforeEach
    void setUp() {
        baseCard = PokemonCard.builder()
                .id("test-1")
                .name("TestMon")
                .hp(100)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.GRASS)
                .build();
        inPlay = new InPlayPokemon(baseCard);
    }

    @Test
    void constructor_SetsInitialState() {
        assertThat(inPlay.getCard()).isSameAs(baseCard);
        assertThat(inPlay.getDamageTaken()).isZero();
        assertThat(inPlay.getEnergies()).isEmpty();
        assertThat(inPlay.getStatuses()).isEmpty();
        assertThat(inPlay.getTool()).isNull();
    }

    @Test
    void getCurrentHp_WhenFullHealth_ReturnsMaxHp() {
        assertThat(inPlay.getCurrentHp()).isEqualTo(100);
    }

    @Test
    void getCurrentHp_WhenDamaged_ReturnsRemainingHp() {
        inPlay.setDamageTaken(30);
        assertThat(inPlay.getCurrentHp()).isEqualTo(70);
    }

    @Test
    void getCurrentHp_WhenDamageExceedsHp_ReturnsZero() {
        inPlay.setDamageTaken(150);
        assertThat(inPlay.getCurrentHp()).isZero();
    }

    @Test
    void isKnockedOut_WhenZeroHp_ReturnsTrue() {
        inPlay.setDamageTaken(100);
        assertThat(inPlay.isKnockedOut()).isTrue();
    }

    @Test
    void isKnockedOut_WhenHasHp_ReturnsFalse() {
        inPlay.setDamageTaken(50);
        assertThat(inPlay.isKnockedOut()).isFalse();
    }

    @Test
    void hasStatus_WhenHasStatus_ReturnsTrue() {
        inPlay.applyStatus(StatusCondition.POISONED);
        assertThat(inPlay.hasStatus(StatusCondition.POISONED)).isTrue();
    }

    @Test
    void hasStatus_WhenDoesNotHaveStatus_ReturnsFalse() {
        assertThat(inPlay.hasStatus(StatusCondition.POISONED)).isFalse();
    }

    @Test
    void applyStatus_ClearsExistingAndSetsNew() {
        inPlay.applyStatus(StatusCondition.POISONED);
        inPlay.applyStatus(StatusCondition.BURNED);
        assertThat(inPlay.hasStatus(StatusCondition.POISONED)).isFalse();
        assertThat(inPlay.hasStatus(StatusCondition.BURNED)).isTrue();
        assertThat(inPlay.getStatuses()).hasSize(1);
    }

    @Test
    void addStatus_AddsAlongsideExisting() {
        inPlay.applyStatus(StatusCondition.POISONED);
        inPlay.addStatus(StatusCondition.PARALYZED);
        assertThat(inPlay.hasStatus(StatusCondition.POISONED)).isTrue();
        assertThat(inPlay.hasStatus(StatusCondition.PARALYZED)).isTrue();
        assertThat(inPlay.getStatuses()).hasSize(2);
    }

    @Test
    void clearStatuses_RemovesAll() {
        inPlay.applyStatus(StatusCondition.POISONED);
        inPlay.addStatus(StatusCondition.PARALYZED);
        inPlay.clearStatuses();
        assertThat(inPlay.getStatuses()).isEmpty();
    }

    @Test
    void countEnergies_ByType() {
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.GRASS).build());
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.GRASS).build());
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        assertThat(inPlay.countEnergies(EnergyType.GRASS)).isEqualTo(2);
        assertThat(inPlay.countEnergies(EnergyType.FIRE)).isEqualTo(1);
        assertThat(inPlay.countEnergies(EnergyType.WATER)).isZero();
    }

    @Test
    void countEnergies_All() {
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.GRASS).build());
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        assertThat(inPlay.countEnergies(null)).isEqualTo(2);
    }

    @Test
    void countDistinctEnergyTypes() {
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.GRASS).build());
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.GRASS).build());
        assertThat(inPlay.countDistinctEnergyTypes()).isEqualTo(2);
    }

    @Test
    void countDistinctEnergyTypes_ExcludesColorless() {
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.COLORLESS).build());
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.GRASS).build());
        assertThat(inPlay.countDistinctEnergyTypes()).isEqualTo(1);
    }

    @Test
    void countDistinctEnergyTypes_ExcludesNullEnergyType() {
        // Cubre la rama t != null == false dentro del filtro.
        inPlay.getEnergies().add(EnergyCard.builder().energyType(null).build());
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.GRASS).build());
        assertThat(inPlay.countDistinctEnergyTypes()).isEqualTo(1);
    }

    @Test
    void removeOneEnergy_ByType() {
        EnergyCard grass = EnergyCard.builder().energyType(EnergyType.GRASS).build();
        EnergyCard fire = EnergyCard.builder().energyType(EnergyType.FIRE).build();
        inPlay.getEnergies().add(grass);
        inPlay.getEnergies().add(fire);
        EnergyCard removed = inPlay.removeOneEnergy(EnergyType.GRASS);
        assertThat(removed).isSameAs(grass);
        assertThat(inPlay.getEnergies()).hasSize(1);
        assertThat(inPlay.getEnergies().get(0).getEnergyType()).isEqualTo(EnergyType.FIRE);
    }

    @Test
    void removeOneEnergy_Any() {
        EnergyCard grass = EnergyCard.builder().energyType(EnergyType.GRASS).build();
        inPlay.getEnergies().add(grass);
        EnergyCard removed = inPlay.removeOneEnergy(null);
        assertThat(removed).isSameAs(grass);
        assertThat(inPlay.getEnergies()).isEmpty();
    }

    @Test
    void removeOneEnergy_NotFound_ReturnsNull() {
        EnergyCard removed = inPlay.removeOneEnergy(EnergyType.GRASS);
        assertThat(removed).isNull();
    }

    @Test
    void removeAllEnergies_ByType() {
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.GRASS).build());
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.GRASS).build());
        List<EnergyCard> removed = inPlay.removeAllEnergies(EnergyType.GRASS);
        assertThat(removed).hasSize(2);
        assertThat(inPlay.getEnergies()).hasSize(1);
        assertThat(inPlay.getEnergies().get(0).getEnergyType()).isEqualTo(EnergyType.FIRE);
    }

    @Test
    void removeAllEnergies_All() {
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.GRASS).build());
        inPlay.getEnergies().add(EnergyCard.builder().energyType(EnergyType.FIRE).build());
        List<EnergyCard> removed = inPlay.removeAllEnergies(null);
        assertThat(removed).hasSize(2);
        assertThat(inPlay.getEnergies()).isEmpty();
    }

    @Test
    void resetTurnFlags_ResetsAllFlags() {
        inPlay.setPreventAllEffects(true);
        inPlay.setPreventAllIncomingDamage(true);
        inPlay.setPreventIncomingDamageThreshold(60);
        inPlay.setIncomingDamageReduction(20);
        inPlay.setCantRetreat(true);
        inPlay.setCantAttack(true);
        inPlay.setCantUseAttackName("XBlast");
        inPlay.setIfAttacksFlipCoin(true);
        inPlay.setAbilitiesDisabled(true);
        inPlay.setMetalWallopBonus(50);
        inPlay.resetTurnFlags();
        assertThat(inPlay.isPreventAllEffects()).isFalse();
        assertThat(inPlay.isPreventAllIncomingDamage()).isFalse();
        assertThat(inPlay.getPreventIncomingDamageThreshold()).isZero();
        assertThat(inPlay.getIncomingDamageReduction()).isZero();
        assertThat(inPlay.isCantRetreat()).isFalse();
        assertThat(inPlay.isCantAttack()).isFalse();
        assertThat(inPlay.getCantUseAttackName()).isNull();
        assertThat(inPlay.isIfAttacksFlipCoin()).isFalse();
        assertThat(inPlay.isAbilitiesDisabled()).isFalse();
        assertThat(inPlay.getMetalWallopBonus()).isZero();
    }
}
