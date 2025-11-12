package v1.foodDeliveryPlatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Random;

@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
public class AppConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .description("Food delivery platform")
                        .version("1.0.0")
                );
    }

    @Bean
    public Random random() {
        return new Random();
    }
}
