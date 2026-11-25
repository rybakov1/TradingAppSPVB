package rybakov.vladislav.tradingappspvb.dto.response;

import rybakov.vladislav.tradingappspvb.enums.Currency;
import rybakov.vladislav.tradingappspvb.enums.InstrumentType;

import java.math.BigDecimal;

public record InstrumentResponse(
        String seccode,
        InstrumentType type,
        Long lotSize,
        BigDecimal minStep,
        Currency currency
) {}