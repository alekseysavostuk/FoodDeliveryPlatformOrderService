package v1.foodDeliveryPlatform.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import v1.foodDeliveryPlatform.config.FeignConfig;
import v1.foodDeliveryPlatform.feign.impl.RestaurantServiceClientFallback;

import java.util.UUID;

@FeignClient(
        name = "restaurant-service",
        url = "${restaurant.service.url}",
        configuration = FeignConfig.class,
        fallback = RestaurantServiceClientFallback.class
)
public interface RestaurantServiceClient {

    @GetMapping("/restaurants/{id}/exists")
    boolean existsRestaurant(@PathVariable final UUID id);

    @GetMapping("/restaurants/{restaurantId}/dishes/{dishId}/exists")
    boolean existsDish(@PathVariable final UUID restaurantId,
                       @PathVariable final UUID dishId);
}
