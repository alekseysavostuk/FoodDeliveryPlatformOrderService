package v1.foodDeliveryPlatform.exception;

public class RestaurantServiceUnavailableException extends RuntimeException {
    public RestaurantServiceUnavailableException(String message) {
        super(message);
    }
}
