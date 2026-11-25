package rybakov.vladislav.tradingappspvb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rybakov.vladislav.tradingappspvb.dto.response.OrderBookResponse;
import rybakov.vladislav.tradingappspvb.dto.response.QuoteResponse;
import rybakov.vladislav.tradingappspvb.service.MarketDataService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

    @GetMapping("/orderbook/{seccode}")
    public OrderBookResponse getOrderBook(
            @PathVariable String seccode
    ) {
        return marketDataService.getOrderBook(seccode);
    }

    @GetMapping("/quotes/{seccode}")
    public QuoteResponse getQuote(
            @PathVariable String seccode
    ) {
        return marketDataService.getQuote(seccode);
    }
}