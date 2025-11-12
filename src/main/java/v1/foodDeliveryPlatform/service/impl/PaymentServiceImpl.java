package v1.foodDeliveryPlatform.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import v1.foodDeliveryPlatform.exception.ModelExistsException;
import v1.foodDeliveryPlatform.model.Order;
import v1.foodDeliveryPlatform.model.Payment;
import v1.foodDeliveryPlatform.model.enums.PaymentMethod;
import v1.foodDeliveryPlatform.model.enums.PaymentStatus;
import v1.foodDeliveryPlatform.repository.PaymentRepository;
import v1.foodDeliveryPlatform.service.OrderService;
import v1.foodDeliveryPlatform.service.PaymentService;

import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final Random random;

    @Override
    public Payment isOrderPaid(UUID orderId) {
        Order order = orderService.getById(orderId);
        Payment payment = Payment.builder()
                .method(getRandomPaymentMethod().name())
                .order(order)
                .amount(order.getTotalPrice())
                .status(PaymentStatus.Paid)
                .build();
        return paymentRepository.save(payment);
    }

    @Override
    public Payment getById(UUID id) {
        return paymentRepository.findById(id).orElseThrow(() ->
                new ModelExistsException("Payment not found"));
    }

    private PaymentMethod getRandomPaymentMethod() {
        PaymentMethod[] methods = PaymentMethod.values();
        return methods[random.nextInt(methods.length)];
    }
}
