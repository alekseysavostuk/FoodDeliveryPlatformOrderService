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
        log.debug("Fetching order by ID: {}", id);
        Order order = orderRepository.findById(id).orElseThrow(() -> {
            log.warn("Order not found with ID: {}", id);
            return new ResourceNotFoundException("Order not found");
        });
        log.debug("Successfully fetched order: {} (Status: {}, User: {})",
                id, order.getStatus(), order.getUserId());
        return order;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "user_orders", allEntries = true),
            @CacheEvict(value = "all_orders", allEntries = true)
    })
    public Order createOrder(UUID restaurantId, List<Item> items) {
        log.info("Creating new order for restaurant: {} with {} items", restaurantId, items.size());

        try {
            log.debug("Validating restaurant existence: {}", restaurantId);
            boolean restaurantExists = restaurantServiceClient.existsRestaurant(restaurantId);

            log.debug("Validating {} dishes in order", items.size());
            for (Item item : items) {
                boolean dishExists = restaurantServiceClient.existsDish(restaurantId, item.getDish_id());
                if (!dishExists) {
                    log.warn("Dish not found - DishId: {}, RestaurantId: {}", item.getDish_id(), restaurantId);
                    throw new ResourceNotFoundException("Dish not found with id: " + item.getDish_id());
                }
            }

            if (!restaurantExists) {
                log.warn("Restaurant not found: {}", restaurantId);
                throw new ResourceNotFoundException("Restaurant not found with id: " + restaurantId);
            }

            UUID currentUserId = securityUtils.getCurrentUserId();
            BigDecimal totalPrice = calculateTotalPrice(items);

            log.debug("Building order - User: {}, Total: {}, Items: {}", currentUserId, totalPrice, items.size());

            Order order = Order.builder()
                    .status(OrderStatus.NEW)
                    .orderDate(LocalDateTime.now())
                    .userId(currentUserId)
                    .restaurantId(restaurantId)
                    .totalPrice(totalPrice)
                    .items(new ArrayList<>())
                    .build();

            Order savedOrder = orderRepository.save(order);
            log.debug("Order saved with ID: {}", savedOrder.getId());

            for (Item item : items) {
                item.setOrder(savedOrder);
                order.getItems().add(item);
                itemRepository.save(item);
            }

            log.info("Order created successfully - OrderId: {}, User: {}, Restaurant: {}, Total: {}",
                    savedOrder.getId(), currentUserId, restaurantId, totalPrice);
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
        log.debug("Fetching all orders");
        List<Order> orders = orderRepository.findAll();
        log.debug("Found {} total orders", orders.size());
        return orders;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "orders", key = "#id"),
            @CacheEvict(value = "user_orders", key = "#result.userId"),
            @CacheEvict(value = "all_orders", allEntries = true)
    })
    public Order updateOrderStatus(UUID id) {
        log.info("Updating order status for order: {}", id);

        Order currentOrder = getById(id);
        OrderStatus oldStatus = currentOrder.getStatus();

        if (!currentOrder.getStatus().equals(OrderStatus.DONE)) {
            OrderStatus nextStatus = getNextStatus(currentOrder.getStatus());
            currentOrder.setStatus(nextStatus);
            log.debug("Order status changed: {} -> {}", oldStatus, nextStatus);
        } else {
            log.debug("Order already completed, status remains: {}", OrderStatus.DONE);
            return currentOrder;
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
        log.info("Deleting order: {}", id);
        try {
            orderRepository.deleteById(id);
            log.info("Order deleted successfully: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete order: {}", id, e);
            throw e;
        }
    }

    @Override
    @Transactional
    @Cacheable(value = "user_orders", key = "#userId")
    public List<Order> getAllByUserId(UUID userId) {
        log.debug("Fetching all orders for user: {}", userId);
        List<Order> orders = orderRepository.findAllByUserId(userId);
        log.debug("Found {} orders for user: {}", orders.size(), userId);
        return orders;
    }

    private OrderStatus getNextStatus(OrderStatus currentStatus) {
        log.trace("Getting next status for: {}", currentStatus);
        OrderStatus nextStatus = switch (currentStatus) {
            case NEW -> OrderStatus.IN_PROGRESS;
            case IN_PROGRESS -> OrderStatus.DONE;
            case DONE -> throw new IllegalStateException("Order is already completed");
        };
        log.trace("Next status: {} -> {}", currentStatus, nextStatus);
        return nextStatus;
    }

    @Override
    public BigDecimal calculateTotalPrice(List<Item> items) {
        log.trace("Calculating total price for {} items", items.size());
        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.trace("Total price calculated: {}", total);
        return total;
    }
}
