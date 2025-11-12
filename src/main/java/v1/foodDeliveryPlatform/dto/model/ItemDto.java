package v1.foodDeliveryPlatform.dto.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;
import v1.foodDeliveryPlatform.dto.validation.OnCreate;
import v1.foodDeliveryPlatform.dto.validation.OnUpdate;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {

    @NotNull(message = "Id must be not null",
            groups = OnUpdate.class)
    private UUID id;

    @NotNull(message = "Quantity must be not null")
    @Range(min = 1, max = 1000, message = "Quantity must be between 1 and 1000")
    private Integer quantity;

    @NotNull(message = "Price must be not null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price format is invalid")
    private BigDecimal price;

    @NotNull(message = "Dish id must be not null",
            groups = {OnCreate.class, OnUpdate.class})
    private UUID dish_id;
}
