package rybakov.vladislav.tradingappspvb.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeResponse(
        Long id,
        String seccode,
        Long buyOrderId,
        Long sellOrderId,
        BigDecimal price,
        Long quantity,
        Instant executedAt
) {}