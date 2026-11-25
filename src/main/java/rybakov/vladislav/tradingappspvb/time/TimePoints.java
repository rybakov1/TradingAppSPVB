package rybakov.vladislav.tradingappspvb.time;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class TimePoints {

    private TimePoints() {
    }

    public static Instant now() {
        return micros(Instant.now());
    }

    public static Instant micros(Instant value) {
        if (value == null) {
            return null;
        }
        return value.truncatedTo(ChronoUnit.MICROS);
    }
}
