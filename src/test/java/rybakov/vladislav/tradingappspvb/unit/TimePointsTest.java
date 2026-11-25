package rybakov.vladislav.tradingappspvb.unit;

import org.junit.jupiter.api.Test;
import rybakov.vladislav.tradingappspvb.time.TimePoints;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TimePointsTest {

    @Test
    void truncatesToMicrosecondsWithoutRoundingUp() {
        Instant raw = Instant.parse("2026-11-25T18:00:00.123456789Z");

        Instant truncated = TimePoints.micros(raw);

        assertThat(truncated).isEqualTo(Instant.parse("2026-11-25T18:00:00.123456Z"));
        assertThat(truncated.getNano() % 1_000).isZero();
        assertThat(TimePoints.micros(null)).isNull();
    }

    @Test
    void nowIsAlreadyAlignedToMicroseconds() {
        Instant now = TimePoints.now();

        assertThat(now.getNano() % 1_000).isZero();
        assertThat(TimePoints.micros(now)).isEqualTo(now);
    }
}
