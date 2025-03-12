package com.ms.comment_service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.comment_service.exception.ResourceNotFoundException;
import com.ms.comment_service.model.Comment;
import com.ms.comment_service.model.Product;
import com.ms.comment_service.repository.CommentRepository;
import com.ms.comment_service.repository.ProductRepository;
import com.ms.comment_service.service.CommentCacheService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
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

import java.time.Duration;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CommentServiceApplicationTests {

	@Container
	@ServiceConnection
	static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.4.4"));

	@Container
	@ServiceConnection
	static ConfluentKafkaContainer kafkaContainer = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

	@Autowired
	KafkaTemplate<String, String> kafkaTemplate;

	@Container
	@ServiceConnection
	static RedisContainer redisContainer = new RedisContainer("redis:6.2.2");
    @Autowired
    private ObjectMapper objectMapper;

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

	@Autowired
	ProductRepository productRepository;

	@Autowired
	CommentRepository commentRepository;

	@Autowired
	CommentCacheService commentCacheService;

	@LocalServerPort
	int port;

	String baseUrl;

	String createdProductTopic = "created-product";
	String deletedProductTopic = "deleted-product";
	String userDeletedTopic = "user-deleted";

	@BeforeEach
	void setUp() {
		baseUrl = "http://localhost:" + port + "/api/comment";

		productRepository.save(new Product("phone"));
		productRepository.save(new Product("apple"));

		commentRepository.save(new Comment("1", "phone", "jhon", "good phone", new Date(), new Date()));
		commentRepository.save(new Comment("2", "apple", "fred", "sweet", new Date(), new Date()));
		commentRepository.save(new Comment("3", "apple", "jhon", "good taste", new Date(), new Date()));
	}

	@AfterEach
	void cleanUp() {
		commentRepository.deleteAll();
		productRepository.deleteAll();
	}

	@Test
	void shouldPostComment(){
		String customerId = "greg";
		String productId = "phone";
		String content = "new comment";
		String requestBody = String.format("""
				{
					"content": "%s"
				}
				""", content);

		HttpEntity<String> httpEntity = createHttpEntity(customerId, null, requestBody);

		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/product/" + productId, HttpMethod.POST, httpEntity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("Comment posted !");

		// check if it was added to the database
		List<Comment> comments = commentCacheService.getCommentsByProductId(productId);
		assertThat(comments.size()).isEqualTo(2);

		boolean isCreated = comments.stream().anyMatch(comment -> comment.getContent().equals(content));
		assertThat(isCreated).isTrue();
	}

	@Test
	void shouldNotPostComment(){
		String customerId = "greg";
		String productId = "invalidProductId";
		String content = "new comment";
		String requestBody = String.format("""
				{
					"content": "%s"
				}
				""", content);

		HttpEntity<String> httpEntity = createHttpEntity(customerId, null, requestBody);

		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/product/" + productId, HttpMethod.POST, httpEntity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		// ensure it was not added to the database
		List<Comment> comments = commentCacheService.getCommentsByProductId(productId);
		assertThat(comments.isEmpty()).isTrue();
	}

	@Test
	void shouldUpdateComment(){
		String customerId = "jhon";
		String commentId = "1";
		String content = "updated comment";

		String requestBody = String.format("""
				{
					"content": "%s"
				}
				""", content);

		HttpEntity<String> httpEntity = createHttpEntity(customerId, null, requestBody);

		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/me/" + commentId , HttpMethod.PUT, httpEntity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("Comment updated !");

		// check if it was updated in the database
		Comment updatedComment = commentCacheService.getCommentById(commentId);
		assertThat(updatedComment.getContent()).isEqualTo(content);
	}

	@Test
	void shouldDeleteCommentsAfterListeningEvent(){
		String deletedCustomerId = "jhon";

		kafkaTemplate.send(userDeletedTopic, deletedCustomerId);

		await().pollInterval(Duration.ofSeconds(2))
				.atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> {
					List<Comment> comments = commentCacheService.getCommentsByCustomerId(deletedCustomerId);
					assertThat(comments.isEmpty()).isTrue();
				});
	}

	@Test
	void shouldDeleteCommentsByCustomerId(){
		String customerId = "jhon";

		HttpEntity<String> httpEntity = createHttpEntity(customerId, null, null);

		ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/me" , HttpMethod.DELETE, httpEntity, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getBody()).isEqualTo("Comments deleted !");

		List<Comment> comments = commentCacheService.getCommentsByCustomerId(customerId);
		assertThat(comments.isEmpty()).isTrue();
	}

	@Test
	void shouldDeleteCommentById(){
		String commentId = "1";

		String wrongCustomerId = "greg";
		String correctCustomerId = "jhon";

		HttpEntity<String> httpEntity = createHttpEntity(wrongCustomerId, null, null);

		// wrong customerId
		ResponseEntity<String> failResponse = restTemplate.exchange(baseUrl + "/me/" + commentId , HttpMethod.DELETE, httpEntity, String.class);
		assertThat(failResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		// assert it was not removed
		Comment comment = commentCacheService.getCommentById(commentId);
		assertThat(comment).isNotNull();
		assertThat(comment.getContent()).isEqualTo("good phone");

		httpEntity = createHttpEntity(correctCustomerId, null, null);

		//correct customerId
		ResponseEntity<String> successResponse = restTemplate.exchange(baseUrl + "/me/" + commentId , HttpMethod.DELETE, httpEntity, String.class);
		assertThat(successResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(successResponse.hasBody()).isTrue();
		assertThat(successResponse.getBody()).isEqualTo("Comment deleted !");

		// assert it was removed
		assertThatThrownBy(() -> commentCacheService.getCommentById(commentId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("No comment found for ID: " + commentId);
	}

	@Test
	void shouldDeleteAnyCommentById(){
		String commentId = "1";
		String adminRole = "ADMIN";
		String userRole = "CUSTOMER";

		HttpEntity<String> httpEntity = createHttpEntity(null, userRole, null);

		// unauthorized request
		ResponseEntity<String> failResponse = restTemplate.exchange(baseUrl + "/" + commentId , HttpMethod.DELETE, httpEntity, String.class);
		assertThat(failResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		// assert it was not removed
		Comment comment = commentCacheService.getCommentById(commentId);
		assertThat(comment).isNotNull();
		assertThat(comment.getContent()).isEqualTo("good phone");

		httpEntity = createHttpEntity(null, adminRole, null);

		// authorized request
		ResponseEntity<String> successResponse = restTemplate.exchange(baseUrl + "/" + commentId , HttpMethod.DELETE, httpEntity, String.class);
		assertThat(successResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(successResponse.hasBody()).isTrue();
		assertThat(successResponse.getBody()).isEqualTo("Comment deleted !");

		// assert it was removed
		assertThatThrownBy(() -> commentCacheService.getCommentById(commentId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("No comment found for ID: " + commentId);
	}

	@Test
	void shouldFetchCommentsByProductId(){
		String productId = "apple";

		ResponseEntity<Comment[]> response = restTemplate.getForEntity(baseUrl + "/product/" + productId, Comment[].class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();

		Comment[] comments = response.getBody();
		assertThat(comments).isNotNull();

		if(comments != null) {
			assertThat(comments.length).isEqualTo(2);
			assertThat(comments[0].getContent()).isEqualTo("sweet");
			assertThat(comments[1].getContent()).isEqualTo("good taste");
		}
	}

	@Test
	void shouldFetchCommentsByCustomerId(){
		String customerId = "fred";

		HttpEntity<String> httpEntity = createHttpEntity(customerId, null, null);

		ResponseEntity<Comment[]> response = restTemplate.exchange(baseUrl + "/me", HttpMethod.GET, httpEntity, Comment[].class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();

		Comment[] comments = response.getBody();
		assertThat(comments).isNotNull();

		if(comments != null) {
			assertThat(comments.length).isEqualTo(1);
			assertThat(comments[0].getContent()).isEqualTo("sweet");
		}
	}

	@Test
	void shouldFetchCommentByProductIdAndCustomerId(){
		String customerId = "fred";
		String productId = "apple";

		HttpEntity<String> httpEntity = createHttpEntity(customerId, null, null);

		ResponseEntity<Comment[]> response = restTemplate.exchange(baseUrl + "/me/product/" + productId, HttpMethod.GET, httpEntity, Comment[].class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();

		Comment[] comments = response.getBody();
		assertThat(comments).isNotNull();

		if(comments != null) {
			assertThat(comments.length).isEqualTo(1);
			assertThat(comments[0].getContent()).isEqualTo("sweet");
		}
	}

	@Test
	void shouldFetchCommentById(){
		String commentId = "3";

		ResponseEntity<Comment> response = restTemplate.getForEntity(baseUrl + "/" + commentId, Comment.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();

		Comment comment = response.getBody();
		assertThat(comment).isNotNull();

		if(comment != null) {
			assertThat(comment.getContent()).isEqualTo("good taste");
		}
	}

	@Test
	void shouldAddProduct(){
		String newProductId = "hammer";
		Product product = new Product(newProductId);
		kafkaTemplate.send(createdProductTopic, parseObjectToJson(product));

		await().pollInterval(Duration.ofSeconds(2))
				.atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> {
					Product fetched = productRepository.findById(newProductId).orElse(null);
					assertThat(fetched).isNotNull();
					if(fetched != null) {
						assertThat(fetched.getProductId()).isEqualTo(newProductId);
					}
				});
    }

	@Test
	void shouldDeleteProduct(){
		String deletedProductId = "phone";
		Product product = new Product(deletedProductId);

		kafkaTemplate.send(deletedProductTopic, parseObjectToJson(product));

		await().pollInterval(Duration.ofSeconds(2))
				.atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> {
					Product fetched = productRepository.findById(deletedProductId).orElse(null);
					assertThat(fetched).isNull();
				});

		// check if product comments where also deleted
		List<Comment> comments = commentCacheService.getCommentsByProductId(deletedProductId);
		assertThat(comments).isNotNull();
		assertThat(comments.isEmpty()).isTrue();
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

