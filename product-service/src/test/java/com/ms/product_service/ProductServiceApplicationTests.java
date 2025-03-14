package com.ms.product_service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.product_service.dto.OrderDTO;
import com.ms.product_service.dto.ProductCategory;
import com.ms.product_service.dto.ProductResponse;
import com.ms.product_service.dto.RejectOrderDTO;
import com.ms.product_service.exception.ResourceNotFoundException;
import com.ms.product_service.model.Product;
import com.ms.product_service.repository.ProductRepository;
import com.ms.product_service.service.ProductCacheService;
import com.redis.testcontainers.RedisContainer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
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
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.shaded.com.google.common.base.Supplier;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

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
	static RedisContainer redisContainer = new RedisContainer("redis:6.2.2");

	@Container
	@ServiceConnection
	static ConfluentKafkaContainer kafkaContainer = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;
	
	static Properties props;

	Consumer<String, String> consumer;

	@Autowired
	ProductCacheService productCacheService;

	@Autowired
	ObjectMapper objectMapper;

    @Autowired
	ProductRepository productRepository;

	@DynamicPropertySource
	static void setTestProperties(DynamicPropertyRegistry registry) {
		Supplier<Object> supplier = () -> false;
		registry.add("eureka.client.register-with-eureka", supplier);
		registry.add("eureka.client.fetch-registry", supplier);

		registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
		registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
		registry.add("spring.data.redis.port", redisContainer::getRedisPort);
		registry.add("spring.data.redis.url", redisContainer::getRedisURI);
	}

	@Autowired
	TestRestTemplate restTemplate;

	// consumes
	String checkOrderTopic = "check-order";
	String stockRecoverTopic = "recovered-stock";

	//produces
	String createdProductTopic = "created-product";
	String updatedProductTopic = "updated-product";
	String deletedProductTopic = "deleted-product";
	String acceptedOrderTopic = "accepted-order";
	String rejectedOrderTopic = "rejected-order";

	@LocalServerPort
	int port;

	String baseUrl;

	@BeforeAll
	static void beforeAll() {
		props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
	}

	@BeforeEach
    void setUp() {
		baseUrl = "http://localhost:" + port + "/api/product";
		
		productRepository.save(new Product("1", 0, "Headphones", "Wireless headphones", BigDecimal.valueOf(39.99), ProductCategory.ELECTRONIC, 100));
		productRepository.save(new Product("2", 0, "Leather Jacket", "Black leather jacket", BigDecimal.valueOf(79.99), ProductCategory.CLOTHING, 30));
		productRepository.save(new Product("3", 0, "Hammer", "Steel hammer", BigDecimal.valueOf(48.30), ProductCategory.TOOL, 79));
	}

	@AfterEach
	void cleanUp(){
		productRepository.deleteAll();

		if(consumer != null){
			consumer.unsubscribe();
			consumer.close();
		}
	}

	@Test
	void shouldRecoverStock(){
		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singletonList(updatedProductTopic));
		String product1Id = "1";
		String product2Id = "2";

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

		kafkaTemplate.send(stockRecoverTopic, dataJson);

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

		ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
		assertThat(records.count()).isGreaterThan(0);
	}

	@Test
	void shouldAddProductsAndSendEvent(){
		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singletonList(createdProductTopic));
		String requestBody = """
					{
						"name": "Apple",
						"description": "Red...",
						"price": 9.50,
						"category": "food",
						"amount": 10
					}
				""";

		HttpEntity<String> request = createHttpEntity(null, "ADMIN", requestBody);

		ResponseEntity<ProductResponse> response = restTemplate.exchange(baseUrl + "/stock", HttpMethod.POST, request, ProductResponse.class);

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

		// check if event was sent
		try {
			String json = objectMapper.writeValueAsString(productResponse);
			await().atMost(Duration.ofSeconds(10))
					.untilAsserted(() -> {
						ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
						assertThat(records.count()).isGreaterThan(0);
						records.forEach(record -> assertThat(record.value()).isEqualTo(json));
					});

		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
    }

	@Test
	void shouldUpdateProductsAndSendEvent(){
		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singleton(updatedProductTopic));

		String id = "1";

		String updatedProductBody = """
					{
						"name": "Headphones PLUS",
						"description": "Phones for the head",
						"price": 49.99,
						"category": "electronic",
						"amount": 100
					}
				""";

		HttpEntity<String> request = createHttpEntity(null,"ADMIN", updatedProductBody);

		ResponseEntity<ProductResponse> response = restTemplate.exchange(baseUrl + "/" + id + "/stock", HttpMethod.PUT, request, ProductResponse.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();

		ProductResponse productResponse = response.getBody();
		assertThat(productResponse).isNotNull();

		if(productResponse != null){
			assertThat(productResponse.productName()).isEqualTo("Headphones PLUS");
			assertThat(productResponse.productPrice()).isEqualTo(BigDecimal.valueOf(49.99));
		}

		// check if it was updated in database
		ProductResponse updatedProduct = productCacheService.getProductById(id);
		assertThat(updatedProduct).isNotNull();
		assertThat(updatedProduct.productName()).isEqualTo("Headphones PLUS");
		assertThat(updatedProduct.productPrice()).isEqualTo(BigDecimal.valueOf(49.99));

		// check if event was sent
		try {
			String json = objectMapper.writeValueAsString(productResponse);
			await().atMost(Duration.ofSeconds(5))
					.untilAsserted(() -> {
						ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
						assertThat(records.count()).isGreaterThan(0);
						records.forEach(record -> assertThat(record.value()).isEqualTo(json));
					});
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
    }

	@Test
	void shouldDeleteProduct(){
		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singleton(deletedProductTopic));

		String id = "1";

		ProductResponse productDeleted = productCacheService.getProductById(id);

		HttpEntity<String> request = createHttpEntity(null, "ADMIN", null);

		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/" + id + "/stock", HttpMethod.DELETE, request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("Product deleted successfully !");

		// check if was deleted from database
		assertThatThrownBy(() -> productCacheService.getProductById(id))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Product not found with ID: " + id);

		// check if event was sent
		try {
			String json = objectMapper.writeValueAsString(productDeleted);
			await().atMost(Duration.ofSeconds(5))
					.untilAsserted(() -> {
						ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
						assertThat(records.count()).isGreaterThan(0);
						records.forEach(record -> assertThat(record.value()).isEqualTo(json));
					});

		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void shouldPriceAndAcceptOrder(){
		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singleton(acceptedOrderTopic));

		String customerId = "fred";

		String product1Id = "1";
		String product2Id = "2";

		ProductResponse product1 = productCacheService.getProductById(product1Id);
		ProductResponse product2 = productCacheService.getProductById(product2Id);

		String acceptedOrder = String.format("""
				{
					"order": [
						{
							"orderId": "1",
							"customerId": "%s",
							"productId": "%s",
							"productName": "%s",
							"price": null,
							"amount": 1
						},
						{
							"orderId": "1",
							"customerId": "%s",
							"productId": "%s",
							"productName": "%s",
							"price": null,
							"amount": 1
						}
					]
				}
				""", customerId, product1Id, product1.productName(), customerId, product2Id, product2.productName());

		kafkaTemplate.send(checkOrderTopic, acceptedOrder);

		await().atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> {
					ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
					assertThat(records.count()).isGreaterThan(0);
					records.forEach(record -> validateOrderPrice(record.value()));
				});

		// check if products where deducted from stock
		ProductResponse afterEventProduct1 = productCacheService.getProductById(product1Id);
		ProductResponse afterEventProduct2 = productCacheService.getProductById(product2Id);


		assertThat(afterEventProduct1).isNotNull();
		assertThat(afterEventProduct2).isNotNull();
		assertThat(afterEventProduct1.amount()).isEqualTo(product1.amount() - 1);
		assertThat(afterEventProduct2.amount()).isEqualTo(product2.amount() - 1);
    }

	@Test
	void shouldRejectOrder(){
		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singleton(rejectedOrderTopic));

		String orderId = "1";
		String customerId = "fred";
		String productId = "invalid id";
		String productName = "invalid name";

		String rejectedOrder = String.format("""
				{
					"order": [
						{
							"id": "%s",
							"customerId": "%s",
							"productId": "%s",
							"productName": "%s",
							"price": null,
							"amount": 1
						}
					]
				}
				""", orderId, customerId, productId, productName);


		kafkaTemplate.send(checkOrderTopic, rejectedOrder);

		RejectOrderDTO rejectOrderDTO = new RejectOrderDTO(orderId, customerId, List.of(productName));
        try {
            String rejectionJson = objectMapper.writeValueAsString(rejectOrderDTO);
            await().atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                        assertThat(records.count()).isGreaterThan(0);
                        records.forEach(record -> {
							assertThat(record.value()).isEqualTo(rejectionJson);
							System.out.println(record.value());
						});
                    });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

	private void validateOrderPrice(String json){
		try {
			OrderDTO order = objectMapper.readValue(json, OrderDTO.class);
			assertThat(order).isNotNull();
			order.order().forEach(product -> {
				assertThat(product.price()).isNotNull();
			});
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void shouldFetchAllProducts() {
		ResponseEntity<ProductResponse[]> allResponse = restTemplate.getForEntity(baseUrl, ProductResponse[].class);
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
		String id = "1";

		ResponseEntity<ProductResponse> singleResponse = restTemplate.getForEntity(baseUrl + "/" + id, ProductResponse.class);
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
		ResponseEntity<ProductResponse[]> response = restTemplate.getForEntity(baseUrl + "/category/" + ProductCategory.ELECTRONIC.name(), ProductResponse[].class);

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
		String id = "1";

		ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/" + id + "/name", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("Headphones");
	}

	@Test
	void shouldFetchProductPrice(){
		String id = "1";

		ResponseEntity<BigDecimal> response = restTemplate.getForEntity(baseUrl + "/" + id + "/price", BigDecimal.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo(BigDecimal.valueOf(39.99));
	}

	@Test
	void shouldCheckIfAvailable(){
		String id = "1";

		int invalidAmount = productCacheService.getProductById(id).amount() + 1;
		int validAmount = productCacheService.getProductById(id).amount();

		ResponseEntity<Boolean> trueResponse = restTemplate.getForEntity(baseUrl + "/" + id + "/available?amount=" + validAmount, Boolean.class);
		assertThat(trueResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(trueResponse.hasBody()).isTrue();
		assertThat(trueResponse.getBody()).isTrue();

		String falseId = "FAKEID";

		ResponseEntity<Boolean> falseResponse = restTemplate.getForEntity(baseUrl + "/" + falseId + "/available?amount=" + invalidAmount, Boolean.class);
		assertThat(falseResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(falseResponse.hasBody()).isTrue();
		assertThat(falseResponse.getBody()).isFalse();
	}

	private HttpEntity<String> createHttpEntity(String emailHeader, String roleHeader, String requestBody){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-USER-EMAIL", emailHeader);
		headers.set("X-USER-ROLE", roleHeader);

		return new HttpEntity<>(requestBody, headers);
	}
}
