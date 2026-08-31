FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace
COPY . .
RUN ./gradlew :ogonggo-api-admin:bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /workspace/ogonggo-api-admin/build/libs/ogonggo-api-admin.jar app.jar
ENV TZ=Asia/Seoul

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java -Duser.timezone=${TZ} -jar /app/app.jar"]
