package v1.foodDeliveryPlatform.facade.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import v1.foodDeliveryPlatform.dto.model.PaymentDto;
import v1.foodDeliveryPlatform.facade.PaymentFacade;
import v1.foodDeliveryPlatform.mapper.PaymentMapper;
import v1.foodDeliveryPlatform.service.PaymentService;

import java.util.UUID;

@Component
@AllArgsConstructor
public class PaymentFacadeImpl implements PaymentFacade {

    private final PaymentService paymentService;
    private final PaymentMapper mapper;

    @Override
    public PaymentDto isOrderPaid(UUID orderId, PaymentDto paymentDto) {
        return mapper.toDto(paymentService.isOrderPaid(orderId, paymentDto.getMethod()));
    }

    @Override
    public PaymentDto getById(UUID id) {
        return mapper.toDto(paymentService.getById(id));
    }
}
