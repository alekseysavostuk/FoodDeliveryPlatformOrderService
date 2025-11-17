package v1.foodDeliveryPlatform.feign.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import v1.foodDeliveryPlatform.exception.RestaurantServiceUnavailableException;
import v1.foodDeliveryPlatform.feign.RestaurantServiceClient;

import java.util.UUID;

@Component
@Slf4j
public class RestaurantServiceClientFallback implements RestaurantServiceClient {

    @Override
    public boolean existsRestaurant(UUID id) {
        log.error("Restaurant service is unavailable! Fallback activated for restaurantId: {}", id);
        throw new RestaurantServiceUnavailableException("Restaurant service is temporarily unavailable. Cannot validate restaurant: " + id);
    }

    @Override
    public boolean existsDish(UUID restaurantId, UUID dishId) {
        log.error("Restaurant service is unavailable! Fallback activated for dishId: {}", dishId);
        throw new RestaurantServiceUnavailableException("Restaurant service is temporarily unavailable. Cannot validate dish: " + dishId);
    }
}
