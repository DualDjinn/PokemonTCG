package ar.edu.utn.frc.tup.piii.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Audit log for card set API fetch operations.
 */
@Entity
@Table(name = "card_set_fetch_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardSetFetchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "set_id", nullable = false, length = 20)
    private String setId;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "card_count")
    private int cardCount;

    @Column(name = "cards_inserted")
    private int cardsInserted;

    @Column(name = "cards_updated")
    private int cardsUpdated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FetchStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
