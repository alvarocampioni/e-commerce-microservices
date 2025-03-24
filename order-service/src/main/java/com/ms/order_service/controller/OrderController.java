package com.ms.order_service.controller;

import com.ms.order_service.dto.OrderDTO;
import com.ms.order_service.events.OrderEventProducer;
import com.ms.order_service.service.OrderCacheService;
import com.ms.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
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
    @Operation(summary = "Place Order", description = "Obtains data from the cart-service to build the order and place it.")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> placeOrder(HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        orderEventProducer.createdOrder(email);
        return new ResponseEntity<>("Order requested !", HttpStatus.ACCEPTED);
    }

    @PutMapping("/{orderId}/archive")
    @Operation(summary = "Archive Order", description = "Archives order so it doesn't show when fetching orders.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> archiveOrderById(@PathVariable String orderId, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        orderService.archiveOrderById(orderId, email);
        return new ResponseEntity<>("Order archived !", HttpStatus.OK);
    }

    @PutMapping("/{orderId}/unarchive")
    @Operation(summary = "Unarchive Order", description = "Unarchive order, making it appear again.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> unarchiveOrderById(@PathVariable String orderId, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        orderService.unarchiveOrderById(orderId, email);
        return new ResponseEntity<>("Order unarchived !", HttpStatus.OK);
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel Order", description = "Cancels order while it is still processing.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> cancelOrder(@PathVariable String orderId, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        orderService.cancelOrder(orderId, email);
        orderService.recoverStock(orderId);
        return new ResponseEntity<>("Order cancellation requested !", HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "All Orders", description = "Returns all orders registered in the system.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<OrderDTO>> getOrders(HttpServletRequest request) {
        String role = request.getHeader("X-USER-ROLE");
        return new ResponseEntity<>(orderCacheService.getOrders(role), HttpStatus.OK);
    }

    @GetMapping("/me")
    @Operation(summary = "Unarchived Orders", description = "Returns all unarchived orders from the user.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<OrderDTO>> getUnarchivedOrdersByEmail(HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        return new ResponseEntity<>(orderCacheService.getUnarchivedOrdersByEmail(email), HttpStatus.OK);
    }

    @GetMapping("/me/archived")
    @Operation(summary = "Archived Orders", description = "Returns all archived orders from the user.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<OrderDTO>> getArchivedOrdersByEmail(HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        return new ResponseEntity<>(orderCacheService.getArchivedOrdersByEmail(email), HttpStatus.OK);
    }

    @GetMapping("me/{orderId}")
    @Operation(summary = "Unarchived Order By ID", description = "Returns an unarchived order from its ID.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<OrderDTO> getUnarchivedOrderByOrderIdAndEmail(@PathVariable String orderId, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        return new ResponseEntity<>(orderCacheService.getUnarchivedOrderByOrderIdAndEmail(orderId, email), HttpStatus.OK);
    }

    @GetMapping("me/{orderId}/archived")
    @Operation(summary = "Archived Order By ID", description = "Returns an archived order from its ID.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<OrderDTO> getArchivedOrderByOrderIdAndEmail(@PathVariable String orderId, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        return new ResponseEntity<>(orderCacheService.getArchivedOrderByOrderIdAndEmail(orderId, email), HttpStatus.OK);
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Delete Order", description = "Deletes the specified order.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteOrderById(@PathVariable String orderId, HttpServletRequest request) {
        String role = request.getHeader("X-USER-ROLE");
        orderService.deleteOrderByOrderId(orderId, role);
        return new ResponseEntity<>("Order deleted !", HttpStatus.OK);
    }
}
