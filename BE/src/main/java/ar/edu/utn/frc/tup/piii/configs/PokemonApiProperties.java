package ar.edu.utn.frc.tup.piii.configs;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Pokemon TCG API integration.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.pokemon-api")
public class PokemonApiProperties {

    private String baseUrl = "https://api.pokemontcg.io/v2";
    private String apiKey = "";
    private List<String> setIds = List.of("xy1");
    private int pageSize = 250;
    private String syncCron = "0 0 3 ? * MON";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 30000;
}
