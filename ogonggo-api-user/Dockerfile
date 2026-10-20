# syntax=docker/dockerfile:1.7
# 미리 빌드된 jar를 받아 실행만 하는 이미지.
#
# jar 빌드는 CI 러너가 담당한다. 러너에서 Gradle 의존성 캐시를 쓰면
# 컨테이너 안에서 매번 새로 받는 것보다 배포가 크게 빨라진다.
#
# 로컬에서 이미지를 만들 때는 jar를 먼저 빌드해야 한다.
#   ./gradlew :ogonggo-api-user:bootJar
#   docker build -f ogonggo-api-user/Dockerfile -t ogonggo-api-user .
FROM eclipse-temurin:17-jre

WORKDIR /app

# Datadog APM 트레이서. 버전을 올릴 때는 checksum도 함께 바꾼다(docs/infra/datadog.md).
# 에이전트(datadog-agent 사이드카)가 없으면 트레이스만 버려지고 애플리케이션은 그대로 동작한다.
ADD --checksum=sha256:5f0eb51160fade367d97404624561b6666f7475fb1453a7a73237eb643e398d8 \
    https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/1.66.0/dd-java-agent-1.66.0.jar \
    /app/dd-java-agent.jar

ARG JAR_FILE=ogonggo-api-user/build/libs/ogonggo-api-user.jar
COPY ${JAR_FILE} app.jar
ENV TZ=Asia/Seoul

# 배포한 커밋. Datadog에서 배포 전후를 비교하는 기준이 된다. CI가 github.sha를 넘긴다.
# 로그는 JSON 한 줄씩 남긴다. 여러 줄짜리 시작 배너는 Datadog에서 줄마다 다른 로그로 쪼개지므로 끈다.
ARG GIT_SHA=local
ENV DD_SERVICE=ogonggo-api-user \
    DD_VERSION=${GIT_SHA} \
    DD_LOGS_INJECTION=true \
    LOG_FORMAT=json \
    SPRING_MAIN_BANNER_MODE=off

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -javaagent:/app/dd-java-agent.jar -Duser.timezone=${TZ} -jar /app/app.jar"]
