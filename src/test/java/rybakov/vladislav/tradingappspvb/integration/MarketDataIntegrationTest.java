package rybakov.vladislav.tradingappspvb.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import rybakov.vladislav.tradingappspvb.enums.OrderSide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarketDataIntegrationTest extends AbstractIntegrationTest {

    @Test
    void bookAndQuoteShowOnlyActiveAggregatedLevels() throws Exception {
        createInstrument("BOOK", 1, "0.01");
        long sellerId = id(createAccount(501, "0"));
        long buyerId = id(createAccount(502, "1000"));
        long otherBuyer = id(createAccount(503, "100"));
        createPosition(sellerId, "BOOK", 100);
        placeOrder(sellerId, "BOOK", OrderSide.SELL, "15", 1);
        placeOrder(buyerId, "BOOK", OrderSide.BUY, "15", 1);
        placeOrder(buyerId, "BOOK", OrderSide.BUY, "11", 1);
        placeOrder(buyerId, "BOOK", OrderSide.BUY, "10", 1);
        placeOrder(buyerId, "BOOK", OrderSide.BUY, "10", 2);
        placeOrder(sellerId, "BOOK", OrderSide.SELL, "13", 1);
        placeOrder(sellerId, "BOOK", OrderSide.SELL, "12", 4);
        long cancelled = id(placeOrder(otherBuyer, "BOOK", OrderSide.BUY, "9", 1));
        exchange(delete("/api/orders/" + cancelled), null, status().isOk());

        JsonNode book = getJson("/api/orderbook/BOOK");
        JsonNode bids = book.get("bids");
        JsonNode asks = book.get("asks");
        assertThat(bids).hasSize(2);
        assertLevel(bids.get(0), "11", 1, 1);
        assertLevel(bids.get(1), "10", 3, 2);
        assertThat(asks).hasSize(2);
        assertLevel(asks.get(0), "12", 4, 1);
        assertLevel(asks.get(1), "13", 1, 1);

        JsonNode quote = getJson("/api/quotes/BOOK");
        assertMoney(quote, "bestBid", "11");
        assertMoney(quote, "bestAsk", "12");
        assertMoney(quote, "spread", "1");
        assertMoney(quote, "lastTradePrice", "15");
        assertThat(quote.get("lastTradeAt").isNull()).isFalse();
    }

    private static void assertLevel(JsonNode level, String price, long quantity, long orderCount) {
        assertThat(level.get("price").decimalValue()).isEqualByComparingTo(price);
        assertThat(level.get("quantity").asLong()).isEqualTo(quantity);
        assertThat(level.get("orderCount").asLong()).isEqualTo(orderCount);
    }
}
