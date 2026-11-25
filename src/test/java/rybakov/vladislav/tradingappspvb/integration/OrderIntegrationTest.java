package rybakov.vladislav.tradingappspvb.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import rybakov.vladislav.tradingappspvb.dto.request.UpdateOrderRequest;
import rybakov.vladislav.tradingappspvb.enums.OrderSide;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderIntegrationTest extends AbstractIntegrationTest {

    @Test
    void buyOrderReservesCash() throws Exception {
        createInstrument("BUY1", 10, "0.01");
        long accountId = id(createAccount(101, "1000"));

        JsonNode order = placeOrder(accountId, "BUY1", OrderSide.BUY, "3", 4);

        assertThat(order.get("status").asText()).isEqualTo("NEW");
        assertThat(order.get("filledQuantity").asLong()).isZero();
        JsonNode account = account(accountId);
        assertMoney(account, "balance", "1000");
        assertMoney(account, "reservedBalance", "120");
        assertMoney(account, "availableBalance", "880");
    }

    @Test
    void sellOrderReservesSecurities() throws Exception {
        createInstrument("SELL1", 10, "0.01");
        long accountId = id(createAccount(102, "0"));
        createPosition(accountId, "SELL1", 100);

        JsonNode order = placeOrder(accountId, "SELL1", OrderSide.SELL, "3", 4);

        assertThat(order.get("status").asText()).isEqualTo("NEW");
        JsonNode position = position(accountId, "SELL1");
        assertThat(position.get("quantity").asLong()).isEqualTo(100);
        assertThat(position.get("reservedQuantity").asLong()).isEqualTo(40);
        assertThat(position.get("availableQuantity").asLong()).isEqualTo(60);
        assertMoney(account(accountId), "reservedBalance", "0");
    }

    @Test
    void insufficientBalanceIsRejectedAndLeavesTheAccountUnchanged() throws Exception {
        createInstrument("CASH", 1, "0.01");
        long accountId = id(createAccount(103, "100"));

        JsonNode error = exchange(
                post("/api/orders"),
                orderRequest(accountId, "CASH", OrderSide.BUY, "10", 11),
                status().is(422)
        );

        assertThat(error.get("message").asText()).isEqualTo("Insufficient available balance");
        assertMoney(account(accountId), "balance", "100");
        assertMoney(account(accountId), "reservedBalance", "0");
        assertThat(getJson("/api/orders?accountId=" + accountId)).isEmpty();
    }

    @Test
    void insufficientSecuritiesAreRejectedAndLeaveThePositionUnchanged() throws Exception {
        createInstrument("SEC", 10, "0.01");
        long accountId = id(createAccount(104, "0"));
        createPosition(accountId, "SEC", 30);

        JsonNode error = exchange(
                post("/api/orders"),
                orderRequest(accountId, "SEC", OrderSide.SELL, "10", 4),
                status().is(422)
        );

        assertThat(error.get("message").asText()).isEqualTo("Insufficient available securities");
        JsonNode position = position(accountId, "SEC");
        assertThat(position.get("quantity").asLong()).isEqualTo(30);
        assertThat(position.get("reservedQuantity").asLong()).isZero();
        assertThat(getJson("/api/orders?accountId=" + accountId)).isEmpty();
    }

    @Test
    void priceThatIsNotAMultipleOfMinStepIsRejected() throws Exception {
        createInstrument("STEP", 1, "0.05");
        long accountId = id(createAccount(105, "1000"));

        JsonNode error = exchange(
                post("/api/orders"),
                orderRequest(accountId, "STEP", OrderSide.BUY, "10.01", 1),
                status().is(422)
        );

        assertThat(error.get("message").asText()).contains("minStep");
        assertMoney(account(accountId), "reservedBalance", "0");
        assertThat(getJson("/api/orders?seccode=STEP")).isEmpty();
    }

    @Test
    void cancelReleasesTheCashReservation() throws Exception {
        createInstrument("CAN", 1, "0.01");
        long accountId = id(createAccount(106, "1000"));
        long orderId = id(placeOrder(accountId, "CAN", OrderSide.BUY, "10", 4));

        JsonNode cancelled = exchange(
                delete("/api/orders/" + orderId),
                null,
                status().isOk()
        );

        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.get("filledQuantity").asLong()).isZero();
        JsonNode account = account(accountId);
        assertMoney(account, "balance", "1000");
        assertMoney(account, "reservedBalance", "0");
        assertMoney(account, "availableBalance", "1000");
    }

    @Test
    void amendRecalculatesTheCashReservation() throws Exception {
        createInstrument("EDT", 1, "0.01");
        long accountId = id(createAccount(107, "100"));
        long orderId = id(placeOrder(accountId, "EDT", OrderSide.BUY, "10", 2));

        JsonNode amended = exchange(
                patch("/api/orders/" + orderId),
                new UpdateOrderRequest(new BigDecimal("12"), null),
                status().isOk()
        );

        assertMoney(amended, "price", "12");
        assertThat(amended.get("quantity").asLong()).isEqualTo(2);
        assertThat(amended.get("status").asText()).isEqualTo("NEW");
        JsonNode account = account(accountId);
        assertMoney(account, "balance", "100");
        assertMoney(account, "reservedBalance", "24");
        assertMoney(account, "availableBalance", "76");
    }

    @Test
    void rejectedAmendKeepsTheOriginalOrderAndReservation() throws Exception {
        createInstrument("ATOM", 1, "0.01");
        long accountId = id(createAccount(108, "20"));
        long orderId = id(placeOrder(accountId, "ATOM", OrderSide.BUY, "10", 2));

        JsonNode error = exchange(
                patch("/api/orders/" + orderId),
                new UpdateOrderRequest(null, 3L),
                status().is(422)
        );

        assertThat(error.get("message").asText()).isEqualTo("Insufficient available balance");
        JsonNode loaded = order(orderId);
        assertMoney(loaded, "price", "10");
        assertThat(loaded.get("quantity").asLong()).isEqualTo(2);
        assertThat(loaded.get("status").asText()).isEqualTo("NEW");
        assertMoney(account(accountId), "reservedBalance", "20");
        assertMoney(account(accountId), "availableBalance", "0");
    }

    @Test
    void filledOrderCannotBeCancelledOrAmended() throws Exception {
        createInstrument("DONE", 1, "0.01");
        long sellerId = id(createAccount(109, "0"));
        long buyerId = id(createAccount(110, "100"));
        createPosition(sellerId, "DONE", 10);
        long sellId = id(placeOrder(sellerId, "DONE", OrderSide.SELL, "10", 1));
        placeOrder(buyerId, "DONE", OrderSide.BUY, "10", 1);

        JsonNode cancelError = exchange(
                delete("/api/orders/" + sellId),
                null,
                status().isConflict()
        );
        JsonNode amendError = exchange(
                patch("/api/orders/" + sellId),
                new UpdateOrderRequest(new BigDecimal("11"), null),
                status().isConflict()
        );

        assertThat(cancelError.get("status").asInt()).isEqualTo(409);
        assertThat(amendError.get("status").asInt()).isEqualTo(409);
        JsonNode sell = order(sellId);
        assertThat(sell.get("status").asText()).isEqualTo("FILLED");
        assertThat(sell.get("filledQuantity").asLong()).isEqualTo(1);
        assertThat(trades("DONE")).hasSize(1);
        assertMoney(account(buyerId), "balance", "90");
        assertMoney(account(sellerId), "balance", "10");
        assertThat(position(sellerId, "DONE").get("quantity").asLong()).isEqualTo(9);
    }
}
