package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.models.IdempotencyRecord;
import ar.edu.utn.frc.tup.piii.repositories.IdempotencyRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Generic idempotency service based on the {@code Idempotency-Key} header.
 *
 * <p>It generalizes the pattern already used in the MercadoPago webhook (pessimistic
 * lock + persisted record) so that any purchase operation becomes idempotent: if a
 * second request arrives with the same key, the operation is not re-executed and the
 * cached result is returned (no re-charging of coins, and the same pack is returned).
 *
 * <p><b>Atomicity:</b> {@link #execute} is transactional, so the operation's mutations
 * (coin deduction, card grants) and the saving of the idempotency record commit or
 * roll back together.
 *
 * <p><b>Concurrency:</b> the primary key on {@code idempotency_key} is the backstop.
 * Given two simultaneous requests with the same new key, both run the operation, but
 * only one manages to insert the record; the other violates the unique constraint and
 * its transaction is rolled back entirely, guaranteeing there is no double charge.
 */
@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * @param repository  idempotency record repository
     * @param objectMapper JSON serializer used to cache and restore the result
     */
    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Runs {@code operation} idempotently with respect to {@code key}.
     *
     * <ul>
     *   <li>If {@code key} is null or empty, runs the operation without deduplicating
     *       (backwards compatibility; a warning is logged).</li>
     *   <li>If a record already exists for {@code key}, returns the cached result
     *       without re-running the operation.</li>
     *   <li>If the key is new, runs the operation, persists its result and returns it.</li>
     * </ul>
     *
     * @param key       idempotency key from the {@code Idempotency-Key} header
     * @param playerId  player triggering the operation (auditing only)
     * @param scope     operation type identifier (e.g. {@code shop:buy})
     * @param type      result class, used to deserialize the cached value
     * @param operation logic to run exactly once per key
     * @param <T>       result type
     * @return the operation result (fresh or cached)
     */
    @Transactional
    public <T> T execute(String key, String playerId, String scope, Class<T> type, Supplier<T> operation) {
        if (key == null || key.isBlank()) {
            logger.warn("Operation '{}' for player {} ran without an Idempotency-Key header; not deduplicated",
                    scope, playerId);
            return operation.get();
        }

        Optional<IdempotencyRecord> existing = repository.findByIdForUpdate(key);
        if (existing.isPresent()) {
            logger.info("Idempotent replay for key={} scope={}; returning cached result", key, scope);
            return deserialize(existing.get(), type);
        }

        T result = operation.get();
        persist(key, playerId, scope, result);
        return result;
    }

    private <T> T deserialize(IdempotencyRecord record, Class<T> type) {
        try {
            return objectMapper.readValue(record.getResponseJson(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Could not deserialize idempotent response for key " + record.getIdempotencyKey(), e);
        }
    }

    private <T> void persist(String key, String playerId, String scope, T result) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setPlayerId(playerId);
        record.setScope(scope);
        record.setCreatedAt(LocalDateTime.now());
        try {
            record.setResponseJson(objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize idempotent response for key " + key, e);
        }
        repository.save(record);
    }
}
