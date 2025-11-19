package v1.foodDeliveryPlatform.dto.model.feign;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dish feign client data transfer object")
public class DishClientDto {

    @NotBlank(message = "Dish name must be not blank")
    @Length(max = 255, message = "Dish name must be smaller 255 characters")
    @Schema(
            description = "Dish name",
            maxLength = 255
    )
    private String dishName;
}
