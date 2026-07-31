package ar.edu.utn.frc.tup.piii.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ar.edu.utn.frc.tup.piii.dtos.player.CardSummaryDTO;
import ar.edu.utn.frc.tup.piii.dtos.quest.NpcRewardClaimResponse;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerNpcReward;
import ar.edu.utn.frc.tup.piii.repositories.PlayerNpcRewardRepository;

class QuestServiceTest {

    private static final String NPC_WITH_REWARD = "Don Alfajor";
    private static final String REWARD_CARD_ID = "pokesecret-8";

    private PlayerNpcRewardRepository rewardRepository;
    private CollectionService collectionService;
    private QuestService questService;
    private Player player;

    @BeforeEach
    void setUp() {
        rewardRepository = mock(PlayerNpcRewardRepository.class);
        collectionService = mock(CollectionService.class);
        questService = new QuestService(rewardRepository, collectionService);

        player = new Player();
        player.setId("p1");
    }

    // ───────── hasReward ─────────

    @Test
    void hasReward_ConfiguredNpc_ReturnsTrue() {
        assertThat(questService.hasReward(NPC_WITH_REWARD)).isTrue();
    }

    @Test
    void hasReward_UnknownNpc_ReturnsFalse() {
        assertThat(questService.hasReward("NpcInexistente")).isFalse();
    }

    // ───────── getClaimedNpcIds ─────────

    @Test
    void getClaimedNpcIds_ReturnsNpcIdsOfClaims() {
        PlayerNpcReward claim = new PlayerNpcReward();
        claim.setNpcId(NPC_WITH_REWARD);
        when(rewardRepository.findByPlayer_Id("p1")).thenReturn(List.of(claim));

        assertThat(questService.getClaimedNpcIds("p1")).containsExactly(NPC_WITH_REWARD);
    }

    @Test
    void getClaimedNpcIds_NoClaims_ReturnsEmptyList() {
        when(rewardRepository.findByPlayer_Id("p1")).thenReturn(List.of());

        assertThat(questService.getClaimedNpcIds("p1")).isEmpty();
    }

    // ───────── claimReward ─────────

    @Test
    void claimReward_FirstClaim_GrantsCardAndRecordsClaim() {
        when(rewardRepository.existsByPlayer_IdAndNpcId("p1", NPC_WITH_REWARD)).thenReturn(false);
        CardSummaryDTO card = new CardSummaryDTO();
        card.setId(REWARD_CARD_ID);
        card.setName("I've Become So Numb");
        when(collectionService.getCardSummary("p1", REWARD_CARD_ID)).thenReturn(card);

        NpcRewardClaimResponse response = questService.claimReward(player, NPC_WITH_REWARD);

        assertThat(response.isAlreadyClaimed()).isFalse();
        assertThat(response.getNpcId()).isEqualTo(NPC_WITH_REWARD);
        assertThat(response.getCard()).isNotNull();
        assertThat(response.getCard().getId()).isEqualTo(REWARD_CARD_ID);

        verify(collectionService).addToCollection("p1", REWARD_CARD_ID);

        ArgumentCaptor<PlayerNpcReward> captor = ArgumentCaptor.forClass(PlayerNpcReward.class);
        verify(rewardRepository).save(captor.capture());
        PlayerNpcReward saved = captor.getValue();
        assertThat(saved.getNpcId()).isEqualTo(NPC_WITH_REWARD);
        assertThat(saved.getCardId()).isEqualTo(REWARD_CARD_ID);
        assertThat(saved.getPlayer()).isSameAs(player);
        assertThat(saved.getClaimedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void claimReward_AlreadyClaimed_DoesNotGrantAgain() {
        when(rewardRepository.existsByPlayer_IdAndNpcId("p1", NPC_WITH_REWARD)).thenReturn(true);

        NpcRewardClaimResponse response = questService.claimReward(player, NPC_WITH_REWARD);

        assertThat(response.isAlreadyClaimed()).isTrue();
        assertThat(response.getCard()).isNull();
        verify(collectionService, never()).addToCollection(any(), any());
        verify(rewardRepository, never()).save(any());
    }

    @Test
    void claimReward_UnknownNpc_Throws() {
        assertThatThrownBy(() -> questService.claimReward(player, "NpcInexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NpcInexistente");

        verify(collectionService, never()).addToCollection(any(), any());
    }
}
