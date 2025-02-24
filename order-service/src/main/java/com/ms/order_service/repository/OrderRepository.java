package com.ms.order_service.repository;

import com.ms.order_service.model.OrderProduct;
import com.ms.order_service.model.OrderProductId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderProduct, OrderProductId> {
    List<OrderProduct> findByCustomerId(String customerId);
    List<OrderProduct> findById(String orderId);
    List<OrderProduct> findByCustomerIdAndIsArchivedIsFalse(String customerId);
    List<OrderProduct> findByCustomerIdAndIsArchivedIsTrue(String customerId);
    List<OrderProduct> findByIdAndCustomerIdAndIsArchivedIsFalse(String orderId, String customerId);
    List<OrderProduct> findByIdAndCustomerIdAndIsArchivedIsTrue(String orderId, String email);
    void deleteById(String orderId);
}
