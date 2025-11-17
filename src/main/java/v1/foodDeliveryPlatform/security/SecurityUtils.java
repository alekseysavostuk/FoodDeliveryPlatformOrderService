package v1.foodDeliveryPlatform.security;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import v1.foodDeliveryPlatform.repository.ItemRepository;
import v1.foodDeliveryPlatform.repository.OrderRepository;
import v1.foodDeliveryPlatform.repository.PaymentRepository;

import java.util.UUID;

@Component("expression")
@RequiredArgsConstructor
public class SecurityUtils {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ItemRepository itemRepository;

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return UUID.fromString(jwt.getClaim("id"));
        }
        throw new IllegalStateException("User not authenticated");
    }

    @Transactional
    public boolean isAccessOrder(UUID orderId, Authentication authentication) {
        UUID userId = getCurrentUserId();
        return orderRepository.existsByIdAndUserId(orderId, userId);
    }

    public boolean isAccessUser(UUID id, Authentication authentication) {
        if (id == null) {
            return false;
        }
        try {
            UUID userId = getCurrentUserId();
            return id.equals(userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public boolean isAccessPayment(UUID paymentId, Authentication authentication) {
        UUID userId = getCurrentUserId();
        return paymentRepository.existsByIdAndOrderUserId(paymentId, userId);
    }

    @Transactional
    public boolean isAccessItem(UUID itemId, Authentication authentication) {
        UUID userId = getCurrentUserId();
        return itemRepository.existsByIdAndOrderUserId(itemId, userId);
    }
}
