package v1.foodDeliveryPlatform.service;

import v1.foodDeliveryPlatform.model.Payment;

import java.util.UUID;

public interface PaymentService {
    Payment isOrderPaid(UUID orderId, String method);

    Payment getById(UUID id);

}
