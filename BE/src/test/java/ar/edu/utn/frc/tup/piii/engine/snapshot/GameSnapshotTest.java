package ar.edu.utn.frc.tup.piii.engine.snapshot;

import ar.edu.utn.frc.tup.piii.engine.log.MatchLogEntry;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import ar.edu.utn.frc.tup.piii.services.CardRegistryService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameSnapshotTest {

    @Mock
    private CardRegistryService cardRegistry;

    @Test
    void from_EmptyContext_ReturnsEmptySnapshot() {
        GameContext ctx = new GameContext();

        GameSnapshot s = GameSnapshot.from(ctx);

        assertThat(s.getTurnNumber()).isEqualTo(1);
        assertThat(s.getCurrentPhase()).isEqualTo("MAIN");
        assertThat(s.isFirstTurn()).isTrue();
        assertThat(s.getAttacker()).isNull();
        assertThat(s.getDefender()).isNull();
        assertThat(s.getAttackerBench()).hasSize(5);
        assertThat(s.getDefenderBench()).hasSize(5);
        assertThat(s.getAttackerHand()).isEmpty();
        assertThat(s.getDefenderHand()).isEmpty();
        assertThat(s.getAttackerDeck()).isEmpty();
        assertThat(s.getDefenderDeck()).isEmpty();
        assertThat(s.getAttackerDiscard()).isEmpty();
        assertThat(s.getDefenderDiscard()).isEmpty();
        assertThat(s.getAttackerPrizes()).isEmpty();
        assertThat(s.getDefenderPrizes()).isEmpty();
    }

    @Test
    void from_toGameContext_RoundTripPreservesAllFields() {
        GameContext ctx = new GameContext();
        ctx.setTurnNumber(5);
        ctx.setCurrentPhase(TurnPhase.ATTACK);
        ctx.setFirstTurn(false);
        ctx.setAttackerEnergyAttachmentsThisTurn(2);
        ctx.setAttackerAdditionalEnergyAttachments(1);
        ctx.setAttackerSupportersPlayedThisTurn(1);
        ctx.setAttackerCantPlaySupporters(true);
        ctx.setPrizesDealt(true);

        GameSnapshot s = GameSnapshot.from(ctx);
        GameContext restored = s.toGameContext(cardRegistry);

        assertThat(restored.getTurnNumber()).isEqualTo(5);
        assertThat(restored.getCurrentPhase()).isEqualTo(TurnPhase.ATTACK);
        assertThat(restored.isFirstTurn()).isFalse();
        assertThat(restored.getAttackerEnergyAttachmentsThisTurn()).isEqualTo(2);
        assertThat(restored.getAttackerAdditionalEnergyAttachments()).isEqualTo(1);
        assertThat(restored.getAttackerSupportersPlayedThisTurn()).isEqualTo(1);
        assertThat(restored.isAttackerCantPlaySupporters()).isTrue();
        assertThat(restored.isPrizesDealt()).isTrue();
    }

    @Test
    void from_WithAttackerAndDefender_PreservesBoth() {
        GameContext ctx = new GameContext();
        InPlayPokemon attacker = makePokemon("pkmn-1");
        InPlayPokemon defender = makePokemon("pkmn-2");
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);

        GameSnapshot s = GameSnapshot.from(ctx);

        assertThat(s.getAttacker()).isNotNull();
        assertThat(s.getAttacker().getCardId()).isEqualTo("pkmn-1");
        assertThat(s.getAttacker().getInstanceId()).isEqualTo(attacker.getInstanceId());
        assertThat(s.getDefender()).isNotNull();
        assertThat(s.getDefender().getCardId()).isEqualTo("pkmn-2");
        assertThat(s.getDefender().getInstanceId()).isEqualTo(defender.getInstanceId());
    }

    @Test
    void from_WithBench_PreservesBenchOrder() {
        GameContext ctx = new GameContext();
        List<InPlayPokemon> bench = new ArrayList<>();
        bench.add(makePokemon("b1"));
        bench.add(null);
        bench.add(makePokemon("b3"));
        bench.add(null);
        bench.add(null);
        ctx.setAttackerBench(bench);

        GameSnapshot s = GameSnapshot.from(ctx);

        assertThat(s.getAttackerBench()).hasSize(5);
        assertThat(s.getAttackerBench().get(0)).isNotNull();
        assertThat(s.getAttackerBench().get(0).getCardId()).isEqualTo("b1");
        assertThat(s.getAttackerBench().get(1)).isNull();
        assertThat(s.getAttackerBench().get(2)).isNotNull();
        assertThat(s.getAttackerBench().get(2).getCardId()).isEqualTo("b3");
        assertThat(s.getAttackerBench().get(3)).isNull();
        assertThat(s.getAttackerBench().get(4)).isNull();
    }

    @Test
    void from_WithHandsAndDecks_UsesCardIds() {
        GameContext ctx = new GameContext();
        PokemonCard c1 = makePokemonCard("c1");
        PokemonCard c2 = makePokemonCard("c2");
        PokemonCard c3 = makePokemonCard("c3");
        ctx.setAttackerHand(List.of(c1, c2));
        ctx.setDefenderDeck(List.of(c3));

        GameSnapshot s = GameSnapshot.from(ctx);

        assertThat(s.getAttackerHand()).containsExactly("c1", "c2");
        assertThat(s.getDefenderDeck()).containsExactly("c3");
    }

    @Test
    void from_WithMatchLog_PreservesLogEntries() {
        GameContext ctx = new GameContext();
        ctx.addLogEntry("p1", "PLAY_BASIC", "Jugó un Pokémon Básico");
        ctx.addLogEntry("p2", "ATTACH_ENERGY", "Unió una Energía");

        GameSnapshot s = GameSnapshot.from(ctx);

        assertThat(s.getMatchLog()).hasSize(2);
        assertThat(s.getMatchLog().get(0).getPlayerId()).isEqualTo("p1");
        assertThat(s.getMatchLog().get(0).getActionType()).isEqualTo("PLAY_BASIC");
        assertThat(s.getMatchLog().get(1).getPlayerId()).isEqualTo("p2");
    }

    @Test
    void toGameContext_WithEnergies_ReloadsFromRegistry() {
        EnergyCard energy = EnergyCard.builder()
                .id("fire-energy").name("Fire Energy")
                .energyType(EnergyType.FIRE).rules(List.of()).build();
        when(cardRegistry.getById("fire-energy")).thenReturn(energy);

        GameSnapshot s = new GameSnapshot();
        InPlaySnapshot attackerSnap = new InPlaySnapshot();
        attackerSnap.setCardId("pkmn-1");
        attackerSnap.setInstanceId("inst-1");
        attackerSnap.setEnergyCardIds(List.of("fire-energy"));
        s.setAttacker(attackerSnap);
        PokemonCard pokemon = makePokemonCard("pkmn-1");
        when(cardRegistry.getById("pkmn-1")).thenReturn(pokemon);

        GameContext restored = s.toGameContext(cardRegistry);

        assertThat(restored.getAttacker()).isNotNull();
        assertThat(restored.getAttacker().getEnergies()).hasSize(1);
        assertThat(restored.getAttacker().getEnergies().get(0).getId()).isEqualTo("fire-energy");
    }

    @Test
    void toGameContext_WithTool_ReloadsToolCard() {
        PokemonCard tool = PokemonCard.builder()
                .id("tool-1").name("Tool").hp(0)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS)
                .attacks(List.of()).abilities(List.of())
                .weaknesses(List.of()).resistances(List.of())
                .costToAttack(List.of()).retreatCost(0)
                .build();
        when(cardRegistry.getById("tool-1")).thenReturn(tool);

        GameSnapshot s = new GameSnapshot();
        InPlaySnapshot attackerSnap = new InPlaySnapshot();
        attackerSnap.setCardId("pkmn-1");
        attackerSnap.setInstanceId("inst-1");
        attackerSnap.setToolCardId("tool-1");
        s.setAttacker(attackerSnap);
        PokemonCard pokemon = makePokemonCard("pkmn-1");
        when(cardRegistry.getById("pkmn-1")).thenReturn(pokemon);

        GameContext restored = s.toGameContext(cardRegistry);

        assertThat(restored.getAttacker().getTool()).isNotNull();
        assertThat(restored.getAttacker().getTool().getId()).isEqualTo("tool-1");
    }

    @Test
    void toGameContext_WithUnderlyingCards_ReloadsChain() {
        PokemonCard prevo = makePokemonCard("prevo");
        when(cardRegistry.getById("prevo")).thenReturn(prevo);

        GameSnapshot s = new GameSnapshot();
        InPlaySnapshot attackerSnap = new InPlaySnapshot();
        attackerSnap.setCardId("pkmn-1");
        attackerSnap.setInstanceId("inst-1");
        attackerSnap.setUnderlyingCardIds(List.of("prevo"));
        s.setAttacker(attackerSnap);
        PokemonCard pokemon = makePokemonCard("pkmn-1");
        when(cardRegistry.getById("pkmn-1")).thenReturn(pokemon);

        GameContext restored = s.toGameContext(cardRegistry);

        assertThat(restored.getAttacker().getUnderlyingCards()).hasSize(1);
        assertThat(restored.getAttacker().getUnderlyingCards().get(0).getId()).isEqualTo("prevo");
    }

    @Test
    void toGameContext_WithPlayedAndEvolved_RestoresSets() {
        PokemonCard played = makePokemonCard("played-1");
        when(cardRegistry.getById("played-1")).thenReturn(played);

        GameSnapshot s = new GameSnapshot();
        s.setAttackerPokemonPlayedThisTurn(List.of("played-1"));
        s.setAttackerEvolvedThisTurn(new ArrayList<>(List.of("inst-evolved")));

        GameContext restored = s.toGameContext(cardRegistry);

        assertThat(restored.getAttackerPokemonPlayedThisTurn()).hasSize(1);
        assertThat(restored.getAttackerPokemonPlayedThisTurn().get(0).getId()).isEqualTo("played-1");
        assertThat(restored.getAttackerEvolvedThisTurn()).contains("inst-evolved");
    }

    @Test
    void toGameContext_WhenCardNotFound_Throws() {
        when(cardRegistry.getById("unknown-id"))
                .thenThrow(new IllegalArgumentException("Card not found: unknown-id"));

        GameSnapshot s = new GameSnapshot();
        InPlaySnapshot attackerSnap = new InPlaySnapshot();
        attackerSnap.setCardId("unknown-id");
        attackerSnap.setInstanceId("inst-1");
        s.setAttacker(attackerSnap);

        assertThatThrownBy(() -> s.toGameContext(cardRegistry))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toGameContext_DisconnectFields_Preserved() {
        GameSnapshot s = new GameSnapshot();
        s.setDisconnectedPlayerId("player-42");
        s.setDisconnectedAtMs(123456789L);

        assertThat(s.getDisconnectedPlayerId()).isEqualTo("player-42");
        assertThat(s.getDisconnectedAtMs()).isEqualTo(123456789L);
    }

    @Test
    void settersAndGetters_WorkCorrectly() {
        GameSnapshot s = new GameSnapshot();
        s.setTurnNumber(3);
        s.setCurrentPhase("DRAW");
        s.setFirstTurn(false);
        s.setAttackerEnergyAttachmentsThisTurn(1);
        s.setAttackerAdditionalEnergyAttachments(2);
        s.setAttackerSupportersPlayedThisTurn(0);
        s.setAttackerCantPlaySupporters(true);
        s.setPrizesDealt(true);

        assertThat(s.getTurnNumber()).isEqualTo(3);
        assertThat(s.getCurrentPhase()).isEqualTo("DRAW");
        assertThat(s.isFirstTurn()).isFalse();
        assertThat(s.getAttackerEnergyAttachmentsThisTurn()).isEqualTo(1);
        assertThat(s.getAttackerAdditionalEnergyAttachments()).isEqualTo(2);
        assertThat(s.getAttackerSupportersPlayedThisTurn()).isZero();
        assertThat(s.isAttackerCantPlaySupporters()).isTrue();
        assertThat(s.isPrizesDealt()).isTrue();
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
