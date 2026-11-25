package rybakov.vladislav.tradingappspvb.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import rybakov.vladislav.tradingappspvb.dto.request.CreateOrderRequest;
import rybakov.vladislav.tradingappspvb.enums.OrderSide;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Test
    @Timeout(20)
    void parallelBuysCannotReserveMoreCashThanAvailable() throws Exception {
        createInstrument("RACEB", 1, "0.01");
        long accountId = id(createAccount(601, "1000"));
        CreateOrderRequest request = orderRequest(accountId, "RACEB", OrderSide.BUY, "10", 100);

        List<Integer> statuses = runTogether(5, () -> httpStatus(post("/api/orders"), request));

        assertThat(statuses.stream().filter(status -> status == 201).count()).isEqualTo(1);
        assertThat(statuses.stream().filter(status -> status == 422).count()).isEqualTo(4);
        JsonNode account = account(accountId);
        assertMoney(account, "balance", "1000");
        assertMoney(account, "reservedBalance", "1000");
        assertMoney(account, "availableBalance", "0");
        assertThat(getJson("/api/orders?accountId=" + accountId)).hasSize(1);
    }

    @Test
    @Timeout(20)
    void parallelBuysCannotFillTheSameSellTwice() throws Exception {
        createInstrument("RACES", 1, "0.01");
        long sellerId = id(createAccount(701, "0"));
        createPosition(sellerId, "RACES", 5);
        placeOrder(sellerId, "RACES", OrderSide.SELL, "10", 1);
        long[] buyers = new long[5];
        CreateOrderRequest[] requests = new CreateOrderRequest[5];
        for (int index = 0; index < buyers.length; index++) {
            buyers[index] = id(createAccount(710L + index, "100"));
            requests[index] = orderRequest(buyers[index], "RACES", OrderSide.BUY, "10", 1);
        }

        List<Integer> statuses = runTogether(
                requests.length,
                index -> httpStatus(post("/api/orders"), requests[index])
        );

        assertThat(statuses).allMatch(status -> status == 201);
        assertThat(trades("RACES")).hasSize(1);
        assertMoney(trades("RACES").get(0), "price", "10");
        assertThat(trades("RACES").get(0).get("quantity").asLong()).isEqualTo(1);
        assertThat(order(trades("RACES").get(0).get("sellOrderId").asLong()).get("status").asText())
                .isEqualTo("FILLED");
        assertMoney(account(sellerId), "balance", "10");
        assertThat(position(sellerId, "RACES").get("quantity").asLong()).isEqualTo(4);
        int filledBuyers = 0;
        BigDecimal buyerCash = BigDecimal.ZERO;
        long buyerSecurities = 0;
        for (long buyerId : buyers) {
            JsonNode account = account(buyerId);
            buyerCash = buyerCash.add(money(account, "balance"));
            JsonNode orders = getJson("/api/orders?accountId=" + buyerId);
            assertThat(orders).hasSize(1);
            if ("FILLED".equals(orders.get(0).get("status").asText())) {
                filledBuyers++;
                assertThat(position(buyerId, "RACES").get("quantity").asLong()).isEqualTo(1);
                buyerSecurities++;
                assertMoney(account, "reservedBalance", "0");
            } else {
                assertThat(orders.get(0).get("status").asText()).isEqualTo("NEW");
                assertMoney(account, "reservedBalance", "10");
            }
        }
        assertThat(filledBuyers).isEqualTo(1);
        assertThat(buyerSecurities).isEqualTo(1);
        assertThat(buyerCash.add(money(account(sellerId), "balance"))).isEqualByComparingTo("500");
        assertThat(position(sellerId, "RACES").get("quantity").asLong() + buyerSecurities).isEqualTo(5);
    }

    private List<Integer> runTogether(int threads, StatusCall call) throws Exception {
        return runTogether(threads, index -> call.status());
    }

    private List<Integer> runTogether(int threads, IndexedStatusCall call) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(threads);
        List<Integer> statuses = new CopyOnWriteArrayList<>();
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        try {
            for (int index = 0; index < threads; index++) {
                int current = index;
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timed out waiting to start");
                        }
                        statuses.add(call.status(current));
                    } catch (Throwable failure) {
                        failures.add(failure);
                    } finally {
                        finished.countDown();
                    }
                });
            }
            assertThat(ready.await(5, TimeUnit.SECONDS))
                    .as("workers reached the start barrier")
                    .isTrue();
            start.countDown();
            assertThat(finished.await(15, TimeUnit.SECONDS))
                    .as("workers finished")
                    .isTrue();
        } finally {
            pool.shutdownNow();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    failures.add(new IllegalStateException("worker threads did not stop"));
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                failures.add(interrupted);
            }
        }
        assertThat(failures).isEmpty();
        return statuses;
    }

    @FunctionalInterface
    private interface StatusCall {
        int status();
    }

    @FunctionalInterface
    private interface IndexedStatusCall {
        int status(int index);
    }
}
