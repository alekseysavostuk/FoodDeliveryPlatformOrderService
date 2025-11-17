package v1.foodDeliveryPlatform.service;

import v1.foodDeliveryPlatform.model.Item;
import v1.foodDeliveryPlatform.model.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OrderService {

    Order getById(UUID id);

    Order createOrder(UUID restaurantId, List<Item> items);

    List<Order> getAll();

    Order updateOrderStatus(UUID id);

    void delete(UUID id);

    List<Order> getAllByUserId(UUID userId);

    BigDecimal calculateTotalPrice(List<Item> items);
}
