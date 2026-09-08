# Ogonggo Server

Ogonggo API server is a Kotlin/Spring Boot multi-module project aligned with the existing LetsCareer server platform.

## Runtime baseline

- Java 17
- Kotlin 1.9.23
- Gradle 8.7
- Spring Boot 3.2.5
- Spring Dependency Management 1.1.4
- QueryDSL 5.0.0
- MySQL
- Redis (사용자 리프레시 토큰)
- Hibernate `ddl-auto=update`
- No Flyway or Liquibase

## Modules

```text
ogonggo-api-user  ---> ogonggo-core <--- ogonggo-api-admin
```

- `ogonggo-core`: user, job, bootcamp and study domain boundaries; JPA persistence
- `ogonggo-api-user`: public API and LetsCareer login integration
- `ogonggo-api-admin`: administrator API boundary

`ogonggo-api-user` and `ogonggo-api-admin` are independent Spring Boot applications and cannot depend on each other.

## Architecture

The API modules own their actor-specific Presentation and Business layers. `ogonggo-core` provides the shared Domain, Implement, and Data Access layers.

Read [오공고 레이어와 모듈 아키텍처](docs/architecture/layers-and-modules.md) before adding a domain feature or changing module dependencies.

## Run tests

Use JDK 17 to run Gradle as well as to compile the applications.

```bash
./gradlew test
```

On macOS, when another JDK is the shell default:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew test
```

## Run locally

Start MySQL and Redis only:

```bash
docker compose up -d mysql redis
```

Start each application:

```bash
./gradlew :ogonggo-api-user:bootRun
./gradlew :ogonggo-api-admin:bootRun
```

- User health check: `GET http://localhost:8080/health`
- Admin health check: `GET http://localhost:8081/health`

`ogonggo-api-user` requires these environment variables:

```text
JWT_SECRET=<base64-encoded HS512 secret, must differ from the LetsCareer secret>
LETSCAREER_BASE_URL=http://localhost:8090
LETSCAREER_INTERNAL_API_KEY=<same value as the LetsCareer server>
```

Users sign in by exchanging a LetsCareer access token at `POST /api/v1/auth/letscareer`. Read [오공고 사용자 인증과 렛츠커리어 연동](docs/architecture/authentication.md) before changing anything in that flow.

Administrator endpoints are still denied until the administrator authentication filter is implemented.

## Docker

이미지는 미리 빌드된 jar를 복사만 합니다. 컨테이너 안에서 Gradle을 돌리지 않으므로 jar를 먼저 만들어야 합니다.

```bash
./gradlew :ogonggo-api-user:bootJar :ogonggo-api-admin:bootJar
```

```bash
docker compose up --build
```

CI는 러너에서 Gradle 의존성 캐시를 사용해 jar를 만든 뒤 `JAR_FILE` 빌드 인자로 경로를 넘깁니다.

## Deployment

`main` 브랜치에 푸시하면 `ogonggo-api-user` → `ogonggo-api-admin` 순서로 ECS에 배포합니다.

운영 설정은 저장소에 없고 GitHub 시크릿(`APPLICATION_SECRET_USER`, `APPLICATION_SECRET_ADMIN`)의 내용을 그대로 `application.yml`로 씁니다. 잘못된 시크릿이 운영 서비스를 죽이지 못하도록, 배포 워크플로는 이미지를 ECR에 올리기 전에 시크릿·설정 계약·AWS 리소스를 검사하고 운영 설정 그대로 컨테이너를 띄워 `/health` 200을 확인합니다. 그래도 배포가 실패하면 직전 태스크 정의로 되돌립니다.

설정 키를 추가하거나 이름을 바꾸면 `.github/config-schema/<모듈>.yml`의 계약도 같은 작업에서 고칩니다. 계약에 없는 키가 설정 파일에 있으면 배포가 멈춥니다.

자세한 내용은 [오공고 배포 검증](docs/infra/ci-cd-validation.md)을 읽습니다.

## Schema management

The project intentionally follows the current LetsCareer approach and does not include a migration tool. Configure schema behavior with `DDL_AUTO`:

```text
DDL_AUTO=update
```

When both ECS services share one database, avoid concurrent schema updates during deployment. Assign schema changes to one deployment step or switch production services to `DDL_AUTO=validate` after the schema is prepared.

Hibernate `update`는 기존 컬럼의 이름 변경이나 제거를 안전하게 처리하지 않습니다. 기존 DB에 파괴적 스키마 변경을 적용해야 할 때는 `docs/schema`의 날짜별 SQL을 검토하고 백업 후 한 번만 실행합니다. 신규 DB에는 Hibernate가 최종 스키마를 생성하므로 기존 스키마 전환 SQL을 실행하지 않습니다.

## Study domain

The Study package boundary exists, but no Study entity is defined yet because the study page behavior and fields have not been specified.
