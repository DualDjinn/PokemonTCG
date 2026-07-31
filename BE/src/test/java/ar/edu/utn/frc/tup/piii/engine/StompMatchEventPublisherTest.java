package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.services.CardBackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompMatchEventPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private CardBackService cardBackService;

    private StompMatchEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new StompMatchEventPublisher(messagingTemplate, cardBackService);
    }

    @Test
    void onMatchUpdated_WithBothViews_SendsToBothPlayers() {
        MatchStateDTO p1View = new MatchStateDTO();
        p1View.setRequestedPlayerId("p1");
        MatchStateDTO p2View = new MatchStateDTO();
        p2View.setRequestedPlayerId("p2");

        MatchUpdatedEvent event = new MatchUpdatedEvent("m1", p1View, p2View);

        publisher.onMatchUpdated(event);

        // Cada vista va a su topic por jugador: /topic/matches/{matchId}/state/{playerId}
        verify(messagingTemplate).convertAndSend("/topic/matches/m1/state/p1", p1View);
        verify(messagingTemplate).convertAndSend("/topic/matches/m1/state/p2", p2View);
    }

    @Test
    void onMatchUpdated_WithViewHavingNullPlayerId_SendsNothingForThatView() {
        MatchStateDTO p1View = new MatchStateDTO();
        p1View.setRequestedPlayerId(null);
        MatchStateDTO p2View = new MatchStateDTO();
        p2View.setRequestedPlayerId("p2");

        MatchUpdatedEvent event = new MatchUpdatedEvent("m1", p1View, p2View);

        publisher.onMatchUpdated(event);

        verify(messagingTemplate, never()).convertAndSend(anyString(), eq(p1View));
        verify(messagingTemplate).convertAndSend("/topic/matches/m1/state/p2", p2View);
    }

    @Test
    void onMatchUpdated_WithNullEvent_DoesNothing() {
        publisher.onMatchUpdated(null);

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void onMatchUpdated_WithBothViewPlayerIdsNull_SendsNothing() {
        MatchStateDTO p1View = new MatchStateDTO();
        MatchStateDTO p2View = new MatchStateDTO();

        MatchUpdatedEvent event = new MatchUpdatedEvent("m1", p1View, p2View);

        publisher.onMatchUpdated(event);

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
