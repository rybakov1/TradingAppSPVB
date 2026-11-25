package rybakov.vladislav.tradingappspvb.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import rybakov.vladislav.tradingappspvb.dto.request.CreateAccountRequest;
import rybakov.vladislav.tradingappspvb.dto.request.CreateInstrumentRequest;
import rybakov.vladislav.tradingappspvb.dto.request.CreateOrderRequest;
import rybakov.vladislav.tradingappspvb.dto.request.CreatePositionRequest;
import rybakov.vladislav.tradingappspvb.enums.Currency;
import rybakov.vladislav.tradingappspvb.enums.InstrumentType;
import rybakov.vladislav.tradingappspvb.enums.OrderSide;

import java.math.BigDecimal;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests share one Spring context and one PostgreSQL 16 container.
 * The container is a Spring bean, so it stays up while the context is cached
 * and stops only when that context closes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.PostgresTestConfiguration.class)
public abstract class AbstractIntegrationTest {

    static final String TEST_DATABASE = "trading_it";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() throws Exception {
        String database = jdbcTemplate.queryForObject(
                "SELECT current_database()",
                String.class
        );
        String jdbcUrl;
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            jdbcUrl = connection.getMetaData().getURL();
        }
        if (!TEST_DATABASE.equals(database)
                || jdbcUrl == null
                || !jdbcUrl.contains("/" + TEST_DATABASE)) {
            throw new IllegalStateException(
                    "Refusing to truncate database '" + database
                            + "' at '" + jdbcUrl
                            + "'. Integration tests may only clean " + TEST_DATABASE
            );
        }

        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    trades,
                    orders,
                    positions,
                    accounts,
                    instruments
                RESTART IDENTITY CASCADE
                """);
    }

    protected JsonNode createInstrument(
            String seccode,
            long lotSize,
            String minStep
    ) throws Exception {
        return postJson(
                "/api/instruments",
                new CreateInstrumentRequest(
                        seccode,
                        InstrumentType.STOCK,
                        lotSize,
                        new BigDecimal(minStep),
                        Currency.RUB
                )
        );
    }

    protected JsonNode createAccount(long userId, String balance) throws Exception {
        return postJson(
                "/api/accounts",
                new CreateAccountRequest(
                        userId,
                        new BigDecimal(balance),
                        Currency.RUB
                )
        );
    }

    protected JsonNode createPosition(
            long accountId,
            String seccode,
            long quantity
    ) throws Exception {
        return postJson(
                "/api/accounts/" + accountId + "/positions",
                new CreatePositionRequest(seccode, quantity)
        );
    }

    protected JsonNode placeOrder(
            long accountId,
            String seccode,
            OrderSide side,
            String price,
            long quantity
    ) throws Exception {
        return postJson(
                "/api/orders",
                orderRequest(accountId, seccode, side, price, quantity)
        );
    }

    protected CreateOrderRequest orderRequest(
            long accountId,
            String seccode,
            OrderSide side,
            String price,
            long quantity
    ) {
        return new CreateOrderRequest(
                accountId,
                seccode,
                side,
                new BigDecimal(price),
                quantity
        );
    }

    protected JsonNode postJson(String path, Object body) throws Exception {
        return exchange(post(path), body, status().isCreated());
    }

    protected JsonNode exchange(
            MockHttpServletRequestBuilder builder,
            Object body,
            ResultMatcher expectedStatus
    ) throws Exception {
        MockHttpServletRequestBuilder request = builder.contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.content(objectMapper.writeValueAsString(body));
        }
        String raw = mockMvc.perform(request)
                .andExpect(expectedStatus)
                .andReturn()
                .getResponse()
                .getContentAsString();
        if (raw.isBlank()) {
            return objectMapper.nullNode();
        }
        return objectMapper.readTree(raw);
    }

    protected int httpStatus(
            MockHttpServletRequestBuilder builder,
            Object body
    ) {
        try {
            MockHttpServletRequestBuilder request = builder.contentType(MediaType.APPLICATION_JSON);
            if (body != null) {
                request.content(objectMapper.writeValueAsString(body));
            }
            return mockMvc.perform(request)
                    .andReturn()
                    .getResponse()
                    .getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    protected JsonNode getJson(String path) throws Exception {
        String raw = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(raw);
    }

    protected JsonNode account(long id) throws Exception {
        return getJson("/api/accounts/" + id);
    }

    protected JsonNode position(long accountId, String seccode) throws Exception {
        return getJson("/api/accounts/" + accountId + "/positions/" + seccode);
    }

    protected JsonNode order(long id) throws Exception {
        return getJson("/api/orders/" + id);
    }

    protected JsonNode trades(String seccode) throws Exception {
        return getJson("/api/trades?seccode=" + seccode);
    }

    protected static BigDecimal money(JsonNode node, String field) {
        return node.get(field).decimalValue();
    }

    protected static void assertMoney(JsonNode node, String field, String expected) {
        assertThat(money(node, field)).isEqualByComparingTo(expected);
    }

    protected static long id(JsonNode node) {
        return node.get("id").asLong();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgresTestConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName(TEST_DATABASE)
                    .withUsername(TEST_DATABASE)
                    .withPassword(TEST_DATABASE);
        }
    }
}
