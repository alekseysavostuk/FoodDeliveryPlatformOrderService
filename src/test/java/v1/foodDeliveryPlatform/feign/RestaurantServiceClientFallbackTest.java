package v1.foodDeliveryPlatform.feign;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import v1.foodDeliveryPlatform.exception.RestaurantServiceUnavailableException;
import v1.foodDeliveryPlatform.feign.impl.RestaurantServiceClientFallback;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceClientFallbackTest {


    @InjectMocks
    private RestaurantServiceClientFallback restaurantServiceClientFallback;

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID dishId = UUID.randomUUID();

    @Test
    void existsRestaurant_WhenServiceUnavailable_ThrowsException() {

        RestaurantServiceUnavailableException exception = assertThrows(
                RestaurantServiceUnavailableException.class,
                () -> restaurantServiceClientFallback.existsRestaurant(restaurantId)
        );

        assertEquals("Restaurant service is temporarily unavailable. Cannot validate restaurant: " + restaurantId,
                exception.getMessage());
    }

    @Test
    void existsDish_WhenServiceUnavailable_ThrowsException() {

        RestaurantServiceUnavailableException exception = assertThrows(
                RestaurantServiceUnavailableException.class,
                () -> restaurantServiceClientFallback.existsDish(restaurantId, dishId)
        );

        assertEquals("Restaurant service is temporarily unavailable. Cannot validate dish: " + dishId,
                exception.getMessage());
    }

    @Test
    void getRestaurantName_WhenServiceUnavailable_ThrowsException() {

        RestaurantServiceUnavailableException exception = assertThrows(
                RestaurantServiceUnavailableException.class,
                () -> restaurantServiceClientFallback.getRestaurantName(restaurantId)
        );

        assertEquals("Restaurant service is temporarily unavailable. Cannot validate restaurant: " + restaurantId,
                exception.getMessage());
    }

    @Test
    void getDishName_WhenServiceUnavailable_ThrowsException() {

        RestaurantServiceUnavailableException exception = assertThrows(
                RestaurantServiceUnavailableException.class,
                () -> restaurantServiceClientFallback.getDishName(dishId)
        );

        assertEquals("Restaurant service is temporarily unavailable. Cannot validate dish: " + dishId,
                exception.getMessage());
    }

    @Test
    void existsRestaurant_WithDifferentUUIDs_ThrowsExceptionWithCorrectId() {

        UUID testRestaurantId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        RestaurantServiceUnavailableException exception = assertThrows(
                RestaurantServiceUnavailableException.class,
                () -> restaurantServiceClientFallback.existsRestaurant(testRestaurantId)
        );

        assertTrue(exception.getMessage().contains(testRestaurantId.toString()));
    }

    @Test
    void existsDish_WithDifferentUUIDs_ThrowsExceptionWithCorrectIds() {

        UUID testRestaurantId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID testDishId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

        RestaurantServiceUnavailableException exception = assertThrows(
                RestaurantServiceUnavailableException.class,
                () -> restaurantServiceClientFallback.existsDish(testRestaurantId, testDishId)
        );

        assertTrue(exception.getMessage().contains(testDishId.toString()));
    }

    @Test
    void allMethods_ThrowSameExceptionType() {

        assertThrows(RestaurantServiceUnavailableException.class,
                () -> restaurantServiceClientFallback.existsRestaurant(restaurantId));

        assertThrows(RestaurantServiceUnavailableException.class,
                () -> restaurantServiceClientFallback.existsDish(restaurantId, dishId));

        assertThrows(RestaurantServiceUnavailableException.class,
                () -> restaurantServiceClientFallback.getRestaurantName(restaurantId));

        assertThrows(RestaurantServiceUnavailableException.class,
                () -> restaurantServiceClientFallback.getDishName(dishId));
    }
}
