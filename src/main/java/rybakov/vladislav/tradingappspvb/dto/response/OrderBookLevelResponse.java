package rybakov.vladislav.tradingappspvb.dto.response;

import java.math.BigDecimal;

public record OrderBookLevelResponse(
        BigDecimal price,
        Long quantity,
        Long orderCount
) {}