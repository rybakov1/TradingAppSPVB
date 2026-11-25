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
import rybakov.vladislav.tradingappspvb.dto.request.CreateOrderRequest;
import rybakov.vladislav.tradingappspvb.dto.request.UpdateOrderRequest;
import rybakov.vladislav.tradingappspvb.dto.response.OrderResponse;
import rybakov.vladislav.tradingappspvb.exception.BusinessException;
import rybakov.vladislav.tradingappspvb.service.OrderService;

import java.util.List;

@Tag(
        name = "Orders",
        description = "Order placement, modification and cancellation"
)
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "Place a new order",
            description = "Reserves cash for a buy or securities for a sell, then matches the order against opposite resting orders. The order may be partially filled. Self-trade prevention is Cancel Aggressor: a compatible opposite order of the same user is not traded and is not skipped. The unfilled remainder is cancelled, its reservation is released, and matching stops. Earlier fills are kept and the resting order is unchanged."
    )
    @BadRequest
    @NotFound
    @Unprocessable
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.create(request);
    }

    @Operation(
            summary = "List orders",
            description = "Returns every order, or the orders of one account or one instrument. An existing account or instrument with no orders returns an empty list. A missing account or instrument returns 404. accountId and seccode cannot be combined."
    )
    @Ok
    @NotFound
    @Unprocessable
    @GetMapping
    public List<OrderResponse> getAll(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String seccode
    ) {
        if (accountId != null && seccode != null) {
            throw new BusinessException(
                    "Use only one filter: accountId or seccode"
            );
        }

        if (accountId != null) {
            return orderService.getByAccountId(accountId);
        }

        if (seccode != null) {
            return orderService.getBySeccode(seccode);
        }

        return orderService.getAll();
    }

    @Operation(summary = "Get an order")
    @Ok
    @NotFound
    @GetMapping("/{id}")
    public OrderResponse getById(
            @PathVariable Long id
    ) {
        return orderService.getById(id);
    }

    @Operation(
            summary = "Modify an active order",
            description = "Releases the current reservation, reserves assets for the new price or quantity, and matches the order again. Time priority is lost when the price changes or the quantity increases. Unchanged parameters keep the existing time priority. Cancel Aggressor self-trade prevention applies to the amended order."
    )
    @Ok
    @BadRequest
    @NotFound
    @Conflict
    @Unprocessable
    @PatchMapping("/{id}")
    public OrderResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        return orderService.update(id, request);
    }

    @Operation(
            summary = "Cancel an active order",
            description = "Releases cash or securities reserved for the unfilled quantity. Only new and partially filled orders can be cancelled."
    )
    @Ok
    @NotFound
    @Conflict
    @DeleteMapping("/{id}")
    public OrderResponse cancel(
            @PathVariable Long id
    ) {
        return orderService.cancel(id);
    }
}