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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ar.edu.utn.frc.tup.piii.dtos.skin.CardSkinDTO;
import ar.edu.utn.frc.tup.piii.dtos.skin.PlayerSkinDTO;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardRarity;
import ar.edu.utn.frc.tup.piii.models.CardSkin;
import ar.edu.utn.frc.tup.piii.models.CardSkinOrigin;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerCard;
import ar.edu.utn.frc.tup.piii.models.PlayerSkin;
import ar.edu.utn.frc.tup.piii.models.Supertype;
import ar.edu.utn.frc.tup.piii.repositories.CardSkinRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerSkinRepository;

class SkinServiceTest {

    private CardSkinRepository cardSkinRepository;
    private PlayerSkinRepository playerSkinRepository;
    private PlayerCardRepository playerCardRepository;
    private SkinService service;

    private Card baseCard;
    private CardSkin skin;
    private Player player;

    @BeforeEach
    void setUp() {
        cardSkinRepository = mock(CardSkinRepository.class);
        playerSkinRepository = mock(PlayerSkinRepository.class);
        playerCardRepository = mock(PlayerCardRepository.class);
        service = new SkinService(cardSkinRepository, playerSkinRepository, playerCardRepository);

        baseCard = new Card() {
            {
                try {
                    var idField = Card.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(this, "xy1-1");
                    var nameField = Card.class.getDeclaredField("name");
                    nameField.setAccessible(true);
                    nameField.set(this, "Venusaur");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Supertype getSupertype() {
                return Supertype.POKEMON;
            }

            @Override
            public boolean canBePlayed(GameContext ctx) {
                return false;
            }
        };

        skin = CardSkin.builder()
                .id("skin-1")
                .name("Full Art Venusaur")
                .baseCard(baseCard)
                .origin(CardSkinOrigin.OFFICIAL)
                .imageUrl("skin-url")
                .imageUrlLarge("skin-url-large")
                .skinTag("FULL_ART")
                .build();

        player = new Player();
        player.setId("p1");
    }

    // ───────── getAllSkins ─────────

    @Test
    void getAllSkins_ReturnsMappedDTOs() {
        when(cardSkinRepository.findAll()).thenReturn(List.of(skin));

        List<CardSkinDTO> result = service.getAllSkins();

        assertThat(result).hasSize(1);
        CardSkinDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo("skin-1");
        assertThat(dto.getName()).isEqualTo("Full Art Venusaur");
        assertThat(dto.getBaseCardId()).isEqualTo("xy1-1");
        assertThat(dto.getOrigin()).isEqualTo("OFFICIAL");
        assertThat(dto.getImageUrl()).isEqualTo("skin-url");
        assertThat(dto.getTag()).isEqualTo("FULL_ART");
    }

    @Test
    void getAllSkins_EmptyCatalog_ReturnsEmpty() {
        when(cardSkinRepository.findAll()).thenReturn(List.of());
        assertThat(service.getAllSkins()).isEmpty();
    }

    // ───────── getSkinsForCard ─────────

    @Test
    void getSkinsForCard_ReturnsSkinsForBaseCard() {
        when(cardSkinRepository.findByBaseCard_Id("xy1-1")).thenReturn(List.of(skin));

        List<CardSkinDTO> result = service.getSkinsForCard("xy1-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBaseCardId()).isEqualTo("xy1-1");
    }

    @Test
    void getSkinsForCard_NoSkins_ReturnsEmpty() {
        when(cardSkinRepository.findByBaseCard_Id("xy1-99")).thenReturn(List.of());
        assertThat(service.getSkinsForCard("xy1-99")).isEmpty();
    }

    // ───────── getPlayerSkins ─────────

    @Test
    void getPlayerSkins_WithEquippedSkin_MarksEquipped() {
        PlayerCard playerCard = new PlayerCard();
        playerCard.setCard(baseCard);
        playerCard.setActiveSkin(skin);
        when(playerCardRepository.findByPlayer_Id("p1")).thenReturn(List.of(playerCard));

        PlayerSkin ps = new PlayerSkin();
        ps.setPlayer(player);
        ps.setSkin(skin);
        ps.setQuantity(1);
        ps.setAcquiredAt(LocalDateTime.now());
        when(playerSkinRepository.findByPlayer_Id("p1")).thenReturn(List.of(ps));

        List<PlayerSkinDTO> result = service.getPlayerSkins("p1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isEquipped()).isTrue();
        assertThat(result.get(0).getSkinId()).isEqualTo("skin-1");
        assertThat(result.get(0).getSkinName()).isEqualTo("Full Art Venusaur");
        assertThat(result.get(0).getBaseCardId()).isEqualTo("xy1-1");
        assertThat(result.get(0).getBaseCardName()).isEqualTo("Venusaur");
        assertThat(result.get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    void getPlayerSkins_WithoutEquippedSkin_MarksNotEquipped() {
        PlayerCard playerCard = new PlayerCard();
        playerCard.setCard(baseCard);
        playerCard.setActiveSkin(null);
        when(playerCardRepository.findByPlayer_Id("p1")).thenReturn(List.of(playerCard));

        PlayerSkin ps = new PlayerSkin();
        ps.setPlayer(player);
        ps.setSkin(skin);
        ps.setQuantity(2);
        ps.setAcquiredAt(LocalDateTime.now());
        when(playerSkinRepository.findByPlayer_Id("p1")).thenReturn(List.of(ps));

        List<PlayerSkinDTO> result = service.getPlayerSkins("p1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isEquipped()).isFalse();
    }

    @Test
    void getPlayerSkins_NoSkins_ReturnsEmpty() {
        when(playerCardRepository.findByPlayer_Id("p1")).thenReturn(List.of());
        when(playerSkinRepository.findByPlayer_Id("p1")).thenReturn(List.of());

        assertThat(service.getPlayerSkins("p1")).isEmpty();
    }

    // ───────── equipSkin ─────────

    @Test
    void equipSkin_Valid_SetsSkinOnPlayerCard() {
        PlayerCard playerCard = new PlayerCard();
        playerCard.setCard(baseCard);

        when(cardSkinRepository.findById("skin-1")).thenReturn(Optional.of(skin));
        when(playerSkinRepository.findByPlayer_IdAndSkin_Id("p1", "skin-1"))
                .thenReturn(Optional.of(new PlayerSkin()));
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "xy1-1"))
                .thenReturn(Optional.of(playerCard));

        service.equipSkin("p1", "xy1-1", "skin-1");

        assertThat(playerCard.getActiveSkin()).isEqualTo(skin);
        verify(playerCardRepository).save(playerCard);
    }

    @Test
    void equipSkin_SkinNotFound_Throws() {
        when(cardSkinRepository.findById("none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.equipSkin("p1", "xy1-1", "none"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Skin not found");
    }

    @Test
    void equipSkin_SkinDoesNotMatchCard_Throws() {
        Card otherCard = new Card() {
            {
                try {
                    var f = Card.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(this, "xy1-99");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Supertype getSupertype() {
                return Supertype.POKEMON;
            }

            @Override
            public boolean canBePlayed(GameContext ctx) {
                return false;
            }
        };
        CardSkin wrongSkin = CardSkin.builder()
                .id("skin-2").name("Other").baseCard(otherCard)
                .origin(CardSkinOrigin.OFFICIAL).imageUrl("x").imageUrlLarge("x").build();

        when(cardSkinRepository.findById("skin-2")).thenReturn(Optional.of(wrongSkin));

        assertThatThrownBy(() -> service.equipSkin("p1", "xy1-1", "skin-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not apply to card");
    }

    @Test
    void equipSkin_PlayerDoesNotOwnSkin_Throws() {
        when(cardSkinRepository.findById("skin-1")).thenReturn(Optional.of(skin));
        when(playerSkinRepository.findByPlayer_IdAndSkin_Id("p1", "skin-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.equipSkin("p1", "xy1-1", "skin-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Player does not own skin");
    }

    @Test
    void equipSkin_PlayerDoesNotOwnCard_Throws() {
        when(cardSkinRepository.findById("skin-1")).thenReturn(Optional.of(skin));
        when(playerSkinRepository.findByPlayer_IdAndSkin_Id("p1", "skin-1"))
                .thenReturn(Optional.of(new PlayerSkin()));
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "xy1-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.equipSkin("p1", "xy1-1", "skin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player does not own card");
    }

    // ───────── unequipSkin ─────────

    @Test
    void unequipSkin_ClearsActiveSkin() {
        PlayerCard playerCard = new PlayerCard();
        playerCard.setCard(baseCard);
        playerCard.setActiveSkin(skin);

        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "xy1-1"))
                .thenReturn(Optional.of(playerCard));

        service.unequipSkin("p1", "xy1-1");

        assertThat(playerCard.getActiveSkin()).isNull();
        verify(playerCardRepository).save(playerCard);
    }

    @Test
    void unequipSkin_PlayerDoesNotOwnCard_Throws() {
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "xy1-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unequipSkin("p1", "xy1-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player does not own card");
    }

    // ───────── grantSkin ─────────

    @Test
    void grantSkin_NewSkin_CreatesPlayerSkin() {
        when(cardSkinRepository.findById("skin-1")).thenReturn(Optional.of(skin));
        when(playerSkinRepository.findByPlayer_IdAndSkin_Id("p1", "skin-1"))
                .thenReturn(Optional.empty());

        service.grantSkin("p1", "skin-1", 1, player);

        ArgumentCaptor<PlayerSkin> captor = ArgumentCaptor.forClass(PlayerSkin.class);
        verify(playerSkinRepository).save(captor.capture());
        PlayerSkin saved = captor.getValue();
        assertThat(saved.getPlayer()).isSameAs(player);
        assertThat(saved.getSkin()).isSameAs(skin);
        assertThat(saved.getQuantity()).isEqualTo(1);
        assertThat(saved.getAcquiredAt()).isNotNull();
    }

    @Test
    void grantSkin_ExistingSkin_IncrementsQuantity() {
        PlayerSkin existing = new PlayerSkin();
        existing.setPlayer(player);
        existing.setSkin(skin);
        existing.setQuantity(2);
        existing.setAcquiredAt(LocalDateTime.now());

        when(cardSkinRepository.findById("skin-1")).thenReturn(Optional.of(skin));
        when(playerSkinRepository.findByPlayer_IdAndSkin_Id("p1", "skin-1"))
                .thenReturn(Optional.of(existing));

        service.grantSkin("p1", "skin-1", 3, player);

        ArgumentCaptor<PlayerSkin> captor = ArgumentCaptor.forClass(PlayerSkin.class);
        verify(playerSkinRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(5);
    }

    @Test
    void grantSkin_SkinNotFound_Throws() {
        when(cardSkinRepository.findById("none")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.grantSkin("p1", "none", 1, player))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Skin not found");
    }
}
