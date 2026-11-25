package rybakov.vladislav.tradingappspvb.dto.request;

import jakarta.validation.constraints.*;
import rybakov.vladislav.tradingappspvb.enums.OrderSide;

import java.math.BigDecimal;

public record CreateOrderRequest(

        @NotNull
        @Positive
        Long accountId,

        @NotBlank
        @Size(max = 32)
        String seccode,

        @NotNull
        OrderSide side,

        @NotNull
        @Positive
        @Digits(integer = 11, fraction = 8)
        BigDecimal price,

        @NotNull
        @Positive
        Long quantity

) {}