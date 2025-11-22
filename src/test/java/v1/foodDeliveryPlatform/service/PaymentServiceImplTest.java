package v1.foodDeliveryPlatform.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import v1.foodDeliveryPlatform.exception.ResourceNotFoundException;
import v1.foodDeliveryPlatform.model.Order;
import v1.foodDeliveryPlatform.model.Payment;
import v1.foodDeliveryPlatform.model.enums.OrderStatus;
import v1.foodDeliveryPlatform.model.enums.PaymentMethod;
import v1.foodDeliveryPlatform.model.enums.PaymentStatus;
import v1.foodDeliveryPlatform.repository.PaymentRepository;
import v1.foodDeliveryPlatform.service.impl.PaymentServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private Random random;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private final UUID paymentId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();

    @Test
    void isOrderPaid_Success() {

        Order order = createTestOrder();
        when(orderService.getById(orderId)).thenReturn(order);
        when(random.nextInt(anyInt())).thenReturn(0);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(paymentId);
            return payment;
        });

        Payment result = paymentService.isOrderPaid(orderId);

        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        assertEquals(order, result.getOrder());
        assertEquals(PaymentStatus.Paid, result.getStatus());
        assertEquals(new BigDecimal("66.75"), result.getAmount());
        assertEquals("CREDIT_CARD", result.getMethod());

        verify(orderService).getById(orderId);
        verify(random).nextInt(PaymentMethod.values().length);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void isOrderPaid_WithDifferentPaymentMethod() {

        Order order = createTestOrder();
        when(orderService.getById(orderId)).thenReturn(order);
        when(random.nextInt(anyInt())).thenReturn(1);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(paymentId);
            return payment;
        });

        Payment result = paymentService.isOrderPaid(orderId);

        assertNotNull(result);
        assertEquals(PaymentStatus.Paid, result.getStatus());

        assertTrue(Arrays.stream(PaymentMethod.values())
                .anyMatch(method -> method.name().equals(result.getMethod())));

        verify(orderService).getById(orderId);
        verify(random).nextInt(PaymentMethod.values().length);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void isOrderPaid_AllPaymentMethodsCovered() {

        Order order = createTestOrder();
        PaymentMethod[] methods = PaymentMethod.values();

        for (int i = 0; i < methods.length; i++) {
            when(orderService.getById(orderId)).thenReturn(order);
            when(random.nextInt(methods.length)).thenReturn(i);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                payment.setId(paymentId);
                return payment;
            });

            Payment result = paymentService.isOrderPaid(orderId);

            assertNotNull(result);
            assertEquals(methods[i].name(), result.getMethod());

            reset(orderService, random, paymentRepository);
        }
    }

    @Test
    void isOrderPaid_OrderNotFound() {

        when(orderService.getById(orderId)).thenThrow(new ResourceNotFoundException("Order not found"));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.isOrderPaid(orderId));

        assertEquals("Order not found", exception.getMessage());
        verify(orderService).getById(orderId);
        verify(random, never()).nextInt(anyInt());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void getById_Success() {

        Payment payment = createTestPayment();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        Payment result = paymentService.getById(paymentId);

        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        assertEquals(PaymentStatus.Paid, result.getStatus());
        assertEquals(new BigDecimal("66.75"), result.getAmount());
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void getById_NotFound() {

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getById(paymentId));

        assertEquals("Payment not found", exception.getMessage());
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void isOrderPaid_UsesCorrectOrderTotalPrice() {

        BigDecimal expectedTotal = new BigDecimal("99.99");
        Order order = createTestOrder();
        order.setTotalPrice(expectedTotal);

        when(orderService.getById(orderId)).thenReturn(order);
        when(random.nextInt(anyInt())).thenReturn(0);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(paymentId);
            return payment;
        });

        Payment result = paymentService.isOrderPaid(orderId);

        assertNotNull(result);
        assertEquals(expectedTotal, result.getAmount());
        verify(orderService).getById(orderId);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void isOrderPaid_PaymentAlwaysHasPaidStatus() {

        Order order = createTestOrder();
        when(orderService.getById(orderId)).thenReturn(order);
        when(random.nextInt(anyInt())).thenReturn(0);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(paymentId);
            return payment;
        });

        Payment result = paymentService.isOrderPaid(orderId);

        assertNotNull(result);
        assertEquals(PaymentStatus.Paid, result.getStatus());
        verify(paymentRepository).save(any(Payment.class));
    }

    private Order createTestOrder() {
        return Order.builder()
                .id(orderId)
                .status(OrderStatus.NEW)
                .orderDate(LocalDateTime.now())
                .userId(userId)
                .restaurantId(restaurantId)
                .totalPrice(new BigDecimal("66.75"))
                .items(new ArrayList<>())
                .build();
    }

    private Payment createTestPayment() {
        return Payment.builder()
                .id(paymentId)
                .method("CREDIT_CARD")
                .order(createTestOrder())
                .amount(new BigDecimal("66.75"))
                .status(PaymentStatus.Paid)
                .build();
    }
}
