package v1.foodDeliveryPlatform.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import v1.foodDeliveryPlatform.exception.ResourceNotFoundException;
import v1.foodDeliveryPlatform.model.Order;
import v1.foodDeliveryPlatform.model.Payment;
import v1.foodDeliveryPlatform.model.enums.PaymentMethod;
import v1.foodDeliveryPlatform.model.enums.PaymentStatus;
import v1.foodDeliveryPlatform.repository.OrderRepository;
import v1.foodDeliveryPlatform.repository.PaymentRepository;
import v1.foodDeliveryPlatform.service.OrderService;
import v1.foodDeliveryPlatform.service.PaymentService;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    @CacheEvict(value = {"payments", "order_payments"}, allEntries = true)
    public Payment isOrderPaid(UUID orderId, String method) {
        log.info("Processing payment for order: {}", orderId);

        Order order = orderService.getById(orderId);
        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            Payment existingPayment = paymentRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalStateException("Payment not found for order: " + orderId));
            log.info("Order: {} already paid", orderId);
            return existingPayment;
        }
        log.debug("Order found - Total price: {}, Status: {}", order.getTotalPrice(), order.getStatus());

        String paymentMethod = setPaymentMethod(method);
        log.debug("Selected payment method: {}", paymentMethod);

        order.setPaid(true);
        orderRepository.save(order);

        Payment payment = Payment.builder()
                .method(paymentMethod)
                .order(order)
                .amount(order.getTotalPrice())
                .status(PaymentStatus.Paid)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment processed successfully - PaymentId: {}, OrderId: {}, Amount: {}, Method: {}",
                savedPayment.getId(), orderId, savedPayment.getAmount(), savedPayment.getMethod());

        return savedPayment;
    }

    @Override
    @Transactional
    @Cacheable(value = "payments", key = "#id")
    public Payment getById(UUID id) {
        log.debug("Fetching payment by ID: {}", id);
        Payment payment = paymentRepository.findById(id).orElseThrow(() -> {
            log.warn("Payment not found with ID: {}", id);
            return new ResourceNotFoundException("Payment not found");
        });
        log.debug("Successfully fetched payment: {} (Order: {}, Amount: {})",
                payment.getId(), payment.getOrder().getId(), payment.getAmount());
        return payment;
    }

    private String setPaymentMethod(String method) {
        Arrays.stream(PaymentMethod.values())
                .filter(pm -> pm.name().equalsIgnoreCase(method))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid payment method: " + method));

        return method;
    }
}
