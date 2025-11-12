package v1.foodDeliveryPlatform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ModelExistsException extends ResponseStatusException {
    public ModelExistsException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}