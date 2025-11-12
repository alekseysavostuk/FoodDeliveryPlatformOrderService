package v1.foodDeliveryPlatform.facade;

import v1.foodDeliveryPlatform.dto.model.ItemDto;
import v1.foodDeliveryPlatform.dto.model.OrderDto;

import java.util.List;
import java.util.UUID;

public interface OrderFacade {

    OrderDto getById(UUID id);

    OrderDto createOrder(UUID userId, UUID restaurantId, List<ItemDto> items);

    List<OrderDto> getAll();

    OrderDto updateOrderStatus(UUID id);

    void delete(UUID id);

    List<OrderDto> getAllByUserId(UUID userId);
}
