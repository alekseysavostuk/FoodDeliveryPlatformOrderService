package v1.foodDeliveryPlatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import v1.foodDeliveryPlatform.model.Item;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    @Modifying
    @Query(value = "DELETE FROM order_item WHERE id = :id", nativeQuery = true)
    void deleteDirectlyById(@Param("id") UUID id);

    @Query(value = "SELECT * FROM order_item WHERE order_id = :orderId", nativeQuery = true)
    List<Item> findAllByOrderId(@Param("orderId") UUID orderId);

    @Query(value = """
            SELECT EXISTS(
                SELECT 1 FROM order_item i 
                JOIN orders o ON i.order_id = o.id 
                WHERE i.id = :itemId AND o.user_id = :userId
            )""",
            nativeQuery = true)
    boolean existsByIdAndOrderUserId(@Param("itemId") UUID itemId, @Param("userId") UUID userId);

}
