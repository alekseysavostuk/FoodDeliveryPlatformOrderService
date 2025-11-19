package v1.foodDeliveryPlatform.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import v1.foodDeliveryPlatform.dto.model.feign.DishClientDto;
import v1.foodDeliveryPlatform.dto.model.feign.RestaurantClientDto;
import v1.foodDeliveryPlatform.feign.RestaurantServiceClient;
import v1.foodDeliveryPlatform.kafka.event.OrderCompletedEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final RestaurantServiceClient restaurantServiceClient;

    public CompletableFuture<SendResult<String, String>> sendOrderCompleted(OrderCompletedEvent event) {
        log.info("=== KAFKA PRODUCER STARTING ===");
        log.info("Preparing to send order completed event for order: {}", event.getOrderId());

        event.setEventId(UUID.randomUUID().toString());

        Counter successCounter = meterRegistry.counter("kafka.producer.success", "topic", "order-completed");
        Counter errorCounter = meterRegistry.counter("kafka.producer.errors", "topic", "order-completed");
        Timer kafkaTimer = meterRegistry.timer("kafka.producer.duration", "topic", "order-completed");

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            enrichEventWithNames(event);

            String message = objectMapper.writeValueAsString(event);
            log.info("Serialized event message: {}", message);

            log.info("Sending to Kafka topic: order-completed, key: {}", event.getOrderId());

            return kafkaTemplate.send("order-completed", event.getOrderId(), message)
                    .whenComplete((result, throwable) -> {
                        sample.stop(kafkaTimer);

                        if (throwable != null) {
                            log.error("=== KAFKA PRODUCER FAILED ===");
                            log.error("Failed to send order completed event for order: {}", event.getOrderId(), throwable);
                            errorCounter.increment();
                            meterRegistry.counter("kafka.producer.failures",
                                            "topic", "order-completed",
                                            "error", throwable.getClass().getSimpleName())
                                    .increment();
                        } else {
                            log.info("=== KAFKA PRODUCER SUCCESS ===");
                            log.info("Order completed event sent successfully!");
                            log.info("Order ID: {}", event.getOrderId());
                            log.info("Event ID: {}", event.getEventId());
                            log.info("Restaurant: {}", event.getRestaurantName());
                            log.info("Partition: {}, Offset: {}",
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                            log.info("Topic: {}", result.getRecordMetadata().topic());
                            successCounter.increment();
                        }
                    });
        } catch (Exception e) {
            log.error("=== KAFKA PRODUCER ERROR ===");
            log.error("Error processing order event for order: {}", event.getOrderId(), e);
            errorCounter.increment();
            return CompletableFuture.failedFuture(e);
        }
    }

    private void enrichEventWithNames(OrderCompletedEvent event) {
        log.info("Enriching event with restaurant and dish names for order: {}", event.getOrderId());

        try {
            RestaurantClientDto restaurantDto = restaurantServiceClient.getRestaurantName(UUID.fromString(event.getRestaurantId()));
            event.setRestaurantName(restaurantDto.getRestaurantName());
            log.info("Retrieved restaurant name: {} for ID: {}", event.getRestaurantName(), event.getRestaurantId());
        } catch (Exception e) {
            log.warn("Failed to get restaurant name for ID: {}, using fallback", event.getRestaurantId(), e);
            event.setRestaurantName("Unknown Restaurant");
        }

        for (OrderCompletedEvent.OrderItem item : event.getItems()) {
            try {
                DishClientDto dishDto = restaurantServiceClient.getDishName(UUID.fromString(item.getDishId()));
                item.setDishName(dishDto.getDishName());
                log.info("Retrieved dish name: {} for ID: {}", item.getDishName(), item.getDishId());
            } catch (Exception e) {
                log.warn("Failed to get dish name for ID: {}, using fallback", item.getDishId(), e);
                item.setDishName("Unknown Dish");
            }
        }

        log.info("Event enriched successfully for order: {}", event.getOrderId());
        log.info("Restaurant: {}, Dishes count: {}", event.getRestaurantName(), event.getItems().size());
    }

    public void sendOrderCompletedWithCallback(OrderCompletedEvent event) {
        log.info("Starting send for order: {}", event.getOrderId());
        sendOrderCompleted(event)
                .thenAccept(result -> {
                    log.info("=== KAFKA PRODUCER CALLBACK SUCCESS ===");
                    log.info("Order {} completed event acknowledged", event.getOrderId());
                })
                .exceptionally(throwable -> {
                    log.error("=== KAFKA PRODUCER CALLBACK FAILED ===");
                    log.error("Failed to send order completed event with callback for order: {}",
                            event.getOrderId(), throwable);
                    return null;
                });
    }
}
