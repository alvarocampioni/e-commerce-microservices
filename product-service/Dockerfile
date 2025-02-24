FROM openjdk:23
COPY ./target/product-service-0.0.1-SNAPSHOT.jar /app/product-service-0.0.1-SNAPSHOT.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "product-service-0.0.1-SNAPSHOT.jar"]
EXPOSE 8080
