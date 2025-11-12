package v1.foodDeliveryPlatform.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import v1.foodDeliveryPlatform.dto.model.ItemDto;
import v1.foodDeliveryPlatform.dto.model.OrderDto;
import v1.foodDeliveryPlatform.dto.model.PaymentDto;
import v1.foodDeliveryPlatform.dto.validation.OnCreate;
import v1.foodDeliveryPlatform.dto.validation.OnUpdate;
import v1.foodDeliveryPlatform.facade.ItemFacade;
import v1.foodDeliveryPlatform.facade.OrderFacade;
import v1.foodDeliveryPlatform.facade.PaymentFacade;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@AllArgsConstructor
@Tag(
        name = "Order Controller",
        description = "Order API"
)
public class OrderController {

    private final OrderFacade orderFacade;
    private final ItemFacade itemFacade;
    private final PaymentFacade paymentFacade;

    @GetMapping("/{id}")
    @Operation(summary = "Get order by id")
    public ResponseEntity<OrderDto> getById(
            @PathVariable final UUID id) {
        return new ResponseEntity<>(orderFacade.getById(id), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order by id")
    public ResponseEntity<Void> deleteById(
            @PathVariable final UUID id) {
        orderFacade.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(summary = "Create order")
    public ResponseEntity<OrderDto> createOrder(
            @Validated(OnCreate.class)
            @RequestParam UUID userId,
            @RequestParam UUID restaurantId,
            @RequestBody List<ItemDto> items) {
        return new ResponseEntity<>(orderFacade.createOrder(userId, restaurantId, items), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all orders (available to admin)")
    public ResponseEntity<List<OrderDto>> getAll() {
        return new ResponseEntity<>(orderFacade.getAll(), HttpStatus.OK);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders by user id")
    public ResponseEntity<List<OrderDto>> getAllByUserId(
            @PathVariable final UUID userId) {
        return new ResponseEntity<>(orderFacade.getAllByUserId(userId), HttpStatus.OK);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status (available to admin)")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @Validated(OnUpdate.class)
            @PathVariable final UUID id) {
        return new ResponseEntity<>(orderFacade.updateOrderStatus(id), HttpStatus.OK);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "Add item to order")
    public ResponseEntity<ItemDto> createItem(
            @Validated(OnCreate.class)
            @RequestBody ItemDto itemDto,
            @PathVariable final UUID id) {
        return new ResponseEntity<>(itemFacade.createItem(itemDto, id), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "Get items by order id")
    public ResponseEntity<List<ItemDto>> getItemsByOrderId(
            @PathVariable final UUID id) {
        return new ResponseEntity<>(itemFacade.getAllByOrderId(id), HttpStatus.OK);
    }

    @PostMapping("/{id}/payment")
    @Operation(summary = "Get payment confirmation")
    public ResponseEntity<PaymentDto> isOrderPaid(
            @Validated(OnCreate.class)
            @PathVariable final UUID id) {
        return new ResponseEntity<>(paymentFacade.isOrderPaid(id), HttpStatus.CREATED);
    }
}
