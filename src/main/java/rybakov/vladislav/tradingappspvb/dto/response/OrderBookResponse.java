package rybakov.vladislav.tradingappspvb.dto.response;

import java.util.List;

public record OrderBookResponse(
        String seccode,
        List<OrderBookLevelResponse> bids,
        List<OrderBookLevelResponse> asks
) {}