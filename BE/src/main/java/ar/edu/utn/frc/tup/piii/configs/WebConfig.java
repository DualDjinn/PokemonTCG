package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.services.PlayerService;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PlayerService playerService;

    public WebConfig(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthenticatedPlayerResolver(playerService));
    }

    /**
     * Expone las imágenes guardadas en MinIO también como recursos estáticos en
     * {@code /media/{bucket}/**}, sirviéndolas desde el classpath
     * ({@code resources/pokesecret/{bucket}/}). Así las cartas, skins y dorsos
     * usan URLs relativas del mismo origen (pasan por el proxy del FE) en vez de
     * apuntar directo a {@code http://localhost:9000}, que rompe al correr la app
     * por un devtunnel HTTPS (mixed content + loopback + CORS).
     *
     * <p>El handler {@code /full-art-pokemon/**} se mantiene por compatibilidad.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Las imagenes de cartas no cambian: cache largo para que no se re-bajen en
        // cada recarga (clave al correr por un devtunnel, donde cada request es lenta).
        CacheControl cache = CacheControl.maxAge(Duration.ofDays(30)).cachePublic();
        registry.addResourceHandler("/media/full-art-pokemons/**")
                .addResourceLocations("classpath:/pokesecret/full-art-pokemons/")
                .setCacheControl(cache);
        registry.addResourceHandler("/media/back-card/**")
                .addResourceLocations("classpath:/pokesecret/back-card/")
                .setCacheControl(cache);
        registry.addResourceHandler("/media/custom-cards/**")
                .addResourceLocations("classpath:/pokesecret/custom-cards/")
                .setCacheControl(cache);
        registry.addResourceHandler("/full-art-pokemon/**")
                .addResourceLocations("classpath:/pokesecret/full-art-pokemons/")
                .setCacheControl(cache);
    }
}
