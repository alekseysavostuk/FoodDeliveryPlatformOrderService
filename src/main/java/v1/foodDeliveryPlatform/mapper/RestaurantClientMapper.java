package v1.foodDeliveryPlatform.mapper;

import org.mapstruct.Mapper;
import v1.foodDeliveryPlatform.dto.model.feign.RestaurantClientDto;
import v1.foodDeliveryPlatform.model.feign.RestaurantClient;

@Mapper(componentModel = "spring")
public interface RestaurantClientMapper extends BaseMapper<RestaurantClient, RestaurantClientDto> {
}
