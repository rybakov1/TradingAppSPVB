package rybakov.vladislav.tradingappspvb.dto.request;

import jakarta.validation.constraints.*;
import rybakov.vladislav.tradingappspvb.enums.Currency;
import rybakov.vladislav.tradingappspvb.enums.InstrumentType;

import java.math.BigDecimal;

public record CreateInstrumentRequest(

        @NotBlank
        @Size(max = 32)
        String seccode,

        @NotNull
        InstrumentType type,

        @NotNull
        @Positive
        Long lotSize,

        @NotNull
        @Positive
        @Digits(integer = 11, fraction = 8)
        BigDecimal minStep,

        @NotNull
        Currency currency

) {
}