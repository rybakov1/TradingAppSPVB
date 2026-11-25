package rybakov.vladislav.tradingappspvb.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
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
            description = "Reserves cash for a buy or securities for a sell, then matches the order against opposite resting orders. The order may be partially filled."
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.create(request);
    }

    @Operation(summary = "List orders")
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
    @GetMapping("/{id}")
    public OrderResponse getById(
            @PathVariable Long id
    ) {
        return orderService.getById(id);
    }

    @Operation(
            summary = "Modify an active order",
            description = "Releases the current reservation, reserves assets for the new price or quantity, and matches the order again. Time priority is lost when the price changes or the quantity increases."
    )
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
    @DeleteMapping("/{id}")
    public OrderResponse cancel(
            @PathVariable Long id
    ) {
        return orderService.cancel(id);
    }
}