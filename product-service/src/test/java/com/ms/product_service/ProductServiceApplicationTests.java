package com.ms.product_service;

import com.ms.product_service.dto.ProductCategory;
import com.ms.product_service.dto.ProductRequest;
import com.ms.product_service.dto.ProductResponse;
import com.ms.product_service.exception.ResourceNotFoundException;
import com.ms.product_service.service.ProductCacheService;
import com.ms.product_service.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductServiceApplicationTests {

	@Container
	@ServiceConnection
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4.4");

	@Container
	@ServiceConnection
	static KafkaContainer kafkaContainer = new KafkaContainer( "confluentinc/cp-kafka:latest");

	@Autowired
	private MongoTemplate mongoTemplate;

	@DynamicPropertySource
	static void overrideBootstrapServer(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
	}

	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	ProductService productService;

	@Autowired
	ProductCacheService productCacheService;

	@Autowired
	TestRestTemplate restTemplate;

	List<String> productIds;

	@BeforeEach
    void setUp() {
		ProductResponse product1 = productService.addProduct(new ProductRequest("Headphones", "Wireless headphones", BigDecimal.valueOf(39.99), "electronic", 100), "ADMIN");
		ProductResponse product2 = productService.addProduct(new ProductRequest("Leather Jacket", "Black leather jacket", BigDecimal.valueOf(79.99), "clothing", 30), "ADMIN");
		ProductResponse product3 = productService.addProduct(new ProductRequest("Hammer", "Steel hammer", BigDecimal.valueOf(48.30), "tool", 79), "ADMIN");

		productIds = new ArrayList<>();
		productIds.add(product1.productId());
		productIds.add(product2.productId());
		productIds.add(product3.productId());
	}

	@AfterEach
	void cleanUp(){
		mongoTemplate.dropCollection("product");
	}

	@Test
	void shouldDeductProducts(){
		String product1Id = productIds.getFirst();
		String product2Id = productIds.get(1);

		String dataJson = String.format("""
				{
					"order": [
						{
							"productId": "%s",
							"amount": 1
						},
						{
							"productId": "%s",
							"amount": 1
						}
					]
				}
				""", product1Id, product2Id);

		kafkaTemplate.send("stock-deducted", dataJson);

		await().pollInterval(Duration.ofSeconds(5))
				.atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> {
					ProductResponse product1 = productCacheService.getProductById(product1Id);
					assertThat(product1).isNotNull();
					assertThat(product1.amount()).isEqualTo(99);

					ProductResponse product2 = productCacheService.getProductById(product2Id);
					assertThat(product2).isNotNull();
					assertThat(product2.amount()).isEqualTo(29);
				});
	}

	@Test
	void shouldRecoverStock(){
		String product1Id = productIds.getFirst();
		String product2Id = productIds.get(1);

		String dataJson = String.format("""
				{
					"order": [
						{
							"productId": "%s",
							"amount": 1
						},
						{
							"productId": "%s",
							"amount": 1
						}
					]
				}
				""", product1Id, product2Id);

		kafkaTemplate.send("stock-recovered", dataJson);

		await().pollInterval(Duration.ofSeconds(5))
				.atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> {
					ProductResponse product1 = productCacheService.getProductById(product1Id);
					assertThat(product1).isNotNull();
					assertThat(product1.amount()).isEqualTo(101);

					ProductResponse product2 = productCacheService.getProductById(product2Id);
					assertThat(product2).isNotNull();
					assertThat(product2.amount()).isEqualTo(31);
				});

	}

	@Test
	void shouldAddProducts(){
		String requestBody = """
					{
						"name": "Apple",
						"description": "Red...",
						"price": 9.50,
						"category": "food",
						"amount": 10
					}
				""";

		HttpHeaders headers = new HttpHeaders();
		headers.add("X-USER-ROLE", "ADMIN");

		HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

		ResponseEntity<ProductResponse> response = restTemplate.exchange("/api/product/stock", HttpMethod.POST, request, ProductResponse.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.hasBody()).isTrue();

		ProductResponse productResponse = response.getBody();
		assertThat(response.getBody()).isNotNull();

		if(productResponse != null) {
			assertThat(productResponse.productName()).isEqualTo("Apple");
		}

		// check if it was added to database
		List<ProductResponse> allProducts = productCacheService.getProducts();
		assertThat(allProducts.size()).isEqualTo(4);
		boolean isAdded = allProducts.stream().anyMatch(product -> product.productName().equals("Apple"));
		assertThat(isAdded).isTrue();
	}

	@Test
	void shouldUpdateProducts(){
		String id = productIds.getFirst();

		String updatedProductBody = """
					{
						"name": "Headphones PLUS",
						"description": "Phones for the head",
						"price": 49.99,
						"category": "electronic",
						"amount": 100
					}
				""";

		HttpHeaders headers = new HttpHeaders();
		headers.add("X-USER-ROLE", "ADMIN");
		HttpEntity<String> request = new HttpEntity<>(updatedProductBody, headers);

		ResponseEntity<ProductResponse> response = restTemplate.exchange("api/product/" + id + "/stock", HttpMethod.PUT, request, ProductResponse.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();

		ProductResponse productResponse = response.getBody();
		assertThat(productResponse).isNotNull();

		if(productResponse != null){
			assertThat(productResponse.productName()).isEqualTo("Headphones PLUS");
			assertThat(productResponse.productPrice()).isEqualTo(49.99);
		}

		// check if it was updated in database
		ProductResponse updatedProduct = productCacheService.getProductById(id);
		assertThat(updatedProduct).isNotNull();
		assertThat(updatedProduct.productName()).isEqualTo("Headphones PLUS");
		assertThat(updatedProduct.productPrice()).isEqualTo(49.99);
	}

	@Test
	void shouldDeleteProduct(){
		String id = productIds.getFirst();

		HttpHeaders headers = new HttpHeaders();
		headers.add("X-USER-ROLE", "ADMIN");
		HttpEntity<String> request = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange("api/product/" + id + "/stock", HttpMethod.DELETE, request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("Product deleted successfully !");

		// check if was deleted from database
		assertThatThrownBy(() -> productCacheService.getProductById(id))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Product not found with ID: " + id);
	}


	@Test
	void shouldFetchAllProducts() {
		ResponseEntity<ProductResponse[]> allResponse = restTemplate.getForEntity("/api/product", ProductResponse[].class);
		assertThat(allResponse).isNotNull();
		assertThat(allResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		ProductResponse[] productResponses = allResponse.getBody();
		assertThat(allResponse.getBody()).isNotNull();

		if(productResponses != null) {
			assertThat(productResponses.length).isEqualTo(3);
		}

	}

	@Test
	void shouldFetchProductById(){
		String id = productIds.getFirst();

		ResponseEntity<ProductResponse> singleResponse = restTemplate.getForEntity("/api/product/" + id, ProductResponse.class);
		assertThat(singleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(singleResponse.hasBody()).isTrue();

		ProductResponse productResponse = singleResponse.getBody();
		assertThat(singleResponse.getBody()).isNotNull();

		if(productResponse != null) {
			assertThat(productResponse.productName()).isEqualTo("Headphones");
		}
	}

	@Test
	void shouldFetchByCategory(){
		ProductCategory category = ProductCategory.ELECTRONIC;
		ResponseEntity<ProductResponse[]> response = restTemplate.getForEntity("/api/product/category/" + ProductCategory.ELECTRONIC.name(), ProductResponse[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();

		ProductResponse[] productResponses = response.getBody();
		assertThat(productResponses).isNotNull();

		if(productResponses != null) {
			assertThat(productResponses.length).isEqualTo(1);
			assertThat(productResponses[0].productCategory()).isEqualTo(category);
		}
	}

	@Test
	void shouldFetchProductName(){
		String id = productIds.getFirst();

		ResponseEntity<String> response = restTemplate.getForEntity("/api/product/" + id + "/name", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("Headphones");
	}

	@Test
	void shouldFetchProductPrice(){
		String id = productIds.getFirst();

		ResponseEntity<BigDecimal> response = restTemplate.getForEntity("/api/product/" + id + "/price", BigDecimal.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo(BigDecimal.valueOf(39.99));
	}

	@Test
	void shouldCheckIfAvailable(){
		String id = productIds.getFirst();

		ResponseEntity<Boolean> trueResponse = restTemplate.getForEntity("/api/product/" + id + "/available", Boolean.class);
		assertThat(trueResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(trueResponse.hasBody()).isTrue();
		assertThat(trueResponse.getBody()).isTrue();

		String falseId = "FAKEID";

		ResponseEntity<Boolean> falseResponse = restTemplate.getForEntity("/api/product/" + falseId + "/available", Boolean.class);
		assertThat(falseResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(falseResponse.hasBody()).isTrue();
		assertThat(falseResponse.getBody()).isFalse();
	}
}
