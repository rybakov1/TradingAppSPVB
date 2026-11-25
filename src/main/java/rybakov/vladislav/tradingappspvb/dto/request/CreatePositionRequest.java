package rybakov.vladislav.tradingappspvb.dto.request;

import jakarta.validation.constraints.*;

public record CreatePositionRequest(

        @NotBlank
        @Size(max = 32)
        String seccode,

        @NotNull
        @Positive
        Long quantity

) {}