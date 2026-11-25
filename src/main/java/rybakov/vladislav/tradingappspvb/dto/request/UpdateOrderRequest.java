package rybakov.vladislav.tradingappspvb.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateOrderRequest(

        @Positive
        @Digits(integer = 11, fraction = 8)
        BigDecimal price,

        @Positive
        Long quantity

) {}