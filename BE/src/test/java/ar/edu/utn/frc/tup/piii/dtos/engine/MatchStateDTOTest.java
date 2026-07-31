package ar.edu.utn.frc.tup.piii.dtos.engine;

import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.Supertype;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchStateDTOTest {

    @Test
    void nestedDtos_StoreVisibleMatchInformation() {
        MatchStateDTO.CardViewDTO card = new MatchStateDTO.CardViewDTO();
        card.setId("xy1-1");
        card.setName("Venusaur-EX");
        card.setSupertype(Supertype.POKEMON);

        MatchStateDTO.InPlayPokemonViewDTO activePokemon = new MatchStateDTO.InPlayPokemonViewDTO();
        activePokemon.setCardId("xy1-1");
        activePokemon.setName("Venusaur-EX");
        activePokemon.setMaxHp(180);
        activePokemon.setCurrentHp(150);
        activePokemon.setDamageTaken(30);
        activePokemon.setAttachedEnergyCount(3);
        activePokemon.setKnockedOut(false);
        activePokemon.setStatuses(Set.of(StatusCondition.POISONED));

        MatchStateDTO.PlayerBattlefieldDTO self = new MatchStateDTO.PlayerBattlefieldDTO();
        self.setPlayerId("player-one");
        self.setHandSize(7);
        self.setRemainingPrizes(6);
        self.setHand(List.of(card));
        self.setActivePokemon(activePokemon);
        self.setBench(List.of());

        MatchStateDTO state = new MatchStateDTO();
        state.setMatchId("match-1");
        state.setRequestedPlayerId("player-one");
        state.setCurrentTurnPlayerId("player-one");
        state.setCurrentTurn(true);
        state.setWaitingForActiveSelection(false);
        state.setRequiresActiveSelection(false);
        state.setFinished(false);
        state.setSelf(self);

        assertThat(state.getMatchId()).isEqualTo("match-1");
        assertThat(state.getSelf().getHand()).containsExactly(card);
        assertThat(state.getSelf().getActivePokemon().getStatuses())
                .containsExactly(StatusCondition.POISONED);
        assertThat(state.getSelf().getActivePokemon().getAttachedEnergyCount()).isEqualTo(3);
    }
}
