package com.ms.user_service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.user_service.dto.LoginResponseDTO;
import com.ms.user_service.dto.UserNotificationDTO;
import com.ms.user_service.model.Role;
import com.ms.user_service.model.User;
import com.ms.user_service.repository.UserRepository;
import com.ms.user_service.service.JwtService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.shaded.com.google.common.base.Supplier;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserServiceApplicationTests {

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.12");

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

		registry.add("JWT.SECRET.KEY", () -> "key");
		registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
		registry.add("spring.datasource.driver-class-name", mysqlContainer::getDriverClassName);
		registry.add("spring.flyway.url", mysqlContainer::getJdbcUrl);
		registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
	}

	@Autowired
	TestRestTemplate restTemplate;

	@Autowired
	UserRepository userRepository;

	@Autowired
	JwtService jwtService;

	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;

	static Properties props;

	static KafkaConsumer<String, String> consumer;

	@LocalServerPort
	int port;

	String baseUrl;

	static String notifyUserTopic = "notify-user";
	static String userDeletedTopic = "user-deleted";

	@BeforeAll
	static void beforeAll() {
		props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "fixed-test");
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(List.of(notifyUserTopic, userDeletedTopic));
	}

	@BeforeEach
	void setUp() {
		baseUrl = "http://localhost:" + port + "/api/user";
		userRepository.save(new User("fred", bCryptPasswordEncoder.encode("1234"), Role.CUSTOMER, "99999", false, null));
		userRepository.save(new User("admin", bCryptPasswordEncoder.encode("admin"), Role.ADMIN, "99999", true, null));
	}

	@AfterEach
	void cleanUp() {
		userRepository.deleteAll();
	}

	@AfterAll
	static void afterAll(){
		consumer.close();
	}

	@Test
	void shouldRegisterUser(){

		String email = "new@example.com";

		String requestBody = String.format("""
				{
					"email": "%s",
					"password": "example"
				}
				""", email);

		HttpEntity<String> httpEntity = createHttpEntity(null, null, requestBody);
		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/auth/register", HttpMethod.POST, httpEntity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("User registered !");

		// check if it was added to database
		User user = userRepository.findById(email).orElse(null);
		assertThat(user).isNotNull();
		if(user != null) {
			assertThat(user.getEmail()).isEqualTo(email);
			String code = user.getCode();
			assertThat(code).isNotEmpty();

			String modelSubject = "Email verification code";
			String modelContent = "Use the following code to verify your email: " + code;
			UserNotificationDTO modelNotification = new UserNotificationDTO(email, modelSubject, modelContent);

			await().atMost(Duration.ofSeconds(10))
					.untilAsserted(() -> {
						ConsumerRecords<String, String> eventSent = consumer.poll(Duration.ofSeconds(5));
						assertThat(eventSent.count()).isGreaterThan(0);
						eventSent.forEach(record -> {
							assertThat(record.value()).isEqualTo(parseObjectToJson(modelNotification));
						});
						consumer.commitSync();
					});
		}
	}

	@Test
	void shouldGenerateNewCodeAndVerifyEmail() {

		String email = "fred";
		String password = "1234";
		String prevCode = "99999";

		String requestBody = String.format("""
				{
					"email": "%s",
					"password": "%s"
				}
				""", email, password);

		HttpEntity<String> httpEntity = createHttpEntity(null, null, requestBody);
		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/auth/code", HttpMethod.PUT, httpEntity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("New code generated !");

		User user = userRepository.findById(email).orElse(null);
		assertThat(user).isNotNull();

		if(user != null) {
			assertThat(user.getEmail()).isEqualTo(email);
			assertThat(user.getCode()).isNotEqualTo(prevCode);

			String newCode = user.getCode();

			String modelSubject = "New email verification code";
			String modelContent = "Use the following code to verify your email: " + newCode;
			UserNotificationDTO modelNotification = new UserNotificationDTO(email, modelSubject, modelContent);

			await().atMost(Duration.ofSeconds(10))
					.untilAsserted(() -> {
						ConsumerRecords<String, String> eventSent = consumer.poll(Duration.ofSeconds(5));
						assertThat(eventSent.count()).isGreaterThan(0);
						eventSent.forEach(record -> {
							assertThat(record.value()).isEqualTo(parseObjectToJson(modelNotification));
						});
						consumer.commitSync();
					});

			requestBody = String.format("""
				{
					"email": "%s",
					"code": "%s"
				}
				""", email, newCode);

			httpEntity = createHttpEntity(null, null, requestBody);
			response = restTemplate.exchange(baseUrl + "/auth/email/verify", HttpMethod.PUT, httpEntity, String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.hasBody()).isTrue();
			assertThat(response.getBody()).isEqualTo("Email verified !");


			User verifiedUser = userRepository.findById(email).orElse(null);
			assertThat(verifiedUser).isNotNull();
			if(verifiedUser != null) {
				assertThat(verifiedUser.getEmail()).isEqualTo(email);
				assertThat(verifiedUser.isVerified()).isTrue();
			}
		}
	}

	@Test
	void shouldLogin(){
		// successful login
		String correctEmail = "admin";
		String correctPassword = "admin";

		String requestBody = String.format("""
				{
					"email": "%s",
					"password": "%s"
				}
				""", correctEmail, correctPassword);

		HttpEntity<String> httpEntity = createHttpEntity(null, null, requestBody);
		ResponseEntity<LoginResponseDTO> response = restTemplate.exchange(baseUrl + "/auth/login", HttpMethod.POST, httpEntity, LoginResponseDTO.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();

		LoginResponseDTO loginResponseDTO = response.getBody();
		assertThat(loginResponseDTO).isNotNull();
		if(loginResponseDTO != null) {
			assertThat(jwtService.validateToken(loginResponseDTO.token())).isNotNull();
		}

		// failed login
		String wrongEmail = "fred";
		String wrongPassword = "fred";

		requestBody = String.format("""
				{
					"email": "%s",
					"password": "%s"
				}
				""", wrongEmail, wrongPassword);

		httpEntity = createHttpEntity(null, null, requestBody);
		response = restTemplate.exchange(baseUrl + "/auth/login", HttpMethod.POST, httpEntity, LoginResponseDTO.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldUpdatePassword(){

		String modelSubject = "Password updated";
		String modelContent = "Your password was successfully updated !";

		String email = "fred";
		String newPassword = "fred";

		UserNotificationDTO modelNotification = new UserNotificationDTO(email, modelSubject, modelContent);

		HttpEntity<String> httpEntity = createHttpEntity(email, null, null);
		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/password?password=" + newPassword, HttpMethod.PUT, httpEntity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("Password updated !");

		User user = userRepository.findById(email).orElse(null);
		assertThat(user).isNotNull();
		if(user != null) {
			assertThat(user.getEmail()).isEqualTo(email);
			assertThat(bCryptPasswordEncoder.matches(newPassword, user.getPassword())).isTrue();
		}

		await().atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> {
					ConsumerRecords<String, String> eventSent = consumer.poll(Duration.ofSeconds(5));
					assertThat(eventSent.count()).isGreaterThan(0);
					eventSent.forEach(record -> {
						assertThat(record.value()).isEqualTo(parseObjectToJson(modelNotification));
					});
					consumer.commitSync();
				});

	}

	@Test
	void shouldDeleteUser(){
		String modelSubject = "Account deleted";
		String modelContent = "Your account has been deleted !";

		String email = "fred";

		UserNotificationDTO modelNotification = new UserNotificationDTO(email, modelSubject, modelContent);

		HttpEntity<String> httpEntity = createHttpEntity(null, "ADMIN", null);
		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/" + email, HttpMethod.DELETE, httpEntity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("User deleted !");

		User user = userRepository.findById(email).orElse(null);
		assertThat(user).isNull();

		await().atMost(Duration.ofSeconds(20))
				.untilAsserted(() -> {
					ConsumerRecords<String, String> eventSent = consumer.poll(Duration.ofSeconds(5));
					assertThat(eventSent.count()).isGreaterThan(0);
					eventSent.forEach(record -> {
						assertThat(record.value()).isIn(email, parseObjectToJson(modelNotification));
					});
					consumer.commitSync();
				});
	}

	private <T> String parseObjectToJson(T object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private HttpEntity<String> createHttpEntity(String emailHeader, String roleHeader, String requestBody){
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-USER-EMAIL", emailHeader);
		headers.set("X-USER-ROLE", roleHeader);

		return new HttpEntity<>(requestBody, headers);
	}
}

