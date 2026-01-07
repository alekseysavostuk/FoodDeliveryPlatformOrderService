package v1.foodDeliveryPlatform.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import v1.foodDeliveryPlatform.model.Order;
import v1.foodDeliveryPlatform.repository.OrderRepository;
import v1.foodDeliveryPlatform.service.OrderCleanupService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCleanupServiceImpl implements OrderCleanupService {

    private final OrderRepository orderRepository;

    @Override
    public void cleanupUnpaidOrders() {
        LocalDateTime temp = LocalDateTime.now().minusMinutes(5);
        List<Order> unpaidOrders = orderRepository
                .findByPaymentStatusFalseAndCreatedAtBefore(temp);

        if (!unpaidOrders.isEmpty()) {
            orderRepository.deleteAll(unpaidOrders);
            log.info("Auto-cleaned {} unpaid orders older than 5 min",
                    unpaidOrders.size());
        }
    }

    @Override
    @Scheduled(cron = "0 */5 * * * ?")
    public void autoCleanup() {
        log.debug("Starting automatic cleanup of unpaid orders");
        cleanupUnpaidOrders();
    }
}
