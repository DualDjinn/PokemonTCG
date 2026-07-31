package ar.edu.utn.frc.tup.piii.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frc.tup.piii.configs.PokemonApiProperties;
import ar.edu.utn.frc.tup.piii.dtos.CardApiDTO;
import ar.edu.utn.frc.tup.piii.dtos.CardApiResponseDTO;

/**
 * Unit tests for {@link PokemonTcgApiClient}. The internally-built {@link RestTemplate}
 * is swapped for a mock via reflection so the HTTP calls are deterministic (no network).
 */
class PokemonTcgApiClientTest {

    private RestTemplate restTemplate;
    private PokemonApiProperties props;
    private PokemonTcgApiClient client;

    @BeforeEach
    void setUp() throws Exception {
        props = new PokemonApiProperties();
        client = new PokemonTcgApiClient(props);
        restTemplate = mock(RestTemplate.class);
        Field field = PokemonTcgApiClient.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        field.set(client, restTemplate);
    }

    private ResponseEntity<CardApiResponseDTO> page(List<CardApiDTO> data, int totalCount) {
        CardApiResponseDTO body = new CardApiResponseDTO();
        body.setData(data);
        body.setTotalCount(totalCount);
        return ResponseEntity.ok(body);
    }

    @Test
    void fetchAllCardsForSet_singlePage_returnsAllCards() {
        CardApiDTO dto = mock(CardApiDTO.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(CardApiResponseDTO.class))).thenReturn(page(List.of(dto), 1));

        List<CardApiDTO> result = client.fetchAllCardsForSet("xy1");

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchAllCardsForSet_multiplePages_followsPagination() {
        CardApiDTO d1 = mock(CardApiDTO.class);
        CardApiDTO d2 = mock(CardApiDTO.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(CardApiResponseDTO.class)))
                .thenReturn(page(List.of(d1), 2))
                .thenReturn(page(List.of(d2), 2));

        List<CardApiDTO> result = client.fetchAllCardsForSet("xy1");

        assertThat(result).containsExactly(d1, d2);
    }

    @Test
    void fetchAllCardsForSet_nullBody_returnsEmpty() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(CardApiResponseDTO.class))).thenReturn(ResponseEntity.ok((CardApiResponseDTO) null));

        assertThat(client.fetchAllCardsForSet("xy1")).isEmpty();
    }

    @Test
    void fetchAllCardsForSet_nullData_returnsEmpty() {
        CardApiResponseDTO body = new CardApiResponseDTO();
        body.setData(null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(CardApiResponseDTO.class))).thenReturn(ResponseEntity.ok(body));

        assertThat(client.fetchAllCardsForSet("xy1")).isEmpty();
    }

    @Test
    void fetchAllCardsForSet_withApiKey_addsHeaderAndFetches() {
        props.setApiKey("secret-key");
        CardApiDTO dto = mock(CardApiDTO.class);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
                eq(CardApiResponseDTO.class))).thenReturn(page(List.of(dto), 1));

        assertThat(client.fetchAllCardsForSet("xy1")).hasSize(1);
    }

    @Test
    void recoverFetchAllCards_returnsEmptyList() {
        List<CardApiDTO> result = client.recoverFetchAllCards(new RestClientException("api down"), "xy1");
        assertThat(result).isEmpty();
    }
}
