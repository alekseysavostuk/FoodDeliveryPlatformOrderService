package v1.foodDeliveryPlatform.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import v1.foodDeliveryPlatform.dto.model.feign.DishClientDto;
import v1.foodDeliveryPlatform.dto.model.feign.RestaurantClientDto;
import v1.foodDeliveryPlatform.feign.RestaurantServiceClient;
import v1.foodDeliveryPlatform.kafka.event.OrderCompletedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    @Mock
    private Counter successCounter;

    @Mock
    private Counter errorCounter;

    @Mock
    private Timer kafkaTimer;

    private MeterRegistry meterRegistry;
    private OrderEventProducer orderEventProducer;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        orderEventProducer = new OrderEventProducer(kafkaTemplate, meterRegistry, objectMapper, restaurantServiceClient);
    }

    @Test
    void sendOrderCompleted_Success_ShouldSendEventAndReturnResult() throws Exception {

        OrderCompletedEvent event = createOrderCompletedEvent();
        String serializedEvent = "serialized-event-json";
        SendResult<String, String> sendResult = createSendResult(event.getOrderId());
        CompletableFuture<SendResult<String, String>> kafkaFuture = CompletableFuture.completedFuture(sendResult);

        when(objectMapper.writeValueAsString(event)).thenReturn(serializedEvent);
        when(kafkaTemplate.send("order-completed", event.getOrderId(), serializedEvent)).thenReturn(kafkaFuture);

        RestaurantClientDto restaurantDto = new RestaurantClientDto("Test Restaurant");
        when(restaurantServiceClient.getRestaurantName(any(UUID.class))).thenReturn(restaurantDto);

        DishClientDto dishDto = new DishClientDto("Test Dish");
        when(restaurantServiceClient.getDishName(any(UUID.class))).thenReturn(dishDto);

        CompletableFuture<SendResult<String, String>> result = orderEventProducer.sendOrderCompleted(event);

        assertNotNull(result);
        assertTrue(result.isDone());
        assertEquals(sendResult, result.get());

        verify(objectMapper).writeValueAsString(event);
        verify(kafkaTemplate).send("order-completed", event.getOrderId(), serializedEvent);

        assertEquals("Test Restaurant", event.getRestaurantName());
        assertEquals("Test Dish", event.getItems().getFirst().getDishName());

        assertNotNull(meterRegistry.find("kafka.producer.success").counter());
        assertNotNull(meterRegistry.find("kafka.producer.duration").timer());
    }

    @Test
    void sendOrderCompleted_KafkaFailure_ShouldReturnFailedFuture() throws Exception {

        OrderCompletedEvent event = createOrderCompletedEvent();
        String serializedEvent = "serialized-event-json";
        Exception kafkaException = new RuntimeException("Kafka error");
        CompletableFuture<SendResult<String, String>> kafkaFuture = CompletableFuture.failedFuture(kafkaException);

        when(objectMapper.writeValueAsString(event)).thenReturn(serializedEvent);
        when(kafkaTemplate.send("order-completed", event.getOrderId(), serializedEvent)).thenReturn(kafkaFuture);

        RestaurantClientDto restaurantDto = new RestaurantClientDto("Test Restaurant");
        when(restaurantServiceClient.getRestaurantName(any(UUID.class))).thenReturn(restaurantDto);

        DishClientDto dishDto = new DishClientDto("Test Dish");
        when(restaurantServiceClient.getDishName(any(UUID.class))).thenReturn(dishDto);

        CompletableFuture<SendResult<String, String>> result = orderEventProducer.sendOrderCompleted(event);

        assertNotNull(result);
        assertTrue(result.isCompletedExceptionally());
        assertThrows(Exception.class, result::get);

        assertNotNull(meterRegistry.find("kafka.producer.errors").counter());
        assertNotNull(meterRegistry.find("kafka.producer.failures").counter());
    }

    @Test
    void sendOrderCompleted_SerializationFailure_ShouldReturnFailedFuture() throws Exception {

        OrderCompletedEvent event = createOrderCompletedEvent();
        JsonProcessingException serializationException = new JsonProcessingException("Serialization failed") {
        };

        when(objectMapper.writeValueAsString(event)).thenThrow(serializationException);

        CompletableFuture<SendResult<String, String>> result = orderEventProducer.sendOrderCompleted(event);

        assertNotNull(result);
        assertTrue(result.isCompletedExceptionally());
        assertThrows(Exception.class, result::get);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        assertNotNull(meterRegistry.find("kafka.producer.errors").counter());
    }

    @Test
    void sendOrderCompleted_RestaurantServiceFailure_ShouldUseFallbackName() throws Exception {

        OrderCompletedEvent event = createOrderCompletedEvent();
        String serializedEvent = "serialized-event-json";
        SendResult<String, String> sendResult = createSendResult(event.getOrderId());
        CompletableFuture<SendResult<String, String>> kafkaFuture = CompletableFuture.completedFuture(sendResult);

        when(objectMapper.writeValueAsString(event)).thenReturn(serializedEvent);
        when(kafkaTemplate.send("order-completed", event.getOrderId(), serializedEvent)).thenReturn(kafkaFuture);

        when(restaurantServiceClient.getRestaurantName(any(UUID.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        DishClientDto dishDto = new DishClientDto("Test Dish");
        when(restaurantServiceClient.getDishName(any(UUID.class))).thenReturn(dishDto);

        CompletableFuture<SendResult<String, String>> result = orderEventProducer.sendOrderCompleted(event);

        assertNotNull(result);
        assertTrue(result.isDone());

        assertEquals("Unknown Restaurant", event.getRestaurantName());
        assertEquals("Test Dish", event.getItems().getFirst().getDishName());

        assertNotNull(meterRegistry.find("kafka.producer.success").counter());
    }

    @Test
    void sendOrderCompleted_DishServiceFailure_ShouldUseFallbackForFailedDishes() throws Exception {

        OrderCompletedEvent event = createOrderCompletedEventWithMultipleItems();
        String serializedEvent = "serialized-event-json";
        SendResult<String, String> sendResult = createSendResult(event.getOrderId());
        CompletableFuture<SendResult<String, String>> kafkaFuture = CompletableFuture.completedFuture(sendResult);

        when(objectMapper.writeValueAsString(event)).thenReturn(serializedEvent);
        when(kafkaTemplate.send("order-completed", event.getOrderId(), serializedEvent)).thenReturn(kafkaFuture);

        RestaurantClientDto restaurantDto = new RestaurantClientDto("Test Restaurant");
        when(restaurantServiceClient.getRestaurantName(any(UUID.class))).thenReturn(restaurantDto);

        DishClientDto successfulDishDto = new DishClientDto("Successful Dish");
        when(restaurantServiceClient.getDishName(UUID.fromString("123e4567-e89b-12d3-a456-426614174000")))
                .thenReturn(successfulDishDto);
        when(restaurantServiceClient.getDishName(UUID.fromString("123e4567-e89b-12d3-a456-426614174001")))
                .thenThrow(new RuntimeException("Dish service error"));

        CompletableFuture<SendResult<String, String>> result = orderEventProducer.sendOrderCompleted(event);

        assertNotNull(result);
        assertTrue(result.isDone());

        assertEquals("Test Restaurant", event.getRestaurantName());
        assertEquals("Successful Dish", event.getItems().get(0).getDishName());
        assertEquals("Unknown Dish", event.getItems().get(1).getDishName());

        assertNotNull(meterRegistry.find("kafka.producer.success").counter());
    }

    @Test
    void sendOrderCompleted_EventIdGenerated_ShouldSetRandomEventId() throws Exception {

        OrderCompletedEvent event = createOrderCompletedEvent();
        event.setEventId(null);
        String serializedEvent = "serialized-event-json";
        SendResult<String, String> sendResult = createSendResult(event.getOrderId());
        CompletableFuture<SendResult<String, String>> kafkaFuture = CompletableFuture.completedFuture(sendResult);

        when(objectMapper.writeValueAsString(any(OrderCompletedEvent.class))).thenReturn(serializedEvent);
        when(kafkaTemplate.send("order-completed", event.getOrderId(), serializedEvent)).thenReturn(kafkaFuture);

        when(restaurantServiceClient.getRestaurantName(any(UUID.class))).thenReturn(new RestaurantClientDto("Test Restaurant"));
        when(restaurantServiceClient.getDishName(any(UUID.class))).thenReturn(new DishClientDto("Test Dish"));

        orderEventProducer.sendOrderCompleted(event);

        assertNotNull(event.getEventId());

        assertDoesNotThrow(() -> UUID.fromString(event.getEventId()));
    }

    @Test
    void sendOrderCompletedWithCallback_Success_ShouldCompleteWithoutException() throws JsonProcessingException {

        OrderCompletedEvent event = createOrderCompletedEvent();
        String serializedEvent = "serialized-event-json";
        SendResult<String, String> sendResult = createSendResult(event.getOrderId());
        CompletableFuture<SendResult<String, String>> kafkaFuture = CompletableFuture.completedFuture(sendResult);

        when(objectMapper.writeValueAsString(event)).thenReturn(serializedEvent);
        when(kafkaTemplate.send("order-completed", event.getOrderId(), serializedEvent)).thenReturn(kafkaFuture);

        when(restaurantServiceClient.getRestaurantName(any(UUID.class))).thenReturn(new RestaurantClientDto("Test Restaurant"));
        when(restaurantServiceClient.getDishName(any(UUID.class))).thenReturn(new DishClientDto("Test Dish"));

        assertDoesNotThrow(() -> orderEventProducer.sendOrderCompletedWithCallback(event));

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void sendOrderCompletedWithCallback_Failure_ShouldHandleException() throws JsonProcessingException {

        OrderCompletedEvent event = createOrderCompletedEvent();
        Exception kafkaException = new RuntimeException("Kafka error");
        CompletableFuture<SendResult<String, String>> kafkaFuture = CompletableFuture.failedFuture(kafkaException);

        when(objectMapper.writeValueAsString(event)).thenReturn("serialized-event");
        when(kafkaTemplate.send("order-completed", event.getOrderId(), "serialized-event")).thenReturn(kafkaFuture);

        when(restaurantServiceClient.getRestaurantName(any(UUID.class))).thenReturn(new RestaurantClientDto("Test Restaurant"));
        when(restaurantServiceClient.getDishName(any(UUID.class))).thenReturn(new DishClientDto("Test Dish"));

        assertDoesNotThrow(() -> orderEventProducer.sendOrderCompletedWithCallback(event));

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private OrderCompletedEvent createOrderCompletedEvent() {
        OrderCompletedEvent.OrderItem orderItem = new OrderCompletedEvent.OrderItem();
        orderItem.setDishId("123e4567-e89b-12d3-a456-426614174000");
        orderItem.setQuantity(2);
        orderItem.setPrice(new BigDecimal("12.75"));

        return OrderCompletedEvent.builder()
                .orderId("order-123")
                .userId("user-456")
                .restaurantId("123e4567-e89b-12d3-a456-426614174002")
                .totalAmount(new BigDecimal("25.50"))
                .completedAt(Instant.now())
                .items(List.of(orderItem))
                .build();
    }

    private OrderCompletedEvent createOrderCompletedEventWithMultipleItems() {
        OrderCompletedEvent.OrderItem orderItem1 = new OrderCompletedEvent.OrderItem();
        orderItem1.setDishId("123e4567-e89b-12d3-a456-426614174000");
        orderItem1.setQuantity(1);
        orderItem1.setPrice(new BigDecimal("25.50"));

        OrderCompletedEvent.OrderItem orderItem2 = new OrderCompletedEvent.OrderItem();
        orderItem2.setDishId("123e4567-e89b-12d3-a456-426614174001");
        orderItem2.setQuantity(1);
        orderItem2.setPrice(new BigDecimal("24.75"));

        return OrderCompletedEvent.builder()
                .orderId("order-123")
                .userId("user-456")
                .restaurantId("123e4567-e89b-12d3-a456-426614174002")
                .totalAmount(new BigDecimal("50.25"))
                .completedAt(Instant.now())
                .items(List.of(orderItem1, orderItem2))
                .build();
    }

    private SendResult<String, String> createSendResult(String key) {
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition("order-completed", 0),
                0L,
                0,
                0L,
                0,
                0
        );
        return new SendResult<>(null, recordMetadata);
    }
}