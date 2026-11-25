package rybakov.vladislav.tradingappspvb.dto.response;

import rybakov.vladislav.tradingappspvb.enums.Currency;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        Long userId,
        BigDecimal balance,
        BigDecimal reservedBalance,
        BigDecimal availableBalance,
        Currency currency
) {}