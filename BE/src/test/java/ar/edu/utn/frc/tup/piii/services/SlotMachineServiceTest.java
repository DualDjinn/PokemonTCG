package ar.edu.utn.frc.tup.piii.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.dtos.slot.SlotSpinResponse;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;

class SlotMachineServiceTest {

    private PlayerRepository playerRepository;
    private SlotMachineService service;
    private Player player;

    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        service = new SlotMachineService(playerRepository);

        player = new Player();
        player.setId("p1");
        player.setCoins(1000);
    }

    @Test
    void spin_Win_ReturnsUpdatedCoins() {
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        SlotSpinResponse response = service.spin("p1", 100, 500);

        assertThat(response.getRemainingCoins()).isEqualTo(1400);
        assertThat(player.getCoins()).isEqualTo(1400);
        verify(playerRepository).save(player);
    }

    @Test
    void spin_Lose_DeductsCoins() {
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        SlotSpinResponse response = service.spin("p1", 100, 0);

        assertThat(response.getRemainingCoins()).isEqualTo(900);
        verify(playerRepository).save(player);
    }

    @Test
    void spin_BetEqualsCoins_AllowsSpin() {
        player.setCoins(100);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        SlotSpinResponse response = service.spin("p1", 100, 0);

        assertThat(response.getRemainingCoins()).isEqualTo(0);
        verify(playerRepository).save(player);
    }

    @Test
    void spin_InsufficientCoins_Throws() {
        player.setCoins(50);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> service.spin("p1", 100, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient coins");

        verify(playerRepository, never()).save(player);
    }

    @Test
    void spin_PlayerNotFound_Throws() {
        when(playerRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.spin("unknown", 100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");
    }

    @Test
    void spin_ZeroBetWithWin_CreditsOnlyWin() {
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        SlotSpinResponse response = service.spin("p1", 0, 200);

        assertThat(response.getRemainingCoins()).isEqualTo(1200);
    }

    @Test
    void spin_LargeWin_CalculatesCorrectly() {
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        SlotSpinResponse response = service.spin("p1", 50, 10000);

        assertThat(response.getRemainingCoins()).isEqualTo(10950);
    }
}
