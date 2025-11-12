package v1.foodDeliveryPlatform.facade;

import v1.foodDeliveryPlatform.dto.model.PaymentDto;

import java.util.UUID;

public interface PaymentFacade {
    PaymentDto isOrderPaid(UUID orderId);

    PaymentDto getById(UUID id);
}
