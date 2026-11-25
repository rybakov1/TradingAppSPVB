package rybakov.vladislav.tradingappspvb.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rybakov.vladislav.tradingappspvb.entity.Account;
import rybakov.vladislav.tradingappspvb.entity.Instrument;
import rybakov.vladislav.tradingappspvb.entity.Order;
import rybakov.vladislav.tradingappspvb.entity.Position;
import rybakov.vladislav.tradingappspvb.enums.OrderSide;
import rybakov.vladislav.tradingappspvb.exception.BusinessException;
import rybakov.vladislav.tradingappspvb.repository.PositionRepository;
import rybakov.vladislav.tradingappspvb.service.ReservationService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private PositionRepository positionRepository;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(positionRepository);
    }

    @Test
    void reservesCashForUnfilledBuyLots() {
        Account account = account("100000", "0");
        Instrument instrument = instrument(10);
        Order order = order(account, instrument, OrderSide.BUY, "240", 10, 4);

        reservationService.reserve(order);

        assertThat(account.getBalance()).isEqualByComparingTo("100000");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("14400");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("85600");
    }

    @Test
    void reservesSecuritiesForASell() {
        Account account = account("0", "0");
        Instrument instrument = instrument(10);
        Position position = position(1000, 0);
        when(positionRepository.findForUpdate(account.getId(), instrument.getSeccode()))
                .thenReturn(Optional.of(position));
        Order order = order(account, instrument, OrderSide.SELL, "240", 5, 0);

        reservationService.reserve(order);

        assertThat(position.getQuantity()).isEqualTo(1000);
        assertThat(position.getReservedQuantity()).isEqualTo(50);
        assertThat(position.getAvailableQuantity()).isEqualTo(950);
        assertThat(account.getReservedBalance()).isEqualByComparingTo("0");
    }

    @Test
    void rejectsBuyWhenAvailableCashIsNotEnough() {
        Account account = account("1000", "800");
        Order order = order(account, instrument(1), OrderSide.BUY, "10", 30, 0);

        assertThatThrownBy(() -> reservationService.reserve(order))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Insufficient available balance");
        assertThat(account.getReservedBalance()).isEqualByComparingTo("800");
        assertThat(account.getBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void rejectsSellWhenAvailableSecuritiesAreNotEnough() {
        Account account = account("0", "0");
        Instrument instrument = instrument(10);
        Position position = position(100, 70);
        when(positionRepository.findForUpdate(account.getId(), instrument.getSeccode()))
                .thenReturn(Optional.of(position));
        Order order = order(account, instrument, OrderSide.SELL, "10", 4, 0);

        assertThatThrownBy(() -> reservationService.reserve(order))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Insufficient available securities");
        assertThat(position.getReservedQuantity()).isEqualTo(70);
        assertThat(position.getQuantity()).isEqualTo(100);
    }

    @Test
    void releasesReservedCashForTheUnfilledBuyQuantity() {
        Account account = account("100000", "14400");
        Order order = order(account, instrument(10), OrderSide.BUY, "240", 10, 4);

        reservationService.release(order);

        assertThat(account.getReservedBalance()).isEqualByComparingTo("0");
        assertThat(account.getBalance()).isEqualByComparingTo("100000");
    }

    @Test
    void releasesReservedSecuritiesForTheUnfilledSellQuantity() {
        Account account = account("0", "0");
        Instrument instrument = instrument(10);
        Position position = position(1000, 50);
        when(positionRepository.findForUpdate(account.getId(), instrument.getSeccode()))
                .thenReturn(Optional.of(position));
        Order order = order(account, instrument, OrderSide.SELL, "240", 5, 0);

        reservationService.release(order);

        assertThat(position.getReservedQuantity()).isEqualTo(0);
        assertThat(position.getQuantity()).isEqualTo(1000);
    }

    @Test
    void rejectsQuantityThatOverflowsLotMultiplication() {
        Instrument instrument = instrument(2);

        assertThatThrownBy(() -> reservationService.calculateUnits(Long.MAX_VALUE, instrument))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Order quantity is too large");
    }

    private static Account account(String balance, String reserved) {
        Account account = new Account();
        account.setId(10L);
        account.setBalance(new BigDecimal(balance));
        account.setReservedBalance(new BigDecimal(reserved));
        return account;
    }

    private static Instrument instrument(long lotSize) {
        Instrument instrument = new Instrument();
        instrument.setSeccode("SBER");
        instrument.setLotSize(lotSize);
        return instrument;
    }

    private static Position position(long quantity, long reserved) {
        Position position = new Position();
        position.setQuantity(quantity);
        position.setReservedQuantity(reserved);
        return position;
    }

    private static Order order(
            Account account,
            Instrument instrument,
            OrderSide side,
            String price,
            long quantity,
            long filled
    ) {
        Order order = new Order();
        order.setAccount(account);
        order.setInstrument(instrument);
        order.setSide(side);
        order.setPrice(new BigDecimal(price));
        order.setQuantity(quantity);
        order.setFilledQuantity(filled);
        return order;
    }
}
