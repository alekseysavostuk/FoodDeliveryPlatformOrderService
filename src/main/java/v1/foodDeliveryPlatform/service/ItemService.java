package v1.foodDeliveryPlatform.service;

import v1.foodDeliveryPlatform.model.Item;

import java.util.List;
import java.util.UUID;

public interface ItemService {

    Item getById(UUID id);

    Item createItem(Item item, UUID orderId);

    Item updateItem(Item item);

    List<Item> getAllByOrderId(UUID orderId);

    void delete(UUID id);
}
