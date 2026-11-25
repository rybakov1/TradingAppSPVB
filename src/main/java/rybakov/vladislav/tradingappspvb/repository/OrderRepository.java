package rybakov.vladislav.tradingappspvb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rybakov.vladislav.tradingappspvb.entity.Order;
import rybakov.vladislav.tradingappspvb.enums.OrderSide;
import rybakov.vladislav.tradingappspvb.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    List<Order> findAllByAccount_Id(Long accountId);

    List<Order> findAllByInstrument_Seccode(String seccode);

    List<Order> findAllByInstrument_SeccodeAndStatusIn(
            String seccode,
            Collection<OrderStatus> statuses
    );

    List<Order>
    findAllByInstrument_SeccodeAndSideAndStatusInAndPriceLessThanEqualOrderByPriceAscPriorityAtAscIdAsc(
            String seccode,
            OrderSide side,
            Collection<OrderStatus> statuses,
            BigDecimal price
    );

    List<Order>
    findAllByInstrument_SeccodeAndSideAndStatusInAndPriceGreaterThanEqualOrderByPriceDescPriorityAtAscIdAsc(
            String seccode,
            OrderSide side,
            Collection<OrderStatus> statuses,
            BigDecimal price
    );
}