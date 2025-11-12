package v1.foodDeliveryPlatform.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import v1.foodDeliveryPlatform.dto.model.PaymentDto;
import v1.foodDeliveryPlatform.facade.PaymentFacade;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment")
@AllArgsConstructor
@Tag(
        name = "Payment Controller",
        description = "Payment API"
)
public class PaymentController {

    private final PaymentFacade paymentFacade;

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by id")
    public ResponseEntity<PaymentDto> getById(
            @PathVariable final UUID id) {
        return new ResponseEntity<>(paymentFacade.getById(id), HttpStatus.OK);
    }
}
