package ar.edu.utn.frc.tup.piii.seeders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.services.CardMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private CardRepository cardRepository;

    @Test
    void run_MigratesCoinsForPlayersWithZeroBalance() throws Exception {
        Player player = mock(Player.class);
        when(player.isBot()).thenReturn(false);
        when(player.getCoins()).thenReturn(0);
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(cardRepository.findAll()).thenReturn(List.of());

        DataSeeder dataSeeder = new DataSeeder(playerRepository, cardRepository);
        dataSeeder.run(new DefaultApplicationArguments());

        verify(player).setCoins(1000);
        verify(playerRepository).saveAll(List.of(player));
    }

    @Test
    void run_SkipsBotPlayers() throws Exception {
        Player player = mock(Player.class);
        when(player.isBot()).thenReturn(true);
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(cardRepository.findAll()).thenReturn(List.of());

        DataSeeder dataSeeder = new DataSeeder(playerRepository, cardRepository);
        dataSeeder.run(new DefaultApplicationArguments());

        verify(player, never()).setCoins(anyInt());
    }

    @Test
    void run_SkipsPlayersWithCoins() throws Exception {
        Player player = mock(Player.class);
        when(player.isBot()).thenReturn(false);
        when(player.getCoins()).thenReturn(500);
        when(playerRepository.findAll()).thenReturn(List.of(player));
        when(cardRepository.findAll()).thenReturn(List.of());

        DataSeeder dataSeeder = new DataSeeder(playerRepository, cardRepository);
        dataSeeder.run(new DefaultApplicationArguments());

        verify(player, never()).setCoins(anyInt());
        verify(playerRepository, never()).saveAll(any());
    }

    @Test
    void run_GrantsDevCoins() throws Exception {
        when(playerRepository.findAll()).thenReturn(List.of());
        when(cardRepository.findAll()).thenReturn(List.of());

        Player devPlayer = mock(Player.class);
        when(devPlayer.getCoins()).thenReturn(0);
        when(playerRepository.findByEmail("josepanichelli90@gmail.com"))
                .thenReturn(Optional.of(devPlayer));

        DataSeeder dataSeeder = new DataSeeder(playerRepository, cardRepository);
        dataSeeder.run(new DefaultApplicationArguments());

        verify(devPlayer).setCoins(99999);
        verify(playerRepository).save(devPlayer);
    }

    @Test
    void run_DevPlayerNotFound_NoOp() throws Exception {
        when(playerRepository.findAll()).thenReturn(List.of());
        when(cardRepository.findAll()).thenReturn(List.of());
        when(playerRepository.findByEmail("josepanichelli90@gmail.com"))
                .thenReturn(Optional.empty());

        DataSeeder dataSeeder = new DataSeeder(playerRepository, cardRepository);
        dataSeeder.run(new DefaultApplicationArguments());

        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    void run_EmptyPlayerList_NoOp() throws Exception {
        when(playerRepository.findAll()).thenReturn(List.of());
        when(cardRepository.findAll()).thenReturn(List.of());
        when(playerRepository.findByEmail("josepanichelli90@gmail.com"))
                .thenReturn(Optional.empty());

        DataSeeder dataSeeder = new DataSeeder(playerRepository, cardRepository);
        dataSeeder.run(new DefaultApplicationArguments());

        verify(playerRepository, never()).saveAll(any());
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    void run_MigratesGifUrlsForPokemonCards() throws Exception {
        when(playerRepository.findAll()).thenReturn(List.of());
        when(playerRepository.findByEmail("josepanichelli90@gmail.com"))
                .thenReturn(Optional.empty());

        PokemonCard pc = mock(PokemonCard.class);
        when(pc.getGifUrl()).thenReturn(null);
        when(pc.getId()).thenReturn("xy1-1");
        when(pc.getName()).thenReturn("Venusaur");

        List<Object> mixedList = new ArrayList<>();
        mixedList.add(pc);
        when(cardRepository.findAll()).thenReturn((List) mixedList);

        try (MockedStatic<CardMapper> cardMapperMock = mockStatic(CardMapper.class)) {
            cardMapperMock.when(() -> CardMapper.resolveGifUrl("xy1-1", "Venusaur"))
                    .thenReturn("venusaur.gif");

            DataSeeder dataSeeder = new DataSeeder(playerRepository, cardRepository);
            dataSeeder.run(new DefaultApplicationArguments());

            verify(pc).setGifUrl("venusaur.gif");
            verify(cardRepository).saveAll(argThat((List<PokemonCard> list) ->
                    list.size() == 1 && list.contains(pc)));
        }
    }

    @Test
    void run_GifUrlAlreadySet_Skips() throws Exception {
        when(playerRepository.findAll()).thenReturn(List.of());
        when(playerRepository.findByEmail("josepanichelli90@gmail.com"))
                .thenReturn(Optional.empty());

        PokemonCard pc = mock(PokemonCard.class);
        when(pc.getGifUrl()).thenReturn("pikachu.gif");
        when(cardRepository.findAll()).thenReturn((List) List.of(pc));

        DataSeeder dataSeeder = new DataSeeder(playerRepository, cardRepository);
        dataSeeder.run(new DefaultApplicationArguments());

        verify(pc, never()).setGifUrl(anyString());
        verify(cardRepository, never()).saveAll(any());
    }

    @Test
    void run_NoPokemonCards_SkipsGifMigration() throws Exception {
        when(playerRepository.findAll()).thenReturn(List.of());
        when(playerRepository.findByEmail("josepanichelli90@gmail.com"))
                .thenReturn(Optional.empty());
        when(cardRepository.findAll()).thenReturn(List.of());

        DataSeeder dataSeeder = new DataSeeder(playerRepository, cardRepository);
        dataSeeder.run(new DefaultApplicationArguments());

        verify(cardRepository, never()).saveAll(any());
    }
}

