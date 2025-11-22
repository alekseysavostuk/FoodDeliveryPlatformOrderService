package v1.foodDeliveryPlatform.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import v1.foodDeliveryPlatform.exception.ResourceNotFoundException;
import v1.foodDeliveryPlatform.exception.RestaurantServiceUnavailableException;
import v1.foodDeliveryPlatform.feign.RestaurantServiceClient;
import v1.foodDeliveryPlatform.model.Item;
import v1.foodDeliveryPlatform.model.Order;
import v1.foodDeliveryPlatform.repository.ItemRepository;
import v1.foodDeliveryPlatform.repository.OrderRepository;
import v1.foodDeliveryPlatform.service.ItemService;
import v1.foodDeliveryPlatform.service.OrderService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderService orderService;
    private final RestaurantServiceClient restaurantServiceClient;

    @Override
    @Transactional
    @Cacheable(value = "items", key = "#id")
    public Item getById(UUID id) {
        log.debug("Fetching item by ID: {}", id);
        Item item = itemRepository.findById(id).orElseThrow(() -> {
            log.warn("Item not found with ID: {}", id);
            return new ResourceNotFoundException("Item not found");
        });
        log.debug("Successfully fetched item: {} (Dish: {}, Quantity: {})",
                id, item.getDish_id(), item.getQuantity());
        return item;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "items", allEntries = true),
            @CacheEvict(value = "order_items", key = "#orderId"),
            @CacheEvict(value = "orders", key = "#orderId"),
            @CacheEvict(value = "user_orders", allEntries = true)
    })
    public Item createItem(Item item, UUID orderId) {
        log.info("Creating new item for order: {} (Dish: {}, Quantity: {})",
                orderId, item.getDish_id(), item.getQuantity());

        try {
            Order order = orderService.getById(orderId);
            log.debug("Validating dish existence - DishId: {}, RestaurantId: {}",
                    item.getDish_id(), order.getRestaurantId());

            boolean dishExists = restaurantServiceClient.existsDish(
                    order.getRestaurantId(),
                    item.getDish_id());

            if (!dishExists) {
                log.warn("Dish not found - DishId: {}, RestaurantId: {}",
                        item.getDish_id(), order.getRestaurantId());
                throw new ResourceNotFoundException("Dish not found with id: " + item.getDish_id());
            }

            item.setOrder(order);
            log.debug("Item assigned to order: {}", orderId);

            boolean itemExists = false;
            log.trace("Checking for existing similar items in order");

            for (Item tempItem : order.getItems()) {
                if (Objects.equals(tempItem.getDish_id(), item.getDish_id()) &&
                        Objects.equals(tempItem.getPrice(), item.getPrice())) {

                    log.debug("Found existing item, updating quantity: {} -> {}",
                            tempItem.getQuantity(), tempItem.getQuantity() + item.getQuantity());

                    tempItem.setQuantity(tempItem.getQuantity() + item.getQuantity());
                    itemRepository.save(tempItem);
                    itemExists = true;
                    break;
                }
            }

            if (!itemExists) {
                log.debug("No existing item found, creating new item");
                Item savedItem = itemRepository.save(item);
                order.getItems().add(savedItem);
                log.debug("New item created with ID: {}", savedItem.getId());
            }

            updateOrderTotalPrice(order);
            log.info("Item successfully created/updated for order: {}", orderId);
            return item;

        } catch (RestaurantServiceUnavailableException e) {
            log.error("Failed to create item due to dish service unavailability: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "items", key = "#item.id"),
            @CacheEvict(value = "order_items", key = "#result.order.id"),
            @CacheEvict(value = "orders", key = "#result.order.id"),
            @CacheEvict(value = "user_orders", key = "#result.order.userId")
    })
    public Item updateItem(Item item) {
        log.info("Updating item: {} (Dish: {}, Quantity: {})",
                item.getId(), item.getDish_id(), item.getQuantity());

        try {
            Item currentItem = getById(item.getId());
            Order order = currentItem.getOrder();

            log.debug("Validating dish existence for update - DishId: {}, RestaurantId: {}",
                    item.getDish_id(), order.getRestaurantId());

            boolean dishExists = restaurantServiceClient.existsDish(
                    order.getRestaurantId(),
                    item.getDish_id());

            if (!dishExists) {
                log.warn("Dish not found during update - DishId: {}, RestaurantId: {}",
                        item.getDish_id(), order.getRestaurantId());
                throw new ResourceNotFoundException("Dish not found with id: " + item.getDish_id());
            }

            boolean itemExists = false;
            log.trace("Checking for similar items to merge with");

            for (Item tempItem : order.getItems()) {
                if (Objects.equals(tempItem.getDish_id(), item.getDish_id()) &&
                        Objects.equals(tempItem.getPrice(), item.getPrice())) {

                    log.debug("Merging with existing item - Quantity: {} -> {}",
                            tempItem.getQuantity(), tempItem.getQuantity() + item.getQuantity());

                    tempItem.setQuantity(tempItem.getQuantity() + item.getQuantity());
                    itemRepository.save(tempItem);
                    itemExists = true;

                    log.debug("Deleting original item: {}", currentItem.getId());
                    delete(currentItem.getId());
                    break;
                }
            }

            if (!itemExists) {
                log.debug("Updating existing item properties");
                currentItem.setPrice(item.getPrice());
                currentItem.setQuantity(item.getQuantity());
                currentItem.setDish_id(item.getDish_id());
                itemRepository.save(currentItem);
            }

            updateOrderTotalPrice(order);
            log.info("Item successfully updated: {}", item.getId());
            return item;

        } catch (RestaurantServiceUnavailableException e) {
            log.error("Failed to update item due to dish service unavailability: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public List<Item> getAllByOrderId(UUID orderId) {
        log.debug("Fetching all items for order: {}", orderId);
        List<Item> items = itemRepository.findAllByOrderId(orderId);
        log.debug("Found {} items for order: {}", items.size(), orderId);
        return items;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "items", key = "#id"),
            @CacheEvict(value = "order_items", allEntries = true),
            @CacheEvict(value = "orders", allEntries = true),
            @CacheEvict(value = "user_orders", allEntries = true)
    })
    public void delete(UUID id) {
        log.info("Deleting item: {}", id);

        Order order = orderRepository.findOrderByItemId(id).orElseThrow(() -> {
            log.warn("Order not found for item: {}", id);
            return new ResourceNotFoundException("Order not found");
        });

        log.debug("Deleting item from repository: {}", id);
        itemRepository.deleteDirectlyById(id);

        log.debug("Removing item from order items list: {}", id);
        order.getItems().removeIf(item -> item.getId().equals(id));

        updateOrderTotalPrice(order);
        log.info("Item deleted successfully: {}", id);
    }

    @Transactional
    @CacheEvict(value = {"orders", "user_orders"}, key = "#order.id")
    private void updateOrderTotalPrice(Order order) {
        log.debug("Updating total price for order: {}", order.getId());
        BigDecimal oldPrice = order.getTotalPrice();
        order.setTotalPrice(orderService.calculateTotalPrice(order.getItems()));
        orderRepository.save(order);
        log.debug("Order total price updated: {} -> {}", oldPrice, order.getTotalPrice());
    }
}
