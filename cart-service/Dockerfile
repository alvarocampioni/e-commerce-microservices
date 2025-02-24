FROM openjdk:23
COPY ./target/cart-service-0.0.1-SNAPSHOT.jar /app/cart-service-0.0.1-SNAPSHOT.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "cart-service-0.0.1-SNAPSHOT.jar"]
EXPOSE 8084