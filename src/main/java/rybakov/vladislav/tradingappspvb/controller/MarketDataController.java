package rybakov.vladislav.tradingappspvb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rybakov.vladislav.tradingappspvb.dto.response.OrderBookResponse;
import rybakov.vladislav.tradingappspvb.dto.response.QuoteResponse;
import rybakov.vladislav.tradingappspvb.service.MarketDataService;

@Tag(
        name = "Market Data",
        description = "Order book and current market quotes"
)
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

    @Operation(summary = "Get current order book")
    @GetMapping("/orderbook/{seccode}")
    public OrderBookResponse getOrderBook(
            @PathVariable String seccode
    ) {
        return marketDataService.getOrderBook(seccode);
    }

    @Operation(summary = "Get current market quote")
    @GetMapping("/quotes/{seccode}")
    public QuoteResponse getQuote(
            @PathVariable String seccode
    ) {
        return marketDataService.getQuote(seccode);
    }
}