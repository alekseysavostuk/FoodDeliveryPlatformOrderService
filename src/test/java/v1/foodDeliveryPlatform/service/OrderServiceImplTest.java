package v1.foodDeliveryPlatform.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import v1.foodDeliveryPlatform.exception.ResourceNotFoundException;
import v1.foodDeliveryPlatform.exception.RestaurantServiceUnavailableException;
import v1.foodDeliveryPlatform.feign.RestaurantServiceClient;
import v1.foodDeliveryPlatform.kafka.OrderEventProducer;
import v1.foodDeliveryPlatform.kafka.event.OrderCompletedEvent;
import v1.foodDeliveryPlatform.model.Item;
import v1.foodDeliveryPlatform.model.Order;
import v1.foodDeliveryPlatform.model.enums.OrderStatus;
import v1.foodDeliveryPlatform.repository.ItemRepository;
import v1.foodDeliveryPlatform.repository.OrderRepository;
import v1.foodDeliveryPlatform.security.SecurityUtils;
import v1.foodDeliveryPlatform.service.impl.OrderServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    private final UUID orderId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID dishId = UUID.randomUUID();

    @Test
    void getById_Success() {
        Order order = createTestOrder();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.getById(orderId);

        assertNotNull(result);
        assertEquals(orderId, result.getId());
        assertEquals(OrderStatus.NEW, result.getStatus());
        verify(orderRepository).findById(orderId);
    }

    @Test
    void getById_NotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> orderService.getById(orderId));

        assertEquals("Order not found", exception.getMessage());
        verify(orderRepository).findById(orderId);
    }

    @Test
    void createOrder_Success() {
        List<Item> items = createTestItems();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(restaurantServiceClient.existsRestaurant(restaurantId)).thenReturn(true);
        when(restaurantServiceClient.existsDish(restaurantId, dishId)).thenReturn(true);
        when(restaurantServiceClient.existsDish(restaurantId, items.get(1).getDish_id())).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(orderId);
            return order;
        });
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.createOrder(restaurantId, items);

        assertNotNull(result);
        assertEquals(orderId, result.getId());
        assertEquals(OrderStatus.NEW, result.getStatus());
        assertEquals(userId, result.getUserId());
        assertEquals(restaurantId, result.getRestaurantId());
        assertEquals(new BigDecimal("66.75"), result.getTotalPrice());

        verify(restaurantServiceClient).existsRestaurant(restaurantId);
        verify(restaurantServiceClient, times(2)).existsDish(any(UUID.class), any(UUID.class));
        verify(orderRepository).save(any(Order.class));
        verify(itemRepository, times(2)).save(any(Item.class));
    }

    @Test
    void createOrder_RestaurantNotFound() {
        List<Item> items = createTestItems();

        when(restaurantServiceClient.existsDish(restaurantId, dishId)).thenReturn(true);
        when(restaurantServiceClient.existsDish(restaurantId, items.get(1).getDish_id())).thenReturn(true);

        when(restaurantServiceClient.existsRestaurant(restaurantId)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> orderService.createOrder(restaurantId, items));

        assertEquals("Restaurant not found with id: " + restaurantId, exception.getMessage());
        verify(restaurantServiceClient).existsRestaurant(restaurantId);
        // Проверяем, что блюда тоже проверялись
        verify(restaurantServiceClient, times(2)).existsDish(any(UUID.class), any(UUID.class));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_DishNotFound() {
        List<Item> items = createTestItems();
        when(restaurantServiceClient.existsRestaurant(restaurantId)).thenReturn(true);
        when(restaurantServiceClient.existsDish(restaurantId, dishId)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> orderService.createOrder(restaurantId, items));

        assertEquals("Dish not found with id: " + dishId, exception.getMessage());
        verify(restaurantServiceClient).existsRestaurant(restaurantId);
        verify(restaurantServiceClient).existsDish(restaurantId, dishId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_RestaurantServiceUnavailable() {
        List<Item> items = createTestItems();
        when(restaurantServiceClient.existsRestaurant(restaurantId))
                .thenThrow(new RestaurantServiceUnavailableException("Service unavailable"));

        RestaurantServiceUnavailableException exception = assertThrows(RestaurantServiceUnavailableException.class,
                () -> orderService.createOrder(restaurantId, items));

        assertEquals("Service unavailable", exception.getMessage());
        verify(restaurantServiceClient).existsRestaurant(restaurantId);
        verify(restaurantServiceClient, never()).existsDish(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getAll_Success() {
        Order order1 = createTestOrder();
        Order order2 = createTestOrder();
        order2.setId(UUID.randomUUID());
        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        List<Order> result = orderService.getAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(orderRepository).findAll();
    }

    @Test
    void getAll_Empty() {
        when(orderRepository.findAll()).thenReturn(List.of());

        List<Order> result = orderService.getAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderRepository).findAll();
    }

    @Test
    void updateOrderStatus_FromNewToInProgress() {

        Order order = createTestOrder();
        order.setStatus(OrderStatus.NEW);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.updateOrderStatus(orderId);

        assertNotNull(result);
        assertEquals(OrderStatus.IN_PROGRESS, result.getStatus());
        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(order);
        verify(orderEventProducer, never()).sendOrderCompleted(any());
    }

    @Test
    void updateOrderStatus_FromInProgressToDone() {

        Order order = createTestOrder();
        order.setStatus(OrderStatus.IN_PROGRESS);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.updateOrderStatus(orderId);

        assertNotNull(result);
        assertEquals(OrderStatus.DONE, result.getStatus());
        verify(orderRepository).findById(orderId);
        verify(orderRepository).save(order);
        verify(orderEventProducer).sendOrderCompleted(any(OrderCompletedEvent.class));
    }

    @Test
    void updateOrderStatus_FromInProgressToDone_SendsKafkaEvent() {

        Order order = createTestOrder();
        order.setStatus(OrderStatus.IN_PROGRESS);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.updateOrderStatus(orderId);

        assertEquals(OrderStatus.DONE, result.getStatus());
        verify(orderEventProducer).sendOrderCompleted(any(OrderCompletedEvent.class));
    }

    @Test
    void updateOrderStatus_AlreadyDone_NoStatusChange() {

        Order order = createTestOrder();
        order.setStatus(OrderStatus.DONE);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.updateOrderStatus(orderId);

        assertNotNull(result);
        assertEquals(OrderStatus.DONE, result.getStatus());
        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any());
        verify(orderEventProducer, never()).sendOrderCompleted(any());
    }

    @Test
    void updateOrderStatus_NotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> orderService.updateOrderStatus(orderId));

        assertEquals("Order not found", exception.getMessage());
        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void delete_Success() {
        doNothing().when(orderRepository).deleteById(orderId);

        assertDoesNotThrow(() -> orderService.delete(orderId));

        verify(orderRepository).deleteById(orderId);
    }

    @Test
    void delete_Exception() {
        doThrow(new RuntimeException("DB error")).when(orderRepository).deleteById(orderId);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.delete(orderId));

        assertEquals("DB error", exception.getMessage());
        verify(orderRepository).deleteById(orderId);
    }

    @Test
    void getAllByUserId_Success() {
        Order order1 = createTestOrder();
        Order order2 = createTestOrder();
        order2.setId(UUID.randomUUID());
        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(order1, order2));

        List<Order> result = orderService.getAllByUserId(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(userId, result.get(0).getUserId());
        assertEquals(userId, result.get(1).getUserId());
        verify(orderRepository).findAllByUserId(userId);
    }

    @Test
    void getAllByUserId_Empty() {
        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of());

        List<Order> result = orderService.getAllByUserId(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderRepository).findAllByUserId(userId);
    }

    @Test
    void calculateTotalPrice_Success() {
        List<Item> items = Arrays.asList(
                createItem(dishId, 2, new BigDecimal("10.00")),
                createItem(UUID.randomUUID(), 3, new BigDecimal("5.00"))
        );

        BigDecimal result = orderService.calculateTotalPrice(items);

        assertEquals(new BigDecimal("35.00"), result);
    }

    @Test
    void calculateTotalPrice_EmptyList() {
        List<Item> items = List.of();

        BigDecimal result = orderService.calculateTotalPrice(items);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateTotalPrice_SingleItem() {
        List<Item> items = List.of(createItem(dishId, 1, new BigDecimal("15.50")));

        BigDecimal result = orderService.calculateTotalPrice(items);

        assertEquals(new BigDecimal("15.50"), result);
    }

    @Test
    void calculateTotalPrice_ZeroQuantity() {
        List<Item> items = List.of(createItem(dishId, 0, new BigDecimal("15.50")));

        BigDecimal result = orderService.calculateTotalPrice(items);

        assertEquals(0, result.compareTo(BigDecimal.ZERO));
    }

    @Test
    void updateOrderStatus_SendsKafkaEventWhenStatusBecomesDone() {
        Order order = createTestOrder();
        order.setStatus(OrderStatus.IN_PROGRESS);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.updateOrderStatus(orderId);

        assertEquals(OrderStatus.DONE, result.getStatus());
        verify(orderEventProducer).sendOrderCompleted(any(OrderCompletedEvent.class));
    }

    @Test
    void calculateTotalPrice_ZeroPrice() {
        List<Item> items = List.of(createItem(dishId, 2, BigDecimal.ZERO));

        BigDecimal result = orderService.calculateTotalPrice(items);

        assertEquals(BigDecimal.ZERO, result);
    }

    private Order createTestOrder() {
        List<Item> items = createTestItems();
        return Order.builder()
                .id(orderId)
                .status(OrderStatus.NEW)
                .orderDate(LocalDateTime.now())
                .userId(userId)
                .restaurantId(restaurantId)
                .totalPrice(new BigDecimal("66.75"))
                .items(items)
                .build();
    }

    private List<Item> createTestItems() {
        Item item1 = createItem(dishId, 2, new BigDecimal("25.50"));
        Item item2 = createItem(UUID.randomUUID(), 1, new BigDecimal("15.75"));
        return Arrays.asList(item1, item2);
    }

    private Item createItem(UUID dishId, int quantity, BigDecimal price) {
        Item item = new Item();
        item.setDish_id(dishId);
        item.setQuantity(quantity);
        item.setPrice(price);
        return item;
    }
}