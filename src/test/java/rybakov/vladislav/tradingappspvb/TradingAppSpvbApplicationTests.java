package rybakov.vladislav.tradingappspvb;

import org.junit.jupiter.api.Test;
import rybakov.vladislav.tradingappspvb.integration.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TradingAppSpvbApplicationTests
        extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }

    @Test
    void shouldReturnEmptyInstrumentList() throws Exception {
        mockMvc.perform(get("/api/instruments"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}