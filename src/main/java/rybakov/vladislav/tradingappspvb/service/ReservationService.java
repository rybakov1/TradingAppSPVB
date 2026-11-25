package rybakov.vladislav.tradingappspvb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rybakov.vladislav.tradingappspvb.entity.Account;
import rybakov.vladislav.tradingappspvb.entity.Instrument;
import rybakov.vladislav.tradingappspvb.entity.Order;
import rybakov.vladislav.tradingappspvb.entity.Position;
import rybakov.vladislav.tradingappspvb.enums.OrderSide;
import rybakov.vladislav.tradingappspvb.exception.BusinessException;
import rybakov.vladislav.tradingappspvb.repository.PositionRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ReservationService {

    private final PositionRepository positionRepository;

    public void reserve(Order order) {

        if (order.getSide() == OrderSide.BUY) {
            reserveMoney(order);
        } else {
            reserveSecurities(order);
        }
    }

    public void release(Order order) {

        if (order.getSide() == OrderSide.BUY) {
            releaseMoney(order);
        } else {
            releaseSecurities(order);
        }
    }

    private void reserveMoney(Order order) {

        Account account = order.getAccount();

        BigDecimal required = calculateAmount(
                order.getPrice(),
                order.getRemainingQuantity(),
                order.getInstrument()
        );

        if (account.getAvailableBalance()
                .compareTo(required) < 0) {

            throw new BusinessException(
                    "Insufficient available balance"
            );
        }

        account.setReservedBalance(
                account.getReservedBalance().add(required)
        );
    }

    private void reserveSecurities(Order order) {

        Position position = findPosition(order);

        long required = calculateUnits(
                order.getRemainingQuantity(),
                order.getInstrument()
        );

        if (position.getAvailableQuantity() < required) {
            throw new BusinessException(
                    "Insufficient available securities"
            );
        }

        position.setReservedQuantity(
                MonetaryLimits.addQuantity(
                        position.getReservedQuantity(),
                        required
                )
        );
    }

    private void releaseMoney(Order order) {

        Account account = order.getAccount();

        BigDecimal amount = calculateAmount(
                order.getPrice(),
                order.getRemainingQuantity(),
                order.getInstrument()
        );

        account.setReservedBalance(
                account.getReservedBalance().subtract(amount)
        );
    }

    private void releaseSecurities(Order order) {

        Position position = findPosition(order);

        long quantity = calculateUnits(
                order.getRemainingQuantity(),
                order.getInstrument()
        );

        position.setReservedQuantity(
                position.getReservedQuantity() - quantity
        );
    }

    private Position findPosition(Order order) {

        return positionRepository.findForUpdate(
                order.getAccount().getId(),
                order.getInstrument().getSeccode()
        ).orElseThrow(() -> new BusinessException(
                "Account has no position for instrument: "
                        + order.getInstrument().getSeccode()
        ));
    }

    public long calculateUnits(
            long quantity,
            Instrument instrument
    ) {
        try {
            return Math.multiplyExact(
                    quantity,
                    instrument.getLotSize()
            );
        } catch (ArithmeticException ex) {
            throw new BusinessException(
                    "Order quantity is too large"
            );
        }
    }

    public BigDecimal calculateAmount(
            BigDecimal price,
            long quantity,
            Instrument instrument
    ) {
        BigDecimal amount = price.multiply(
                BigDecimal.valueOf(
                        calculateUnits(quantity, instrument)
                )
        );
        MonetaryLimits.requireAmount(amount);
        return amount;
    }
}