FROM openjdk:23
COPY ./target/order-service-0.0.1-SNAPSHOT.jar /app/order-service-0.0.1-SNAPSHOT.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "order-service-0.0.1-SNAPSHOT.jar"]
EXPOSE 8082