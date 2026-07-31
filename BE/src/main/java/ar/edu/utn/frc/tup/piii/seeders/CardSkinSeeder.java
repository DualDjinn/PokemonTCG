package ar.edu.utn.frc.tup.piii.seeders;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frc.tup.piii.dtos.skin.SkinMappingDTO;
import ar.edu.utn.frc.tup.piii.dtos.skin.SkinMappingDTO.AlternativeSkinEntry;
import ar.edu.utn.frc.tup.piii.dtos.skin.SkinMappingDTO.OfficialSkinEntry;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardSkin;
import ar.edu.utn.frc.tup.piii.models.CardSkinOrigin;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.CardSkinRepository;

/**
 * Siembra la tabla card_skin con las skins oficiales y alternativas.
 */
@Component
@Order(5)
public class CardSkinSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CardSkinSeeder.class);
    private static final String MAPPING_PATH = "classpath:data/skin-mapping.json";

    private final CardSkinRepository cardSkinRepository;
    private final CardRepository cardRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final String fullArtBucket;

    public CardSkinSeeder(CardSkinRepository cardSkinRepository,
                          CardRepository cardRepository,
                          ObjectMapper objectMapper,
                          ResourceLoader resourceLoader,
                          @Value("${app.minio.full-art-bucket}") String fullArtBucket) {
        this.cardSkinRepository = cardSkinRepository;
        this.cardRepository = cardRepository;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.fullArtBucket = fullArtBucket;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (cardSkinRepository.count() > 0) {
            log.info("Card skins already seeded, skipping.");
            return;
        }
        try {
            Resource resource = resourceLoader.getResource(MAPPING_PATH);
            InputStream is = resource.getInputStream();
            SkinMappingDTO mapping = objectMapper.readValue(is, SkinMappingDTO.class);

            List<CardSkin> skins = new ArrayList<>();
            seedOfficial(mapping.getOfficial(), skins);
            seedAlternative(mapping.getAlternative(), skins);

            cardSkinRepository.saveAll(skins);
            log.info("Seeded {} card skins ({} official, {} alternative).",
                    skins.size(),
                    mapping.getOfficial().size(),
                    mapping.getAlternative().size());
        } catch (Exception e) {
            log.warn("Failed to seed card skins: {}", e.getMessage());
        }
    }

    private void seedOfficial(List<OfficialSkinEntry> entries, List<CardSkin> skins) {
        for (OfficialSkinEntry entry : entries) {
            Card baseCard = cardRepository.findById(entry.getBaseCardId()).orElse(null);
            if (baseCard == null) {
                log.warn("Base card {} not found for official skin {}, skipping.",
                        entry.getBaseCardId(), entry.getSkinId());
                continue;
            }
            skins.add(CardSkin.builder()
                    .id(entry.getSkinId())
                    .name(entry.getName())
                    .baseCard(baseCard)
                    .origin(CardSkinOrigin.OFFICIAL)
                    .imageUrl(entry.getImageUrl())
                    .imageUrlLarge(entry.getImageUrlLarge())
                    .setCode(entry.getSetCode())
                    .skinTag(entry.getTag())
                    .build());
        }
    }

    private void seedAlternative(List<AlternativeSkinEntry> entries, List<CardSkin> skins) {
        for (AlternativeSkinEntry entry : entries) {
            Card baseCard = cardRepository.findById(entry.getBaseCardId()).orElse(null);
            if (baseCard == null) {
                log.warn("Base card {} not found for alternative skin {}, skipping.",
                        entry.getBaseCardId(), entry.getSkinId());
                continue;
            }
            String imageUrl = "/media/" + fullArtBucket + "/" + entry.getFilename();
            skins.add(CardSkin.builder()
                    .id(entry.getSkinId())
                    .name(entry.getName())
                    .baseCard(baseCard)
                    .origin(CardSkinOrigin.ALTERNATIVE)
                    .imageUrl(imageUrl)
                    .imageUrlLarge(imageUrl)
                    .skinTag(entry.getTag())
                    .build());
        }
    }
}
