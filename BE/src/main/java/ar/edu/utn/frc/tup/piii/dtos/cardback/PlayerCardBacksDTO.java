package ar.edu.utn.frc.tup.piii.dtos.cardback;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dorsos que posee el jugador y cuál tiene equipado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCardBacksDTO {

    /** Id del dorso equipado; null = dorso clásico por defecto. */
    private String activeCardBackId;
    private List<CardBackDTO> owned;
}
