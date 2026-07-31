package ar.edu.utn.frc.tup.piii.models;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for {@link PackPurchase} entity.
 */
class PackPurchaseTest {

    @Test
    void allFieldsAreSetAndRetrievedCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        PackPurchase p = new PackPurchase();
        p.setId("uuid-123");
        p.setPlayerId("player-1");
        p.setPackType(PackType.GOLD);
        p.setPrice(500);
        p.setCardIds("xy1-1,xy1-2,xy1-3");
        p.setCreatedAt(now);

        assertThat(p.getId()).isEqualTo("uuid-123");
        assertThat(p.getPlayerId()).isEqualTo("player-1");
        assertThat(p.getPackType()).isEqualTo(PackType.GOLD);
        assertThat(p.getPrice()).isEqualTo(500);
        assertThat(p.getCardIds()).isEqualTo("xy1-1,xy1-2,xy1-3");
        assertThat(p.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void defaultInstanceHasNullsAndZero() {
        PackPurchase p = new PackPurchase();
        assertThat(p.getId()).isNull();
        assertThat(p.getPlayerId()).isNull();
        assertThat(p.getPackType()).isNull();
        assertThat(p.getPrice()).isZero();
        assertThat(p.getCardIds()).isNull();
        assertThat(p.getCreatedAt()).isNull();
    }

    @Test
    void equalsAndHashCodeRespectFields() {
        LocalDateTime t = LocalDateTime.of(2026, 1, 1, 0, 0);
        PackPurchase a = new PackPurchase();
        a.setId("x"); a.setPlayerId("p"); a.setPackType(PackType.BRONZE);
        a.setPrice(100); a.setCardIds("a,b,c"); a.setCreatedAt(t);

        PackPurchase b = new PackPurchase();
        b.setId("x"); b.setPlayerId("p"); b.setPackType(PackType.BRONZE);
        b.setPrice(100); b.setCardIds("a,b,c"); b.setCreatedAt(t);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
