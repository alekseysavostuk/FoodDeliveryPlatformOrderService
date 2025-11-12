package v1.foodDeliveryPlatform.dto.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import v1.foodDeliveryPlatform.dto.validation.OnCreate;
import v1.foodDeliveryPlatform.dto.validation.OnUpdate;
import v1.foodDeliveryPlatform.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

    @NotNull(message = "Id must be not null",
            groups = OnUpdate.class)
    private UUID id;

    @NotBlank(message = "Payment method name must be not blank",
            groups = {OnCreate.class, OnUpdate.class})
    @Length(max = 255, message = "Payment method must be smaller 255 characters",
            groups = {OnCreate.class, OnUpdate.class})
    private String method;

    @NotNull(message = "Amount must be not null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Amount format is invalid")
    private BigDecimal amount;

    private PaymentStatus status;
}
