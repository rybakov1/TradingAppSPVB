package rybakov.vladislav.tradingappspvb.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import rybakov.vladislav.tradingappspvb.dto.request.CreateInstrumentRequest;
import rybakov.vladislav.tradingappspvb.enums.Currency;
import rybakov.vladislav.tradingappspvb.enums.InstrumentType;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InstrumentIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createsAnInstrument() throws Exception {
        JsonNode created = createInstrument("GAZP", 10, "0.01");

        assertThat(created.get("seccode").asText()).isEqualTo("GAZP");
        assertThat(created.get("type").asText()).isEqualTo("STOCK");
        assertThat(created.get("lotSize").asLong()).isEqualTo(10);
        assertMoney(created, "minStep", "0.01");
        assertThat(created.get("currency").asText()).isEqualTo("RUB");

        JsonNode loaded = getJson("/api/instruments/GAZP");
        assertThat(loaded.get("seccode").asText()).isEqualTo("GAZP");
        assertThat(loaded.get("lotSize").asLong()).isEqualTo(10);
        assertMoney(loaded, "minStep", "0.01");
        assertThat(getJson("/api/instruments").size()).isEqualTo(1);
    }

    @Test
    void rejectsADuplicateSeccode() throws Exception {
        createInstrument("GAZP", 10, "0.01");

        JsonNode error = exchange(
                post("/api/instruments"),
                new CreateInstrumentRequest(
                        "GAZP",
                        InstrumentType.STOCK,
                        1L,
                        new BigDecimal("0.01"),
                        Currency.RUB
                ),
                status().isConflict()
        );

        assertThat(error.get("status").asInt()).isEqualTo(409);
        assertThat(error.get("message").asText()).contains("GAZP");
        assertThat(getJson("/api/instruments")).hasSize(1);
    }

    @Test
    void rejectsInvalidInstrumentParameters() throws Exception {
        JsonNode error = exchange(
                post("/api/instruments"),
                new CreateInstrumentRequest(
                        "BAD",
                        InstrumentType.STOCK,
                        0L,
                        new BigDecimal("0.01"),
                        Currency.RUB
                ),
                status().isBadRequest()
        );

        assertThat(error.get("status").asInt()).isEqualTo(400);
        assertThat(getJson("/api/instruments").size()).isZero();
    }
}
