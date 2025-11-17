package v1.foodDeliveryPlatform.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import v1.foodDeliveryPlatform.exception.ModelExistsException;
import v1.foodDeliveryPlatform.exception.RestaurantServiceUnavailableException;
import v1.foodDeliveryPlatform.feign.RestaurantServiceClient;
import v1.foodDeliveryPlatform.model.Item;
import v1.foodDeliveryPlatform.model.Order;
import v1.foodDeliveryPlatform.repository.ItemRepository;
import v1.foodDeliveryPlatform.repository.OrderRepository;
import v1.foodDeliveryPlatform.service.ItemService;
import v1.foodDeliveryPlatform.service.OrderService;

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
    public Item getById(UUID id) {
        return itemRepository.findById(id).orElseThrow(() ->
                new ModelExistsException("Order not found"));
    }

    @Override
    public Item createItem(Item item, UUID orderId) {
        try {
            boolean dishExists = restaurantServiceClient.existsDish(
                    orderService.getById(orderId).getRestaurantId(),
                    item.getDish_id());

            if (!dishExists) {
                throw new ModelExistsException("Dish not found with id: " + item.getDish_id());
            }
            Order order = orderService.getById(orderId);
            item.setOrder(order);

            boolean itemExists = false;

            for (Item tempItem : order.getItems()) {
                if (Objects.equals(tempItem.getDish_id(), item.getDish_id()) &&
                        Objects.equals(tempItem.getPrice(), item.getPrice())) {

                    tempItem.setQuantity(tempItem.getQuantity() + item.getQuantity());
                    itemRepository.save(tempItem);
                    itemExists = true;
                    break;
                }
            }

            if (!itemExists) {
                Item savedItem = itemRepository.save(item);
                order.getItems().add(savedItem);
            }

            updateOrderTotalPrice(order);
            return item;
        } catch (RestaurantServiceUnavailableException e) {
            log.error("Failed to create item due to dish service unavailability: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public Item updateItem(Item item) {
        try {
            Item currentItem = getById(item.getId());
            boolean dishExists = restaurantServiceClient.existsDish(
                    currentItem.getOrder().getRestaurantId(),
                    item.getDish_id());

            if (!dishExists) {
                throw new ModelExistsException("Dish not found with id: " + item.getDish_id());
            }

            Order order = currentItem.getOrder();

            boolean itemExists = false;

            for (Item tempItem : order.getItems()) {
                if (Objects.equals(tempItem.getDish_id(), item.getDish_id()) &&
                        Objects.equals(tempItem.getPrice(), item.getPrice())) {

                    tempItem.setQuantity(tempItem.getQuantity() + item.getQuantity());
                    itemRepository.save(tempItem);
                    itemExists = true;
                    delete(currentItem.getId());
                    break;
                }
            }

            if (!itemExists) {
                currentItem.setPrice(item.getPrice());
                currentItem.setQuantity(item.getQuantity());
                currentItem.setDish_id(item.getDish_id());
                itemRepository.save(currentItem);
            }

            updateOrderTotalPrice(order);
            return item;
        } catch (RestaurantServiceUnavailableException e) {
            log.error("Failed to update item due to dish service unavailability: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public List<Item> getAllByOrderId(UUID orderId) {
        return itemRepository.findAllByOrderId(orderId);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Order order = orderRepository.findOrderByItemId(id).orElseThrow(() ->
                new ModelExistsException("Order not found"));
        itemRepository.deleteDirectlyById(id);
        order.getItems().removeIf(item -> item.getId().equals(id));
        updateOrderTotalPrice(order);
    }

    private void updateOrderTotalPrice(Order order) {
        order.setTotalPrice(orderService.calculateTotalPrice(order.getItems()));
        orderRepository.save(order);
    }

}
