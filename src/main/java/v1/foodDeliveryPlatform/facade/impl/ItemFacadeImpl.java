package v1.foodDeliveryPlatform.facade.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import v1.foodDeliveryPlatform.dto.model.ItemDto;
import v1.foodDeliveryPlatform.facade.ItemFacade;
import v1.foodDeliveryPlatform.mapper.ItemMapper;
import v1.foodDeliveryPlatform.model.Item;
import v1.foodDeliveryPlatform.service.ItemService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class ItemFacadeImpl implements ItemFacade {

    private final ItemService itemService;
    private final ItemMapper mapper;

    @Override
    public ItemDto getById(UUID id) {
        return mapper.toDto(itemService.getById(id));
    }

    @Override
    public ItemDto createItem(ItemDto itemDto, UUID orderId) {
        return mapper.toDto(itemService.createItem(mapper.toEntity(itemDto), orderId));
    }

    @Override
    public ItemDto updateItem(ItemDto itemDto) {
        return mapper.toDto(itemService.updateItem(mapper.toEntity(itemDto)));
    }

    @Override
    public List<ItemDto> getAllByOrderId(UUID orderId) {
        List<Item> items = itemService.getAllByOrderId(orderId);
        return items.stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        itemService.delete(id);
    }
}
