
# E-Commerce Microservice
A Scalable E-Commerce System designed with a Microservice Architecture, benefiting from its key advantages such as scalability, fault isolation, and flexibility. The system ensures asynchronous communication between services while maintaining availability and performance. The system interacts with users through an integrated email service, facilitating notifications, order updates, and transactional communication.


## Technologies
- Java – Primary backend language.

- Spring Boot – Builds the microservices' RESTful APIs and manages caching efficiently.

- Spring Cloud – Configures the API Gateway to ensure secure and efficient routing between microservices.

- Service Discovery – Implemented with Spring Eureka for dynamic service registration, load balancing, and system scalability.

- Kafka – Enables asynchronous communication between microservices, improving fault tolerance, scalability, and overall performance.

- MySQL – Integrated with Spring Data JPA for structured data persistence and optimized querying.

- MongoDB – Utilized via Spring Data MongoDB for flexible, high-performance NoSQL data management.

- Redis – Integrated with Spring Data Redis to cache API responses, improve performance, and ensure rate limiting.

- Stripe – Handles real-time payment processing via Stripe Client integration.

- Swagger – Used for API documentation to enhance development and integration processes.

- Docker – Facilitates dependency management, ensures portability, and simplifies scalability across environments by containerizing the services.

## Security
- JWT: Authentication and Authorization based on generated JWT tokens to securely transmit information to the API.

- Email Verification: Registered users must verify their email to use the system, verification code sent by email. 

- Circuit Breaker: Integrated with Spring Cloud Circuit Breaker to ensure system resilience by effectively handling service failures.

- Rate Limiter: Integrated with Spring Cloud Rate Limiter to avoid overloading services by limiting the amount of requests to the API.


## Testing

- Integration tests are configured using the TestContainers library, which uses Docker containers to simulate real environments. This approach ensures more robust testing by providing a realistic and isolated execution environment, ensuring reliability and accuracy.


# Run Locally

## Prerequisites
- Docker
- Git

### 1. Clone the Repository
```
git clone https://github.com/your-username/e-commerce-microservices.git
cd e-commerce-microservices
```
### 2. Configure .env
Create a `.env` file with the required data found in `.env.example`

### 3. Run
```
docker compose up --build
```
- API Gateway will be accessible on : `http://localhost:9000/webjars/swagger-ui/index.html`

## Contact
Email: alvarocampioni@usp.br


