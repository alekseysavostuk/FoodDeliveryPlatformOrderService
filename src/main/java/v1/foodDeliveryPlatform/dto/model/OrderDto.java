package v1.foodDeliveryPlatform.dto.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import v1.foodDeliveryPlatform.dto.validation.OnCreate;
import v1.foodDeliveryPlatform.dto.validation.OnUpdate;
import v1.foodDeliveryPlatform.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    @NotNull(message = "Id must be not null",
            groups = OnUpdate.class)
    private UUID id;

    private OrderStatus status;

    private LocalDateTime orderDate;

    @NotNull(message = "User id must be not null",
            groups = {OnCreate.class, OnUpdate.class})
    private UUID userId;

    @NotNull(message = "Restaurant id must be not null",
            groups = {OnCreate.class, OnUpdate.class})
    private UUID restaurantId;

    @NotNull(message = "Total price must be not null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Total price format is invalid")
    private BigDecimal totalPrice;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<ItemDto> items;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private PaymentDto payment;
}
