package v1.foodDeliveryPlatform.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import v1.foodDeliveryPlatform.exception.ResourceNotFoundException;
import v1.foodDeliveryPlatform.exception.RestaurantServiceUnavailableException;
import v1.foodDeliveryPlatform.feign.RestaurantServiceClient;
import v1.foodDeliveryPlatform.model.Item;
import v1.foodDeliveryPlatform.model.Order;
import v1.foodDeliveryPlatform.model.enums.OrderStatus;
import v1.foodDeliveryPlatform.repository.ItemRepository;
import v1.foodDeliveryPlatform.repository.OrderRepository;
import v1.foodDeliveryPlatform.service.impl.ItemServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    @InjectMocks
    private ItemServiceImpl itemService;

    private final UUID itemId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID dishId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void getById_Success() {
        Item item = createTestItem();
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        Item result = itemService.getById(itemId);

        assertNotNull(result);
        assertEquals(itemId, result.getId());
        assertEquals(dishId, result.getDish_id());
        verify(itemRepository).findById(itemId);
    }

    @Test
    void getById_NotFound() {
        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> itemService.getById(itemId));

        assertEquals("Item not found", exception.getMessage());
        verify(itemRepository).findById(itemId);
    }

    @Test
    void createItem_Success_NewItem() {
        Item item = createTestItem();
        Order order = createTestOrder();
        when(orderService.getById(orderId)).thenReturn(order);
        when(restaurantServiceClient.existsDish(restaurantId, dishId)).thenReturn(true);
        when(itemRepository.save(item)).thenReturn(item);

        Item result = itemService.createItem(item, orderId);

        assertNotNull(result);
        assertEquals(item, result);
        verify(orderService).getById(orderId);
        verify(restaurantServiceClient).existsDish(restaurantId, dishId);
        verify(itemRepository).save(item);
        verify(orderRepository).save(order);
    }

    @Test
    void createItem_Success_ExistingItemMerge() {
        Item newItem = createTestItem();
        Order order = createTestOrder();
        Item existingItem = createTestItem();
        existingItem.setQuantity(1);
        order.getItems().add(existingItem);

        when(orderService.getById(orderId)).thenReturn(order);
        when(restaurantServiceClient.existsDish(restaurantId, dishId)).thenReturn(true);
        when(itemRepository.save(existingItem)).thenReturn(existingItem);

        Item result = itemService.createItem(newItem, orderId);

        assertNotNull(result);
        assertEquals(3, existingItem.getQuantity());
        verify(orderService).getById(orderId);
        verify(restaurantServiceClient).existsDish(restaurantId, dishId);
        verify(itemRepository).save(existingItem);
        verify(orderRepository).save(order);
    }

    @Test
    void createItem_OrderNotFound() {
        Item item = createTestItem();
        when(orderService.getById(orderId)).thenThrow(new ResourceNotFoundException("Order not found"));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> itemService.createItem(item, orderId));

        assertEquals("Order not found", exception.getMessage());
        verify(orderService).getById(orderId);
        verify(restaurantServiceClient, never()).existsDish(any(), any());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void createItem_DishNotFound() {
        Item item = createTestItem();
        Order order = createTestOrder();
        when(orderService.getById(orderId)).thenReturn(order);
        when(restaurantServiceClient.existsDish(restaurantId, dishId)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> itemService.createItem(item, orderId));

        assertEquals("Dish not found with id: " + dishId, exception.getMessage());
        verify(orderService).getById(orderId);
        verify(restaurantServiceClient).existsDish(restaurantId, dishId);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void createItem_RestaurantServiceUnavailable() {
        Item item = createTestItem();
        Order order = createTestOrder();
        when(orderService.getById(orderId)).thenReturn(order);
        when(restaurantServiceClient.existsDish(restaurantId, dishId))
                .thenThrow(new RestaurantServiceUnavailableException("Service unavailable"));

        RestaurantServiceUnavailableException exception = assertThrows(RestaurantServiceUnavailableException.class,
                () -> itemService.createItem(item, orderId));

        assertEquals("Service unavailable", exception.getMessage());
        verify(orderService).getById(orderId);
        verify(restaurantServiceClient).existsDish(restaurantId, dishId);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItem_Success_UpdateExisting() {
        Item updateData = createTestItem();
        updateData.setQuantity(5);
        updateData.setPrice(new BigDecimal("30.00"));

        Item existingItem = createTestItem();

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
        when(restaurantServiceClient.existsDish(restaurantId, dishId)).thenReturn(true);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Item result = itemService.updateItem(updateData);

        assertNotNull(result);
        assertEquals(5, existingItem.getQuantity());
        assertEquals(0, new BigDecimal("30.00").compareTo(existingItem.getPrice()));
        verify(itemRepository).findById(itemId);
        verify(restaurantServiceClient).existsDish(restaurantId, dishId);
        verify(itemRepository).save(any(Item.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateItem_Success_WhenSimilarItemExists_UpdatesExisting() {

        Item updateData = createTestItem();
        updateData.setQuantity(3);

        Item existingItem = createTestItem();
        existingItem.setQuantity(1);
        Order order = createTestOrder();

        Item similarItem = createTestItem();
        similarItem.setId(UUID.randomUUID());
        similarItem.setQuantity(2);
        order.getItems().add(similarItem);
        order.getItems().add(existingItem);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
        when(restaurantServiceClient.existsDish(restaurantId, dishId)).thenReturn(true);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Item result = itemService.updateItem(updateData);

        assertNotNull(result);
        assertEquals(3, existingItem.getQuantity());
        assertEquals(2, similarItem.getQuantity());

        verify(itemRepository).findById(itemId);
        verify(restaurantServiceClient).existsDish(restaurantId, dishId);
        verify(itemRepository).save(existingItem);
        verify(orderRepository).save(any(Order.class));
        verify(itemRepository, never()).deleteDirectlyById(any());
    }

    @Test
    void updateItem_Success_UpdateExisting_WhenNoSimilarItems() {

        Item updateData = createTestItem();
        updateData.setQuantity(5);
        updateData.setPrice(new BigDecimal("30.00"));

        Item existingItem = createTestItem();
        existingItem.setQuantity(1);
        Order order = createTestOrder();
        order.getItems().add(existingItem);

        Item differentItem = createTestItem();
        differentItem.setId(UUID.randomUUID());
        differentItem.setDish_id(UUID.randomUUID());
        differentItem.setQuantity(2);
        order.getItems().add(differentItem);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
        when(restaurantServiceClient.existsDish(restaurantId, dishId)).thenReturn(true);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Item result = itemService.updateItem(updateData);

        assertNotNull(result);
        assertEquals(5, existingItem.getQuantity());
        assertEquals(0, new BigDecimal("30.00").compareTo(existingItem.getPrice()));
        assertEquals(2, differentItem.getQuantity());

        verify(itemRepository).save(existingItem);
        verify(itemRepository, never()).deleteDirectlyById(any());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateItem_Success_MergeWithDifferentPrice() {
        Item updateData = createTestItem();
        updateData.setQuantity(3);
        updateData.setPrice(new BigDecimal("20.00"));

        Item existingItem = createTestItem();
        existingItem.setQuantity(1);
        existingItem.setPrice(new BigDecimal("25.50"));
        Order order = createTestOrder();
        order.getItems().add(existingItem);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
        when(restaurantServiceClient.existsDish(restaurantId, dishId)).thenReturn(true);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Item result = itemService.updateItem(updateData);

        assertNotNull(result);
        assertEquals(3, existingItem.getQuantity());
        assertEquals(0, new BigDecimal("20.00").compareTo(existingItem.getPrice()));

        verify(itemRepository).save(existingItem);
        verify(itemRepository, never()).deleteDirectlyById(any());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateItem_Success_MergeWithSameItem() {

        Item updateData = createTestItem();
        updateData.setQuantity(3);

        Item existingItem = createTestItem();
        existingItem.setQuantity(1);
        Order order = createTestOrder();
        order.getItems().add(existingItem);

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
        when(restaurantServiceClient.existsDish(restaurantId, dishId)).thenReturn(true);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Item result = itemService.updateItem(updateData);

        assertNotNull(result);
        assertEquals(3, existingItem.getQuantity());

        verify(itemRepository).save(existingItem);
        verify(itemRepository, never()).deleteDirectlyById(any());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateItem_ItemNotFound() {
        Item updateData = createTestItem();
        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> itemService.updateItem(updateData));

        assertEquals("Item not found", exception.getMessage());
        verify(itemRepository).findById(itemId);
        verify(restaurantServiceClient, never()).existsDish(any(), any());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItem_DishNotFound() {
        Item updateData = createTestItem();
        Item existingItem = createTestItem();
        Order order = createTestOrder();

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
        when(restaurantServiceClient.existsDish(restaurantId, dishId)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> itemService.updateItem(updateData));

        assertEquals("Dish not found with id: " + dishId, exception.getMessage());
        verify(itemRepository).findById(itemId);
        verify(restaurantServiceClient).existsDish(restaurantId, dishId);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void getAllByOrderId_Success() {
        List<Item> items = Arrays.asList(createTestItem(), createTestItem());
        when(itemRepository.findAllByOrderId(orderId)).thenReturn(items);

        List<Item> result = itemService.getAllByOrderId(orderId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(itemRepository).findAllByOrderId(orderId);
    }

    @Test
    void getAllByOrderId_Empty() {
        when(itemRepository.findAllByOrderId(orderId)).thenReturn(Collections.emptyList());

        List<Item> result = itemService.getAllByOrderId(orderId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(itemRepository).findAllByOrderId(orderId);
    }

    @Test
    void delete_Success() {
        Item item = createTestItem();
        Order order = createTestOrder();
        order.getItems().add(item);

        when(orderRepository.findOrderByItemId(itemId)).thenReturn(Optional.of(order));
        doNothing().when(itemRepository).deleteDirectlyById(itemId);
        when(orderService.calculateTotalPrice(anyList())).thenReturn(BigDecimal.ZERO);

        itemService.delete(itemId);

        verify(orderRepository).findOrderByItemId(itemId);
        verify(itemRepository).deleteDirectlyById(itemId);
        verify(orderRepository).save(order);
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    void delete_OrderNotFound() {
        when(orderRepository.findOrderByItemId(itemId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> itemService.delete(itemId));

        assertEquals("Order not found", exception.getMessage());
        verify(orderRepository).findOrderByItemId(itemId);
        verify(itemRepository, never()).deleteDirectlyById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createItem_NoSimilarItemsFound() {
        Item newItem = createTestItem();
        newItem.setDish_id(UUID.randomUUID());
        Order order = createTestOrder();
        Item existingItem = createTestItem();
        existingItem.setDish_id(UUID.randomUUID());
        order.getItems().add(existingItem);

        when(orderService.getById(orderId)).thenReturn(order);
        when(restaurantServiceClient.existsDish(restaurantId, newItem.getDish_id())).thenReturn(true);
        when(itemRepository.save(newItem)).thenReturn(newItem);

        Item result = itemService.createItem(newItem, orderId);

        assertNotNull(result);
        assertEquals(newItem, result);
        verify(orderService).getById(orderId);
        verify(restaurantServiceClient).existsDish(restaurantId, newItem.getDish_id());
        verify(itemRepository).save(newItem);
        verify(orderRepository).save(order);
    }

    @Test
    void updateItem_NoSimilarItemsFound() {
        Item updateData = createTestItem();
        updateData.setDish_id(UUID.randomUUID());

        Item existingItem = createTestItem();
        existingItem.setDish_id(UUID.randomUUID());

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(existingItem));
        when(restaurantServiceClient.existsDish(restaurantId, updateData.getDish_id())).thenReturn(true);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Item result = itemService.updateItem(updateData);

        assertNotNull(result);
        assertEquals(updateData.getDish_id(), existingItem.getDish_id());
        verify(itemRepository).findById(itemId);
        verify(restaurantServiceClient).existsDish(restaurantId, updateData.getDish_id());
        verify(itemRepository).save(any(Item.class));
        verify(orderRepository).save(any(Order.class));
    }

    private Item createTestItem() {
        Item item = new Item();
        item.setId(itemId);
        item.setDish_id(dishId);
        item.setQuantity(2);
        item.setPrice(new BigDecimal("25.50"));
        item.setOrder(createTestOrder());
        return item;
    }

    private Order createTestOrder() {
        Order order = new Order();
        order.setId(orderId);
        order.setRestaurantId(restaurantId);
        order.setUserId(userId);
        order.setStatus(OrderStatus.NEW);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalPrice(new BigDecimal("25.50"));
        order.setItems(new ArrayList<>());
        return order;
    }
}