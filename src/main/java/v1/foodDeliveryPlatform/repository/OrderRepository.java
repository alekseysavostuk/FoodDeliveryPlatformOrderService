package v1.foodDeliveryPlatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import v1.foodDeliveryPlatform.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query(value = "SELECT * FROM orders WHERE user_id = :userId", nativeQuery = true)
    List<Order> findAllByUserId(@Param("userId") UUID userId);

    @Query(value = """
            SELECT * FROM orders
                WHERE EXISTS (
                    SELECT 1 FROM order_item
                    WHERE order_item.order_id = orders.id AND order_item.id = :itemId
                )""",
            nativeQuery = true)
    Optional<Order> findOrderByItemId(@Param("itemId") UUID itemId);
}
