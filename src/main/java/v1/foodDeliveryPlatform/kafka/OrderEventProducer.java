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
        log.info("Sending order completed event - Order: {}", event.getOrderId());

        event.setEventId(UUID.randomUUID().toString());

        Counter successCounter = meterRegistry.counter("kafka.producer.success", "topic", "order-completed");
        Counter errorCounter = meterRegistry.counter("kafka.producer.errors", "topic", "order-completed");
        Timer kafkaTimer = meterRegistry.timer("kafka.producer.duration", "topic", "order-completed");

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            enrichEventWithNames(event);

            String message = objectMapper.writeValueAsString(event);
            log.debug("Event serialized - Order: {}, EventId: {}", event.getOrderId(), event.getEventId());

            log.debug("Sending to Kafka - Topic: order-completed, Key: {}", event.getOrderId());

            return kafkaTemplate.send("order-completed", event.getOrderId(), message)
                    .whenComplete((result, throwable) -> {
                        sample.stop(kafkaTimer);

                        if (throwable != null) {
                            log.error("Failed to send order event - Order: {}, Error: {}",
                                    event.getOrderId(), throwable.getMessage());
                            errorCounter.increment();
                            meterRegistry.counter("kafka.producer.failures",
                                            "topic", "order-completed",
                                            "error", throwable.getClass().getSimpleName())
                                    .increment();
                        } else {
                            log.info("Order event sent successfully - Order: {}, Partition: {}, Offset: {}",
                                    event.getOrderId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                            successCounter.increment();
                        }
                    });
        } catch (Exception e) {
            log.error("Error processing order event - Order: {}, Error: {}",
                    event.getOrderId(), e.getMessage());
            errorCounter.increment();
            return CompletableFuture.failedFuture(e);
        }
    }

    private void enrichEventWithNames(OrderCompletedEvent event) {
        log.debug("Enriching event with names - Order: {}", event.getOrderId());

        try {
            RestaurantClientDto restaurantDto = restaurantServiceClient.getRestaurantName(UUID.fromString(event.getRestaurantId()));
            event.setRestaurantName(restaurantDto.getRestaurantName());
            log.debug("Restaurant name retrieved - ID: {}, Name: {}",
                    event.getRestaurantId(), event.getRestaurantName());
        } catch (Exception e) {
            log.warn("Failed to get restaurant name - ID: {}, using fallback", event.getRestaurantId());
            event.setRestaurantName("Unknown Restaurant");
        }

        int successfulDishNames = 0;
        for (OrderCompletedEvent.OrderItem item : event.getItems()) {
            try {
                DishClientDto dishDto = restaurantServiceClient.getDishName(UUID.fromString(item.getDishId()));
                item.setDishName(dishDto.getDishName());
                successfulDishNames++;
                log.trace("Dish name retrieved - ID: {}, Name: {}", item.getDishId(), item.getDishName());
            } catch (Exception e) {
                log.warn("Failed to get dish name - ID: {}, using fallback", item.getDishId());
                item.setDishName("Unknown Dish");
            }
        }

        log.debug("Event enriched - Order: {}, Restaurant: {}, Successful dish names: {}/{}",
                event.getOrderId(), event.getRestaurantName(), successfulDishNames, event.getItems().size());
    }

    public void sendOrderCompletedWithCallback(OrderCompletedEvent event) {
        log.debug("Sending order with callback - Order: {}", event.getOrderId());
        sendOrderCompleted(event)
                .thenAccept(result -> {
                    log.debug("Order event callback success - Order: {}", event.getOrderId());
                })
                .exceptionally(throwable -> {
                    log.error("Order event callback failed - Order: {}", event.getOrderId(), throwable);
                    return null;
                });
    }
}
