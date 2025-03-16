package com.ms.order_service.controller;

import com.ms.order_service.dto.OrderDTO;
import com.ms.order_service.events.OrderEventProducer;
import com.ms.order_service.service.OrderCacheService;
import com.ms.order_service.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;
    private final OrderCacheService orderCacheService;
    private final OrderEventProducer orderEventProducer;

    @Autowired
    public OrderController(OrderService orderService, OrderCacheService orderCacheService, OrderEventProducer orderEventProducer) {
        this.orderService = orderService;
        this.orderCacheService = orderCacheService;
        this.orderEventProducer = orderEventProducer;
    }

    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestHeader(value = "X-USER-EMAIL") String email) {
        orderEventProducer.createdOrder(email);
        return new ResponseEntity<>("Order requested !", HttpStatus.ACCEPTED);
    }

    @PutMapping("/{orderId}/archive")
    public ResponseEntity<String> archiveOrderById(@PathVariable String orderId, @RequestHeader(value = "X-USER-EMAIL") String email) {
        orderService.archiveOrderById(orderId, email);
        return new ResponseEntity<>("Order archived !", HttpStatus.OK);
    }

    @PutMapping("/{orderId}/unarchive")
    public ResponseEntity<String> unarchiveOrderById(@PathVariable String orderId, @RequestHeader(value = "X-USER-EMAIL") String email) {
        orderService.unarchiveOrderById(orderId, email);
        return new ResponseEntity<>("Order unarchived !", HttpStatus.OK);
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable String orderId, @RequestHeader(value = "X-USER-EMAIL") String email) {
        orderService.cancelOrder(orderId, email);
        orderService.recoverStock(orderId);
        return new ResponseEntity<>("Order cancellation requested !", HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getOrders(@RequestHeader(value = "X-USER-ROLE") String role) {
        return new ResponseEntity<>(orderCacheService.getOrders(role), HttpStatus.OK);
    }

    @GetMapping("/me")
    public ResponseEntity<List<OrderDTO>> getUnarchivedOrdersByEmail(@RequestHeader(value = "X-USER-EMAIL") String email) {
        return new ResponseEntity<>(orderCacheService.getUnarchivedOrdersByEmail(email), HttpStatus.OK);
    }

    @GetMapping("/me/archived")
    public ResponseEntity<List<OrderDTO>> getArchivedOrdersByEmail(@RequestHeader(value = "X-USER-EMAIL") String email) {
        return new ResponseEntity<>(orderCacheService.getArchivedOrdersByEmail(email), HttpStatus.OK);
    }

    @GetMapping("me/{orderId}")
    public ResponseEntity<OrderDTO> getUnarchivedOrderByOrderIdAndEmail(@PathVariable String orderId, @RequestHeader(value = "X-USER-EMAIL") String email) {
        return new ResponseEntity<>(orderCacheService.getUnarchivedOrderByOrderIdAndEmail(orderId, email), HttpStatus.OK);
    }

    @GetMapping("me/{orderId}/archived")
    public ResponseEntity<OrderDTO> getArchivedOrderByOrderIdAndEmail(@PathVariable String orderId, @RequestHeader(value = "X-USER-EMAIL") String email) {
        return new ResponseEntity<>(orderCacheService.getArchivedOrderByOrderIdAndEmail(orderId, email), HttpStatus.OK);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<String> deleteOrderById(@PathVariable String orderId, @RequestHeader(value = "X-USER-ROLE") String role) {
        orderService.deleteOrderByOrderId(orderId, role);
        return new ResponseEntity<>("Order deleted !", HttpStatus.OK);
    }
}
