package ar.edu.utn.frc.tup.piii.engine.snapshot;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.services.CardRegistryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InPlaySnapshotTest {

    @Mock
    private CardRegistryService cardRegistry;

    @Test
    void from_BasicPokemon_MapsCardIdAndInstanceId() {
        InPlayPokemon p = makePokemon("pkmn-1");

        InPlaySnapshot s = InPlaySnapshot.from(p);

        assertThat(s.getCardId()).isEqualTo("pkmn-1");
        assertThat(s.getInstanceId()).isEqualTo(p.getInstanceId());
    }

    @Test
    void from_WithDamageTaken_PreservesDamage() {
        InPlayPokemon p = makePokemon("pkmn-1");
        p.setDamageTaken(50);

        InPlaySnapshot s = InPlaySnapshot.from(p);

        assertThat(s.getDamageTaken()).isEqualTo(50);
    }

    @Test
    void from_WithEnergies_MapsEnergyCardIds() {
        InPlayPokemon p = makePokemon("pkmn-1");
        EnergyCard e1 = EnergyCard.builder().id("fire").name("Fire")
                .energyType(EnergyType.FIRE).rules(List.of()).build();
        EnergyCard e2 = EnergyCard.builder().id("water").name("Water")
                .energyType(EnergyType.WATER).rules(List.of()).build();
        p.getEnergies().add(e1);
        p.getEnergies().add(e2);

        InPlaySnapshot s = InPlaySnapshot.from(p);

        assertThat(s.getEnergyCardIds()).containsExactly("fire", "water");
    }

    @Test
    void from_WithTool_MapsToolCardId() {
        InPlayPokemon p = makePokemon("pkmn-1");
        Card tool = PokemonCard.builder().id("tool-1").name("Tool").hp(0)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS)
                .attacks(List.of()).abilities(List.of())
                .weaknesses(List.of()).resistances(List.of())
                .costToAttack(List.of()).retreatCost(0)
                .build();
        p.setTool(tool);

        InPlaySnapshot s = InPlaySnapshot.from(p);

        assertThat(s.getToolCardId()).isEqualTo("tool-1");
    }

    @Test
    void from_WithUnderlyingCards_MapsIds() {
        InPlayPokemon p = makePokemon("pkmn-2");
        PokemonCard prevo = makePokemonCard("prevo");
        p.setUnderlyingCards(List.of(prevo));

        InPlaySnapshot s = InPlaySnapshot.from(p);

        assertThat(s.getUnderlyingCardIds()).containsExactly("prevo");
    }

    @Test
    void from_AllProtectionFlags_MapsCorrectly() {
        InPlayPokemon p = makePokemon("pkmn-1");
        p.setPreventAllEffects(true);
        p.setPreventAllIncomingDamage(true);
        p.setPreventIncomingDamageThreshold(60);
        p.setIncomingDamageReduction(20);

        InPlaySnapshot s = InPlaySnapshot.from(p);

        assertThat(s.isPreventAllEffects()).isTrue();
        assertThat(s.isPreventAllIncomingDamage()).isTrue();
        assertThat(s.getPreventIncomingDamageThreshold()).isEqualTo(60);
        assertThat(s.getIncomingDamageReduction()).isEqualTo(20);
    }

    @Test
    void from_AllRestrictionFlags_MapsCorrectly() {
        InPlayPokemon p = makePokemon("pkmn-1");
        p.setCantRetreat(true);
        p.setCantAttack(true);
        p.setCantUseAttackName("Psychic");
        p.setIfAttacksFlipCoin(true);
        p.setAbilitiesDisabled(true);

        InPlaySnapshot s = InPlaySnapshot.from(p);

        assertThat(s.isCantRetreat()).isTrue();
        assertThat(s.isCantAttack()).isTrue();
        assertThat(s.getCantUseAttackName()).isEqualTo("Psychic");
        assertThat(s.isIfAttacksFlipCoin()).isTrue();
        assertThat(s.isAbilitiesDisabled()).isTrue();
    }

    @Test
    void from_MetalWallopBonus_MapsCorrectly() {
        InPlayPokemon p = makePokemon("pkmn-1");
        p.setMetalWallopBonus(30);

        InPlaySnapshot s = InPlaySnapshot.from(p);

        assertThat(s.getMetalWallopBonus()).isEqualTo(30);
    }

    @Test
    void toInPlayPokemon_Basic_RestoresPokemon() {
        PokemonCard card = makePokemonCard("pkmn-1");
        when(cardRegistry.getById("pkmn-1")).thenReturn(card);

        InPlaySnapshot s = new InPlaySnapshot();
        s.setCardId("pkmn-1");
        s.setInstanceId("inst-original");

        InPlayPokemon restored = s.toInPlayPokemon(cardRegistry);

        assertThat(restored.getCard()).isSameAs(card);
        assertThat(restored.getInstanceId()).isEqualTo("inst-original");
        assertThat(restored.getDamageTaken()).isZero();
    }

    @Test
    void toInPlayPokemon_WithEnergies_RestoresEnergies() {
        PokemonCard card = makePokemonCard("pkmn-1");
        EnergyCard energy = EnergyCard.builder().id("fire").name("Fire")
                .energyType(EnergyType.FIRE).rules(List.of()).build();
        when(cardRegistry.getById("pkmn-1")).thenReturn(card);
        when(cardRegistry.getById("fire")).thenReturn(energy);

        InPlaySnapshot s = new InPlaySnapshot();
        s.setCardId("pkmn-1");
        s.setInstanceId("inst-1");
        s.setEnergyCardIds(List.of("fire"));

        InPlayPokemon restored = s.toInPlayPokemon(cardRegistry);

        assertThat(restored.getEnergies()).hasSize(1);
        assertThat(restored.getEnergies().get(0)).isSameAs(energy);
    }

    @Test
    void toInPlayPokemon_WithTool_RestoresTool() {
        PokemonCard card = makePokemonCard("pkmn-1");
        Card tool = PokemonCard.builder().id("tool-1").name("Tool").hp(0)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS)
                .attacks(List.of()).abilities(List.of())
                .weaknesses(List.of()).resistances(List.of())
                .costToAttack(List.of()).retreatCost(0)
                .build();
        when(cardRegistry.getById("pkmn-1")).thenReturn(card);
        when(cardRegistry.getById("tool-1")).thenReturn(tool);

        InPlaySnapshot s = new InPlaySnapshot();
        s.setCardId("pkmn-1");
        s.setInstanceId("inst-1");
        s.setToolCardId("tool-1");

        InPlayPokemon restored = s.toInPlayPokemon(cardRegistry);

        assertThat(restored.getTool()).isSameAs(tool);
    }

    @Test
    void toInPlayPokemon_WithUnderlying_RestoresChain() {
        PokemonCard card = makePokemonCard("pkmn-2");
        PokemonCard prevo = makePokemonCard("prevo");
        when(cardRegistry.getById("pkmn-2")).thenReturn(card);
        when(cardRegistry.getById("prevo")).thenReturn(prevo);

        InPlaySnapshot s = new InPlaySnapshot();
        s.setCardId("pkmn-2");
        s.setInstanceId("inst-1");
        s.setUnderlyingCardIds(List.of("prevo"));

        InPlayPokemon restored = s.toInPlayPokemon(cardRegistry);

        assertThat(restored.getUnderlyingCards()).hasSize(1);
        assertThat(restored.getUnderlyingCards().get(0)).isSameAs(prevo);
    }

    @Test
    void toInPlayPokemon_WhenCardNotFound_Throws() {
        when(cardRegistry.getById("invalid-id"))
                .thenThrow(new IllegalArgumentException("Card not found: invalid-id"));

        InPlaySnapshot s = new InPlaySnapshot();
        s.setCardId("invalid-id");
        s.setInstanceId("inst-1");

        assertThatThrownBy(() -> s.toInPlayPokemon(cardRegistry))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roundTrip_PreservesAllFields() {
        InPlayPokemon original = makePokemon("pkmn-1");
        original.setDamageTaken(30);
        EnergyCard energy = EnergyCard.builder().id("lightning").name("Lightning")
                .energyType(EnergyType.LIGHTNING).rules(List.of()).build();
        original.getEnergies().add(energy);
        original.setPreventAllEffects(true);
        original.setCantRetreat(true);
        original.setMetalWallopBonus(20);

        PokemonCard card = makePokemonCard("pkmn-1");
        when(cardRegistry.getById("pkmn-1")).thenReturn(card);
        when(cardRegistry.getById("lightning")).thenReturn(energy);

        InPlaySnapshot s = InPlaySnapshot.from(original);
        InPlayPokemon restored = s.toInPlayPokemon(cardRegistry);
        InPlaySnapshot s2 = InPlaySnapshot.from(restored);

        assertThat(s2.getCardId()).isEqualTo("pkmn-1");
        assertThat(s2.getInstanceId()).isEqualTo(original.getInstanceId());
        assertThat(s2.getDamageTaken()).isEqualTo(30);
        assertThat(s2.getEnergyCardIds()).containsExactly("lightning");
        assertThat(s2.isPreventAllEffects()).isTrue();
        assertThat(s2.isCantRetreat()).isTrue();
        assertThat(s2.getMetalWallopBonus()).isEqualTo(20);
    }

    private static InPlayPokemon makePokemon(String id) {
        return new InPlayPokemon(makePokemonCard(id));
    }

    private static PokemonCard makePokemonCard(String id) {
        return PokemonCard.builder()
                .id(id).name(id).hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE)
                .attacks(List.of()).abilities(List.of())
                .weaknesses(List.of()).resistances(List.of())
                .costToAttack(List.of()).retreatCost(0)
                .build();
    }
}
