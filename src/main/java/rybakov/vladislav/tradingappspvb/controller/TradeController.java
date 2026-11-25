package rybakov.vladislav.tradingappspvb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rybakov.vladislav.tradingappspvb.dto.response.TradeResponse;
import rybakov.vladislav.tradingappspvb.service.TradeService;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @GetMapping
    public List<TradeResponse> getAll(
            @RequestParam(required = false) String seccode
    ) {
        if (seccode != null) {
            return tradeService.getBySeccode(seccode);
        }

        return tradeService.getAll();
    }

    @GetMapping("/{id}")
    public TradeResponse getById(
            @PathVariable Long id
    ) {
        return tradeService.getById(id);
    }
}