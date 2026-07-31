package ar.edu.utn.frc.tup.piii.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frc.tup.piii.configs.PokemonApiProperties;
import ar.edu.utn.frc.tup.piii.dtos.CardApiDTO;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardRarity;
import ar.edu.utn.frc.tup.piii.models.CardSetFetchLog;
import ar.edu.utn.frc.tup.piii.models.CollectibleCard;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.FetchStatus;
import ar.edu.utn.frc.tup.piii.models.ItemCard;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.CardSetFetchLogRepository;

/**
 * Unit tests for {@link CardApiSyncService}. All collaborators are mocked, including the
 * external {@link PokemonTcgApiClient}. {@link CardApiDTO} is mocked too (it only exposes
 * getters), and the package-private methods are invoked directly since the test shares the
 * service package.
 */
class CardApiSyncServiceTest {

    private static final String SET_ID = "xy1";

    private PokemonTcgApiClient apiClient;
    private CardMapper cardMapper;
    private CardRepository cardRepository;
    private CardSetFetchLogRepository fetchLogRepository;
    private CardRegistryService cardRegistryService;
    private CardService cardService;
    private PokemonApiProperties apiProperties;
    private ObjectMapper objectMapper;

    private CardApiSyncService service;
    private File cacheFile;

    @BeforeEach
    void setUp() {
        apiClient = mock(PokemonTcgApiClient.class);
        cardMapper = mock(CardMapper.class);
        cardRepository = mock(CardRepository.class);
        fetchLogRepository = mock(CardSetFetchLogRepository.class);
        cardRegistryService = mock(CardRegistryService.class);
        cardService = mock(CardService.class);
        apiProperties = new PokemonApiProperties(); // default setIds = ["xy1"]
        objectMapper = mock(ObjectMapper.class);

        service = new CardApiSyncService(apiClient, cardMapper, cardRepository, fetchLogRepository,
                cardRegistryService, cardService, apiProperties, objectMapper);

        cacheFile = new File(System.getProperty("java.io.tmpdir"),
                "cardapisync-test/api_cache_" + SET_ID + ".json");
    }

    // ───────── fixtures ─────────

    private CardApiDTO dto(String id, String rarity) {
        CardApiDTO d = mock(CardApiDTO.class);
        when(d.getId()).thenReturn(id);
        when(d.getRarity()).thenReturn(rarity);
        return d;
    }

    private PokemonCard pokemon(String id, CardRarity rarity, boolean mutableLists) {
        PokemonCard p = PokemonCard.builder()
                .id(id).name("Card-" + id).hp(60)
                .stage(PokemonStage.BASIC).isEx(false).isMega(false)
                .pokemonType(EnergyType.GRASS)
                .attacks(mutableLists ? new ArrayList<>() : List.of())
                .abilities(mutableLists ? new ArrayList<>() : List.of())
                .weaknesses(mutableLists ? new ArrayList<>() : List.of())
                .resistances(mutableLists ? new ArrayList<>() : List.of())
                .retreatCost(1).evolvesFrom(null).evolvesTo(List.of()).rules(List.of())
                .build();
        p.setRarity(rarity);
        return p;
    }

    private EnergyCard energy(String id, CardRarity rarity) {
        EnergyCard e = EnergyCard.builder()
                .id(id).name("Energy-" + id)
                .energyType(EnergyType.FIRE)
                .rules(new ArrayList<>())
                .build();
        e.setRarity(rarity);
        return e;
    }

    private void setSyncing(boolean value) throws Exception {
        Field field = CardApiSyncService.class.getDeclaredField("syncing");
        field.setAccessible(true);
        ((AtomicBoolean) field.get(service)).set(value);
    }

    /** Stubs a one-card successful API fetch for the default set. */
    private CardApiDTO stubSuccessfulFetch() {
        CardApiDTO d = dto("xy1-1", "Common");
        when(apiClient.fetchAllCardsForSet(SET_ID)).thenReturn(List.of(d));
        when(cardMapper.toDomain(d)).thenReturn(pokemon("xy1-1", CardRarity.COMMON, false));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.empty());
        when(cardRegistryService.getCacheFile(SET_ID)).thenReturn(cacheFile);
        return d;
    }

    // ───────── onStartup ─────────

    @Test
    void onStartup_databaseHasCards_skipsSyncAndSeedsMissingPokesecret() {
        when(cardRepository.count()).thenReturn(150L);
        Card secretMissing = pokemon("pokesecret-1", CardRarity.RARE, false);
        Card secretExisting = pokemon("pokesecret-2", CardRarity.RARE, false);
        Card normal = pokemon("xy1-1", CardRarity.COMMON, false);
        when(cardRegistryService.getAll()).thenReturn(List.of(secretMissing, secretExisting, normal));
        when(cardRepository.existsById("pokesecret-1")).thenReturn(false);
        when(cardRepository.existsById("pokesecret-2")).thenReturn(true);

        service.run(new DefaultApplicationArguments());

        verify(apiClient, never()).fetchAllCardsForSet(anyString());
        verify(cardRepository).save(secretMissing);
        verify(cardRepository, never()).save(secretExisting);
        verify(cardRepository, never()).save(normal);
    }

    @Test
    void onStartup_databaseEmpty_apiSuccess_persistsAndRefreshesCaches() {
        when(cardRepository.count()).thenReturn(0L);
        stubSuccessfulFetch();
        when(cardRegistryService.getAll()).thenReturn(List.of());

        service.run(new DefaultApplicationArguments());

        verify(cardRepository).save(any(Card.class));
        verify(cardRegistryService).reloadFromDtos(anyList());
        verify(cardService).refreshCache();
    }

    @Test
    void onStartup_databaseEmpty_apiFails_fallsBackToDbJson() {
        when(cardRepository.count()).thenReturn(0L);
        when(apiClient.fetchAllCardsForSet(SET_ID)).thenReturn(List.of()); // empty => FAILED
        CardApiDTO fallbackDto = dto("xy1-2", "Common");
        when(cardRegistryService.loadFallbackJson()).thenReturn(List.of(fallbackDto));
        when(cardMapper.toDomain(fallbackDto)).thenReturn(pokemon("xy1-2", CardRarity.COMMON, false));
        when(cardRegistryService.getCacheFile(SET_ID)).thenReturn(cacheFile);
        when(cardRegistryService.getAll()).thenReturn(List.of());

        service.run(new DefaultApplicationArguments());

        verify(cardRegistryService).loadFallbackJson();
        verify(cardRepository).saveAll(anyList());
        verify(cardRegistryService).reloadFromDtos(anyList());
        verify(cardService).refreshCache();
    }

    @Test
    void onStartup_databaseEmpty_apiFails_fallbackEmpty_doesNotRefresh() {
        when(cardRepository.count()).thenReturn(0L);
        when(apiClient.fetchAllCardsForSet(SET_ID)).thenReturn(List.of());
        when(cardRegistryService.loadFallbackJson()).thenReturn(List.of());
        when(cardRegistryService.getAll()).thenReturn(List.of());

        service.run(new DefaultApplicationArguments());

        verify(cardRepository, never()).saveAll(anyList());
        verify(cardRegistryService, never()).reloadFromDtos(anyList());
        verify(cardService, never()).refreshCache();
    }

    // ───────── scheduledSync ─────────

    @Test
    void scheduledSync_setNeedsSync_syncsAndRefreshesFromDiskCache() {
        when(fetchLogRepository.findTopBySetIdAndStatusOrderByFetchedAtDesc(SET_ID, FetchStatus.SUCCESS))
                .thenReturn(Optional.empty()); // never synced => should sync
        stubSuccessfulFetch();

        service.scheduledSync();

        verify(apiClient).fetchAllCardsForSet(SET_ID);
        verify(cardRegistryService).reloadFromDiskCache();
        verify(cardService).refreshCache();
    }

    @Test
    void scheduledSync_setUpToDate_skipsAndDoesNotRefresh() {
        CardSetFetchLog recent = CardSetFetchLog.builder()
                .setId(SET_ID)
                .fetchedAt(LocalDateTime.now())
                .status(FetchStatus.SUCCESS)
                .build();
        when(fetchLogRepository.findTopBySetIdAndStatusOrderByFetchedAtDesc(SET_ID, FetchStatus.SUCCESS))
                .thenReturn(Optional.of(recent));

        service.scheduledSync();

        verify(apiClient, never()).fetchAllCardsForSet(anyString());
        verify(cardRegistryService, never()).reloadFromDiskCache();
        verify(cardService, never()).refreshCache();
    }

    // ───────── syncSet ─────────

    @Test
    void syncSet_alreadyInProgress_returnsFailedWithoutCallingApi() throws Exception {
        setSyncing(true);

        CardApiSyncService.SyncResult result = service.syncSet(SET_ID);

        assertThat(result.status).isEqualTo(FetchStatus.FAILED);
        verify(apiClient, never()).fetchAllCardsForSet(anyString());
    }

    @Test
    void syncSet_apiReturnsEmpty_logsFailedAndReturnsFailed() {
        when(apiClient.fetchAllCardsForSet(SET_ID)).thenReturn(List.of());

        CardApiSyncService.SyncResult result = service.syncSet(SET_ID);

        assertThat(result.status).isEqualTo(FetchStatus.FAILED);
        ArgumentCaptor<CardSetFetchLog> captor = ArgumentCaptor.forClass(CardSetFetchLog.class);
        verify(fetchLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(FetchStatus.FAILED);
    }

    @Test
    void syncSet_apiThrows_returnsFailedAndReleasesLock() {
        doThrow(new RuntimeException("network down")).when(apiClient).fetchAllCardsForSet(SET_ID);

        CardApiSyncService.SyncResult result = service.syncSet(SET_ID);

        assertThat(result.status).isEqualTo(FetchStatus.FAILED);

        // The lock is released in finally, so a second call reaches the API again.
        doReturn(List.of()).when(apiClient).fetchAllCardsForSet(SET_ID);
        assertThat(service.syncSet(SET_ID).status).isEqualTo(FetchStatus.FAILED);
        verify(apiClient, times(2)).fetchAllCardsForSet(SET_ID);
    }

    @Test
    void syncSet_success_writeDiskCacheFails_stillReturnsSuccess() throws Exception {
        stubSuccessfulFetch();
        doThrow(new IOException("disk full")).when(objectMapper).writeValue(any(File.class), any());

        CardApiSyncService.SyncResult result = service.syncSet(SET_ID);

        assertThat(result.status).isEqualTo(FetchStatus.SUCCESS);
        verify(cardRepository).save(any(Card.class));
    }

    // ───────── upsertCards ─────────

    @Test
    void upsertCards_insertsUpdatesAndSkipsAppropriately() {
        CardApiDTO insertDto = dto("xy1-1", "Common");
        CardApiDTO updateDto = dto("xy1-2", "Common");
        CardApiDTO rareUltraDto = dto("xy1-3", "Rare Ultra");
        CardApiDTO nullMapDto = dto("xy1-4", "Common");
        CardApiDTO throwsDto = dto("xy1-5", "Common");

        PokemonCard incomingForUpdate = pokemon("xy1-2", CardRarity.RARE, false);
        PokemonCard existing = pokemon("xy1-2", CardRarity.COMMON, true);

        when(cardMapper.toDomain(insertDto)).thenReturn(pokemon("xy1-1", CardRarity.COMMON, false));
        when(cardMapper.toDomain(updateDto)).thenReturn(incomingForUpdate);
        when(cardMapper.toDomain(nullMapDto)).thenReturn(null);
        when(cardMapper.toDomain(throwsDto)).thenThrow(new RuntimeException("mapper boom"));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.empty());
        when(cardRepository.findById("xy1-2")).thenReturn(Optional.of(existing));

        int[] counts = service.upsertCards(
                List.of(insertDto, updateDto, rareUltraDto, nullMapDto, throwsDto));

        assertThat(counts).containsExactly(1, 1); // [inserted, updated]
        verify(cardMapper, never()).toDomain(rareUltraDto); // skipped before mapping
        verify(cardRepository).save(existing); // updated existing persisted
    }

    @Test
    void upsertCards_updatesTrainerCardBranch() {
        CardApiDTO trainerDto = dto("xy1-7", "Uncommon");
        ItemCard incoming = ItemCard.builder().id("xy1-7").name("Potion")
                .rules(new ArrayList<>()).artist("Artist A").build();
        incoming.setRarity(CardRarity.UNCOMMON);
        ItemCard existing = ItemCard.builder().id("xy1-7").name("Old Potion")
                .rules(new ArrayList<>()).artist("Artist B").build();
        existing.setRarity(CardRarity.UNCOMMON);
        when(cardMapper.toDomain(trainerDto)).thenReturn(incoming);
        when(cardRepository.findById("xy1-7")).thenReturn(Optional.of(existing));

        int[] counts = service.upsertCards(List.of(trainerDto));

        assertThat(counts).containsExactly(0, 1);
        verify(cardRepository).save(existing);
    }

    @Test
    void upsertCards_updatesCollectibleCardBranch() {
        CardApiDTO collectibleDto = dto("xy1-8", "Rare");
        CollectibleCard incoming = CollectibleCard.builder().id("xy1-8").name("Coin")
                .rules(new ArrayList<>()).artist("Artist A").build();
        incoming.setRarity(CardRarity.RARE);
        CollectibleCard existing = CollectibleCard.builder().id("xy1-8").name("Old Coin")
                .rules(new ArrayList<>()).artist("Artist B").build();
        existing.setRarity(CardRarity.RARE);
        when(cardMapper.toDomain(collectibleDto)).thenReturn(incoming);
        when(cardRepository.findById("xy1-8")).thenReturn(Optional.of(existing));

        int[] counts = service.upsertCards(List.of(collectibleDto));

        assertThat(counts).containsExactly(0, 1);
        verify(cardRepository).save(existing);
    }

    @Test
    void scheduledSync_lastSuccessOlderThanThreshold_syncs() {
        CardSetFetchLog oldLog = CardSetFetchLog.builder()
                .setId(SET_ID)
                .fetchedAt(LocalDateTime.now().minusDays(10))
                .status(FetchStatus.SUCCESS)
                .build();
        when(fetchLogRepository.findTopBySetIdAndStatusOrderByFetchedAtDesc(SET_ID, FetchStatus.SUCCESS))
                .thenReturn(Optional.of(oldLog));
        stubSuccessfulFetch();

        service.scheduledSync();

        verify(apiClient).fetchAllCardsForSet(SET_ID);
        verify(cardRegistryService).reloadFromDiskCache();
    }

    @Test
    void upsertCards_updatesEnergyCardBranch() {
        CardApiDTO energyDto = dto("xy1-9", "Common");
        EnergyCard incoming = energy("xy1-9", CardRarity.COMMON);
        EnergyCard existing = energy("xy1-9", CardRarity.COMMON);
        when(cardMapper.toDomain(energyDto)).thenReturn(incoming);
        when(cardRepository.findById("xy1-9")).thenReturn(Optional.of(existing));

        int[] counts = service.upsertCards(List.of(energyDto));

        assertThat(counts).containsExactly(0, 1);
        verify(cardRepository).save(existing);
    }

    // ───────── persistDtos ─────────

    @Test
    void persistDtos_skipsRareUltraAndMapperErrors_savesTheRest() {
        CardApiDTO ok = dto("xy1-1", "Common");
        CardApiDTO rareUltraByString = dto("xy1-2", "Rare Ultra");
        CardApiDTO rareUltraByModel = dto("xy1-3", "Common");
        CardApiDTO nullMap = dto("xy1-4", "Common");
        CardApiDTO throwsMap = dto("xy1-5", "Common");

        when(cardMapper.toDomain(ok)).thenReturn(pokemon("xy1-1", CardRarity.COMMON, false));
        when(cardMapper.toDomain(rareUltraByModel)).thenReturn(pokemon("xy1-3", CardRarity.RARE_ULTRA, false));
        when(cardMapper.toDomain(nullMap)).thenReturn(null);
        when(cardMapper.toDomain(throwsMap)).thenThrow(new RuntimeException("mapper boom"));

        service.persistDtos(List.of(ok, rareUltraByString, rareUltraByModel, nullMap, throwsMap));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Card>> captor = ArgumentCaptor.forClass(List.class);
        verify(cardRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getId()).isEqualTo("xy1-1");
        verify(cardMapper, never()).toDomain(rareUltraByString);
    }
}
