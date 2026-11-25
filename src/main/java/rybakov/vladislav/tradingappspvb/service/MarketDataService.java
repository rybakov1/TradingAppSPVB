package rybakov.vladislav.tradingappspvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import rybakov.vladislav.tradingappspvb.dto.response.OrderBookLevelResponse;
import rybakov.vladislav.tradingappspvb.dto.response.OrderBookResponse;
import rybakov.vladislav.tradingappspvb.dto.response.QuoteResponse;
import rybakov.vladislav.tradingappspvb.entity.Instrument;
import rybakov.vladislav.tradingappspvb.entity.Order;
import rybakov.vladislav.tradingappspvb.entity.Trade;
import rybakov.vladislav.tradingappspvb.enums.OrderSide;
import rybakov.vladislav.tradingappspvb.enums.OrderStatus;
import rybakov.vladislav.tradingappspvb.repository.OrderRepository;
import rybakov.vladislav.tradingappspvb.repository.TradeRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketDataService {

    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(
                    OrderStatus.NEW,
                    OrderStatus.PARTIALLY_FILLED
            );

    private final InstrumentService instrumentService;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;

    public OrderBookResponse getOrderBook(String seccode) {

        Instrument instrument =
                instrumentService.findInstrument(seccode);

        List<Order> activeOrders = orderRepository
                .findAllByInstrument_SeccodeAndStatusIn(
                        instrument.getSeccode(),
                        ACTIVE_STATUSES
                );

        List<OrderBookLevelResponse> bids = aggregateOrders(
                activeOrders,
                OrderSide.BUY,
                Comparator.reverseOrder()
        );

        List<OrderBookLevelResponse> asks = aggregateOrders(
                activeOrders,
                OrderSide.SELL,
                Comparator.naturalOrder()
        );

        return new OrderBookResponse(
                instrument.getSeccode(),
                bids,
                asks
        );
    }

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public QuoteResponse getQuote(String seccode) {

        OrderBookResponse orderBook = getOrderBook(seccode);

        BigDecimal bestBid = orderBook.bids().isEmpty()
                ? null
                : orderBook.bids().getFirst().price();

        BigDecimal bestAsk = orderBook.asks().isEmpty()
                ? null
                : orderBook.asks().getFirst().price();

        BigDecimal spread = bestBid != null && bestAsk != null
                ? bestAsk.subtract(bestBid)
                : null;

        Trade lastTrade = tradeRepository
                .findFirstByInstrument_SeccodeOrderByExecutedAtDescIdDesc(
                        orderBook.seccode()
                )
                .orElse(null);

        return new QuoteResponse(
                orderBook.seccode(),
                bestBid,
                bestAsk,
                spread,
                lastTrade != null
                        ? lastTrade.getPrice()
                        : null,
                lastTrade != null
                        ? lastTrade.getExecutedAt()
                        : null
        );
    }

    private List<OrderBookLevelResponse> aggregateOrders(
            List<Order> orders,
            OrderSide side,
            Comparator<BigDecimal> comparator
    ) {
        Map<BigDecimal, OrderBookLevelResponse> levels =
                new TreeMap<>(comparator);

        for (Order order : orders) {

            if (order.getSide() != side
                    || order.getRemainingQuantity() <= 0) {
                continue;
            }

            OrderBookLevelResponse currentLevel =
                    levels.get(order.getPrice());

            long quantity = order.getRemainingQuantity();
            long orderCount = 1L;

            if (currentLevel != null) {
                quantity = Math.addExact(
                        quantity,
                        currentLevel.quantity()
                );

                orderCount = currentLevel.orderCount() + 1;
            }

            levels.put(
                    order.getPrice(),
                    new OrderBookLevelResponse(
                            order.getPrice(),
                            quantity,
                            orderCount
                    )
            );
        }

        return List.copyOf(levels.values());
    }
}