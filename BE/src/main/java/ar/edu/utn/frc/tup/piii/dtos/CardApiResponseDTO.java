package ar.edu.utn.frc.tup.piii.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper for the paginated response from the Pokemon TCG API.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardApiResponseDTO {

    private List<CardApiDTO> data;
    private int page;
    private int pageSize;
    private int count;
    private int totalCount;
}
