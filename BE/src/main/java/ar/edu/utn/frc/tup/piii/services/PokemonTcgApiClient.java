package ar.edu.utn.frc.tup.piii.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ar.edu.utn.frc.tup.piii.configs.PokemonApiProperties;
import ar.edu.utn.frc.tup.piii.dtos.CardApiDTO;
import ar.edu.utn.frc.tup.piii.dtos.CardApiResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for the Pokemon TCG API (pokemontcg.io v2).
 */
@Service
public class PokemonTcgApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(PokemonTcgApiClient.class);

    private final RestTemplate restTemplate;
    private final PokemonApiProperties props;

    /**
     * Creates the client with a dedicated RestTemplate configured from API properties.
     *
     * @param props API configuration properties
     */
    public PokemonTcgApiClient(PokemonApiProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectTimeoutMs());
        factory.setReadTimeout(props.getReadTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Fetches all cards for a given set, handling pagination automatically.
     *
     * @param setId the set identifier (e.g. "xy1")
     * @return list of all card DTOs from the API
     */
    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public List<CardApiDTO> fetchAllCardsForSet(String setId) {
        List<CardApiDTO> allCards = new ArrayList<>();
        int page = 1;
        int totalCount;

        do {
            String url = String.format("%s/cards?q=set.id:%s&pageSize=%d&page=%d",
                    props.getBaseUrl(), setId, props.getPageSize(), page);

            LOG.info("Fetching cards from API: {}", url);

            ResponseEntity<CardApiResponseDTO> response = restTemplate.exchange(
                    url, HttpMethod.GET, buildRequestEntity(), CardApiResponseDTO.class);

            CardApiResponseDTO body = response.getBody();
            if (body == null || body.getData() == null) {
                LOG.warn("Empty response from API for set {} page {}", setId, page);
                break;
            }

            allCards.addAll(body.getData());
            totalCount = body.getTotalCount();
            page++;
        } while (allCards.size() < totalCount);

        LOG.info("Fetched {} cards for set {}", allCards.size(), setId);
        return allCards;
    }

    /**
     * Recovery method invoked after all retry attempts are exhausted.
     *
     * @param ex    the last exception that caused the retry failure
     * @param setId the set identifier that failed
     * @return empty list as a signal that the fetch failed
     */
    @Recover
    public List<CardApiDTO> recoverFetchAllCards(RestClientException ex, String setId) {
        LOG.error("All retry attempts exhausted for set {}. Error: {}", setId, ex.getMessage());
        return Collections.emptyList();
    }

    private HttpEntity<Void> buildRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            headers.set("X-Api-Key", props.getApiKey());
        }
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }
}
