package v1.foodDeliveryPlatform.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import v1.foodDeliveryPlatform.security.SecurityUtils;

import static org.mockito.Mockito.mock;
import static org.springframework.security.config.Customizer.withDefaults;

@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class ControllerTestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/{id}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/orders/{id}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/user/{userId}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/orders/{id}/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/{id}/items").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/orders/{id}/items").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/{id}/payment").authenticated()
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(withDefaults());
        return http.build();
    }

    @Bean(name = "expression")
    @Primary
    public SecurityUtils customSecurityExpression() {
        return mock(SecurityUtils.class);
    }
}