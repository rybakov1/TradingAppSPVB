package rybakov.vladislav.tradingappspvb.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import rybakov.vladislav.tradingappspvb.enums.OrderSide;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MatchingIntegrationTest extends AbstractIntegrationTest {

    @Test
    void fullyFillsBothOrdersAtTheRestingPrice() throws Exception {
        createInstrument("FULL", 1, "0.01");
        long sellerId = id(createAccount(201, "0"));
        long buyerId = id(createAccount(202, "100"));
        createPosition(sellerId, "FULL", 10);
        long sellId = id(placeOrder(sellerId, "FULL", OrderSide.SELL, "18", 1));

        JsonNode buy = placeOrder(buyerId, "FULL", OrderSide.BUY, "20", 1);

        assertThat(buy.get("status").asText()).isEqualTo("FILLED");
        assertThat(order(sellId).get("status").asText()).isEqualTo("FILLED");
        JsonNode trade = trades("FULL").get(0);
        assertMoney(trade, "price", "18");
        assertThat(trade.get("quantity").asLong()).isEqualTo(1);
        assertThat(trade.get("sellOrderId").asLong()).isEqualTo(sellId);
        assertThat(trade.get("buyOrderId").asLong()).isEqualTo(id(buy));
        assertMoney(account(buyerId), "balance", "82");
        assertMoney(account(buyerId), "reservedBalance", "0");
        assertMoney(account(sellerId), "balance", "18");
        assertThat(position(sellerId, "FULL").get("quantity").asLong()).isEqualTo(9);
        assertThat(position(buyerId, "FULL").get("quantity").asLong()).isEqualTo(1);
    }

    @Test
    void executesFiveLotsAtTheRestingPriceAndReleasesOnlyTheRemainder() throws Exception {
        createInstrument("SPEC", 10, "0.01");
        long buyerId = id(createAccount(301, "100000"));
        long sellerId = id(createAccount(302, "0"));
        createPosition(sellerId, "SPEC", 1000);
        long sellId = id(placeOrder(sellerId, "SPEC", OrderSide.SELL, "240", 5));

        JsonNode buy = placeOrder(buyerId, "SPEC", OrderSide.BUY, "250", 10);

        long buyId = id(buy);
        assertThat(order(sellId).get("status").asText()).isEqualTo("FILLED");
        assertThat(buy.get("status").asText()).isEqualTo("PARTIALLY_FILLED");
        assertThat(buy.get("filledQuantity").asLong()).isEqualTo(5);
        assertThat(buy.get("remainingQuantity").asLong()).isEqualTo(5);
        JsonNode trade = trades("SPEC").get(0);
        assertThat(trades("SPEC")).hasSize(1);
        assertMoney(trade, "price", "240");
        assertThat(trade.get("quantity").asLong()).isEqualTo(5);
        assertMoney(account(buyerId), "balance", "88000");
        assertMoney(account(buyerId), "reservedBalance", "12500");
        assertMoney(account(buyerId), "availableBalance", "75500");
        assertMoney(account(sellerId), "balance", "12000");
        assertThat(position(buyerId, "SPEC").get("quantity").asLong()).isEqualTo(50);
        assertThat(position(sellerId, "SPEC").get("quantity").asLong()).isEqualTo(950);
        assertThat(position(sellerId, "SPEC").get("reservedQuantity").asLong()).isZero();
        assertThat(money(account(buyerId), "balance").add(money(account(sellerId), "balance")))
                .isEqualByComparingTo("100000");
        assertThat(position(buyerId, "SPEC").get("quantity").asLong()
                + position(sellerId, "SPEC").get("quantity").asLong()).isEqualTo(1000);

        JsonNode cancelled = exchange(
                delete("/api/orders/" + buyId),
                null,
                status().isOk()
        );

        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.get("filledQuantity").asLong()).isEqualTo(5);
        assertThat(cancelled.get("remainingQuantity").asLong()).isEqualTo(5);
        assertMoney(account(buyerId), "balance", "88000");
        assertMoney(account(buyerId), "reservedBalance", "0");
        assertMoney(account(buyerId), "availableBalance", "88000");
        assertMoney(account(sellerId), "balance", "12000");
        assertThat(position(buyerId, "SPEC").get("quantity").asLong()).isEqualTo(50);
        assertThat(position(sellerId, "SPEC").get("quantity").asLong()).isEqualTo(950);
        assertThat(trades("SPEC")).hasSize(1);
        assertThat(trades("SPEC").get(0).get("id").asLong()).isEqualTo(trade.get("id").asLong());
    }

    @Test
    void bestPriceHasPriorityOverAnEarlierWorseOrder() throws Exception {
        createInstrument("BEST", 1, "0.01");
        long expensiveSeller = id(createAccount(401, "0"));
        long cheapSeller = id(createAccount(402, "0"));
        long buyerId = id(createAccount(403, "100"));
        createPosition(expensiveSeller, "BEST", 10);
        createPosition(cheapSeller, "BEST", 10);
        long expensiveSell = id(placeOrder(expensiveSeller, "BEST", OrderSide.SELL, "20", 1));
        long cheapSell = id(placeOrder(cheapSeller, "BEST", OrderSide.SELL, "18", 1));

        placeOrder(buyerId, "BEST", OrderSide.BUY, "20", 1);

        JsonNode trade = trades("BEST").get(0);
        assertThat(trades("BEST")).hasSize(1);
        assertMoney(trade, "price", "18");
        assertThat(trade.get("sellOrderId").asLong()).isEqualTo(cheapSell);
        assertThat(order(cheapSell).get("status").asText()).isEqualTo("FILLED");
        assertThat(order(expensiveSell).get("status").asText()).isEqualTo("NEW");
        assertMoney(account(buyerId), "balance", "82");
        assertMoney(account(cheapSeller), "balance", "18");
        assertMoney(account(expensiveSeller), "balance", "0");
    }

    @Test
    void equalPricesAreFilledFirstInFirstOut() throws Exception {
        createInstrument("FIFO", 1, "0.01");
        long firstSeller = id(createAccount(411, "0"));
        long secondSeller = id(createAccount(412, "0"));
        long buyerId = id(createAccount(413, "100"));
        createPosition(firstSeller, "FIFO", 10);
        createPosition(secondSeller, "FIFO", 10);
        long firstSell = id(placeOrder(firstSeller, "FIFO", OrderSide.SELL, "10", 1));
        long secondSell = id(placeOrder(secondSeller, "FIFO", OrderSide.SELL, "10", 1));

        placeOrder(buyerId, "FIFO", OrderSide.BUY, "10", 1);

        assertThat(trades("FIFO")).hasSize(1);
        assertThat(trades("FIFO").get(0).get("sellOrderId").asLong()).isEqualTo(firstSell);
        assertThat(order(firstSell).get("status").asText()).isEqualTo("FILLED");
        assertThat(order(secondSell).get("status").asText()).isEqualTo("NEW");
        assertThat(position(secondSeller, "FIFO").get("reservedQuantity").asLong()).isEqualTo(1);
    }

    @Test
    void oneBuyFillsSeveralRestingSellsAtTheirOwnPrices() throws Exception {
        createInstrument("MULTI", 1, "0.01");
        long firstSeller = id(createAccount(421, "0"));
        long secondSeller = id(createAccount(422, "0"));
        long thirdSeller = id(createAccount(423, "0"));
        long buyerId = id(createAccount(424, "100"));
        createPosition(firstSeller, "MULTI", 10);
        createPosition(secondSeller, "MULTI", 10);
        createPosition(thirdSeller, "MULTI", 10);
        placeOrder(firstSeller, "MULTI", OrderSide.SELL, "12", 1);
        placeOrder(secondSeller, "MULTI", OrderSide.SELL, "10", 1);
        placeOrder(thirdSeller, "MULTI", OrderSide.SELL, "11", 1);

        JsonNode buy = placeOrder(buyerId, "MULTI", OrderSide.BUY, "12", 3);

        assertThat(buy.get("status").asText()).isEqualTo("FILLED");
        assertThat(buy.get("filledQuantity").asLong()).isEqualTo(3);
        JsonNode executed = trades("MULTI");
        assertThat(executed).hasSize(3);
        Set<BigDecimal> prices = new HashSet<>();
        for (JsonNode trade : executed) {
            prices.add(trade.get("price").decimalValue());
            assertThat(trade.get("quantity").asLong()).isEqualTo(1);
        }
        assertThat(prices)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactlyInAnyOrder(
                        new BigDecimal("10"),
                        new BigDecimal("11"),
                        new BigDecimal("12")
                );
        assertMoney(account(buyerId), "balance", "67");
        assertMoney(account(buyerId), "reservedBalance", "0");
        assertMoney(account(firstSeller), "balance", "12");
        assertMoney(account(secondSeller), "balance", "10");
        assertMoney(account(thirdSeller), "balance", "11");
        assertThat(position(buyerId, "MULTI").get("quantity").asLong()).isEqualTo(3);
    }

    @Test
    void cancelAggressorStopsAtTheUsersOwnOrder() throws Exception {
        createInstrument("STP1", 1, "0.01");
        long userId = 431L;
        long sellerId = id(createAccount(userId, "0"));
        long buyerId = id(createAccount(userId, "10000"));
        createPosition(sellerId, "STP1", 100);
        long sellId = id(placeOrder(sellerId, "STP1", OrderSide.SELL, "100", 1));

        JsonNode buy = placeOrder(buyerId, "STP1", OrderSide.BUY, "110", 1);

        assertThat(buy.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(buy.get("filledQuantity").asLong()).isZero();
        assertThat(order(sellId).get("status").asText()).isEqualTo("NEW");
        assertThat(trades("STP1")).isEmpty();
        assertMoney(account(buyerId), "balance", "10000");
        assertMoney(account(buyerId), "reservedBalance", "0");
        assertMoney(account(sellerId), "balance", "0");
        assertThat(position(sellerId, "STP1").get("quantity").asLong()).isEqualTo(100);
        assertThat(position(sellerId, "STP1").get("reservedQuantity").asLong()).isEqualTo(1);
        JsonNode quote = getJson("/api/quotes/STP1");
        assertThat(quote.get("bestBid").isNull()).isTrue();
        assertMoney(quote, "bestAsk", "100");
        assertThat(quote.get("spread").isNull()).isTrue();
    }

    @Test
    void cancelAggressorKeepsTheFillThatHappenedBeforeTheOwnOrder() throws Exception {
        createInstrument("STP2", 1, "0.01");
        long userId = 441L;
        long otherSeller = id(createAccount(451, "0"));
        long ownSeller = id(createAccount(userId, "0"));
        long ownBuyer = id(createAccount(userId, "10000"));
        createPosition(otherSeller, "STP2", 10);
        createPosition(ownSeller, "STP2", 10);
        long otherSell = id(placeOrder(otherSeller, "STP2", OrderSide.SELL, "100", 1));
        long ownSell = id(placeOrder(ownSeller, "STP2", OrderSide.SELL, "101", 1));

        JsonNode buy = placeOrder(ownBuyer, "STP2", OrderSide.BUY, "110", 2);

        assertThat(buy.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(buy.get("filledQuantity").asLong()).isEqualTo(1);
        assertThat(buy.get("remainingQuantity").asLong()).isEqualTo(1);
        assertThat(trades("STP2")).hasSize(1);
        assertMoney(trades("STP2").get(0), "price", "100");
        assertThat(trades("STP2").get(0).get("sellOrderId").asLong()).isEqualTo(otherSell);
        assertThat(order(otherSell).get("status").asText()).isEqualTo("FILLED");
        assertThat(order(ownSell).get("status").asText()).isEqualTo("NEW");
        assertThat(order(ownSell).get("filledQuantity").asLong()).isZero();
        assertMoney(account(ownBuyer), "balance", "9900");
        assertMoney(account(ownBuyer), "reservedBalance", "0");
        assertMoney(account(otherSeller), "balance", "100");
        assertMoney(account(ownSeller), "balance", "0");
        assertThat(position(otherSeller, "STP2").get("quantity").asLong()).isEqualTo(9);
        assertThat(position(ownBuyer, "STP2").get("quantity").asLong()).isEqualTo(1);
        assertThat(position(ownSeller, "STP2").get("quantity").asLong()).isEqualTo(10);
        assertThat(position(ownSeller, "STP2").get("reservedQuantity").asLong()).isEqualTo(1);
        JsonNode quote = getJson("/api/quotes/STP2");
        assertThat(quote.get("bestBid").isNull()).isTrue();
        assertMoney(quote, "bestAsk", "101");
    }

    @Test
    void balanceOverflowRollsTheWholeOrderBack() throws Exception {
        createInstrument("OVF", 1, "0.00000001");
        long sellerId = id(createAccount(461, "60000000000"));
        long buyerId = id(createAccount(462, "60000000000"));
        createPosition(sellerId, "OVF", 100);
        long sellId = id(placeOrder(sellerId, "OVF", OrderSide.SELL, "10000000000", 6));

        JsonNode error = exchange(
                post("/api/orders"),
                orderRequest(buyerId, "OVF", OrderSide.BUY, "10000000000", 6),
                status().is(422)
        );

        assertThat(error.get("message").asText())
                .isEqualTo("Amount exceeds the supported monetary limit of 99999999999.99999999");
        JsonNode sell = order(sellId);
        assertThat(sell.get("status").asText()).isEqualTo("NEW");
        assertThat(sell.get("filledQuantity").asLong()).isZero();
        assertThat(trades("OVF")).isEmpty();
        assertThat(getJson("/api/orders?seccode=OVF")).hasSize(1);
        assertMoney(account(sellerId), "balance", "60000000000");
        assertMoney(account(buyerId), "balance", "60000000000");
        assertMoney(account(buyerId), "reservedBalance", "0");
        assertThat(position(sellerId, "OVF").get("quantity").asLong()).isEqualTo(100);
        assertThat(position(sellerId, "OVF").get("reservedQuantity").asLong()).isEqualTo(6);
    }
}
