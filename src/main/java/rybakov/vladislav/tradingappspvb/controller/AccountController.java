package rybakov.vladislav.tradingappspvb.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rybakov.vladislav.tradingappspvb.dto.request.CreateAccountRequest;
import rybakov.vladislav.tradingappspvb.dto.request.CreatePositionRequest;
import rybakov.vladislav.tradingappspvb.dto.response.AccountResponse;
import rybakov.vladislav.tradingappspvb.dto.response.PositionResponse;
import rybakov.vladislav.tradingappspvb.service.AccountService;
import rybakov.vladislav.tradingappspvb.service.PositionService;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final PositionService positionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        return accountService.create(request);
    }

    @GetMapping
    public List<AccountResponse> getAll(
            @RequestParam(required = false) Long userId
    ) {
        if (userId != null) {
            return accountService.getByUserId(userId);
        }

        return accountService.getAll();
    }

    @GetMapping("/{id}")
    public AccountResponse getById(
            @PathVariable Long id
    ) {
        return accountService.getById(id);
    }

    @PostMapping("/{id}/positions")
    @ResponseStatus(HttpStatus.CREATED)
    public PositionResponse createPosition(
            @PathVariable Long id,
            @Valid @RequestBody CreatePositionRequest request
    ) {
        return positionService.create(id, request);
    }

    @GetMapping("/{id}/positions")
    public List<PositionResponse> getPositions(
            @PathVariable Long id
    ) {
        return positionService.getByAccountId(id);
    }

    @GetMapping("/{id}/positions/{seccode}")
    public PositionResponse getPosition(
            @PathVariable Long id,
            @PathVariable String seccode
    ) {
        return positionService.getByAccountIdAndSeccode(
                id,
                seccode
        );
    }
}