package v1.foodDeliveryPlatform.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import v1.foodDeliveryPlatform.dto.model.OrderDto;
import v1.foodDeliveryPlatform.model.Order;


@Mapper(componentModel = "spring",
        uses = {ItemMapper.class, PaymentMapper.class})
public interface OrderMapper extends BaseMapper<Order, OrderDto> {

    @Override
    @Mapping(target = "items", source = "items")
    @Mapping(target = "payment", source = "payment")
    OrderDto toDto(Order order);

    @Override
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "payment", ignore = true)
    Order toEntity(OrderDto orderDto);
}
