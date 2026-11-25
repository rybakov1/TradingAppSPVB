package rybakov.vladislav.tradingappspvb.dto.request;

import jakarta.validation.constraints.*;
import rybakov.vladislav.tradingappspvb.enums.Currency;

import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotNull
        @Positive
        Long userId,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 11, fraction = 8)
        BigDecimal balance,

        @NotNull
        Currency currency

) {}