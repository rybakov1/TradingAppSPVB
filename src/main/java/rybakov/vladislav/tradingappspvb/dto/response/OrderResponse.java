package rybakov.vladislav.tradingappspvb.dto.response;

import rybakov.vladislav.tradingappspvb.enums.OrderSide;
import rybakov.vladislav.tradingappspvb.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long accountId,
        String seccode,
        OrderSide side,
        BigDecimal price,
        Long quantity,
        Long filledQuantity,
        Long remainingQuantity,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {}