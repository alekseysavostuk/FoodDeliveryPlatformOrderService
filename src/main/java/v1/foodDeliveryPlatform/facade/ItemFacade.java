package v1.foodDeliveryPlatform.facade;

import v1.foodDeliveryPlatform.dto.model.ItemDto;

import java.util.List;
import java.util.UUID;

public interface ItemFacade {

    ItemDto getById(UUID id);

    ItemDto createItem(ItemDto itemDto, UUID orderId);

    ItemDto updateItem(ItemDto itemDto);

    List<ItemDto> getAllByOrderId(UUID orderId);

    void delete(UUID id);
}
