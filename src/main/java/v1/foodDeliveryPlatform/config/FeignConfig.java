package v1.foodDeliveryPlatform.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication != null && authentication.isAuthenticated()) {
                    if (authentication.getPrincipal() instanceof Jwt jwt) {
                        String token = jwt.getTokenValue();
                        requestTemplate.header("Authorization", "Bearer " + token);
                        log.debug("JWT token added to Feign request");
                    } else {
                        log.warn("Authentication principal is not JWT. Cannot add token to Feign request");
                    }
                } else {
                    log.warn("No authentication found. Feign request will be without JWT token");
                }
            } catch (Exception e) {
                log.error("Failed to add JWT token to Feign request", e);
            }
        };
    }
}
