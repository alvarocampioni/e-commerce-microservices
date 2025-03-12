package com.ms.api_gateway;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.redis.testcontainers.RedisContainer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.base.Supplier;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApiGatewayApplicationTests {

	@Container
	@ServiceConnection
	static RedisContainer redisContainer = new RedisContainer("redis:6.2.2");

	@Container
	static MockServerContainer eurekaContainer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver"));

	static MockWebServer mockWebServer;

	@DynamicPropertySource
	static void setTestProperties(DynamicPropertyRegistry registry) {
		eurekaContainer.start();
		Supplier<Object> supplier = () -> false;
		registry.add("eureka.client.service-url.defaultZone", () -> "http://" + eurekaContainer.getHost() + ":" + eurekaContainer.getServerPort() + "/eureka");
		System.out.println(eurekaContainer.getHost() + ":" + eurekaContainer.getServerPort());

		registry.add("JWT.SECRET.KEY", () -> "key");
		registry.add("spring.data.redis.port", redisContainer::getRedisPort);
		registry.add("spring.data.redis.url", redisContainer::getRedisURI);
	}

	@Autowired
	TestRestTemplate restTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@LocalServerPort
	int port;

	String baseUrl;

	@BeforeAll
	static void beforeAll() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
	}

	@BeforeEach
	void setUp() {
		baseUrl = "http://localhost:" + port;
	}

	@AfterEach
	void cleanUp() {
	}

	@AfterAll
	static void tearDown() throws IOException {
		//mockWebServer.shutdown();
	}

	@Test
	void testProductServiceRouting() throws Exception {
		String productServiceResponse = String.format("{\"application\":{\"name\":\"PRODUCT-SERVICE\",\"instance\":[{\"instanceId\":\"product-1\",\"hostName\":\"localhost\",\"port\":{\"$\":%d},\"status\":\"UP\"}]}}", mockWebServer.getPort());
		System.out.println(mockWebServer.getPort());

		// Mock the /eureka/apps/PRODUCT-SERVICE endpoint
		try (MockServerClient client = new MockServerClient(eurekaContainer.getHost(), eurekaContainer.getServerPort())) {
			client.when(request().withPath("/eureka/apps/PRODUCT-SERVICE"))
					.respond(response().withBody(productServiceResponse));
			Thread.sleep(5000);
			mockWebServer.enqueue(new MockResponse()
					.setBody("{\"message\": \"User found\"}")
					.setResponseCode(200));

			ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/api/product", String.class);
			assertThat(response.getStatusCode()).isEqualTo(200);
		}
	}
}