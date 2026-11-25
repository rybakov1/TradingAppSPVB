package rybakov.vladislav.tradingappspvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rybakov.vladislav.tradingappspvb.dto.request.CreateOrderRequest;
import rybakov.vladislav.tradingappspvb.dto.request.UpdateOrderRequest;
import rybakov.vladislav.tradingappspvb.dto.response.OrderResponse;
import rybakov.vladislav.tradingappspvb.entity.Account;
import rybakov.vladislav.tradingappspvb.entity.Instrument;
import rybakov.vladislav.tradingappspvb.entity.Order;
import rybakov.vladislav.tradingappspvb.enums.OrderStatus;
import rybakov.vladislav.tradingappspvb.exception.BusinessException;
import rybakov.vladislav.tradingappspvb.exception.ConflictException;
import rybakov.vladislav.tradingappspvb.exception.ResourceNotFoundException;
import rybakov.vladislav.tradingappspvb.repository.AccountRepository;
import rybakov.vladislav.tradingappspvb.repository.InstrumentRepository;
import rybakov.vladislav.tradingappspvb.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(
                    OrderStatus.NEW,
                    OrderStatus.PARTIALLY_FILLED
            );

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final InstrumentRepository instrumentRepository;

    private final ReservationService reservationService;
    private final MatchingService matchingService;

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {

        Instrument instrument =
                lockInstrument(request.seccode());

        Map<Long, Account> accounts =
                lockAccounts(instrument, request.accountId());

        Account account = accounts.get(request.accountId());

        if (account == null) {
            throw new ResourceNotFoundException(
                    "Account not found: " + request.accountId()
            );
        }

        validateCurrency(account, instrument);

        validateOrderParameters(
                request.price(),
                request.quantity(),
                instrument
        );

        Order order = new Order();

        order.setAccount(account);
        order.setInstrument(instrument);
        order.setSide(request.side());
        order.setPrice(request.price());
        order.setQuantity(request.quantity());
        order.setFilledQuantity(0L);
        order.setStatus(OrderStatus.NEW);

        Instant now = Instant.now();

        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setPriorityAt(now);

        reservationService.reserve(order);

        orderRepository.save(order);

        matchingService.match(order);

        return toResponse(order);
    }

    @Transactional
    public OrderResponse update(
            Long id,
            UpdateOrderRequest request
    ) {
        Instrument instrument =
                lockInstrumentByOrderId(id);

        Order order = findOrder(id);

        validateActiveOrder(order);

        lockAccounts(
                instrument,
                order.getAccount().getId()
        );

        if (request.price() == null
                && request.quantity() == null) {
            throw new BusinessException(
                    "At least one field must be provided"
            );
        }

        BigDecimal newPrice = request.price() != null
                ? request.price()
                : order.getPrice();

        long newQuantity = request.quantity() != null
                ? request.quantity()
                : order.getQuantity();

        validateOrderParameters(
                newPrice,
                newQuantity,
                instrument
        );

        if (newQuantity <= order.getFilledQuantity()) {
            throw new BusinessException(
                    "Quantity must exceed already filled quantity"
            );
        }

        boolean priceChanged =
                newPrice.compareTo(order.getPrice()) != 0;

        boolean quantityChanged =
                newQuantity != order.getQuantity();

        if (!priceChanged && !quantityChanged) {
            return toResponse(order);
        }

        boolean priorityLost =
                priceChanged
                        || newQuantity > order.getQuantity();

        reservationService.release(order);

        order.setPrice(newPrice);
        order.setQuantity(newQuantity);

        if (priorityLost) {
            order.setPriorityAt(Instant.now());
        }

        order.setUpdatedAt(Instant.now());

        reservationService.reserve(order);

        matchingService.match(order);

        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(Long id) {

        lockInstrumentByOrderId(id);

        Order order = findOrder(id);

        validateActiveOrder(order);

        accountRepository.findByIdForUpdate(
                order.getAccount().getId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Account not found"
        ));

        reservationService.release(order);

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());

        return toResponse(order);
    }

    public OrderResponse getById(Long id) {
        return toResponse(findOrder(id));
    }

    public List<OrderResponse> getAll() {
        return orderRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderResponse> getByAccountId(
            Long accountId
    ) {
        return orderRepository.findAllByAccount_Id(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OrderResponse> getBySeccode(
            String seccode
    ) {
        return orderRepository
                .findAllByInstrument_Seccode(
                        normalizeSeccode(seccode)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + id
                ));
    }

    private Instrument lockInstrument(String seccode) {

        String normalized = normalizeSeccode(seccode);

        return instrumentRepository
                .findBySeccodeForUpdate(normalized)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Instrument not found: " + normalized
                ));
    }

    private Instrument lockInstrumentByOrderId(Long id) {

        String seccode = orderRepository
                .findSeccodeById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + id
                ));

        return lockInstrument(seccode);
    }

    private Map<Long, Account> lockAccounts(
            Instrument instrument,
            Long incomingAccountId
    ) {
        Set<Long> accountIds = new TreeSet<>();

        accountIds.add(incomingAccountId);

        List<Order> activeOrders = orderRepository
                .findAllByInstrument_SeccodeAndStatusIn(
                        instrument.getSeccode(),
                        ACTIVE_STATUSES
                );

        for (Order order : activeOrders) {
            accountIds.add(order.getAccount().getId());
        }

        return accountRepository
                .findAllByIdsForUpdate(accountIds)
                .stream()
                .collect(Collectors.toMap(
                        Account::getId,
                        Function.identity()
                ));
    }

    private void validateActiveOrder(Order order) {

        if (!ACTIVE_STATUSES.contains(order.getStatus())) {
            throw new ConflictException(
                    "Order cannot be modified in status: "
                            + order.getStatus()
            );
        }
    }

    private void validateCurrency(
            Account account,
            Instrument instrument
    ) {
        if (account.getCurrency() != instrument.getCurrency()) {
            throw new BusinessException(
                    "Account currency does not match instrument currency"
            );
        }
    }

    private void validateOrderParameters(
            BigDecimal price,
            long quantity,
            Instrument instrument
    ) {
        if (price == null || price.signum() <= 0) {
            throw new BusinessException(
                    "Price must be positive"
            );
        }

        if (quantity <= 0) {
            throw new BusinessException(
                    "Quantity must be positive"
            );
        }

        BigDecimal normalizedPrice =
                price.stripTrailingZeros();

        if (normalizedPrice.scale() > 8
                || normalizedPrice.precision()
                - normalizedPrice.scale() > 11) {
            throw new BusinessException(
                    "Price exceeds supported precision"
            );
        }

        if (price.remainder(instrument.getMinStep())
                .compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(
                    "Price must be a multiple of minStep: "
                            + instrument.getMinStep()
            );
        }

        reservationService.calculateUnits(
                quantity,
                instrument
        );
    }

    private String normalizeSeccode(String seccode) {
        return seccode.trim().toUpperCase(Locale.ROOT);
    }

    private OrderResponse toResponse(Order order) {

        return new OrderResponse(
                order.getId(),
                order.getAccount().getId(),
                order.getInstrument().getSeccode(),
                order.getSide(),
                order.getPrice(),
                order.getQuantity(),
                order.getFilledQuantity(),
                order.getRemainingQuantity(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}