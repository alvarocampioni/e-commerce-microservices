package com.ms.order_service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.order_service.dto.*;
import com.ms.order_service.model.OrderProduct;
import com.ms.order_service.model.OrderStatus;
import com.ms.order_service.repository.OrderRepository;
import com.ms.order_service.service.OrderCacheService;
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
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderServiceApplicationTests {

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

	//produces
	static String checkOrderTopic = "check-order";
	static String createdOrderTopic = "created-order";
	static String canceledOrderTopic = "canceled-order";
	static String recoveredStockTopic = "recovered-stock";

	//consumes
	String loadedOrderTopic = "loaded-order";
	String acceptedOrderTopic = "accepted-order";
	String rejectedOrderTopic = "rejected-order";
	String succeededPaymentTopic = "succeeded-payment";
	String failedPaymentTopic = "failed-payment";

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
	OrderCacheService orderCacheService;

	@Autowired
	OrderRepository orderRepository;

	@Autowired
	ObjectMapper objectMapper;

	@LocalServerPort
	int port;

	String baseUrl;

	@BeforeAll
	static void beforeAll(){
		props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(List.of(checkOrderTopic, createdOrderTopic, canceledOrderTopic, recoveredStockTopic));
	}

	@BeforeEach
	void setUp() {
		baseUrl = "http://localhost:" + port + "/api/order";

		orderRepository.save(new OrderProduct("1", "jhon", "200", "apple", 10, OrderStatus.PROCESSING, null, false, null, null));
		orderRepository.save(new OrderProduct("1", "jhon", "300", "phone", 1, OrderStatus.PROCESSING, null, false, null, null));
		orderRepository.save(new OrderProduct("2", "fred", "200", "apple", 2, OrderStatus.PROCESSING, null, false, null, null));
		orderRepository.save(new OrderProduct("3", "greg", "100", "table", 1, OrderStatus.SUCCESSFUL, BigDecimal.valueOf(50), false, null, null));
	}

	@AfterEach
	void cleanUp() {
		orderRepository.deleteAll();
	}

	@AfterAll
	static void afterAll(){
		consumer.close();
	}

	@Test
	void shouldHandleOrderAccepted(){
		String email = "fred";
		String orderId = "2";
		OrderDTO order = orderCacheService.getUnarchivedOrderByOrderIdAndEmail(orderId, email);
		order.order().getFirst().setPrice(BigDecimal.valueOf(50));

		kafkaTemplate.send(acceptedOrderTopic, parseObjectToJson(order));

		await().pollInterval(Duration.ofSeconds(2))
				.atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> {
					OrderDTO pricedOrder = orderCacheService.getUnarchivedOrderByOrderIdAndEmail(orderId, email);
					assertThat(pricedOrder).isNotNull();
					assertThat(pricedOrder.order()).isNotNull();
					assertThat(pricedOrder.order().size()).isEqualTo(1);

					// check if prices were updated
					assertThat(pricedOrder.order().getFirst().getPrice()).isNotNull();
					assertThat(pricedOrder.order().getFirst().getProductName()).isEqualTo("apple");
				});
	}

	@Test
	void shouldHandleOrderRejected(){
		String email = "fred";
		String orderId = "2";

		RejectOrderDTO rejectOrderDTO = new RejectOrderDTO(orderId);

		kafkaTemplate.send(rejectedOrderTopic, parseObjectToJson(rejectOrderDTO));

		await().pollInterval(Duration.ofSeconds(2))
				.atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> {
					OrderDTO rejectedOrder = orderCacheService.getUnarchivedOrderByOrderIdAndEmail(orderId, email);
					assertThat(rejectedOrder).isNotNull();
					assertThat(rejectedOrder.order()).isNotNull();
					assertThat(rejectedOrder.order().size()).isEqualTo(1);
					assertThat(rejectedOrder.order().getFirst().getStatus()).isEqualTo(OrderStatus.FAILED);
				});
	}

	@Test
	void shouldLoadAndPlaceOrder(){
		String email = "nick";
		String product1Id = "200";
		String product2Id = "300";
		String product1Name = "apple";
		String product2Name = "phone";

        List<CartProductDTO> products = new ArrayList<>(List.of(new CartProductDTO(email, product1Id, product1Name, 1), new CartProductDTO(email, product2Id, product2Name, 1)));
		CartDTO cart = new CartDTO(products);

		kafkaTemplate.send(loadedOrderTopic, parseObjectToJson(cart));

		await().pollInterval(Duration.ofSeconds(2))
					.atMost(Duration.ofSeconds(20))
					.untilAsserted(() -> {
						List<OrderDTO> orders = orderCacheService.getUnarchivedOrdersByEmail(email);
						assertThat(orders).isNotNull();
						assertThat(orders.size()).isEqualTo(1);
						assertThat(orders.getFirst().order()).isNotNull();
						assertThat(orders.getFirst().order().size()).isEqualTo(2);
						assertThat(orders.getFirst().order().getFirst().getPrice()).isNull();
						assertThat(orders.getFirst().order().get(1).getPrice()).isNull();

						ConsumerRecord<String, String> cancelRecord = KafkaTestUtils.getSingleRecord(consumer, checkOrderTopic);
						assertThat(cancelRecord).isNotNull();
						assertThat(cancelRecord.value()).contains(email, product1Id, product2Id, product1Name, product2Name);
					});
	}

	@Test
	void shouldHandlePaymentSuccess(){
		String email = "fred";
		String orderId = "2";

		PaymentStatusChangedDTO paymentStatusChangedDTO = new PaymentStatusChangedDTO(email, orderId);
		kafkaTemplate.send(succeededPaymentTopic, parseObjectToJson(paymentStatusChangedDTO));

			await().pollInterval(Duration.ofSeconds(2))
					.atMost(Duration.ofSeconds(10))
					.untilAsserted(() -> {
						OrderDTO successfulOrder = orderCacheService.getUnarchivedOrderByOrderIdAndEmail(orderId, email);
						assertThat(successfulOrder).isNotNull();
						assertThat(successfulOrder.order()).isNotNull();
						assertThat(successfulOrder.order().size()).isEqualTo(1);
						assertThat(successfulOrder.order().getFirst().getStatus()).isEqualTo(OrderStatus.SUCCESSFUL);
					});
	}

	@Test
	void shouldThenHandlePaymentFail() {
		String email = "fred";
		String orderId = "2";

		PaymentStatusChangedDTO paymentStatusChangedDTO = new PaymentStatusChangedDTO(email, orderId);
		kafkaTemplate.send(failedPaymentTopic, parseObjectToJson(paymentStatusChangedDTO));


			await().pollInterval(Duration.ofSeconds(2))
					.atMost(Duration.ofSeconds(20))
					.untilAsserted(() -> {
						OrderDTO failedOrder = orderCacheService.getUnarchivedOrderByOrderIdAndEmail(orderId, email);
						assertThat(failedOrder).isNotNull();
						assertThat(failedOrder.order()).isNotNull();
						assertThat(failedOrder.order().size()).isEqualTo(1);
						assertThat(failedOrder.order().getFirst().getStatus()).isEqualTo(OrderStatus.FAILED);

						ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, recoveredStockTopic);
						assertThat(record).isNotNull();
						assertThat(record.value()).contains(email, orderId);
					});
	}

	@Test
	void shouldRequestCreatingOrder(){
		String email = "nick";

		HttpEntity<String> request = createHttpEntity(email, null);
		ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody()).isEqualTo("Order requested !");

		await().pollInterval(Duration.ofSeconds(2))
				.atMost(Duration.ofSeconds(20))
				.untilAsserted(() -> {
					ConsumerRecord<String, String> cancelRecord = KafkaTestUtils.getSingleRecord(consumer, createdOrderTopic);
					assertThat(cancelRecord).isNotNull();
					assertThat(cancelRecord.value()).isEqualTo(email);
				});

	}

	@Test
	void shouldCancelOrder(){
		String email = "fred";
		String orderId = "2";

		HttpEntity<String> request = createHttpEntity(email, null);
		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/" + orderId + "/cancel", HttpMethod.PUT, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody()).isEqualTo("Order cancellation requested !");

		await().pollInterval(Duration.ofSeconds(1))
				.atMost(Duration.ofSeconds(20))
				.untilAsserted(() -> {
					OrderDTO canceledOrder = orderCacheService.getUnarchivedOrderByOrderIdAndEmail(orderId, email);
					assertThat(canceledOrder).isNotNull();
					assertThat(canceledOrder.order()).isNotNull();
					assertThat(canceledOrder.order().size()).isEqualTo(1);
					assertThat(canceledOrder.order().getFirst().getStatus()).isEqualTo(OrderStatus.CANCELED);

					// check events sent
					ConsumerRecord<String, String> recoverRecord = KafkaTestUtils.getSingleRecord(consumer, recoveredStockTopic);
					assertThat(recoverRecord).isNotNull();
					assertThat(recoverRecord.value()).isEqualTo(parseObjectToJson(canceledOrder));
					ConsumerRecord<String, String> cancelRecord = KafkaTestUtils.getSingleRecord(consumer, canceledOrderTopic);
					assertThat(cancelRecord).isNotNull();
					assertThat(cancelRecord.value()).isEqualTo(orderId);
				});
	}

	@Test
	void shouldArchiveOrderAndUnarchiveOrder(){
		String email = "greg";
		String orderId = "3";

		// archiving
		HttpEntity<String> request = createHttpEntity(email, null);
		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/" + orderId + "/archive", HttpMethod.PUT, request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody()).isEqualTo("Order archived !");

		List<OrderDTO> unarchived = orderCacheService.getUnarchivedOrdersByEmail(email);
		List<OrderDTO> archived = orderCacheService.getArchivedOrdersByEmail(email);
		assertThat(unarchived).isNotNull();
		assertThat(unarchived.isEmpty()).isTrue();
		assertThat(archived).isNotNull();
		assertThat(archived.size()).isEqualTo(1);

		// un-archiving
		ResponseEntity<String> responseTwo = restTemplate.exchange(baseUrl + "/" + orderId + "/unarchive", HttpMethod.PUT, request, String.class);
		assertThat(responseTwo.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseTwo.getBody()).isNotNull();
		assertThat(responseTwo.getBody()).isEqualTo("Order unarchived !");

		unarchived = orderCacheService.getUnarchivedOrdersByEmail(email);
		archived = orderCacheService.getArchivedOrdersByEmail(email);
		assertThat(unarchived).isNotNull();
		assertThat(unarchived.size()).isEqualTo(1);
		assertThat(archived).isNotNull();
		assertThat(archived.isEmpty()).isTrue();
	}

	@Test
	void shouldDeleteOrder(){
		String role = "ADMIN";
		String orderId = "3";

		HttpEntity<String> request = createHttpEntity(null, role);
		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/" + orderId, HttpMethod.DELETE, request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody()).isEqualTo("Order deleted !");

		// check if it was removed from database
		List<OrderDTO> orders = orderCacheService.getOrders(role);
		assertThat(orders).isNotNull();
		assertThat(orders.size()).isEqualTo(2);
		boolean exists = orders.stream().anyMatch(orderDTO -> orderDTO.order().getFirst().getId().equals(orderId));
		assertThat(exists).isFalse();
	}

	@Test
	void shouldFetchOwnOrdersByEmail(){
		String email = "fred";
		String orderId = "2";

		HttpEntity<String> request = createHttpEntity(email, null);
		// get unarchived orders
		ResponseEntity<OrderDTO[]> listResponse = restTemplate.exchange(baseUrl + "/me" , HttpMethod.GET, request, OrderDTO[].class);
		assertResponseBodyLength(listResponse, 1);

		// get archived orders
		listResponse = restTemplate.exchange(baseUrl + "/me/archived" , HttpMethod.GET, request, OrderDTO[].class);
		assertResponseBodyLength(listResponse, 0);

		// get single unarchived order
		ResponseEntity<OrderDTO> singleResponse = restTemplate.exchange(baseUrl + "/me/" + orderId , HttpMethod.GET, request, OrderDTO.class);
		assertThat(singleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(singleResponse.getBody()).isNotNull();

		if(singleResponse.getBody() != null) {
			OrderDTO orderDTO = singleResponse.getBody();
			assertThat(orderDTO.order()).isNotNull();
			assertThat(orderDTO.order().size()).isEqualTo(1);
			assertThat(orderDTO.order().getFirst().getId()).isEqualTo(orderId);
		}

		// get single archived order ( doesn't find )
		singleResponse = restTemplate.exchange(baseUrl + "/me/" + orderId + "/archived", HttpMethod.GET, request, OrderDTO.class);
		assertThat(singleResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(singleResponse.getBody()).isNotNull();
	}

	@Test
	void shouldFetchAllOrders(){
		String role = "ADMIN";

		HttpEntity<String> request = createHttpEntity(null, role);
		ResponseEntity<OrderDTO[]> response = restTemplate.exchange(baseUrl, HttpMethod.GET, request, OrderDTO[].class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		if(response.getBody() != null) {
			assertThat(response.getBody().length).isEqualTo(3);
		}
	}

	private HttpEntity<String> createHttpEntity(String emailHeader, String roleHeader){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-USER-EMAIL", emailHeader);
		headers.set("X-USER-ROLE", roleHeader);

		return new HttpEntity<>(headers);
	}

	private <T> void assertResponseBodyLength(ResponseEntity<T[]> response, int length){
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		if(response.getBody() != null) {
			assertThat(response.getBody().length).isEqualTo(length);
		}
	}

	private <T> String parseObjectToJson(T obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}