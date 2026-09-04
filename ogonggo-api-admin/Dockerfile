# 미리 빌드된 jar를 받아 실행만 하는 이미지.
#
# jar 빌드는 CI 러너가 담당한다. 러너에서 Gradle 의존성 캐시를 쓰면
# 컨테이너 안에서 매번 새로 받는 것보다 배포가 크게 빨라진다.
#
# 로컬에서 이미지를 만들 때는 jar를 먼저 빌드해야 한다.
#   ./gradlew :ogonggo-api-admin:bootJar
#   docker build -f ogonggo-api-admin/Dockerfile -t ogonggo-api-admin .
FROM eclipse-temurin:17-jre

WORKDIR /app
ARG JAR_FILE=ogonggo-api-admin/build/libs/ogonggo-api-admin.jar
COPY ${JAR_FILE} app.jar
ENV TZ=Asia/Seoul

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java -Duser.timezone=${TZ} -jar /app/app.jar"]
