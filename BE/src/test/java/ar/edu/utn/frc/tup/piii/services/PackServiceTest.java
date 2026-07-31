package ar.edu.utn.frc.tup.piii.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ar.edu.utn.frc.tup.piii.dtos.pack.OpenedPackResponse;
import ar.edu.utn.frc.tup.piii.dtos.pack.PackInfoDTO;
import ar.edu.utn.frc.tup.piii.dtos.skin.PlayerSkinDTO;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardRarity;
import ar.edu.utn.frc.tup.piii.models.CardSkin;
import ar.edu.utn.frc.tup.piii.models.CardSkinOrigin;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.PackPurchase;
import ar.edu.utn.frc.tup.piii.models.PackType;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerCard;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.IdempotencyRecordRepository;
import ar.edu.utn.frc.tup.piii.repositories.PackPurchaseRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PackService}.
 *
 * <p>Uso Mockito puro (sin anotaciones Spring) — el servicio es puro Java
 * y no requiere contexto. Los repositorios y el randomizer se mockean.
 */
class PackServiceTest {

    private PlayerRepository playerRepository;
    private CardRepository cardRepository;
    private PlayerCardRepository playerCardRepository;
    private PackPurchaseRepository packPurchaseRepository;
    private PackRandomizer packRandomizer;
    private ar.edu.utn.frc.tup.piii.repositories.CardSkinRepository cardSkinRepository;
    private SkinService skinService;
    private PackService packService;

    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        cardRepository = mock(CardRepository.class);
        playerCardRepository = mock(PlayerCardRepository.class);
        packPurchaseRepository = mock(PackPurchaseRepository.class);
        packRandomizer = mock(PackRandomizer.class);
        cardSkinRepository = mock(ar.edu.utn.frc.tup.piii.repositories.CardSkinRepository.class);
        skinService = mock(SkinService.class);

        packService = new PackService(
                playerRepository, cardRepository, playerCardRepository,
                packPurchaseRepository, packRandomizer,
                cardSkinRepository, skinService,
                mock(CardBackService.class),
                new IdempotencyService(mock(IdempotencyRecordRepository.class), new ObjectMapper()));

        // El save retorna lo que le pasen (común para tests)
        when(playerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(playerCardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(packPurchaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ============================ helpers ============================

    private Player buildPlayer(int coins) {
        Player p = new Player();
        p.setId("p1");
        p.setUsername("test");
        p.setTrainerName("Ash");
        p.setEmail("a@b.c");
        p.setPassword("x");
        p.setCoins(coins);
        return p;
    }

    private PokemonCard pokeCard(String id, CardRarity rarity) {
        PokemonCard p = PokemonCard.builder()
                .id(id).name("Card-" + id).hp(60)
                .stage(PokemonStage.BASIC).isEx(false).isMega(false)
                .pokemonType(EnergyType.GRASS)
                .attacks(List.of()).weaknesses(List.of()).resistances(List.of())
                .retreatCost(1).evolvesFrom(null).evolvesTo(List.of()).rules(List.of())
                .build();
        p.setRarity(rarity);
        return p;
    }

    private EnergyCard basicEnergyCard(String id) {
        EnergyCard e = EnergyCard.builder()
                .id(id).name("Energy-" + id)
                .energyType(EnergyType.FIRE)
                .rules(List.of())
                .build();
        e.setRarity(CardRarity.COMMON);  // las básicas son COMMON
        return e;
    }

    private EnergyCard specialEnergyCard(String id) {
        EnergyCard e = EnergyCard.builder()
                .id(id).name("SpecialEnergy-" + id)
                .energyType(EnergyType.COLORLESS)
                .rules(List.of("special rule"))
                .build();
        e.setRarity(CardRarity.UNCOMMON);
        return e;
    }

    private List<Card> sampleCatalog() {
        return new ArrayList<>(List.of(
                pokeCard("c1", CardRarity.COMMON),
                pokeCard("c2", CardRarity.UNCOMMON),
                pokeCard("c3", CardRarity.RARE)));
    }

    // ============================ listPacks ============================

    @Test
    void listPacksReturnsAllPackTypes() {
        List<PackInfoDTO> packs = packService.listPacks();

        assertThat(packs).hasSize(4);
        assertThat(packs).extracting(PackInfoDTO::getType)
                .containsExactly("BRONZE", "SILVER", "GOLD", "DIAMOND");
    }

    @Test
    void listPacksReturnsCorrectPricesForEachType() {
        List<PackInfoDTO> packs = packService.listPacks();
        assertThat(packs.get(0).getPrice()).isEqualTo(PackType.BRONZE.getPrice());
        assertThat(packs.get(1).getPrice()).isEqualTo(PackType.SILVER.getPrice());
        assertThat(packs.get(2).getPrice()).isEqualTo(PackType.GOLD.getPrice());
        assertThat(packs.get(3).getPrice()).isEqualTo(PackType.DIAMOND.getPrice());
    }

    @Test
    void listPacksReturnsCardsPerPackByType() {
        for (PackInfoDTO pack : packService.listPacks()) {
            int expected = "DIAMOND".equals(pack.getType()) ? 1 : PackType.CARDS_PER_PACK;
            assertThat(pack.getCardsPerPack()).isEqualTo(expected);
        }
    }

    @Test
    void listPacksIncludesAllBucketProbabilities() {
        for (PackInfoDTO pack : packService.listPacks()) {
            Map<String, Double> probs = pack.getProbabilities();
            if ("DIAMOND".equals(pack.getType())) {
                // El sobre full-art no sortea por rareza.
                assertThat(probs).isEmpty();
                continue;
            }
            assertThat(probs).containsKeys("HIGH", "MEDIUM", "LOW");
            double sum = probs.values().stream().mapToDouble(Double::doubleValue).sum();
            assertThat(sum).isCloseTo(1.0, org.assertj.core.api.Assertions.within(0.001));
        }
    }

    @Test
    void listPacksDoesNotHitDatabase() {
        packService.listPacks();
        verify(cardRepository, never()).findAll();
        verify(playerRepository, never()).findById(any());
    }

    // ============================ openPack — happy path ============================

    @Test
    void openPackDeductsCoinsFromPlayer() {
        Player player = buildPlayer(500);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(sampleCatalog());
        when(packRandomizer.drawCards(any(), anyList()))
                .thenReturn(List.of(pokeCard("c1", CardRarity.COMMON),
                                    pokeCard("c2", CardRarity.UNCOMMON),
                                    pokeCard("c3", CardRarity.RARE)));

        OpenedPackResponse response = packService.openPack("p1", "BRONZE", null);

        assertThat(player.getCoins()).isEqualTo(500 - PackType.BRONZE.getPrice());
        assertThat(response.getRemainingCoins()).isEqualTo(player.getCoins());
    }

    @Test
    void openPackReturnsCorrectResponseFormat() {
        Player player = buildPlayer(500);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(sampleCatalog());
        when(packRandomizer.drawCards(any(), anyList()))
                .thenReturn(List.of(pokeCard("a", CardRarity.COMMON),
                                    pokeCard("b", CardRarity.UNCOMMON),
                                    pokeCard("c", CardRarity.RARE)));

        OpenedPackResponse response = packService.openPack("p1", "SILVER", null);

        assertThat(response.getPackType()).isEqualTo("SILVER");
        assertThat(response.getCards()).hasSize(3);
        assertThat(response.getCards()).extracting("id").containsExactly("a", "b", "c");
        assertThat(response.getCards()).extracting("name").containsExactly("Card-a", "Card-b", "Card-c");
        assertThat(response.getCards()).extracting("rarity").containsExactly("COMMON", "UNCOMMON", "RARE");
    }

    @Test
    void openPackAddsNewCardsToPlayerCollection() {
        Player player = buildPlayer(500);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(sampleCatalog());
        Card c1 = pokeCard("c1", CardRarity.COMMON);
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(c1, c1, c1));

        packService.openPack("p1", "BRONZE", null);

        assertThat(player.getCollection()).hasSize(1);
        assertThat(player.getCollection().get(0).getId()).isEqualTo("c1");
    }

    @Test
    void openPackDoesNotDuplicateCardInCollectionIfAlreadyOwned() {
        Player player = buildPlayer(500);
        Card existing = pokeCard("c1", CardRarity.COMMON);
        player.getCollection().add(existing);

        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(sampleCatalog());
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(existing, existing, existing));

        packService.openPack("p1", "BRONZE", null);

        // La colección "set de únicos" sigue teniendo 1 sola entrada de c1.
        assertThat(player.getCollection()).hasSize(1);
    }

    @Test
    void openPackCreatesNewPlayerCardWhenCardNotOwned() {
        Player player = buildPlayer(500);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(sampleCatalog());
        Card drawn = pokeCard("c1", CardRarity.COMMON);
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(drawn, drawn, drawn));

        // Simulamos persistencia: el primer find devuelve empty, después devuelve lo guardado.
        // Esto refleja el comportamiento real de la BD: tras `save`, el siguiente `findBy`
        // ya encuentra la fila.
        final PlayerCard[] persisted = { null };
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "c1"))
                .thenAnswer(inv -> persisted[0] == null ? Optional.empty() : Optional.of(persisted[0]));
        when(playerCardRepository.save(any(PlayerCard.class)))
                .thenAnswer(inv -> {
                    persisted[0] = inv.getArgument(0);
                    return persisted[0];
                });

        packService.openPack("p1", "BRONZE", null);

        // 3 cartas iguales → save llamado 3 veces, todas sobre la misma fila lógica.
        ArgumentCaptor<PlayerCard> captor = ArgumentCaptor.forClass(PlayerCard.class);
        verify(playerCardRepository, times(3)).save(captor.capture());
        // La fila final tiene quantity = 3.
        assertThat(persisted[0]).isNotNull();
        assertThat(persisted[0].getQuantity()).isEqualTo(3);
    }

    @Test
    void openPackIncrementsExistingPlayerCardQuantity() {
        Player player = buildPlayer(500);
        Card drawn = pokeCard("c1", CardRarity.COMMON);
        PlayerCard existing = new PlayerCard();
        existing.setPlayer(player);
        existing.setCard(drawn);
        existing.setQuantity(5);

        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(sampleCatalog());
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(drawn, drawn, drawn));
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "c1")).thenReturn(Optional.of(existing));

        packService.openPack("p1", "BRONZE", null);

        assertThat(existing.getQuantity()).isEqualTo(5 + 3);
    }

    @Test
    void openPackSavesPackPurchaseHistory() {
        Player player = buildPlayer(500);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(sampleCatalog());
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(
                pokeCard("a", CardRarity.COMMON),
                pokeCard("b", CardRarity.UNCOMMON),
                pokeCard("c", CardRarity.RARE)));

        packService.openPack("p1", "GOLD", null);

        ArgumentCaptor<PackPurchase> captor = ArgumentCaptor.forClass(PackPurchase.class);
        verify(packPurchaseRepository).save(captor.capture());
        PackPurchase saved = captor.getValue();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getPlayerId()).isEqualTo("p1");
        assertThat(saved.getPackType()).isEqualTo(PackType.GOLD);
        assertThat(saved.getPrice()).isEqualTo(PackType.GOLD.getPrice());
        assertThat(saved.getCardIds()).isEqualTo("a,b,c");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void openPackSavesPlayerToRepository() {
        Player player = buildPlayer(500);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(sampleCatalog());
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(
                pokeCard("c1", CardRarity.COMMON),
                pokeCard("c2", CardRarity.COMMON),
                pokeCard("c3", CardRarity.COMMON)));

        packService.openPack("p1", "BRONZE", null);

        verify(playerRepository).save(player);
    }

    @Test
    void openPackAcceptsLowercasePackType() {
        Player player = buildPlayer(500);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(sampleCatalog());
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(
                pokeCard("c1", CardRarity.COMMON),
                pokeCard("c1", CardRarity.COMMON),
                pokeCard("c1", CardRarity.COMMON)));

        // "bronze" en minúsculas debería funcionar
        OpenedPackResponse response = packService.openPack("p1", "bronze", null);
        assertThat(response.getPackType()).isEqualTo("BRONZE");
    }

    // ============================ openPack — errores ============================

    @Test
    void openPackThrowsWhenPlayerNotFound() {
        when(playerRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> packService.openPack("nope", "BRONZE", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");

        verify(playerRepository, never()).save(any());
        verify(packPurchaseRepository, never()).save(any());
    }

    @Test
    void openPackThrowsWhenPackTypeIsNull() {
        assertThatThrownBy(() -> packService.openPack("p1", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("packType");

        // Si el packType es null, ni siquiera debe consultar el player
        verify(playerRepository, never()).findById(any());
    }

    @Test
    void openPackThrowsWhenPackTypeIsInvalid() {
        assertThatThrownBy(() -> packService.openPack("p1", "PLATINUM", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid pack type");
    }

    @Test
    void openPackThrowsWhenPackTypeIsEmptyString() {
        assertThatThrownBy(() -> packService.openPack("p1", "", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void openPackThrowsWhenInsufficientCoins() {
        Player player = buildPlayer(50); // < 100 que cuesta BRONZE
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> packService.openPack("p1", "BRONZE", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient coins");

        // No debe modificar nada
        assertThat(player.getCoins()).isEqualTo(50);
        verify(playerRepository, never()).save(any());
        verify(packPurchaseRepository, never()).save(any());
        verify(packRandomizer, never()).drawCards(any(), anyList());
    }

    @Test
    void openPackWithExactlyEnoughCoinsSucceeds() {
        // Edge case: el jugador tiene EXACTAMENTE el precio del sobre.
        Player player = buildPlayer(PackType.BRONZE.getPrice());
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(sampleCatalog());
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(
                pokeCard("a", CardRarity.COMMON),
                pokeCard("b", CardRarity.COMMON),
                pokeCard("c", CardRarity.COMMON)));

        OpenedPackResponse response = packService.openPack("p1", "BRONZE", null);

        assertThat(response.getRemainingCoins()).isZero();
    }

    @Test
    void openPackThrowsWhenCatalogIsEmpty() {
        Player player = buildPlayer(500);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> packService.openPack("p1", "BRONZE", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");

        // Ya validó las monedas pero no descontó ni guardó nada
        assertThat(player.getCoins()).isEqualTo(500);
    }

    @Test
    void openPackThrowsWhenCatalogHasOnlyExcludedCards() {
        Player player = buildPlayer(500);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        // Solo energías básicas y pokesecret — todas excluidas
        Card basicEnergy = basicEnergyCard("xy1-132");
        Card pokesecret = pokeCard("pokesecret-1", CardRarity.UNCOMMON);
        when(cardRepository.findAll()).thenReturn(List.of(basicEnergy, pokesecret));

        assertThatThrownBy(() -> packService.openPack("p1", "BRONZE", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
    }

    // ============================ openPack — filtros del pool ============================

    @Test
    void openPackExcludesBasicEnergyCardsFromPool() {
        Player player = buildPlayer(500);
        Card basicEnergy = basicEnergyCard("xy1-132");
        Card poke = pokeCard("c1", CardRarity.COMMON);

        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(List.of(basicEnergy, poke));
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(poke, poke, poke));

        packService.openPack("p1", "BRONZE", null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Card>> poolCaptor = ArgumentCaptor.forClass(List.class);
        verify(packRandomizer).drawCards(any(), poolCaptor.capture());
        List<Card> pool = poolCaptor.getValue();

        assertThat(pool).hasSize(1);
        assertThat(pool.get(0).getId()).isEqualTo("c1");
        assertThat(pool).noneMatch(c -> "xy1-132".equals(c.getId()));
    }

    @Test
    void openPackKeepsSpecialEnergyInPool() {
        // Energy + UNCOMMON ≠ "básica" → debe quedar en pool.
        Player player = buildPlayer(500);
        Card specialEnergy = specialEnergyCard("xy1-130");
        Card poke = pokeCard("c1", CardRarity.COMMON);

        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(List.of(specialEnergy, poke));
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(poke, poke, poke));

        packService.openPack("p1", "BRONZE", null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Card>> poolCaptor = ArgumentCaptor.forClass(List.class);
        verify(packRandomizer).drawCards(any(), poolCaptor.capture());

        assertThat(poolCaptor.getValue()).hasSize(2);
        assertThat(poolCaptor.getValue()).extracting(Card::getId)
                .containsExactlyInAnyOrder("xy1-130", "c1");
    }

    @Test
    void openPackExcludesPokesecretCardsFromPool() {
        Player player = buildPlayer(500);
        Card secret1 = pokeCard("pokesecret-1", CardRarity.UNCOMMON);
        Card secret2 = pokeCard("pokesecret-42", CardRarity.RARE);
        Card regular = pokeCard("xy1-1", CardRarity.COMMON);

        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(List.of(secret1, secret2, regular));
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(regular, regular, regular));

        packService.openPack("p1", "BRONZE", null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Card>> poolCaptor = ArgumentCaptor.forClass(List.class);
        verify(packRandomizer).drawCards(any(), poolCaptor.capture());

        assertThat(poolCaptor.getValue()).hasSize(1);
        assertThat(poolCaptor.getValue().get(0).getId()).isEqualTo("xy1-1");
        assertThat(poolCaptor.getValue()).noneMatch(c -> c.getId().startsWith("pokesecret-"));
    }

    @Test
    void openPackFiltersBothBasicEnergyAndPokesecretSimultaneously() {
        Player player = buildPlayer(500);
        Card basic = basicEnergyCard("xy1-132");
        Card secret = pokeCard("pokesecret-1", CardRarity.RARE);
        Card normal1 = pokeCard("xy1-1", CardRarity.COMMON);
        Card normal2 = pokeCard("xy1-2", CardRarity.UNCOMMON);

        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(List.of(basic, secret, normal1, normal2));
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(normal1, normal2, normal1));

        packService.openPack("p1", "BRONZE", null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Card>> poolCaptor = ArgumentCaptor.forClass(List.class);
        verify(packRandomizer).drawCards(any(), poolCaptor.capture());

        assertThat(poolCaptor.getValue()).hasSize(2);
        assertThat(poolCaptor.getValue()).extracting(Card::getId)
                .containsExactlyInAnyOrder("xy1-1", "xy1-2");
    }

    @Test
    void openPackHandlesCardWithNullId() {
        // Edge case: una carta con id null no debe ser tratada como pokesecret.
        Player player = buildPlayer(500);
        Card noIdCard = pokeCard("xy1-99", CardRarity.COMMON);
        noIdCard.toString(); // ensure built; rely on it having an id from builder

        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardRepository.findAll()).thenReturn(List.of(noIdCard));
        when(packRandomizer.drawCards(any(), anyList())).thenReturn(List.of(noIdCard, noIdCard, noIdCard));

        // No debe explotar
        packService.openPack("p1", "BRONZE", null);
    }

    // ============================ openPack — DIAMOND (full art) ============================

    private CardSkin fullArtSkin(String id, Card baseCard) {
        return CardSkin.builder()
                .id(id).name("FullArt-" + id)
                .baseCard(baseCard)
                .origin(CardSkinOrigin.ALTERNATIVE)
                .imageUrl("http://minio/skins/" + id + ".png")
                .imageUrlLarge("http://minio/skins/" + id + "-lg.png")
                .skinTag("fullart")
                .build();
    }

    @Test
    void openDiamondPackGrantsAndEquipsSkinAndAddsMissingBaseCard() {
        Player player = buildPlayer(2500);
        Card base = pokeCard("base-1", CardRarity.RARE);
        CardSkin skin = fullArtSkin("skin-1", base);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardSkinRepository.findAll()).thenReturn(List.of(skin));
        when(skinService.getPlayerSkins("p1")).thenReturn(List.of());
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "base-1"))
                .thenReturn(Optional.empty());

        OpenedPackResponse response = packService.openPack("p1", "DIAMOND", null);

        assertThat(response.getPackType()).isEqualTo("DIAMOND");
        assertThat(response.getCards()).hasSize(1);
        assertThat(response.getCards().get(0).getImageUrl()).isEqualTo(skin.getImageUrl());
        assertThat(response.getCards().get(0).isFullArt()).isTrue();
        assertThat(response.getBonusSkin().getId()).isEqualTo("skin-1");
        assertThat(player.getCoins()).isEqualTo(2500 - PackType.DIAMOND.getPrice());
        assertThat(player.getCollection()).extracting(Card::getId).contains("base-1");
        verify(playerCardRepository).save(any());
        verify(skinService).grantSkin("p1", "skin-1", 1, player);
        verify(skinService).equipSkin("p1", "base-1", "skin-1");
    }

    @Test
    void openDiamondPackPrefersUnownedSkins() {
        Player player = buildPlayer(2000);
        CardSkin ownedSkin = fullArtSkin("skin-owned", pokeCard("base-1", CardRarity.RARE));
        CardSkin newSkin = fullArtSkin("skin-new", pokeCard("base-2", CardRarity.RARE));
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardSkinRepository.findAll()).thenReturn(List.of(ownedSkin, newSkin));
        when(skinService.getPlayerSkins("p1")).thenReturn(List.of(
                PlayerSkinDTO.builder().skinId("skin-owned").build()));
        when(playerCardRepository.findByPlayer_IdAndCard_Id(any(), any()))
                .thenReturn(Optional.empty());

        OpenedPackResponse response = packService.openPack("p1", "DIAMOND", null);

        assertThat(response.getBonusSkin().getId()).isEqualTo("skin-new");
        verify(skinService).equipSkin("p1", "base-2", "skin-new");
    }

    @Test
    void openDiamondPackDoesNotDuplicateOwnedBaseCard() {
        Player player = buildPlayer(2000);
        Card base = pokeCard("base-1", CardRarity.RARE);
        player.addToCollection(base);
        CardSkin skin = fullArtSkin("skin-1", base);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardSkinRepository.findAll()).thenReturn(List.of(skin));
        when(skinService.getPlayerSkins("p1")).thenReturn(List.of());
        when(playerCardRepository.findByPlayer_IdAndCard_Id("p1", "base-1"))
                .thenReturn(Optional.of(new PlayerCard()));

        packService.openPack("p1", "DIAMOND", null);

        // Ya posee la carta base: no se crea ni suma otra copia.
        verify(playerCardRepository, never()).save(any());
        verify(skinService).grantSkin("p1", "skin-1", 1, player);
        verify(skinService).equipSkin("p1", "base-1", "skin-1");
    }

    @Test
    void openDiamondPackWithoutSkinsInCatalogThrows() {
        Player player = buildPlayer(2000);
        when(playerRepository.findById("p1")).thenReturn(Optional.of(player));
        when(cardSkinRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> packService.openPack("p1", "DIAMOND", null))
                .isInstanceOf(IllegalStateException.class);
        assertThat(player.getCoins()).isEqualTo(2000);
    }
}
