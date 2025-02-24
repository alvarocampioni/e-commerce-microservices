package com.ms.order_service.service;

import com.ms.order_service.client.ProductClient;
import com.ms.order_service.dto.OrderCanceledDTO;
import com.ms.order_service.dto.CartDTO;
import com.ms.order_service.dto.CartProductDTO;
import com.ms.order_service.dto.OrderDTO;
import com.ms.order_service.events.OrderEventProducer;
import com.ms.order_service.exception.ResourceNotFoundException;
import com.ms.order_service.exception.UnauthorizedException;
import com.ms.order_service.model.OrderProduct;
import com.ms.order_service.model.OrderStatus;
import com.ms.order_service.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    private final ProductClient productClient;


    @Autowired
    public OrderService(OrderRepository orderRepository, OrderCacheService orderCacheService, OrderEventProducer orderEventProducer, CacheManager cacheManager, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.orderCacheService = orderCacheService;
        this.orderEventProducer = orderEventProducer;
        this.cacheManager = cacheManager;
        this.productClient = productClient;
    }

    @Transactional
    @CircuitBreaker(name = "order", fallbackMethod = "fallback")
    @Caching(evict = {
            @CacheEvict(value = "unarchived", key = "#cart.cart().getFirst().customerId()"),
            @CacheEvict(value = "all-orders", allEntries = true)
    })
    public void placeCartOrder(CartDTO cart){
        String orderId = UUID.randomUUID().toString();
        OrderDTO orderDTO = new OrderDTO(new ArrayList<>());
        ArrayList<String> unavailable = new ArrayList<>();

        //check if products are still available
        for (CartProductDTO product : cart.cart()) {
            if(productClient.isAvailable(product.productId(), product.amount()) && unavailable.isEmpty()) {
                OrderProduct orderProduct = new OrderProduct();
                orderProduct.setId(orderId);
                orderProduct.setCustomerId(product.customerId());
                orderProduct.setProductId(product.productId());
                orderProduct.setAmount(product.amount());
                orderProduct.setStatus(OrderStatus.PROCESSING);
                orderProduct.setExecution_date(null);
                orderProduct.setArchived(false);
                orderProduct.setPrice(productClient.getPrice(product.productId()));
                orderProduct.setProductName(product.productName());
                orderDTO.order().add(orderProduct);
            } else {
                unavailable.add(product.productName());
            }
        }
        //if any item is unavailable
        if(!unavailable.isEmpty()) {
            String customerId = cart.cart().getFirst().customerId();
            orderEventProducer.canceledOrder(new OrderCanceledDTO(customerId, unavailable));
        } else {
            //deduct from the stock
            orderEventProducer.requestDeductStock(orderDTO);

            //create the order
            orderRepository.saveAll(orderDTO.order());

            //warn the payment service
            orderEventProducer.sendOrderData(orderDTO);

            //confirm the order and delete cart
            orderEventProducer.createdOrder(cart.cart().getFirst().customerId());
        }
    }

    public void fallback(CartDTO cart, Throwable throwable){
        orderEventProducer.canceledOrder(new OrderCanceledDTO(cart.cart().getFirst().customerId(), new ArrayList<>()));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "unarchived", key = "#orderId+#email"),
            @CacheEvict(value = "unarchived", key = "#email"),
            @CacheEvict(value = "archived", key = "#email"),
            @CacheEvict(value = "all-orders", allEntries = true)
    })
    public void archiveOrderById(String orderId, String email){
        OrderDTO orderDTO = orderCacheService.getUnarchivedOrderByOrderIdAndCustomerId(orderId, email);

        if(!orderDTO.order().getFirst().getStatus().equals(OrderStatus.PROCESSING)) {
            for (OrderProduct orderProduct : orderDTO.order()) {
                orderProduct.setArchived(true);
                orderRepository.save(orderProduct);
            }
        } else {
            throw new IllegalStateException("Order cannot be archived while processing");
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
        OrderDTO orderDTO = orderCacheService.getArchivedOrderByOrderIdAndCustomerId(orderId, email);

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
        if(orderProducts != null && !orderProducts.isEmpty()){
            OrderDTO orderDTO = new OrderDTO(orderProducts);
            if(!orderDTO.order().getFirst().getStatus().equals(OrderStatus.PROCESSING)){
                String customerId = orderDTO.order().getFirst().getCustomerId();

                if(orderDTO.order().getFirst().isArchived()) {
                    Objects.requireNonNull(cacheManager.getCache("archived")).evict(orderId + customerId);
                    Objects.requireNonNull(cacheManager.getCache("archived")).evict(customerId);
                } else {
                    Objects.requireNonNull(cacheManager.getCache("unarchived")).evict(orderId+customerId);
                    Objects.requireNonNull(cacheManager.getCache("unarchived")).evict(customerId);
                }

                orderRepository.deleteById(orderId);
            } else {
                throw new IllegalStateException("Order is processing and cannot be deleted");
            }
        } else {
            throw new ResourceNotFoundException("Order not found");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "unarchived", key = "#orderId+#email"),
            @CacheEvict(value = "unarchived", key = "#email"),
            @CacheEvict(value = "all-orders", allEntries = true)
    })
    public void cancelOrder(String orderId, String email){
        OrderDTO orderDTO = orderCacheService.getUnarchivedOrderByOrderIdAndCustomerId(orderId, email);

        if(orderDTO.order().getFirst().getStatus().equals(OrderStatus.PROCESSING)){
            revokeOrder(orderId, OrderStatus.CANCELED);
            orderEventProducer.requestedCancelPayment(orderId);
        } else {
            throw new IllegalStateException("Order is not processing so it cannot be cancelled");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "all-orders", allEntries = true)
    })
    public void revokeOrder(String orderId, OrderStatus status){
        OrderDTO orderDTO = new OrderDTO(orderRepository.findById(orderId));
        if(orderDTO.order() != null && !orderDTO.order().isEmpty()) {
            String customerId = orderDTO.order().getFirst().getCustomerId();
            Objects.requireNonNull(cacheManager.getCache("unarchived")).evict(orderId + customerId);
            Objects.requireNonNull(cacheManager.getCache("unarchived")).evict(customerId);
            for (OrderProduct orderProduct : orderDTO.order()) {
                orderProduct.setStatus(status);
                orderRepository.save(orderProduct);
            }
            orderEventProducer.requestRecoverStock(orderDTO);
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "all-orders", allEntries = true)
    })
    public void confirmOrder(String orderId){
        OrderDTO orderDTO = new OrderDTO(orderRepository.findById(orderId));
        long time = System.currentTimeMillis();
        if(orderDTO.order() != null && !orderDTO.order().isEmpty()) {
            String customerId = orderDTO.order().getFirst().getCustomerId();
            Objects.requireNonNull(cacheManager.getCache("unarchived")).evict(orderId + customerId);
            Objects.requireNonNull(cacheManager.getCache("unarchived")).evict(customerId);
            for (OrderProduct orderProduct : orderDTO.order()) {
                orderProduct.setStatus(OrderStatus.SUCCESSFUL);
                orderProduct.setExecution_date(new Timestamp(time));
                orderRepository.save(orderProduct);
            }
        }
    }
}
