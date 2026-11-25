package rybakov.vladislav.tradingappspvb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tradingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trading API")
                        .description(
                                "Trading system REST API. " +
                                        "Order management, matching engine, " +
                                        "trade registry and market data."
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Vladislav Rybakov")
                        )
                );
    }
}