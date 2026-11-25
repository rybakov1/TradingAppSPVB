package rybakov.vladislav.tradingappspvb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rybakov.vladislav.tradingappspvb.dto.response.TradeResponse;
import rybakov.vladislav.tradingappspvb.service.TradeService;

import java.util.List;

@Tag(
        name = "Trades",
        description = "Executed trade registry"
)
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @Operation(summary = "Get trade registry")
    @GetMapping
    public List<TradeResponse> getAll(
            @RequestParam(required = false) String seccode
    ) {
        if (seccode != null) {
            return tradeService.getBySeccode(seccode);
        }

        return tradeService.getAll();
    }

    @Operation(summary = "Get an executed trade")
    @GetMapping("/{id}")
    public TradeResponse getById(
            @PathVariable Long id
    ) {
        return tradeService.getById(id);
    }
}