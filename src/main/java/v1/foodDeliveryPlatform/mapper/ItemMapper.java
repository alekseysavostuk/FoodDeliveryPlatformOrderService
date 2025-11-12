package v1.foodDeliveryPlatform.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import v1.foodDeliveryPlatform.dto.model.ItemDto;
import v1.foodDeliveryPlatform.model.Item;

@Mapper(componentModel = "spring", uses = {OrderMapper.class})
public interface ItemMapper extends BaseMapper<Item, ItemDto> {

    @Override
    @Mapping(target = "order", ignore = true)
    Item toEntity(ItemDto itemDto);

}

