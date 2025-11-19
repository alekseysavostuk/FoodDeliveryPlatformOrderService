package v1.foodDeliveryPlatform.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent {
    private String eventId;
    private String orderId;
    private String userId;
    private String restaurantId;
    private String restaurantName;
    private BigDecimal totalAmount;
    private Instant completedAt;
    private List<OrderItem> items;

    @Data
    public static class OrderItem {
        private String dishId;
        private String dishName;
        private Integer quantity;
        private BigDecimal price;
    }
}
