package rybakov.vladislav.tradingappspvb.unit;

import org.junit.jupiter.api.Test;
import rybakov.vladislav.tradingappspvb.exception.BusinessException;
import rybakov.vladislav.tradingappspvb.service.MonetaryLimits;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonetaryLimitsTest {

    @Test
    void acceptsTheNumericBoundaryAndRejectsAnythingBeyondIt() {
        MonetaryLimits.requireAmount(new BigDecimal("99999999999.99999999"));
        MonetaryLimits.requireAmount(new BigDecimal("100.00000000"));
        MonetaryLimits.requireAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> MonetaryLimits.requireAmount(
                new BigDecimal("99999999999.99999999").add(new BigDecimal("0.00000001"))
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Amount exceeds the supported monetary limit of 99999999999.99999999");

        assertThatThrownBy(() -> MonetaryLimits.requireAmount(new BigDecimal("100000000000")))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() -> MonetaryLimits.requireAmount(new BigDecimal("0.000000001")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("99999999999.99999999");

        assertThatThrownBy(() -> MonetaryLimits.requireAmount(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reportsQuantityOverflowInsteadOfWrapping() {
        assertThat(MonetaryLimits.addQuantity(10, 5)).isEqualTo(15);

        assertThatThrownBy(() -> MonetaryLimits.addQuantity(Long.MAX_VALUE, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Quantity exceeds the supported limit");
    }
}
