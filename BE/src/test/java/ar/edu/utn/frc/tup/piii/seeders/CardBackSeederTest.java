package ar.edu.utn.frc.tup.piii.seeders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import ar.edu.utn.frc.tup.piii.models.CardBack;
import ar.edu.utn.frc.tup.piii.models.CardBackOrigin;
import ar.edu.utn.frc.tup.piii.repositories.CardBackRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CardBackSeederTest {

    @Mock
    private CardBackRepository cardBackRepository;

    private CardBackSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new CardBackSeeder(cardBackRepository);
    }

    @Test
    void run_NoExistingBacks_SeedsAll6() throws Exception {
        when(cardBackRepository.count()).thenReturn(0L);

        seeder.run(new DefaultApplicationArguments());

        ArgumentCaptor<List<CardBack>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(cardBackRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(6);

        CardBack bio = captor.getValue().stream()
                .filter(b -> "bio".equals(b.getId())).findFirst().orElseThrow();
        assertThat(bio.getPrice()).isEqualTo(200);
        assertThat(bio.getOrigin()).isEqualTo(CardBackOrigin.SHOP);

        CardBack pixel = captor.getValue().stream()
                .filter(b -> "pixel".equals(b.getId())).findFirst().orElseThrow();
        assertThat(pixel.getPrice()).isEqualTo(100);
        assertThat(pixel.getOrigin()).isEqualTo(CardBackOrigin.DEFAULT);
    }

    @Test
    void run_BacksAlreadyExist_RunsMigrations() throws Exception {
        CardBack pixel = CardBack.builder().id("pixel").origin(CardBackOrigin.SHOP).price(null)
                .name("Pixel").imageUrl("/media/back-card/pixel.png").build();
        CardBack tori = CardBack.builder().id("tori").origin(CardBackOrigin.PACK).price(null)
                .name("Tori").imageUrl("/media/back-card/tori.png").build();

        when(cardBackRepository.count()).thenReturn(1L);
        when(cardBackRepository.findById("pixel")).thenReturn(Optional.of(pixel));
        when(cardBackRepository.findById("tori")).thenReturn(Optional.of(tori));
        when(cardBackRepository.findById("mundial-2022")).thenReturn(Optional.empty());
        when(cardBackRepository.findById("mundial-2026")).thenReturn(Optional.empty());

        seeder.run(new DefaultApplicationArguments());

        verify(cardBackRepository, never()).saveAll(any());

        assertThat(pixel.getOrigin()).isEqualTo(CardBackOrigin.DEFAULT);
        assertThat(pixel.getPrice()).isEqualTo(100);
        assertThat(pixel.getUnlockValue()).isNull();

        assertThat(tori.getOrigin()).isEqualTo(CardBackOrigin.SHOP);
        assertThat(tori.getPrice()).isEqualTo(500);
        assertThat(tori.getUnlockValue()).isNull();
    }

    @Test
    void run_PixelAlreadyDefault_DoesNotMigrateAgain() throws Exception {
        CardBack pixel = CardBack.builder().id("pixel").origin(CardBackOrigin.DEFAULT).price(100)
                .name("Pixel").imageUrl("/media/back-card/pixel.png").build();

        when(cardBackRepository.count()).thenReturn(1L);
        when(cardBackRepository.findById("pixel")).thenReturn(Optional.of(pixel));
        when(cardBackRepository.findById("tori")).thenReturn(Optional.empty());
        when(cardBackRepository.findById("mundial-2022")).thenReturn(Optional.empty());
        when(cardBackRepository.findById("mundial-2026")).thenReturn(Optional.empty());

        seeder.run(new DefaultApplicationArguments());

        verify(cardBackRepository, never()).save(any());
        assertThat(pixel.getOrigin()).isEqualTo(CardBackOrigin.DEFAULT);
    }

    @Test
    void run_Mundial2026MigratedToShop() throws Exception {
        CardBack m2026 = CardBack.builder().id("mundial-2026").origin(CardBackOrigin.BATTLE)
                .price(null).name("Mundial 2026")
                .imageUrl("/media/back-card/m26.png").build();

        when(cardBackRepository.count()).thenReturn(1L);
        when(cardBackRepository.findById("pixel")).thenReturn(Optional.empty());
        when(cardBackRepository.findById("tori")).thenReturn(Optional.empty());
        when(cardBackRepository.findById("mundial-2022")).thenReturn(Optional.empty());
        when(cardBackRepository.findById("mundial-2026")).thenReturn(Optional.of(m2026));

        seeder.run(new DefaultApplicationArguments());

        assertThat(m2026.getOrigin()).isEqualTo(CardBackOrigin.SHOP);
        assertThat(m2026.getPrice()).isEqualTo(5000);
        assertThat(m2026.getUnlockValue()).isNull();
    }
}
