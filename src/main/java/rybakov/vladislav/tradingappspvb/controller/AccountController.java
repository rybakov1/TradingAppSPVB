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
import rybakov.vladislav.tradingappspvb.config.ApiErrorResponses.Unprocessable;
import rybakov.vladislav.tradingappspvb.dto.request.CreateAccountRequest;
import rybakov.vladislav.tradingappspvb.dto.request.CreatePositionRequest;
import rybakov.vladislav.tradingappspvb.dto.response.AccountResponse;
import rybakov.vladislav.tradingappspvb.dto.response.PositionResponse;
import rybakov.vladislav.tradingappspvb.service.AccountService;
import rybakov.vladislav.tradingappspvb.service.PositionService;

import java.util.List;

@Tag(
        name = "Accounts",
        description = "Trading accounts and securities positions"
)
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final PositionService positionService;

    @Operation(summary = "Open a trading account")
    @BadRequest
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        return accountService.create(request);
    }

    @Operation(summary = "List trading accounts")
    @Ok
    @GetMapping
    public List<AccountResponse> getAll(
            @RequestParam(required = false) Long userId
    ) {
        if (userId != null) {
            return accountService.getByUserId(userId);
        }

        return accountService.getAll();
    }

    @Operation(summary = "Get a trading account")
    @Ok
    @NotFound
    @GetMapping("/{id}")
    public AccountResponse getById(
            @PathVariable Long id
    ) {
        return accountService.getById(id);
    }

    @Operation(summary = "Open a securities position")
    @BadRequest
    @NotFound
    @Conflict
    @Unprocessable
    @PostMapping("/{id}/positions")
    @ResponseStatus(HttpStatus.CREATED)
    public PositionResponse createPosition(
            @PathVariable Long id,
            @Valid @RequestBody CreatePositionRequest request
    ) {
        return positionService.create(id, request);
    }

    @Operation(summary = "List account positions")
    @Ok
    @NotFound
    @GetMapping("/{id}/positions")
    public List<PositionResponse> getPositions(
            @PathVariable Long id
    ) {
        return positionService.getByAccountId(id);
    }

    @Operation(summary = "Get an account position")
    @Ok
    @NotFound
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