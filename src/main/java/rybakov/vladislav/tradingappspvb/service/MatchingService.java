package rybakov.vladislav.tradingappspvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rybakov.vladislav.tradingappspvb.entity.Account;
import rybakov.vladislav.tradingappspvb.entity.Instrument;
import rybakov.vladislav.tradingappspvb.entity.Order;
import rybakov.vladislav.tradingappspvb.entity.Position;
import rybakov.vladislav.tradingappspvb.entity.Trade;
import rybakov.vladislav.tradingappspvb.enums.OrderSide;
import rybakov.vladislav.tradingappspvb.enums.OrderStatus;
import rybakov.vladislav.tradingappspvb.repository.OrderRepository;
import rybakov.vladislav.tradingappspvb.repository.PositionRepository;
import rybakov.vladislav.tradingappspvb.repository.TradeRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class MatchingService {

    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(
                    OrderStatus.NEW,
                    OrderStatus.PARTIALLY_FILLED
            );

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;
    private final ReservationService reservationService;

    public void match(Order incomingOrder) {

        List<Order> oppositeOrders =
                findOppositeOrders(incomingOrder);

        for (Order oppositeOrder : oppositeOrders) {

            if (incomingOrder.getRemainingQuantity() == 0) {
                break;
            }

            if (oppositeOrder.getRemainingQuantity() == 0) {
                continue;
            }

            if (incomingOrder.getAccount().getUserId()
                    .equals(oppositeOrder.getAccount().getUserId())) {
                continue;
            }

            executeTrade(incomingOrder, oppositeOrder);
        }
    }

    private List<Order> findOppositeOrders(Order order) {

        String seccode = order.getInstrument().getSeccode();

        if (order.getSide() == OrderSide.BUY) {

            return orderRepository
                    .findAllByInstrument_SeccodeAndSideAndStatusInAndPriceLessThanEqualOrderByPriceAscPriorityAtAscIdAsc(
                            seccode,
                            OrderSide.SELL,
                            ACTIVE_STATUSES,
                            order.getPrice()
                    );
        }

        return orderRepository
                .findAllByInstrument_SeccodeAndSideAndStatusInAndPriceGreaterThanEqualOrderByPriceDescPriorityAtAscIdAsc(
                        seccode,
                        OrderSide.BUY,
                        ACTIVE_STATUSES,
                        order.getPrice()
                );
    }

    private void executeTrade(
            Order incomingOrder,
            Order oppositeOrder
    ) {
        Order buyOrder = incomingOrder.getSide() == OrderSide.BUY
                ? incomingOrder
                : oppositeOrder;

        Order sellOrder = incomingOrder.getSide() == OrderSide.SELL
                ? incomingOrder
                : oppositeOrder;

        Instrument instrument = incomingOrder.getInstrument();

        BigDecimal tradePrice = oppositeOrder.getPrice();

        long tradeQuantity = Math.min(
                buyOrder.getRemainingQuantity(),
                sellOrder.getRemainingQuantity()
        );

        long securitiesQuantity =
                reservationService.calculateUnits(
                        tradeQuantity,
                        instrument
                );

        BigDecimal tradeAmount =
                reservationService.calculateAmount(
                        tradePrice,
                        tradeQuantity,
                        instrument
                );

        BigDecimal reservedAmount =
                reservationService.calculateAmount(
                        buyOrder.getPrice(),
                        tradeQuantity,
                        instrument
                );

        transferMoney(
                buyOrder.getAccount(),
                sellOrder.getAccount(),
                tradeAmount,
                reservedAmount
        );

        transferSecurities(
                buyOrder,
                sellOrder,
                securitiesQuantity
        );

        updateOrder(buyOrder, tradeQuantity);
        updateOrder(sellOrder, tradeQuantity);

        createTrade(
                buyOrder,
                sellOrder,
                instrument,
                tradePrice,
                tradeQuantity
        );
    }

    private void transferMoney(
            Account buyer,
            Account seller,
            BigDecimal tradeAmount,
            BigDecimal reservedAmount
    ) {
        buyer.setBalance(
                buyer.getBalance().subtract(tradeAmount)
        );

        buyer.setReservedBalance(
                buyer.getReservedBalance().subtract(reservedAmount)
        );

        seller.setBalance(
                seller.getBalance().add(tradeAmount)
        );
    }

    private void transferSecurities(
            Order buyOrder,
            Order sellOrder,
            long quantity
    ) {
        String seccode =
                buyOrder.getInstrument().getSeccode();

        Position sellerPosition = positionRepository
                .findForUpdate(
                        sellOrder.getAccount().getId(),
                        seccode
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Seller position is missing"
                ));

        Position buyerPosition = positionRepository
                .findForUpdate(
                        buyOrder.getAccount().getId(),
                        seccode
                )
                .orElseGet(() -> createBuyerPosition(buyOrder));

        sellerPosition.setQuantity(
                sellerPosition.getQuantity() - quantity
        );

        sellerPosition.setReservedQuantity(
                sellerPosition.getReservedQuantity() - quantity
        );

        buyerPosition.setQuantity(
                Math.addExact(
                        buyerPosition.getQuantity(),
                        quantity
                )
        );
    }

    private Position createBuyerPosition(Order buyOrder) {

        Position position = new Position();

        position.setAccount(buyOrder.getAccount());
        position.setInstrument(buyOrder.getInstrument());
        position.setQuantity(0L);
        position.setReservedQuantity(0L);

        return positionRepository.save(position);
    }

    private void updateOrder(
            Order order,
            long executedQuantity
    ) {
        order.setFilledQuantity(
                order.getFilledQuantity() + executedQuantity
        );

        if (order.getRemainingQuantity() == 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
    }

    private void createTrade(
            Order buyOrder,
            Order sellOrder,
            Instrument instrument,
            BigDecimal price,
            long quantity
    ) {
        Trade trade = new Trade();

        trade.setBuyOrder(buyOrder);
        trade.setSellOrder(sellOrder);
        trade.setInstrument(instrument);
        trade.setPrice(price);
        trade.setQuantity(quantity);

        tradeRepository.save(trade);
    }
}