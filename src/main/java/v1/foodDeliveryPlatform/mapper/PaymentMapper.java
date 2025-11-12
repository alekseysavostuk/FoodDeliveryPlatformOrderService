package v1.foodDeliveryPlatform.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import v1.foodDeliveryPlatform.dto.model.PaymentDto;
import v1.foodDeliveryPlatform.model.Payment;

@Mapper(componentModel = "spring")
public interface PaymentMapper extends BaseMapper<Payment, PaymentDto> {

    @Override
    @Mapping(target = "order", ignore = true)
    Payment toEntity(PaymentDto itemDto);
}
