package v1.foodDeliveryPlatform.mapper;

import org.mapstruct.Mapper;
import v1.foodDeliveryPlatform.dto.model.feign.DishClientDto;
import v1.foodDeliveryPlatform.model.feign.DishClient;

@Mapper(componentModel = "spring")
public interface DishClientMapper extends BaseMapper<DishClient, DishClientDto> {
}
