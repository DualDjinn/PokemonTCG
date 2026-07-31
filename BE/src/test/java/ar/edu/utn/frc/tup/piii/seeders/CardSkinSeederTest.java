package ar.edu.utn.frc.tup.piii.seeders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frc.tup.piii.dtos.skin.SkinMappingDTO;
import ar.edu.utn.frc.tup.piii.dtos.skin.SkinMappingDTO.AlternativeSkinEntry;
import ar.edu.utn.frc.tup.piii.dtos.skin.SkinMappingDTO.OfficialSkinEntry;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardSkin;
import ar.edu.utn.frc.tup.piii.models.CardSkinOrigin;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.CardSkinRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CardSkinSeederTest {

    @Mock
    private CardSkinRepository cardSkinRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ResourceLoader resourceLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String fullArtBucket = "full-art-pokemons";

    private CardSkinSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new CardSkinSeeder(cardSkinRepository, cardRepository,
                objectMapper, resourceLoader, fullArtBucket);
    }

    @Test
    void run_SkinsAlreadyExist_Skips() throws Exception {
        when(cardSkinRepository.count()).thenReturn(1L);

        seeder.run(new DefaultApplicationArguments());

        verify(cardSkinRepository, never()).saveAll(anyList());
    }

    @Test
    void run_SeedsOfficialAndAlternative() throws Exception {
        when(cardSkinRepository.count()).thenReturn(0L);

        String json = """
        {
          "official": [
            {
              "skinId": "skin-official-1",
              "name": "Official Venusaur",
              "baseCardId": "xy1-1",
              "imageUrl": "/media/official/s1.png",
              "imageUrlLarge": "/media/official/s1-lg.png",
              "setCode": "xy1",
              "tag": "official"
            }
          ],
          "alternative": [
            {
              "skinId": "skin-alt-1",
              "name": "Alternative Venusaur",
              "baseCardId": "xy1-1",
              "filename": "alt1.png",
              "tag": "fullart"
            }
          ]
        }
        """;
        Resource resource = mockResource(json);
        when(resourceLoader.getResource("classpath:data/skin-mapping.json")).thenReturn(resource);

        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(org.mockito.Mockito.mock(Card.class)));

        ArgumentCaptor<List<CardSkin>> captor = ArgumentCaptor.forClass((Class) List.class);
        seeder.run(new DefaultApplicationArguments());
        verify(cardSkinRepository).saveAll(captor.capture());

        List<CardSkin> skins = captor.getValue();
        assertThat(skins).hasSize(2);

        CardSkin official = skins.stream()
                .filter(s -> "skin-official-1".equals(s.getId())).findFirst().orElseThrow();
        assertThat(official.getOrigin()).isEqualTo(CardSkinOrigin.OFFICIAL);
        assertThat(official.getImageUrl()).isEqualTo("/media/official/s1.png");

        CardSkin alt = skins.stream()
                .filter(s -> "skin-alt-1".equals(s.getId())).findFirst().orElseThrow();
        assertThat(alt.getOrigin()).isEqualTo(CardSkinOrigin.ALTERNATIVE);
        assertThat(alt.getImageUrl()).isEqualTo("/media/full-art-pokemons/alt1.png");
    }

    @Test
    void run_BaseCardNotFound_SkipsSkin() throws Exception {
        when(cardSkinRepository.count()).thenReturn(0L);

        String json = """
        {
          "official": [
            {
              "skinId": "skin-1",
              "name": "Missing Skin",
              "baseCardId": "nonexistent",
              "imageUrl": "/media/official/s.png",
              "imageUrlLarge": "/media/official/s-lg.png",
              "setCode": "xy1",
              "tag": "off"
            }
          ],
          "alternative": []
        }
        """;
        Resource resource = mockResource(json);
        when(resourceLoader.getResource("classpath:data/skin-mapping.json")).thenReturn(resource);
        when(cardRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ArgumentCaptor<List<CardSkin>> captor = ArgumentCaptor.forClass((Class) List.class);
        seeder.run(new DefaultApplicationArguments());
        verify(cardSkinRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void run_ResourceNotFound_NoOp() throws Exception {
        when(cardSkinRepository.count()).thenReturn(0L);
        when(resourceLoader.getResource(any())).thenThrow(new RuntimeException("not found"));

        seeder.run(new DefaultApplicationArguments());

        verify(cardSkinRepository, never()).saveAll(anyList());
    }

    private Resource mockResource(String content) throws Exception {
        Resource res = org.mockito.Mockito.mock(Resource.class);
        when(res.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
        return res;
    }
}
