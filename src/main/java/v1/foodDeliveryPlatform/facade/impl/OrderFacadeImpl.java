package v1.foodDeliveryPlatform.facade.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import v1.foodDeliveryPlatform.dto.model.ItemDto;
import v1.foodDeliveryPlatform.dto.model.OrderDto;
import v1.foodDeliveryPlatform.facade.OrderFacade;
import v1.foodDeliveryPlatform.mapper.ItemMapper;
import v1.foodDeliveryPlatform.mapper.OrderMapper;
import v1.foodDeliveryPlatform.model.Item;
import v1.foodDeliveryPlatform.model.Order;
import v1.foodDeliveryPlatform.service.OrderService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
@AllArgsConstructor
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final ItemMapper itemMapper;

    @Override
    public OrderDto getById(UUID id) {
        return orderMapper.toDto(orderService.getById(id));
    }

    @Override
    public OrderDto createOrder(UUID restaurantId, List<ItemDto> itemsDto) {
        List<Item> items = itemsDto.stream()
                .map(itemMapper::toEntity)
                .collect(Collectors.toList());
        return orderMapper.toDto(orderService.createOrder(restaurantId, items));
    }

    @Override
    public List<OrderDto> getAll() {
        List<Order> orders = orderService.getAll();
        return orders.stream().map(orderMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public OrderDto updateOrderStatus(UUID id) {
        return orderMapper.toDto(orderService.updateOrderStatus(id));
    }

    @Override
    public void delete(UUID id) {
        orderService.delete(id);
    }

    @Override
    public List<OrderDto> getAllByUserId(UUID userId) {
        List<Order> orders = orderService.getAllByUserId(userId);
        return orders.stream().map(orderMapper::toDto).collect(Collectors.toList());
    }
}
