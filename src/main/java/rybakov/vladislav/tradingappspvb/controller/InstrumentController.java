package rybakov.vladislav.tradingappspvb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rybakov.vladislav.tradingappspvb.config.ApiErrorResponses.BadRequest;
import rybakov.vladislav.tradingappspvb.config.ApiErrorResponses.Conflict;
import rybakov.vladislav.tradingappspvb.config.ApiErrorResponses.NotFound;
import rybakov.vladislav.tradingappspvb.config.ApiErrorResponses.Ok;
import rybakov.vladislav.tradingappspvb.dto.request.CreateInstrumentRequest;
import rybakov.vladislav.tradingappspvb.dto.response.InstrumentResponse;
import rybakov.vladislav.tradingappspvb.service.InstrumentService;

import java.util.List;

@Tag(
        name = "Instruments",
        description = "Trading instrument management"
)
@RestController
@RequestMapping("/api/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService instrumentService;

    @Operation(summary = "Register a trading instrument")
    @BadRequest
    @Conflict
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstrumentResponse create(
            @Valid @RequestBody CreateInstrumentRequest request
    ) {
        return instrumentService.create(request);
    }

    @Operation(summary = "List trading instruments")
    @Ok
    @GetMapping
    public List<InstrumentResponse> getAll() {
        return instrumentService.getAll();
    }

    @Operation(summary = "Get a trading instrument")
    @Ok
    @NotFound
    @GetMapping("/{seccode}")
    public InstrumentResponse getBySeccode(
            @PathVariable String seccode
    ) {
        return instrumentService.getBySeccode(seccode);
    }
}