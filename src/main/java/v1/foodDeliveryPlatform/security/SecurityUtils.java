package v1.foodDeliveryPlatform.security;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SecurityUtils {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ItemRepository itemRepository;

    public UUID getCurrentUserId() {
        log.trace("Getting current user ID from security context");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            UUID userId = UUID.fromString(jwt.getClaim("id"));
            log.trace("Current user ID extracted: {}", userId);
            return userId;
        }
        log.warn("User not authenticated - no valid JWT token found");
        throw new IllegalStateException("User not authenticated");
    }

    @Transactional
    public boolean isAccessOrder(UUID orderId, Authentication authentication) {
        log.debug("Checking order access - OrderId: {}", orderId);
        UUID userId = getCurrentUserId();
        boolean hasAccess = orderRepository.existsByIdAndUserId(orderId, userId);

        if (hasAccess) {
            log.debug("Order access GRANTED - User: {}, Order: {}", userId, orderId);
        } else {
            log.warn("Order access DENIED - User: {}, Order: {}", userId, orderId);
        }

        return hasAccess;
    }

    public boolean isAccessUser(UUID id, Authentication authentication) {
        log.debug("Checking user access - Target UserId: {}", id);

        if (id == null) {
            log.warn("User access DENIED - target user ID is null");
            return false;
        }

        try {
            UUID userId = getCurrentUserId();
            boolean hasAccess = id.equals(userId);

            if (hasAccess) {
                log.debug("User access GRANTED - Current user: {}, Target user: {}", userId, id);
            } else {
                log.warn("User access DENIED - Current user: {}, Target user: {}", userId, id);
            }

            return hasAccess;
        } catch (Exception e) {
            log.warn("User access check failed due to exception: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean isAccessPayment(UUID paymentId, Authentication authentication) {
        log.debug("Checking payment access - PaymentId: {}", paymentId);
        UUID userId = getCurrentUserId();
        boolean hasAccess = paymentRepository.existsByIdAndOrderUserId(paymentId, userId);

        if (hasAccess) {
            log.debug("Payment access GRANTED - User: {}, Payment: {}", userId, paymentId);
        } else {
            log.warn("Payment access DENIED - User: {}, Payment: {}", userId, paymentId);
        }

        return hasAccess;
    }

    @Transactional
    public boolean isAccessItem(UUID itemId, Authentication authentication) {
        log.debug("Checking item access - ItemId: {}", itemId);
        UUID userId = getCurrentUserId();
        boolean hasAccess = itemRepository.existsByIdAndOrderUserId(itemId, userId);

        if (hasAccess) {
            log.debug("Item access GRANTED - User: {}, Item: {}", userId, itemId);
        } else {
            log.warn("Item access DENIED - User: {}, Item: {}", userId, itemId);
        }

        return hasAccess;
    }
}
