FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace
COPY . .
RUN ./gradlew :ogonggo-api-user:bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /workspace/ogonggo-api-user/build/libs/ogonggo-api-user.jar app.jar
ENV TZ=Asia/Seoul

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Duser.timezone=${TZ} -jar /app/app.jar"]
