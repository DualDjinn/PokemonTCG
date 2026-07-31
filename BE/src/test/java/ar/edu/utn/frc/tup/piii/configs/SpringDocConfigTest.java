package ar.edu.utn.frc.tup.piii.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SpringDocConfigTest {

    private SpringDocConfig config;

    @BeforeEach
    void setUp() {
        config = new SpringDocConfig();
        ReflectionTestUtils.setField(config, "url", "http://localhost:8080");
        ReflectionTestUtils.setField(config, "devName", "Dev Team");
        ReflectionTestUtils.setField(config, "devEmail", "dev@example.com");
    }

    @Test
    void openApi_PopulatesInfoBlock() {
        OpenAPI api = config.openApi("Pokemon TCG", "Backend API", "1.0.0");

        assertThat(api.getInfo()).isNotNull();
        assertThat(api.getInfo().getTitle()).isEqualTo("Pokemon TCG");
        assertThat(api.getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(api.getInfo().getDescription()).isEqualTo("Backend API");
    }

    @Test
    void openApi_PopulatesContactFromInjectedValues() {
        OpenAPI api = config.openApi("App", "Desc", "v1");

        assertThat(api.getInfo().getContact()).isNotNull();
        assertThat(api.getInfo().getContact().getName()).isEqualTo("Dev Team");
        assertThat(api.getInfo().getContact().getEmail()).isEqualTo("dev@example.com");
    }

    @Test
    void openApi_RegistersServerWithUrlAndDescription() {
        OpenAPI api = config.openApi("App", "My description", "v1");

        assertThat(api.getServers()).hasSize(1);
        Server server = api.getServers().get(0);
        assertThat(server.getUrl()).isEqualTo("http://localhost:8080");
        assertThat(server.getDescription()).isEqualTo("My description");
    }

    @Test
    void openApi_InitializesEmptyComponents() {
        OpenAPI api = config.openApi("App", "Desc", "v1");

        assertThat(api.getComponents()).isNotNull();
    }

    @Test
    void openApi_AcceptsNullDeveloperValues() {
        ReflectionTestUtils.setField(config, "devName", null);
        ReflectionTestUtils.setField(config, "devEmail", null);

        OpenAPI api = config.openApi("App", "Desc", "v1");

        assertThat(api.getInfo().getContact().getName()).isNull();
        assertThat(api.getInfo().getContact().getEmail()).isNull();
    }

    @Test
    void modelResolver_ReturnsBeanBackedByGivenObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        ModelResolver resolver = config.modelResolver(mapper);

        assertThat(resolver).isNotNull();
        assertThat(resolver.objectMapper()).isSameAs(mapper);
    }
}
