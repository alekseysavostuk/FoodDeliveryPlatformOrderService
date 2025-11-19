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

                log.info("=== FEIGN DEBUG ===");
                log.info("URL: {}", requestTemplate.url());
                log.info("Authentication: {}", authentication);
                log.info("Is authenticated: {}", authentication != null && authentication.isAuthenticated());

                if (authentication != null && authentication.isAuthenticated()) {
                    log.info("Principal class: {}", authentication.getPrincipal().getClass().getName());

                    if (authentication.getPrincipal() instanceof Jwt jwt) {
                        String token = jwt.getTokenValue();
                        requestTemplate.header("Authorization", "Bearer " + token);
                        log.info("JWT token added to Feign request");
                    } else {
                        log.warn("Principal is not JWT, it's: {}", authentication.getPrincipal().getClass().getSimpleName());
                    }
                } else {
                    log.warn("No authentication found in SecurityContext");
                }
            } catch (Exception e) {
                log.error("Error in FeignConfig", e);
            }
        };
    }
}
