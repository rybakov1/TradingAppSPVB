package rybakov.vladislav.tradingappspvb.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record QuoteResponse(
        String seccode,
        BigDecimal bestBid,
        BigDecimal bestAsk,
        BigDecimal spread,
        BigDecimal lastTradePrice,
        Instant lastTradeAt
) {}