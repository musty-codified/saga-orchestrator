FROM eclipse-temurin:17-jre-alpine

EXPOSE 443 8088 9779

ENV JAVA_OPTS=""
ARG JAR_FILE=./target/*.jar
COPY ${JAR_FILE} /usr/notification.jar
WORKDIR /usr/
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /usr/notification.jar"]



