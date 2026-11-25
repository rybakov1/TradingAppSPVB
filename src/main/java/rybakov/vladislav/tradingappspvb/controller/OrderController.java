package rybakov.vladislav.tradingappspvb.controller;

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

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.create(request);
    }

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

    @GetMapping("/{id}")
    public OrderResponse getById(
            @PathVariable Long id
    ) {
        return orderService.getById(id);
    }

    @PatchMapping("/{id}")
    public OrderResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        return orderService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public OrderResponse cancel(
            @PathVariable Long id
    ) {
        return orderService.cancel(id);
    }
}