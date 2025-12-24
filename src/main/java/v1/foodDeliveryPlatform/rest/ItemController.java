package v1.foodDeliveryPlatform.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import v1.foodDeliveryPlatform.dto.model.ItemDto;
import v1.foodDeliveryPlatform.dto.validation.OnUpdate;
import v1.foodDeliveryPlatform.facade.ItemFacade;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/items")
@CrossOrigin(
        origins = "http://localhost:5173",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
        allowCredentials = "true"
)
@AllArgsConstructor
@Tag(
        name = "Order item Controller",
        description = "Order item API"
)
public class ItemController {

    private final ItemFacade itemFacade;

    @GetMapping("/{id}")
    @Operation(summary = "Get order item by id")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @expression.isAccessItem(#id, authentication)")
    public ResponseEntity<ItemDto> getById(
            @PathVariable final UUID id) {
        return new ResponseEntity<>(itemFacade.getById(id), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order item by id")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @expression.isAccessItem(#id, authentication)")
    public ResponseEntity<Void> deleteById(
            @PathVariable final UUID id) {
        itemFacade.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    @Operation(summary = "Update order item")
    @PreAuthorize("@expression.isAccessItem(#itemDto.id, authentication)")
    public ResponseEntity<ItemDto> updateItem(
            @Validated(OnUpdate.class)
            @RequestBody ItemDto itemDto) {
        return new ResponseEntity<>(itemFacade.updateItem(itemDto), HttpStatus.OK);
    }
}
