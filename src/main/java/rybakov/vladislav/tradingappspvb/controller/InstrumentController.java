package rybakov.vladislav.tradingappspvb.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rybakov.vladislav.tradingappspvb.dto.request.CreateInstrumentRequest;
import rybakov.vladislav.tradingappspvb.dto.response.InstrumentResponse;
import rybakov.vladislav.tradingappspvb.service.InstrumentService;

import java.util.List;

@RestController
@RequestMapping("/api/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService instrumentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstrumentResponse create(
            @Valid @RequestBody CreateInstrumentRequest request
    ) {
        return instrumentService.create(request);
    }

    @GetMapping
    public List<InstrumentResponse> getAll() {
        return instrumentService.getAll();
    }

    @GetMapping("/{seccode}")
    public InstrumentResponse getBySeccode(
            @PathVariable String seccode
    ) {
        return instrumentService.getBySeccode(seccode);
    }
}