package rybakov.vladislav.tradingappspvb.service;

import rybakov.vladislav.tradingappspvb.exception.BusinessException;

import java.math.BigDecimal;

public final class MonetaryLimits {

    public static final BigDecimal MAX_AMOUNT =
            new BigDecimal("99999999999.99999999");

    private static final String AMOUNT_MESSAGE =
            "Amount exceeds the supported monetary limit of 99999999999.99999999";

    private static final String QUANTITY_MESSAGE =
            "Quantity exceeds the supported limit";

    private MonetaryLimits() {
    }

    public static void requireAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessException(AMOUNT_MESSAGE);
        }

        int scale = amount.stripTrailingZeros().scale();
        if (scale > 8) {
            throw new BusinessException(AMOUNT_MESSAGE);
        }
    }

    public static long addQuantity(long current, long increment) {
        try {
            return Math.addExact(current, increment);
        } catch (ArithmeticException ex) {
            throw new BusinessException(QUANTITY_MESSAGE);
        }
    }
}
