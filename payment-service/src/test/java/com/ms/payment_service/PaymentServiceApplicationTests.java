package com.ms.payment_service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.payment_service.dto.OrderDTO;
import com.ms.payment_service.dto.OrderProduct;
import com.ms.payment_service.dto.PaymentCreatedDTO;
import com.ms.payment_service.model.PaymentRequest;
import com.ms.payment_service.model.Status;
import com.ms.payment_service.repository.PaymentRequestRepository;
import com.ms.payment_service.service.PaymentRequestService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
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
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentServiceApplicationTests {

	@Container
	@ServiceConnection
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4.4.4");

	@Container
	@ServiceConnection
	static ConfluentKafkaContainer kafkaContainer = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

	@Autowired
	private ObjectMapper objectMapper;

	@DynamicPropertySource
	static void setTestProperties(DynamicPropertyRegistry registry) {
		Supplier<Object> supplier = () -> false;
		registry.add("eureka.client.register-with-eureka", supplier);
		registry.add("eureka.client.fetch-registry", supplier);

		registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
		registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
	}

	@Autowired
	PaymentRequestRepository paymentRequestRepository;

	@Autowired
	PaymentRequestService paymentRequestService;

	static Properties props;

	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;

	KafkaConsumer<String, String> consumer;

	String acceptedOrderTopic = "accepted-order";
	String canceledOrderTopic = "canceled-order";

	String createdPaymentTopic = "created-payment";
	String canceledPaymentTopic = "canceled-payment";

	@BeforeAll
	static void beforeAll() {
		props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
	}

	@BeforeEach
	void setUp() {
	}

	@AfterEach
	void cleanUp() {
		paymentRequestRepository.deleteAll();

		if (consumer != null) {
			consumer.unsubscribe();
			consumer.close();
		}
	}

	@Test
	void shouldCreateAndCancelPayment() {
		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(List.of(canceledPaymentTopic, createdPaymentTopic));

		String orderId = UUID.randomUUID().toString();
		String email = UUID.randomUUID().toString();

		List<OrderProduct> products = List.of(new OrderProduct(orderId, email, "1", "apple", BigDecimal.valueOf(15), 1));
		OrderDTO orderDTO = new OrderDTO(products);

		kafkaTemplate.send(acceptedOrderTopic, parseObjectToJson(orderDTO));
		await().pollInterval(Duration.ofSeconds(2))
				.atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> {
					PaymentRequest paymentRequest = paymentRequestService.getPaymentRequestById(orderId);
					assertThat(paymentRequest).isNotNull();
					assertThat(paymentRequest.getStatus()).isEqualTo(Status.CREATED);
					ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
					assertThat(records.count()).isGreaterThan(0);
					records.forEach(record -> assertThat(record.value()).contains(email, "1", "apple"));
				});


		kafkaTemplate.send(canceledOrderTopic, orderId);

		await().pollInterval(Duration.ofSeconds(2))
				.atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> {
					PaymentRequest paymentRequest = paymentRequestService.getPaymentRequestById(orderId);
					assertThat(paymentRequest).isNotNull();
					assertThat(paymentRequest.getStatus()).isEqualTo(Status.CANCELED);
					ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
					assertThat(records.count()).isGreaterThan(0);
					records.forEach(record -> assertThat(record.value()).contains(email, orderId));
				});
	}

	private <T> String parseObjectToJson(T order) {
		try {
			return objectMapper.writeValueAsString(order);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}