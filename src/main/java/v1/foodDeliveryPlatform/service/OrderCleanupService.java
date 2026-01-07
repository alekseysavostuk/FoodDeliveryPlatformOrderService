package v1.foodDeliveryPlatform.service;

public interface OrderCleanupService {

    void cleanupUnpaidOrders();

    void autoCleanup();
}
