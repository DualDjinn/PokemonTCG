package ar.edu.utn.frc.tup.piii.dtos.player;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardSummaryDTO {
    private String id;
    private String name;
    private String supertype;
    private String imageUrl;
    private String imageUrlLarge;
    private String rarity;
    private boolean isFullArt;
    private String activeSkinId;
    private String activeSkinName;
}
