package v1.foodDeliveryPlatform.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import v1.foodDeliveryPlatform.exception.ModelExistsException;
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
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderService orderService;

    @Override
    public Item getById(UUID id) {
        return itemRepository.findById(id).orElseThrow(() ->
                new ModelExistsException("Order not found"));
    }

    @Override
    public Item createItem(Item item, UUID orderId) {
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
    }

    @Override
    @Transactional
    public Item updateItem(Item item) {
        Item currentItem = getById(item.getId());
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
