package com.ms.order_service.service;

import com.ms.order_service.dto.OrderDTO;
import com.ms.order_service.exception.ResourceNotFoundException;
import com.ms.order_service.exception.UnauthorizedException;
import com.ms.order_service.model.OrderProduct;
import com.ms.order_service.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrderCacheService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderCacheService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Cacheable(value = "all-orders", key = "#role")
    public List<OrderDTO> getOrders(String role){
        log.info("getOrders called -- accessing database");
        if(role.equals("ADMIN")){
            List<OrderProduct> products = orderRepository.findAll();
            return mapProductsToOrderDTO(products);
        } else {
            throw new UnauthorizedException("Unauthorized to perform this action");
        }
    }

    @Cacheable(value = "unarchived", key = "#customerId")
    public List<OrderDTO> getUnarchivedOrdersByCustomerId(String customerId){
        log.info("getUnarchivedOrdersByCustomerId called -- accessing database");
        List<OrderProduct> products = orderRepository.findByCustomerIdAndIsArchivedIsFalse(customerId);
        return mapProductsToOrderDTO(products);
    }

    @Cacheable(value = "archived", key = "#customerId")
    public List<OrderDTO> getArchivedOrdersByCustomerId(String customerId){
        log.info("getArchivedOrdersByCustomerId called -- accessing database");
        List<OrderProduct> products = orderRepository.findByCustomerIdAndIsArchivedIsTrue(customerId);
        return mapProductsToOrderDTO(products);
    }

    @Cacheable(value = "unarchived", key = "#orderId+#email")
    public OrderDTO getUnarchivedOrderByOrderIdAndCustomerId(String orderId, String email){
        log.info("getUnarchivedOrdersByOrderIdCustomerId called -- accessing database");
        List<OrderProduct> orderProducts = orderRepository.findByIdAndCustomerIdAndIsArchivedIsFalse(orderId, email);
        if(orderProducts != null && !orderProducts.isEmpty()){
            return new OrderDTO(orderProducts);
        }
        throw new ResourceNotFoundException("Order not found");
    }

    @Cacheable(value = "archived", key = "#orderId+#email")
    public OrderDTO getArchivedOrderByOrderIdAndCustomerId(String orderId, String email){
        log.info("getArchivedOrdersByOrderIdCustomerId called -- accessing database");
        List<OrderProduct> orderProducts = orderRepository.findByIdAndCustomerIdAndIsArchivedIsTrue(orderId, email);
        if(orderProducts != null && !orderProducts.isEmpty()){
            return new OrderDTO(orderProducts);
        }
        throw new ResourceNotFoundException("Order not found");
    }

    private ArrayList<OrderDTO> mapProductsToOrderDTO(List<OrderProduct> products){
        Map<String, OrderDTO> map = new HashMap<>();
        for (OrderProduct orderProduct : products) {
            if(map.containsKey(orderProduct.getId())){
                map.get(orderProduct.getId()).order().add(orderProduct);
            } else {
                List<OrderProduct> orderProducts = new ArrayList<>();
                orderProducts.add(orderProduct);
                map.put(orderProduct.getId(), new OrderDTO(orderProducts));
            }
        }
        return new ArrayList<>(map.values());
    }
}
