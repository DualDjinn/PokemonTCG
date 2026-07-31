package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Idempotency record: maps an {@code Idempotency-Key} sent by the client to the
 * serialized result of the operation it triggered.
 *
 * <p>If a second request arrives with the same key, the operation is not re-executed:
 * the cached {@code responseJson} is returned. This way a double click or a network
 * retry does not charge coins again nor open a new pack.
 *
 * <p>The primary key on {@code idempotency_key} is the concurrency backstop: two
 * simultaneous requests with the same key compete to insert the same row; the loser
 * violates the unique constraint and its transaction is rolled back without charging.
 */
@Data
@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {

    /** Idempotency key supplied by the client in the {@code Idempotency-Key} header. */
    @Id
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    /** Player that triggered the operation; for auditing only. */
    @Column(name = "player_id")
    private String playerId;

    /** Identifies the operation type (e.g. {@code shop:buy}, {@code pack:open}). */
    @Column(name = "scope", nullable = false)
    private String scope;

    /** Operation result serialized as JSON, returned verbatim on a replay. */
    @Column(name = "response_json", columnDefinition = "TEXT")
    private String responseJson;

    /** Date and time the operation was first processed. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
