package com.ms.order_service.service;

import com.ms.order_service.dto.CartDTO;
import com.ms.order_service.dto.CartProductDTO;
import com.ms.order_service.dto.OrderDTO;
import com.ms.order_service.events.OrderEventProducer;
import com.ms.order_service.exception.ResourceNotFoundException;
import com.ms.order_service.exception.UnauthorizedException;
import com.ms.order_service.model.OrderProduct;
import com.ms.order_service.model.OrderStatus;
import com.ms.order_service.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderCacheService orderCacheService;
    private final OrderEventProducer orderEventProducer;
    private final CacheManager cacheManager;

    @Autowired
    public OrderService(OrderRepository orderRepository, OrderCacheService orderCacheService, OrderEventProducer orderEventProducer, CacheManager cacheManager) {
        this.orderRepository = orderRepository;
        this.orderCacheService = orderCacheService;
        this.orderEventProducer = orderEventProducer;
        this.cacheManager = cacheManager;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "unarchived", key = "#cart.cart().getFirst().email()"),
            @CacheEvict(value = "all-orders", allEntries = true)
    })
    public void placeCartOrder(CartDTO cart){
        String orderId = UUID.randomUUID().toString();
        OrderDTO orderDTO = new OrderDTO(new ArrayList<>());
        for (CartProductDTO product : cart.cart()) {
            OrderProduct orderProduct = mapCartProductToOrderProduct(product, orderId);
            orderDTO.order().add(orderProduct);
            orderRepository.save(orderProduct);
        }
        orderEventProducer.checkOrder(orderDTO);
    }

    private OrderProduct mapCartProductToOrderProduct(CartProductDTO product, String orderId) {
        OrderProduct orderProduct = new OrderProduct();
        orderProduct.setId(orderId);
        orderProduct.setEmail(product.email());
        orderProduct.setProductId(product.productId());
        orderProduct.setAmount(product.amount());
        orderProduct.setStatus(OrderStatus.PROCESSING);
        orderProduct.setExecution_date(null);
        orderProduct.setArchived(false);
        orderProduct.setPrice(null);
        orderProduct.setProductName(product.productName());
        return orderProduct;
    }

    @Transactional
    @CacheEvict(value = "all-orders", allEntries = true)
    public void acceptOrder(OrderDTO orderDTO){
        if (orderDTO.order() != null && !orderDTO.order().isEmpty()) {
            String orderId = orderDTO.order().getFirst().getId();
            String email = orderDTO.order().getFirst().getEmail();
            OrderDTO savedOrder = orderCacheService.getUnarchivedOrderByOrderIdAndEmail(orderId, email);

            List<OrderProduct> orderProducts = savedOrder.order();
            for (int i = 0; i < orderDTO.order().size(); i++) {
                orderProducts.get(i).setPrice(orderDTO.order().get(i).getPrice());
            }

            orderRepository.saveAll(orderProducts);
            evictOrderCache(orderDTO.order().getFirst().getId());
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "unarchived", key = "#orderId+#email"),
            @CacheEvict(value = "unarchived", key = "#email"),
            @CacheEvict(value = "archived", key = "#email"),
            @CacheEvict(value = "all-orders", allEntries = true)
    })
    public void archiveOrderById(String orderId, String email){
        OrderDTO orderDTO = orderCacheService.getUnarchivedOrderByOrderIdAndEmail(orderId, email);

        if(!orderDTO.order().getFirst().getStatus().equals(OrderStatus.PROCESSING)) {
            for (OrderProduct orderProduct : orderDTO.order()) {
                orderProduct.setArchived(true);
                orderRepository.save(orderProduct);
            }
        } else {
            throw new UnauthorizedException("Order cannot be archived while processing");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "archived", key = "#orderId+#email"),
            @CacheEvict(value = "archived", key = "#email"),
            @CacheEvict(value = "unarchived", key = "#email"),
            @CacheEvict(value = "all-orders", allEntries = true)
    })
    public void unarchiveOrderById(String orderId, String email){
        OrderDTO orderDTO = orderCacheService.getArchivedOrderByOrderIdAndEmail(orderId, email);

        for(OrderProduct orderProduct : orderDTO.order()){
            orderProduct.setArchived(false);
            orderRepository.save(orderProduct);
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "all-orders", allEntries = true)
    })
    public void deleteOrderByOrderId(String orderId, String role){
        if(!role.equals("ADMIN")){ throw new UnauthorizedException("Unauthorized to perform this action"); }
        List<OrderProduct> orderProducts = orderRepository.findById(orderId);
        OrderDTO orderDTO = new OrderDTO(orderProducts);
        if(!orderDTO.order().getFirst().getStatus().equals(OrderStatus.PROCESSING)){
            evictOrderCache(orderId);
            orderRepository.deleteById(orderId);
        } else {
            throw new UnauthorizedException("Order is processing and cannot be deleted");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "all-orders", allEntries = true)
    })
    public void cancelOrder(String orderId, String email){
        OrderDTO orderDTO = orderCacheService.getUnarchivedOrderByOrderIdAndEmail(orderId, email);

        if(orderDTO.order().getFirst().getStatus().equals(OrderStatus.PROCESSING)){
            updateOrderStatus(orderId, OrderStatus.CANCELED);
            orderEventProducer.canceledOrder(orderId);
        } else {
            throw new UnauthorizedException("Order is not processing so it cannot be cancelled");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "all-orders", allEntries = true)
    })
    public void updateOrderStatus(String orderId, OrderStatus status){
        OrderDTO orderDTO = new OrderDTO(orderRepository.findById(orderId));
        evictOrderCache(orderId);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        for (OrderProduct orderProduct : orderDTO.order()) {
            orderProduct.setStatus(status);
            orderProduct.setExecution_date(timestamp);
            orderRepository.save(orderProduct);
        }
    }

    private void evictOrderCache(String orderId){
        OrderDTO orderDTO = new OrderDTO(orderRepository.findById(orderId));
        if(orderDTO.order() != null && !orderDTO.order().isEmpty()) {
            String email = orderDTO.order().getFirst().getEmail();
            if (orderDTO.order().getFirst().isArchived()) {
                Objects.requireNonNull(cacheManager.getCache("archived")).evict(orderId+email);
                Objects.requireNonNull(cacheManager.getCache("archived")).evict(email);
            } else {
                Objects.requireNonNull(cacheManager.getCache("unarchived")).evict(orderId+email);
                Objects.requireNonNull(cacheManager.getCache("unarchived")).evict(email);
            }
        } else {
            throw new ResourceNotFoundException("Order not found with ID: " + orderId);
        }
    }

    public void recoverStock(String orderId){
        OrderDTO orderDTO = new OrderDTO(orderRepository.findById(orderId));
        if(orderDTO.order() != null && !orderDTO.order().isEmpty()) {
            orderEventProducer.recoveredStock(orderDTO);
        }
    }
}
