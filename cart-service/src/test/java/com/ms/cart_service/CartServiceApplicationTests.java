package com.ms.cart_service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.cart_service.dto.CartDTO;
import com.ms.cart_service.model.CartProduct;
import com.ms.cart_service.model.Product;
import com.ms.cart_service.repository.CartProductRepository;
import com.ms.cart_service.repository.ProductRepository;
import com.ms.cart_service.service.CartCacheService;
import com.ms.cart_service.service.ProductService;
import com.redis.testcontainers.RedisContainer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.shaded.com.google.common.base.Supplier;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CartServiceApplicationTests {

	@Container
	@ServiceConnection
	static RedisContainer redisContainer = new RedisContainer("redis:6.2.2");

	@Container
	@ServiceConnection
	static ConfluentKafkaContainer kafkaContainer = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.12");

	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;

	static KafkaConsumer<String, String> consumer;

	static Properties props;

	String createdProductTopic = "created-product";
	String updatedProductTopic = "updated-product";
	String deletedProductTopic = "deleted-product";
	String userDeletedTopic = "user-deleted";
	String createdOrderTopic = "created-order";

	static String loadedOrderTopic = "loaded-order";

	@DynamicPropertySource
	static void setTestProperties(DynamicPropertyRegistry registry) {
		Supplier<Object> supplier = () -> false;
		registry.add("eureka.client.register-with-eureka", supplier);
		registry.add("eureka.client.fetch-registry", supplier);

		registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
		registry.add("spring.datasource.driver-class-name", mysqlContainer::getDriverClassName);
		registry.add("spring.flyway.url", mysqlContainer::getJdbcUrl);
		registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
		registry.add("spring.data.redis.port", redisContainer::getRedisPort);
		registry.add("spring.data.redis.url", redisContainer::getRedisURI);
	}

	@Autowired
	TestRestTemplate restTemplate;

	@Autowired
	CartCacheService cartCacheService;

	@Autowired
	ProductService productService;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	CartProductRepository cartProductRepository;

	@Autowired
	ObjectMapper objectMapper;

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

		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(List.of(loadedOrderTopic));
	}

	@BeforeEach
	void setUp() {
		baseUrl = "http://localhost:" + port + "/api/cart";
		productRepository.save(new Product("1", "phone", BigDecimal.valueOf(100), 100));
		productRepository.save(new Product("2", "apple", BigDecimal.valueOf(15.99), 200));

		cartProductRepository.save(new CartProduct("jhon", "1", "phone", 1));
		cartProductRepository.save(new CartProduct("jhon", "2", "apple", 1));
		cartProductRepository.save(new CartProduct("fred", "1", "phone", 2));

	}

	@AfterEach
	void cleanUp() {
		productRepository.deleteAll();
		cartProductRepository.deleteAll();
	}

	@AfterAll
	static void afterAll() {
		consumer.close();
	}

	@Test
	void shouldDeleteCartAfterUserDeleted(){
		String email = "jhon";
		kafkaTemplate.send(userDeletedTopic, email);

		await().pollInterval(Duration.ofSeconds(2))
				.atMost(Duration.ofSeconds(20))
				.untilAsserted(() -> {
					// check if cart was deleted
					CartDTO cart = cartCacheService.getCartByEmail(email);
					assertThat(cart.cart().isEmpty()).isTrue();
				});
	}

	@Test
	void shouldSaveProduct(){
		try {
			Product product = new Product("2", "socks", BigDecimal.valueOf(15.99), 200);
			String json = objectMapper.writeValueAsString(product);
			kafkaTemplate.send(createdProductTopic, json);

			await().pollInterval(Duration.ofSeconds(2))
					.atMost(Duration.ofSeconds(20))
					.untilAsserted(() -> {
						//check if product was added
						boolean isAdded = productService.existsProduct(product.getProductId(), product.getAmount());
						assertThat(isAdded).isTrue();
					});

		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void shouldUpdateProduct(){
		try {
			String productId = "1";
			Product updated = productRepository.findById(productId).orElse(null);
			assertThat(updated).isNotNull();
			int greaterAmount = 200;
			if(updated != null) {
				updated.setAmount(greaterAmount);
				String json = objectMapper.writeValueAsString(updated);
				kafkaTemplate.send(updatedProductTopic, json);

				await().pollInterval(Duration.ofSeconds(2))
						.atMost(Duration.ofSeconds(20))
						.untilAsserted(() -> {
							// check if product was updated
							boolean isUpdated = productService.existsProduct(updated.getProductId(), greaterAmount);
							assertThat(isUpdated).isTrue();
						});
			}
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void shouldVerifyDeleteProduct(){
		try {
			String productId = "1";
			Product product = productRepository.findById(productId).orElse(null);
			assertThat(product).isNotNull();

			if(product != null) {
				String json = objectMapper.writeValueAsString(product);
				kafkaTemplate.send(deletedProductTopic, json);

				await().pollInterval(Duration.ofSeconds(1))
						.atMost(Duration.ofSeconds(20))
						.untilAsserted(() -> {
							// check if the product was removed
							boolean exists = productService.existsProduct(product.getProductId(), product.getAmount());
							assertThat(exists).isFalse();
						});
			}
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void shouldSendOrderDataAfterRequest() {
		String email = "jhon";
		CartDTO cart = cartCacheService.getCartByEmail(email);
        try {
			String json = objectMapper.writeValueAsString(cart);
			kafkaTemplate.send(createdOrderTopic, email);

			await().atMost(Duration.ofSeconds(10))
					.untilAsserted(() -> {
						ConsumerRecord<String, String> loadedOrder = KafkaTestUtils.getSingleRecord(consumer, loadedOrderTopic);
						assertThat(loadedOrder).isNotNull();
						assertThat(loadedOrder.value()).isEqualTo(json);

						// check if cart was deleted after sending data to order
						CartDTO afterRequestCart = cartCacheService.getCartByEmail(email);
						assertThat(afterRequestCart.cart().isEmpty()).isTrue();
					});
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void shouldAddProductToCart() {
		String email = "fred";
		String productId = "2";

		String requestBody = String.format("""
				{
					"productId": "%s",
					"amount": 1
				}
				""", productId);

		HttpEntity<String> request = createHttpEntity(email, requestBody);

		ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("Product added successfully !");

		// check if it was added to the database
		CartDTO cart = cartCacheService.getCartByEmail(email);
		assertThat(cart.cart().isEmpty()).isFalse();
		assertThat(cart.cart().size()).isEqualTo(2);
		assertThat(cart.cart().stream().anyMatch(product -> product.getProductId().equals(productId))).isTrue();
	}

	@Test
	void shouldUpdateProductInCart() {
		String email = "fred";
		String productId = "1";

		String requestBody = String.format("""
				{
					"productId": "%s",
					"amount": 1
				}
				""", productId);

		HttpEntity<String> request = createHttpEntity(email, requestBody);

		ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.PUT, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("Product updated successfully !");

		// check if it was added to the database
		CartDTO cart = cartCacheService.getCartByEmail(email);
		assertThat(cart.cart().isEmpty()).isFalse();
		assertThat(cart.cart().size()).isEqualTo(1);
		assertThat(cart.cart().stream().anyMatch(product -> product.getProductId().equals(productId) && product.getAmount() == 1)).isTrue();
	}

	@Test
	void shouldRemoveProductFromCart() {
		String email = "jhon";
		String productId = "1";

		HttpEntity<String> request = createHttpEntity(email, null);

		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/product/" + productId, HttpMethod.DELETE, request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("Product removed successfully !");

		// check if it was removed from database
		CartDTO cart = cartCacheService.getCartByEmail(email);
		assertThat(cart.cart().size()).isEqualTo(1);
		assertThat(cart.cart().stream().anyMatch(product -> product.getProductId().equals(productId))).isFalse();
	}

	@Test
	void shouldEmptyCart(){
		String email = "jhon";

		HttpEntity<String> request = createHttpEntity(email, null);

		ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.DELETE, request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("Cart cleared successfully !");

		// check if it was emptied from database
		CartDTO cart = cartCacheService.getCartByEmail(email);
		assertThat(cart.cart().size()).isEqualTo(0);
	}

	@Test
	void shouldGetCart(){
		String email = "jhon";

		HttpEntity<String> request = createHttpEntity(email, null);

		ResponseEntity<CartDTO> response = restTemplate.exchange(baseUrl, HttpMethod.GET, request, CartDTO.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();

		CartDTO cart = cartCacheService.getCartByEmail(email);
		assertThat(cart.cart().isEmpty()).isFalse();
		assertThat(cart).isEqualTo(response.getBody());
	}

	private HttpEntity<String> createHttpEntity(String headerValue, String requestBody) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-USER-EMAIL", headerValue);
		headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(requestBody, headers);
	}
}