FROM eclipse-temurin:17-jre-alpine

EXPOSE 8086 8087 443 8088 9779

ENV JAVA_OPTS=""
ARG JAR_FILE=./target/*.jar
COPY ${JAR_FILE} /usr/order.jar
WORKDIR /usr/
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /usr/order.jar"]



