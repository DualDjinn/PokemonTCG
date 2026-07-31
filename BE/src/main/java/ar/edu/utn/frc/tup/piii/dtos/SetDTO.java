package ar.edu.utn.frc.tup.piii.dtos;

import lombok.Getter;

@Getter
public class SetDTO {
    private String id;
    private String name;
    private String series;
    private int printedTotal;
    private int total;
    private LegalitiesDTO legalities;
    private String ptcgoCode;
    private String releaseDate;
    private String updatedAt;
    private ImagesDTO images;
}
