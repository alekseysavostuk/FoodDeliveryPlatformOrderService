package v1.foodDeliveryPlatform.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import v1.foodDeliveryPlatform.exception.ModelExistsException;
import v1.foodDeliveryPlatform.exception.RestaurantServiceUnavailableException;
import v1.foodDeliveryPlatform.feign.RestaurantServiceClient;
import v1.foodDeliveryPlatform.model.Item;
import v1.foodDeliveryPlatform.model.Order;
import v1.foodDeliveryPlatform.model.enums.OrderStatus;
import v1.foodDeliveryPlatform.repository.ItemRepository;
import v1.foodDeliveryPlatform.repository.OrderRepository;
import v1.foodDeliveryPlatform.security.SecurityUtils;
import v1.foodDeliveryPlatform.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final SecurityUtils securityUtils;
    private final RestaurantServiceClient restaurantServiceClient;

    @Override
    public Order getById(UUID id) {
        return orderRepository.findById(id).orElseThrow(() ->
                new ModelExistsException("Order not found"));
    }

    @Override
    @Transactional
    public Order createOrder(UUID restaurantId, List<Item> items) {
        try {
            boolean restaurantExists = restaurantServiceClient.existsRestaurant(restaurantId);
            for (Item item : items) {
                boolean dishExists = restaurantServiceClient.existsDish(restaurantId, item.getDish_id());
                if (!dishExists) {
                    throw new ModelExistsException("Dish not found with id: " + item.getDish_id());
                }
            }
            if (!restaurantExists) {
                throw new ModelExistsException("Restaurant not found with id: " + restaurantId);
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
    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    @Override
    public Order updateOrderStatus(UUID id) {
        Order currentOrder = getById(id);
        OrderStatus nextStatus = getNextStatus(currentOrder.getStatus());
        currentOrder.setStatus(nextStatus);
        return orderRepository.save(currentOrder);
    }

    @Override
    public void delete(UUID id) {
        orderRepository.deleteById(id);
    }

    @Override
    @Transactional
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
