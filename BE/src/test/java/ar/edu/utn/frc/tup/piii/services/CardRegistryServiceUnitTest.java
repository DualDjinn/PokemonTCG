package ar.edu.utn.frc.tup.piii.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frc.tup.piii.configs.PokemonApiProperties;
import ar.edu.utn.frc.tup.piii.dtos.CardApiDTO;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardRarity;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;

/**
 * Pure-unit tests for {@link CardRegistryService} covering the reload / fallback / init
 * paths that the {@code @SpringBootTest}-based {@link CardRegistryServiceTest} does not
 * exercise. All collaborators are mocked; the {@code ResourceLoader} is stubbed to return
 * controlled JSON payloads.
 */
class CardRegistryServiceUnitTest {

    private CardMapper cardMapper;
    private ObjectMapper objectMapper;
    private ResourceLoader resourceLoader;
    private PokemonApiProperties apiProperties;
    private CardRegistryService service;

    @BeforeEach
    void setUp() {
        cardMapper = mock(CardMapper.class);
        objectMapper = new ObjectMapper();
        resourceLoader = mock(ResourceLoader.class);
        apiProperties = new PokemonApiProperties();
        service = new CardRegistryService(cardMapper, objectMapper, resourceLoader, apiProperties);
    }

    private CardApiDTO dto(String id, String rarity) {
        CardApiDTO d = mock(CardApiDTO.class);
        when(d.getId()).thenReturn(id);
        when(d.getName()).thenReturn("name-" + id);
        when(d.getRarity()).thenReturn(rarity);
        return d;
    }

    private PokemonCard pokemon(String id, CardRarity rarity) {
        PokemonCard p = PokemonCard.builder()
                .id(id).name("Card-" + id).hp(60)
                .stage(PokemonStage.BASIC).isEx(false).isMega(false)
                .pokemonType(EnergyType.GRASS)
                .attacks(List.of()).abilities(List.of())
                .weaknesses(List.of()).resistances(List.of())
                .retreatCost(1).evolvesFrom(null).evolvesTo(List.of()).rules(List.of())
                .build();
        p.setRarity(rarity);
        return p;
    }

    private void stubClasspathJson(String json) throws IOException {
        Resource resource = mock(Resource.class);
        when(resource.getInputStream())
                .thenAnswer(inv -> new ByteArrayInputStream(json.getBytes()));
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
    }

    // ───────── reloadFromDtos ─────────

    @Test
    void reloadFromDtos_populatesRegistrySkippingRareUltraNullAndMapperErrors() throws Exception {
        stubClasspathJson("{\"data\":[]}"); // empty pokesecret

        CardApiDTO common = dto("xy1-1", "Common");
        CardApiDTO rareUltra = dto("xy1-2", "Rare Ultra");
        CardApiDTO nullMap = dto("xy1-3", "Common");
        CardApiDTO throwsMap = dto("xy1-4", "Common");

        when(cardMapper.toDomain(common)).thenReturn(pokemon("xy1-1", CardRarity.COMMON));
        when(cardMapper.toDomain(rareUltra)).thenReturn(pokemon("xy1-2", CardRarity.RARE_ULTRA));
        when(cardMapper.toDomain(nullMap)).thenReturn(null);
        when(cardMapper.toDomain(throwsMap)).thenThrow(new RuntimeException("mapper boom"));

        service.reloadFromDtos(new ArrayList<>(List.of(common, rareUltra, nullMap, throwsMap)));

        assertThat(service.getAll()).extracting(Card::getId).containsExactly("xy1-1");
        assertThat(service.findById("xy1-1")).isPresent();
        assertThat(service.findById("xy1-3")).isEmpty();
        // rawCards keeps every non-"Rare Ultra" DTO (3), Rare Ultra excluded
        assertThat(service.getRawCards()).hasSize(3);
    }

    @Test
    void getById_unknownAfterReload_throws() throws Exception {
        stubClasspathJson("{\"data\":[]}");
        service.reloadFromDtos(new ArrayList<>());

        assertThatThrownBy(() -> service.getById("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    // ───────── loadFallbackJson ─────────

    @Test
    void loadFallbackJson_returnsDtosFromClasspathResource() throws Exception {
        stubClasspathJson("{\"data\":[{\"id\":\"xy1-1\",\"name\":\"X\","
                + "\"rarity\":\"Common\",\"supertype\":\"Pokemon\"}]}");

        List<CardApiDTO> dtos = service.loadFallbackJson();

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getId()).isEqualTo("xy1-1");
    }

    // ───────── init ─────────

    @Test
    void init_noCacheFile_loadsFromDbJsonAndPokesecret() throws Exception {
        apiProperties.setSetIds(List.of("set-without-cache-file"));
        stubClasspathJson("{\"data\":[{\"id\":\"xy1-1\",\"name\":\"X\","
                + "\"rarity\":\"Common\",\"supertype\":\"Pokemon\"}]}");
        when(cardMapper.toDomain(any(CardApiDTO.class))).thenReturn(pokemon("xy1-1", CardRarity.COMMON));

        service.init();

        assertThat(service.getRawCards()).isNotNull();
        assertThat(service.findById("xy1-1")).isPresent();
    }

    @Test
    void init_resourceFails_handledGracefullyWithEmptyRegistry() throws Exception {
        apiProperties.setSetIds(List.of("set-without-cache-file"));
        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenThrow(new IOException("cannot read"));
        when(resourceLoader.getResource(anyString())).thenReturn(resource);

        service.init();

        assertThat(service.getAll()).isEmpty();
        assertThat(service.getRawCards()).isEmpty();
    }

    // ───────── reloadFromDiskCache ─────────

    @Test
    void reloadFromDiskCache_runsWithoutErrorAndRefreshesRegistry() throws Exception {
        apiProperties.setSetIds(List.of("set-without-cache-file"));
        stubClasspathJson("{\"data\":[]}");

        service.reloadFromDiskCache();

        assertThat(service.getAll()).isNotNull();
        assertThat(service.getRawCards()).isNotNull();
    }
}
