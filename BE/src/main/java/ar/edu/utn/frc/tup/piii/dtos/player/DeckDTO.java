package ar.edu.utn.frc.tup.piii.dtos.player;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeckDTO {
    private String id;
    private String name;
    private int cardCount;
    private List<CardSummaryDTO> cards;
    private boolean valid;
    private boolean favorite;
    private List<String> errors;
}
