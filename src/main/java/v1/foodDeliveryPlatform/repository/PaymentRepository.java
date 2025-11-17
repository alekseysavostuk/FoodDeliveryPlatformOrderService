package v1.foodDeliveryPlatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import v1.foodDeliveryPlatform.model.Payment;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query(value = """
        SELECT EXISTS(
            SELECT 1 FROM payment p 
            JOIN orders o ON p.order_id = o.id 
            WHERE p.id = :paymentId AND o.user_id = :userId
        )""",
            nativeQuery = true)
    boolean existsByIdAndOrderUserId(@Param("paymentId") UUID paymentId, @Param("userId") UUID userId);
}
