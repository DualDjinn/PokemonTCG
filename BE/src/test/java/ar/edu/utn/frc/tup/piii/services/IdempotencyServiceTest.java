package ar.edu.utn.frc.tup.piii.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frc.tup.piii.dtos.shop.BuyCartResponse;
import ar.edu.utn.frc.tup.piii.models.IdempotencyRecord;
import ar.edu.utn.frc.tup.piii.repositories.IdempotencyRecordRepository;

/**
 * Unit tests for {@link IdempotencyService}. The repository is mocked with an
 * in-memory map to simulate the real behavior: the first key runs the operation
 * and stores it; a repeated key returns the cached result.
 */
class IdempotencyServiceTest {

    private IdempotencyRecordRepository repository;
    private IdempotencyService service;
    private Map<String, IdempotencyRecord> store;

    @BeforeEach
    void setUp() {
        repository = mock(IdempotencyRecordRepository.class);
        store = new HashMap<>();
        when(repository.findByIdForUpdate(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
        when(repository.save(any(IdempotencyRecord.class))).thenAnswer(inv -> {
            IdempotencyRecord r = inv.getArgument(0);
            store.put(r.getIdempotencyKey(), r);
            return r;
        });
        service = new IdempotencyService(repository, new ObjectMapper());
    }

    @Test
    void execute_sameKeyTwice_runsOperationOnceAndReturnsCachedResult() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<BuyCartResponse> op = () -> new BuyCartResponse(100 - calls.incrementAndGet());

        BuyCartResponse first = service.execute("k1", "p1", "shop:buy", BuyCartResponse.class, op);
        BuyCartResponse second = service.execute("k1", "p1", "shop:buy", BuyCartResponse.class, op);

        assertThat(calls.get()).isEqualTo(1);
        assertThat(first.getRemainingCoins()).isEqualTo(99);
        assertThat(second.getRemainingCoins()).isEqualTo(99);
    }

    @Test
    void execute_newKey_runsOperationAndPersists() {
        BuyCartResponse result = service.execute("k2", "p1", "shop:buy", BuyCartResponse.class,
                () -> new BuyCartResponse(42));

        assertThat(result.getRemainingCoins()).isEqualTo(42);
        assertThat(store).containsKey("k2");
        verify(repository).save(any(IdempotencyRecord.class));
    }

    @Test
    void execute_nullKey_runsOperationWithoutTouchingRepository() {
        AtomicInteger calls = new AtomicInteger();

        BuyCartResponse a = service.execute(null, "p1", "shop:buy", BuyCartResponse.class,
                () -> new BuyCartResponse(calls.incrementAndGet()));
        BuyCartResponse b = service.execute(null, "p1", "shop:buy", BuyCartResponse.class,
                () -> new BuyCartResponse(calls.incrementAndGet()));

        assertThat(a.getRemainingCoins()).isEqualTo(1);
        assertThat(b.getRemainingCoins()).isEqualTo(2);
        verify(repository, never()).findByIdForUpdate(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void execute_blankKey_isTreatedAsNoKey() {
        AtomicInteger calls = new AtomicInteger();

        service.execute("   ", "p1", "shop:buy", BuyCartResponse.class,
                () -> new BuyCartResponse(calls.incrementAndGet()));
        service.execute("   ", "p1", "shop:buy", BuyCartResponse.class,
                () -> new BuyCartResponse(calls.incrementAndGet()));

        assertThat(calls.get()).isEqualTo(2);
        verify(repository, never()).save(any());
    }

    @Test
    void execute_cachedResultWithCorruptJson_throwsIllegalState() {
        IdempotencyRecord corrupt = new IdempotencyRecord();
        corrupt.setIdempotencyKey("kbad");
        corrupt.setResponseJson("{ this is not valid json");
        store.put("kbad", corrupt);

        assertThatThrownBy(() -> service.execute("kbad", "p1", "shop:buy", BuyCartResponse.class,
                () -> new BuyCartResponse(0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not deserialize idempotent response for key kbad");
    }

    @Test
    void execute_resultNotSerializable_throwsIllegalState() throws Exception {
        ObjectMapper failing = mock(ObjectMapper.class);
        when(failing.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { });
        IdempotencyService failingService = new IdempotencyService(repository, failing);

        assertThatThrownBy(() -> failingService.execute("knew", "p1", "shop:buy", BuyCartResponse.class,
                () -> new BuyCartResponse(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not serialize idempotent response for key knew");

        verify(repository, never()).save(any());
    }
}
