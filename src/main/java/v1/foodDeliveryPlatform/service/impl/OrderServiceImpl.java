package v1.foodDeliveryPlatform.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import v1.foodDeliveryPlatform.exception.ResourceNotFoundException;
import v1.foodDeliveryPlatform.exception.RestaurantServiceUnavailableException;
import v1.foodDeliveryPlatform.feign.RestaurantServiceClient;
import v1.foodDeliveryPlatform.kafka.OrderEventProducer;
import v1.foodDeliveryPlatform.kafka.event.OrderCompletedEvent;
import v1.foodDeliveryPlatform.model.Item;
import v1.foodDeliveryPlatform.model.Order;
import v1.foodDeliveryPlatform.model.enums.OrderStatus;
import v1.foodDeliveryPlatform.repository.ItemRepository;
import v1.foodDeliveryPlatform.repository.OrderRepository;
import v1.foodDeliveryPlatform.security.SecurityUtils;
import v1.foodDeliveryPlatform.service.OrderService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final SecurityUtils securityUtils;
    private final RestaurantServiceClient restaurantServiceClient;
    private final OrderEventProducer orderEventProducer;

    @Override
    @Transactional
    @Cacheable(value = "orders", key = "#id")
    public Order getById(UUID id) {
        return orderRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Order not found"));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "user_orders", allEntries = true),
            @CacheEvict(value = "all_orders", allEntries = true)
    })
    public Order createOrder(UUID restaurantId, List<Item> items) {
        try {
            boolean restaurantExists = restaurantServiceClient.existsRestaurant(restaurantId);
            for (Item item : items) {
                boolean dishExists = restaurantServiceClient.existsDish(restaurantId, item.getDish_id());
                if (!dishExists) {
                    throw new ResourceNotFoundException("Dish not found with id: " + item.getDish_id());
                }
            }
            if (!restaurantExists) {
                throw new ResourceNotFoundException("Restaurant not found with id: " + restaurantId);
            }

            Order order = Order.builder()
                    .status(OrderStatus.NEW)
                    .orderDate(LocalDateTime.now())
                    .userId(securityUtils.getCurrentUserId())
                    .restaurantId(restaurantId)
                    .totalPrice(calculateTotalPrice(items))
                    .items(new ArrayList<>())
                    .build();

            Order savedOrder = orderRepository.save(order);

            for (Item item : items) {
                item.setOrder(savedOrder);
                order.getItems().add(item);
                itemRepository.save(item);
            }
            return savedOrder;
        } catch (RestaurantServiceUnavailableException e) {
            log.error("Failed to create order due to restaurant service unavailability: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    @Cacheable(value = "all_orders")
    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", key = "#id"),
            @CacheEvict(value = "user_orders", key = "#result.userId"),
            @CacheEvict(value = "all_orders", allEntries = true)
    })
    public Order updateOrderStatus(UUID id) {
        Order currentOrder = getById(id);
        if (!currentOrder.getStatus().equals(OrderStatus.DONE)) {
            OrderStatus nextStatus = getNextStatus(currentOrder.getStatus());
            currentOrder.setStatus(nextStatus);
        }
        orderRepository.save(currentOrder);

        log.info("Order {} status updated to: {}", id, currentOrder.getStatus());

        if (currentOrder.getStatus().equals(OrderStatus.DONE)) {
            log.info("Order {} completed. Preparing Kafka event...", id);

            List<OrderCompletedEvent.OrderItem> eventItems = currentOrder.getItems().stream()
                    .map(item -> {
                        OrderCompletedEvent.OrderItem eventItem = new OrderCompletedEvent.OrderItem();
                        eventItem.setDishId(item.getDish_id().toString());
                        eventItem.setQuantity(item.getQuantity());
                        eventItem.setPrice(item.getPrice());
                        return eventItem;
                    })
                    .collect(Collectors.toList());

            OrderCompletedEvent event = new OrderCompletedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setOrderId(currentOrder.getId().toString());
            event.setUserId(currentOrder.getUserId().toString());
            event.setRestaurantId(currentOrder.getRestaurantId().toString());
            event.setTotalAmount(currentOrder.getTotalPrice());
            event.setCompletedAt(Instant.now());
            event.setItems(eventItems);

            log.info("Sending Kafka event for completed order: {}", id);
            orderEventProducer.sendOrderCompleted(event);
            log.info("Kafka event sent successfully for order: {}", id);
        }
        return currentOrder;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", key = "#id"),
            @CacheEvict(value = "user_orders", allEntries = true),
            @CacheEvict(value = "all_orders", allEntries = true)
    })
    public void delete(UUID id) {
        orderRepository.deleteById(id);
    }

    @Override
    @Transactional
    @Cacheable(value = "user_orders", key = "#userId")
    public List<Order> getAllByUserId(UUID userId) {
        return orderRepository.findAllByUserId(userId);
    }

    private OrderStatus getNextStatus(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case NEW -> OrderStatus.IN_PROGRESS;
            case IN_PROGRESS -> OrderStatus.DONE;
            case DONE -> throw new IllegalStateException("Order is already completed");
        };
    }

    @Override
    public BigDecimal calculateTotalPrice(List<Item> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
