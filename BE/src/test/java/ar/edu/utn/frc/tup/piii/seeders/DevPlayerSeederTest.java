package ar.edu.utn.frc.tup.piii.seeders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerCard;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DevPlayerSeederTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private PlayerCardRepository playerCardRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DevPlayerSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DevPlayerSeeder(playerRepository, cardRepository,
                playerCardRepository, passwordEncoder, objectMapper);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encrypted");
    }

    @Test
    void run_BothPlayersExist_Skips() throws Exception {
        when(playerRepository.findByEmail("hernanmorais@gmail.com"))
                .thenReturn(Optional.of(new Player()));
        when(playerRepository.findByEmail("santoro@gmail.com"))
                .thenReturn(Optional.of(new Player()));

        seeder.run(new DefaultApplicationArguments());

        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    void run_HernanExists_SantoroDoesNot_SeedsSantoroOnly() throws Exception {
        when(playerRepository.findByEmail("hernanmorais@gmail.com"))
                .thenReturn(Optional.of(new Player()));
        when(playerRepository.findByEmail("santoro@gmail.com"))
                .thenReturn(Optional.empty());

        when(cardRepository.findAll()).thenReturn(List.of());
        // loadDeckSeeds returns empty -> no decks
        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        seeder.run(new DefaultApplicationArguments());
        verify(playerRepository).save(captor.capture());

        Player saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("santoro");
        assertThat(saved.getCoins()).isEqualTo(99999);
    }

    @Test
    void run_NeitherExists_SeedsBoth() throws Exception {
        when(playerRepository.findByEmail("hernanmorais@gmail.com"))
                .thenReturn(Optional.empty());
        when(playerRepository.findByEmail("santoro@gmail.com"))
                .thenReturn(Optional.empty());
        when(cardRepository.findAll()).thenReturn(List.of());

        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        seeder.run(new DefaultApplicationArguments());
        verify(playerRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        List<Player> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getCoins()).isEqualTo(99999);
        assertThat(saved.get(1).getCoins()).isEqualTo(99999);
    }

    @Test
    void run_SantoroGetsAllCards() throws Exception {
        when(playerRepository.findByEmail("hernanmorais@gmail.com"))
                .thenReturn(Optional.of(new Player()));
        when(playerRepository.findByEmail("santoro@gmail.com"))
                .thenReturn(Optional.empty());

        Card c1 = mockCard("xy1-1");
        Card c2 = mockCard("xy1-2");
        when(cardRepository.findAll()).thenReturn(List.of(c1, c2));
        when(playerCardRepository.findByPlayer_IdAndCard_Id(anyString(), anyString()))
                .thenReturn(Optional.empty());

        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        seeder.run(new DefaultApplicationArguments());
        verify(playerRepository).save(captor.capture());

        Player santoro = captor.getValue();
        assertThat(santoro.getCollection()).hasSize(2);
    }

    private Card mockCard(String id) {
        Card card = org.mockito.Mockito.mock(Card.class);
        when(card.getId()).thenReturn(id);
        return card;
    }
}
