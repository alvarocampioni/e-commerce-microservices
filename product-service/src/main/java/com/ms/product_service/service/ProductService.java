package com.ms.product_service.service;

import com.ms.product_service.dto.*;
import com.ms.product_service.events.ProductEventProducer;
import com.ms.product_service.exception.ResourceNotFoundException;
import com.ms.product_service.exception.UnauthorizedException;
import com.ms.product_service.model.Product;
import com.ms.product_service.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCacheService productCacheService;
    private final ProductEventProducer productEventProducer;
    private final CacheManager cacheManager;

    @Autowired
    public ProductService(ProductRepository productRepository, ProductCacheService productCacheService, ProductEventProducer productEventProducer, CacheManager cacheManager) {
        this.productRepository = productRepository;
        this.productCacheService = productCacheService;
        this.productEventProducer = productEventProducer;
        this.cacheManager = cacheManager;
    }

    @Transactional
    @Caching(evict = {@CacheEvict(value = "product", key = "'all'")})
    public ProductResponse addProduct(ProductRequest productRequest, String role) {
        if(!role.equals("ADMIN")) {
            throw new UnauthorizedException("Unauthorized to perform this action");
        }
        Product product = new Product();
        product.setName(productRequest.name());
        product.setDescription(productRequest.description());
        product.setPrice(productRequest.price());

        ProductCategory category = ProductCategory.fromString(productRequest.category());
        product.setCategory(category);
        product.setAmount(productRequest.amount());

        Objects.requireNonNull(cacheManager.getCache("product-cat")).evict(productRequest.category().toUpperCase());

        ProductResponse response = productCacheService.mapProductToResponse(productRepository.save(product));
        productEventProducer.createdProduct(response);
        return response;
    }

    @Transactional
    @Caching(
            evict = {@CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "product", key = "'all'"),
            @CacheEvict(value = "product-price", key = "#id"),
            @CacheEvict(value = "product-name", key = "#id")}
    )
    public void deleteProduct(String id, String role) {
        if(!role.equals("ADMIN")) {
            throw new UnauthorizedException("Unauthorized to perform this action");
        }
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        String productCategory = product.getCategory().name();
        Objects.requireNonNull(cacheManager.getCache("product-cat")).evict(productCategory.toUpperCase());
        productRepository.deleteById(id);
        productEventProducer.deletedProduct(productCacheService.mapProductToResponse(product));
    }

    @Transactional
    @Caching(
            evict = {@CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = "product", key = "'all'"),
            @CacheEvict(value = "product-price", key = "#id"),
            @CacheEvict(value = "product-name", key = "#id")}
    )
    public ProductResponse updateProduct(String id, ProductRequest productRequest, String role) {
        if(!role.equals("ADMIN")) {
            throw new UnauthorizedException("Unauthorized to perform this action");
        }

        Product updatedProduct = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found with ID:" + id));

        String productCategory = updatedProduct.getCategory().name();

        updatedProduct.setName(productRequest.name());
        updatedProduct.setDescription(productRequest.description());
        updatedProduct.setPrice(productRequest.price());

        ProductCategory category = ProductCategory.fromString(productRequest.category());
        updatedProduct.setCategory(category);
        updatedProduct.setAmount(productRequest.amount());
        productRepository.save(updatedProduct);

        ProductResponse response = productCacheService.mapProductToResponse(updatedProduct);
        productEventProducer.updatedProduct(response);

        Objects.requireNonNull(cacheManager.getCache("product-cat")).evict(productCategory.toUpperCase());

        if(!productCategory.equalsIgnoreCase(productRequest.category())) {
            Objects.requireNonNull(cacheManager.getCache("product-cat")).evict(updatedProduct.getCategory().name().toUpperCase());
        }

        return response;
    }


    @Transactional
    @Caching(
            evict = {@CacheEvict(value = "product", key = "#orderDTO.order().getFirst().productId()"),
                    @CacheEvict(value = "product", key = "'all'")}
    )
    public void recoverStock(OrderDTO orderDTO){
        for(OrderProduct orderProduct: orderDTO.order()){
            Optional<Product> optionalProduct = productRepository.findById(orderProduct.productId());
            if(optionalProduct.isPresent()) {
                Product product = optionalProduct.get();

                String productCategory = product.getCategory().name();
                Objects.requireNonNull(cacheManager.getCache("product-cat")).evict(productCategory.toUpperCase());

                product.setAmount(product.getAmount() + orderProduct.amount());
                productRepository.save(product);
                productEventProducer.updatedProduct(productCacheService.mapProductToResponse(product));
            }
        }
    }

    public void checkOrder(OrderDTO orderDTO) {
        if(orderDTO.order() == null || orderDTO.order().isEmpty()) {
            throw new ResourceNotFoundException("Order not found");
        }
        List<String> unavailable = new ArrayList<>();
        List<OrderProduct> pricedOrderProducts = new ArrayList<>();
        String orderId = orderDTO.order().getFirst().orderId();
        String customerId = orderDTO.order().getFirst().customerId();
        for(OrderProduct orderProduct: orderDTO.order()){
            if(!isAvailable(orderProduct.productId(), orderProduct.amount())) {
                unavailable.add(orderProduct.productName());
            } else if(unavailable.isEmpty()) {
                BigDecimal price = productCacheService.getPrice(orderProduct.productId());
                OrderProduct pricedProduct = new OrderProduct(orderProduct.orderId(), orderProduct.customerId(), orderProduct.productId(), orderProduct.productName(), price, orderProduct.amount());
                pricedOrderProducts.add(pricedProduct);
            }
        }

        if(!unavailable.isEmpty()){
            productEventProducer.rejectedOrder(new RejectOrderDTO(orderId, customerId, unavailable));
        } else {
            deductProduct(orderDTO);
            productEventProducer.acceptedOrder(new OrderDTO(pricedOrderProducts));
        }

    }

    private void deductProduct(OrderDTO orderDTO) {
        for(OrderProduct products: orderDTO.order()){
            Optional<Product> optionalProduct = productRepository.findById(products.productId());
            if(optionalProduct.isPresent()) {
                Product product  = optionalProduct.get();

                String productCategory = product.getCategory().name();
                Objects.requireNonNull(cacheManager.getCache("product")).evict(product.getId());
                Objects.requireNonNull(cacheManager.getCache("product-cat")).evict(productCategory.toUpperCase());

                product.setAmount(product.getAmount() - products.amount());
                productRepository.save(product);
                productEventProducer.updatedProduct(productCacheService.mapProductToResponse(product));
            }
        }
        Objects.requireNonNull(cacheManager.getCache("product")).evict("all");
    }

    public boolean isAvailable(String id, int amount) {
        return productRepository.existsByIdAndAmountIsGreaterThanEqual(id, amount);
    }
}
