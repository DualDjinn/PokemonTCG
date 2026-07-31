package ar.edu.utn.frc.tup.piii.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.frc.tup.piii.dtos.cardback.CardBackDTO;
import ar.edu.utn.frc.tup.piii.dtos.cardback.PlayerCardBacksDTO;
import ar.edu.utn.frc.tup.piii.models.CardBack;
import ar.edu.utn.frc.tup.piii.models.CardBackOrigin;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerCardBack;
import ar.edu.utn.frc.tup.piii.repositories.CardBackRepository;
import ar.edu.utn.frc.tup.piii.repositories.IdempotencyRecordRepository;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardBackRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

class CardBackServiceTest {

    private CardBackRepository cardBackRepository;
    private PlayerCardBackRepository playerCardBackRepository;
    private PlayerRepository playerRepository;
    private MatchRepository matchRepository;
    private CardBackService service;
    private Player player;

    private final CardBack shopBack = CardBack.builder()
            .id("bio").name("Bio").imageUrl("http://minio/back-card/bio.png")
            .origin(CardBackOrigin.SHOP).price(200).build();
    private final CardBack packBack = CardBack.builder()
            .id("tori").name("Tori").imageUrl("http://minio/back-card/tori.png")
            .origin(CardBackOrigin.PACK).build();
    private final CardBack battleBack = CardBack.builder()
            .id("mundial-2026").name("Mundial 2026").imageUrl("http://minio/back-card/m26.png")
            .origin(CardBackOrigin.BATTLE).unlockValue(3).build();
    private final CardBack levelBack = CardBack.builder()
            .id("nivel-5").name("Nivel 5").imageUrl("http://minio/back-card/n5.png")
            .origin(CardBackOrigin.LEVEL).unlockValue(5).build();
    private final CardBack defaultBack = CardBack.builder()
            .id("pixel").name("Pixel").imageUrl("http://minio/back-card/pixel.png")
            .origin(CardBackOrigin.DEFAULT).build();

    @BeforeEach
    void setUp() {
        cardBackRepository = mock(CardBackRepository.class);
        playerCardBackRepository = mock(PlayerCardBackRepository.class);
        playerRepository = mock(PlayerRepository.class);
        matchRepository = mock(MatchRepository.class);
        service = new CardBackService(cardBackRepository, playerCardBackRepository,
                playerRepository, matchRepository,
                new IdempotencyService(mock(IdempotencyRecordRepository.class), new ObjectMapper()));

        player = new Player();
        player.setId("p1");
        player.setCoins(1000);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(playerCardBackRepository.findByPlayer_Id("p1")).thenReturn(List.of());
        when(cardBackRepository.findAll()).thenReturn(
                List.of(shopBack, packBack, battleBack, levelBack));
    }

    // ───────── purchase ─────────

    @Test
    void purchase_ShopBack_DebitsCoinsAndGrants() {
        when(cardBackRepository.findById("bio")).thenReturn(Optional.of(shopBack));
        when(playerCardBackRepository.existsByPlayer_IdAndCardBack_Id("p1", "bio")).thenReturn(false);

        service.purchase("p1", "bio", null);

        assertThat(player.getCoins()).isEqualTo(800);
        ArgumentCaptor<PlayerCardBack> captor = ArgumentCaptor.forClass(PlayerCardBack.class);
        verify(playerCardBackRepository).save(captor.capture());
        assertThat(captor.getValue().getCardBack().getId()).isEqualTo("bio");
    }

    @Test
    void purchase_InsufficientCoins_Conflict() {
        player.setCoins(100);
        when(cardBackRepository.findById("bio")).thenReturn(Optional.of(shopBack));
        when(playerCardBackRepository.existsByPlayer_IdAndCardBack_Id("p1", "bio")).thenReturn(false);

        assertThatThrownBy(() -> service.purchase("p1", "bio", null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(playerCardBackRepository, never()).save(any());
    }

    @Test
    void purchase_AlreadyOwned_Conflict() {
        when(cardBackRepository.findById("bio")).thenReturn(Optional.of(shopBack));
        when(playerCardBackRepository.existsByPlayer_IdAndCardBack_Id("p1", "bio")).thenReturn(true);

        assertThatThrownBy(() -> service.purchase("p1", "bio", null))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(player.getCoins()).isEqualTo(1000);
    }

    @Test
    void purchase_NotForSale_Conflict() {
        when(cardBackRepository.findById("tori")).thenReturn(Optional.of(packBack));

        assertThatThrownBy(() -> service.purchase("p1", "tori", null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void purchase_UnknownBack_BadRequest() {
        when(cardBackRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.purchase("p1", "nope", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ───────── grant ─────────

    @Test
    void grant_NewBack_SavesOwnership() {
        when(playerCardBackRepository.existsByPlayer_IdAndCardBack_Id("p1", "tori")).thenReturn(false);
        when(cardBackRepository.findById("tori")).thenReturn(Optional.of(packBack));

        assertThat(service.grant("p1", "tori")).isTrue();
        verify(playerCardBackRepository).save(any());
    }

    @Test
    void grant_AlreadyOwned_IsIdempotent() {
        when(playerCardBackRepository.existsByPlayer_IdAndCardBack_Id("p1", "tori")).thenReturn(true);

        assertThat(service.grant("p1", "tori")).isFalse();
        verify(playerCardBackRepository, never()).save(any());
    }

    // ───────── equip ─────────

    @Test
    void equip_OwnedBack_SetsActive() {
        when(playerCardBackRepository.existsByPlayer_IdAndCardBack_Id("p1", "bio")).thenReturn(true);

        service.equip("p1", "bio");

        assertThat(player.getActiveCardBackId()).isEqualTo("bio");
        verify(playerRepository).save(player);
    }

    @Test
    void equip_UnownedBack_Conflict() {
        when(playerCardBackRepository.existsByPlayer_IdAndCardBack_Id("p1", "bio")).thenReturn(false);

        assertThatThrownBy(() -> service.equip("p1", "bio"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(player.getActiveCardBackId()).isNull();
    }

    @Test
    void equip_Null_RestoresDefault() {
        player.setActiveCardBackId("bio");

        PlayerCardBacksDTO result = service.equip("p1", null);

        assertThat(player.getActiveCardBackId()).isNull();
        assertThat(result.getActiveCardBackId()).isNull();
    }

    // ───────── syncProgressUnlocks ─────────

    @Test
    void syncProgressUnlocks_ThresholdsMet_GrantsBattleAndLevelBacks() {
        when(matchRepository.countByWinnerId("p1")).thenReturn(3L);
        player.setExperience(10_000); // nivel alto, supera el umbral 5
        when(playerCardBackRepository.existsByPlayer_IdAndCardBack_Id(any(), any())).thenReturn(false);
        when(cardBackRepository.findById("mundial-2026")).thenReturn(Optional.of(battleBack));
        when(cardBackRepository.findById("nivel-5")).thenReturn(Optional.of(levelBack));

        service.syncProgressUnlocks("p1");

        verify(playerCardBackRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void syncProgressUnlocks_ThresholdsNotMet_GrantsNothing() {
        when(matchRepository.countByWinnerId("p1")).thenReturn(0L);
        player.setExperience(0);

        service.syncProgressUnlocks("p1");

        verify(playerCardBackRepository, never()).save(any());
    }

    @Test
    void syncProgressUnlocks_DefaultOrigin_GrantsWithoutProgress() {
        when(cardBackRepository.findAll()).thenReturn(List.of(defaultBack));
        when(matchRepository.countByWinnerId("p1")).thenReturn(0L);
        player.setExperience(0);
        when(playerCardBackRepository.existsByPlayer_IdAndCardBack_Id("p1", "pixel")).thenReturn(false);
        when(cardBackRepository.findById("pixel")).thenReturn(Optional.of(defaultBack));

        service.syncProgressUnlocks("p1");

        ArgumentCaptor<PlayerCardBack> captor = ArgumentCaptor.forClass(PlayerCardBack.class);
        verify(playerCardBackRepository).save(captor.capture());
        assertThat(captor.getValue().getCardBack().getId()).isEqualTo("pixel");
    }

    // ───────── dropRandomPackCardBack ─────────

    @Test
    void dropRandomPackCardBack_GrantsUnownedPackBack() {
        when(playerCardBackRepository.existsByPlayer_IdAndCardBack_Id("p1", "tori")).thenReturn(false);
        when(cardBackRepository.findById("tori")).thenReturn(Optional.of(packBack));

        CardBackDTO dropped = service.dropRandomPackCardBack("p1");

        assertThat(dropped).isNotNull();
        assertThat(dropped.getId()).isEqualTo("tori");
        assertThat(dropped.getOrigin()).isEqualTo("PACK");
    }

    @Test
    void dropRandomPackCardBack_AllOwned_ReturnsNull() {
        PlayerCardBack owned = new PlayerCardBack();
        owned.setCardBack(packBack);
        when(playerCardBackRepository.findByPlayer_Id("p1")).thenReturn(List.of(owned));

        assertThat(service.dropRandomPackCardBack("p1")).isNull();
        verify(playerCardBackRepository, never()).save(any());
    }
}
