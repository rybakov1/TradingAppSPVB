package rybakov.vladislav.tradingappspvb.dto.response;

public record PositionResponse(
        Long id,
        Long accountId,
        String seccode,
        Long quantity,
        Long reservedQuantity,
        Long availableQuantity
) {}