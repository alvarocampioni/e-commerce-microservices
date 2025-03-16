package com.ms.order_service.repository;

import com.ms.order_service.model.OrderProduct;
import com.ms.order_service.model.OrderProductId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderProduct, OrderProductId> {
    List<OrderProduct> findById(String orderId);
    List<OrderProduct> findByEmailAndIsArchivedIsFalse(String email);
    List<OrderProduct> findByEmailAndIsArchivedIsTrue(String email);
    List<OrderProduct> findByIdAndEmailAndIsArchivedIsFalse(String orderId, String email);
    List<OrderProduct> findByIdAndEmailAndIsArchivedIsTrue(String orderId, String email);
    void deleteById(String orderId);

    @CacheEvict(value = "*", allEntries = true)
    void deleteAll();
}
