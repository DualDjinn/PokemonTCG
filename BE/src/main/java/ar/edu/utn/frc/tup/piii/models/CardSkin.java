package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Variante cosmética de una carta. No afecta la jugabilidad.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "card_skin")
public class CardSkin {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_card_id", nullable = false)
    private Card baseCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardSkinOrigin origin;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String imageUrlLarge;

    @Column
    private String setCode;

    @Column
    private String skinTag;
}
