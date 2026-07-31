package ar.edu.utn.frc.tup.piii.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import ar.edu.utn.frc.tup.piii.dtos.quest.NpcRewardClaimResponse;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.QuestService;

/**
 * Unit tests de {@link QuestController} sin levantar el contexto de Spring:
 * cubren el branch 404 (NPC sin recompensa) y la delegación al service.
 */
class QuestControllerUnitTest {

    private QuestService questService;
    private QuestController controller;
    private Player player;

    @BeforeEach
    void setUp() {
        questService = mock(QuestService.class);
        controller = new QuestController(questService);

        player = new Player();
        player.setId("p1");
    }

    @Test
    void getClaimedNpcRewards_DelegatesToService() {
        when(questService.getClaimedNpcIds("p1")).thenReturn(List.of("Don Alfajor"));

        var response = controller.getClaimedNpcRewards(player);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly("Don Alfajor");
    }

    @Test
    void claimNpcReward_NpcWithoutReward_Returns404() {
        when(questService.hasReward("Profesor Oak")).thenReturn(false);

        var response = controller.claimNpcReward(player, "Profesor Oak");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(questService, never()).claimReward(player, "Profesor Oak");
    }

    @Test
    void claimNpcReward_NpcWithReward_ReturnsClaimResult() {
        when(questService.hasReward("Don Alfajor")).thenReturn(true);
        NpcRewardClaimResponse claim = new NpcRewardClaimResponse("Don Alfajor", false, null);
        when(questService.claimReward(player, "Don Alfajor")).thenReturn(claim);

        var response = controller.claimNpcReward(player, "Don Alfajor");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(claim);
    }
}
