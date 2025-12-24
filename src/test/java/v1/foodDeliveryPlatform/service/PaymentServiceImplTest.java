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

        String paymentMethod = "CREDIT_CARD";
        Order order = createTestOrder();

        when(orderService.getById(orderId)).thenReturn(order);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(paymentId);
            return payment;
        });

        Payment result = paymentService.isOrderPaid(orderId, paymentMethod);

        assertNotNull(result);
        assertEquals(paymentId, result.getId());
        assertEquals(order, result.getOrder());
        assertEquals(PaymentStatus.Paid, result.getStatus());
        assertEquals(new BigDecimal("66.75"), result.getAmount());
        assertEquals("CREDIT_CARD", result.getMethod());

        verify(orderService).getById(orderId);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void isOrderPaid_WithDifferentPaymentMethods() {

        Order order = createTestOrder();
        when(orderService.getById(orderId)).thenReturn(order);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(paymentId);
            return payment;
        });

        for (PaymentMethod method : PaymentMethod.values()) {

            Payment result = paymentService.isOrderPaid(orderId, method.name());

            assertNotNull(result);
            assertEquals(PaymentStatus.Paid, result.getStatus());
            assertEquals(method.name().toUpperCase(), result.getMethod().toUpperCase());
        }

        verify(orderService, times(PaymentMethod.values().length)).getById(orderId);
        verify(paymentRepository, times(PaymentMethod.values().length)).save(any(Payment.class));
    }

    @Test
    void isOrderPaid_WithInvalidPaymentMethod() {

        Order order = createTestOrder();
        String invalidMethod = "INVALID_METHOD";

        when(orderService.getById(orderId)).thenReturn(order);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> paymentService.isOrderPaid(orderId, invalidMethod));

        assertTrue(exception.getMessage().contains("Invalid payment method"));

        verify(orderService).getById(orderId);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void isOrderPaid_OrderNotFound() {

        String paymentMethod = "CREDIT_CARD";
        when(orderService.getById(orderId)).thenThrow(new ResourceNotFoundException("Order not found"));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> paymentService.isOrderPaid(orderId, paymentMethod));

        assertEquals("Order not found", exception.getMessage());
        verify(orderService).getById(orderId);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void isOrderPaid_WithNullPaymentMethod() {

        Order order = createTestOrder();
        when(orderService.getById(orderId)).thenReturn(order);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> paymentService.isOrderPaid(orderId, null));

        assertTrue(exception.getMessage().contains("Invalid payment method"));

        verify(orderService).getById(orderId);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void isOrderPaid_WithEmptyPaymentMethod() {

        Order order = createTestOrder();
        when(orderService.getById(orderId)).thenReturn(order);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> paymentService.isOrderPaid(orderId, ""));

        assertTrue(exception.getMessage().contains("Invalid payment method"));

        verify(orderService).getById(orderId);
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
        String paymentMethod = "CREDIT_CARD";
        Order order = createTestOrder();
        order.setTotalPrice(expectedTotal);

        when(orderService.getById(orderId)).thenReturn(order);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(paymentId);
            return payment;
        });

        Payment result = paymentService.isOrderPaid(orderId, paymentMethod);

        assertNotNull(result);
        assertEquals(expectedTotal, result.getAmount());
        verify(orderService).getById(orderId);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void isOrderPaid_PaymentAlwaysHasPaidStatus() {
        // Given
        String paymentMethod = "CREDIT_CARD";
        Order order = createTestOrder();
        when(orderService.getById(orderId)).thenReturn(order);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(paymentId);
            return payment;
        });

        Payment result = paymentService.isOrderPaid(orderId, paymentMethod);

        assertNotNull(result);
        assertEquals(PaymentStatus.Paid, result.getStatus());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void setPaymentMethod_ValidMethod_ReturnsMethodName() {

        Order order = createTestOrder();
        String paymentMethod = "CREDIT_CARD";

        when(orderService.getById(orderId)).thenReturn(order);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(paymentId);
            return payment;
        });

        Payment result = paymentService.isOrderPaid(orderId, paymentMethod);

        assertEquals(paymentMethod.toUpperCase(), result.getMethod());
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