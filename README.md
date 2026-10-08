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
- Hibernate `ddl-auto=none` in production (schema changes are applied to the production DB before deploy)
- No Flyway or Liquibase

## Modules

```text
ogonggo-api-user  ---> ogonggo-core <--- ogonggo-api-admin
```

- `ogonggo-core`: user, job, bootcamp, recruitment post and announcement domain boundaries; JPA persistence
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

Administrator console endpoints (`/api/v1/admin/**`) accept a user API access token whose account has `UserRole.ADMIN`, granted directly in the database. `ogonggo-api-admin` must be configured with the same `ogonggo.auth.jwt.secret` as the user API. Read section 7-3 of [오공고 사용자 인증과 렛츠커리어 연동](docs/architecture/authentication.md) before changing it.

## Work24 (고용24) Open API

관리자 API의 `GET /api/v1/admin/work24/{apiName}`가 고용24 Open API를 대신 호출합니다. 인증키는 사용 신청한 서비스마다 따로 발급되므로 서비스별로 넣습니다. 비워 두면 해당 서비스 호출만 503으로 실패합니다.

```yaml
ogonggo:
  work24:
    recruitment-auth-key:              # 채용정보
    tomorrow-learning-card-auth-key:   # 국민내일배움카드 훈련과정
    work-study-auth-key:               # 일학습병행 훈련과정
    government-job-auth-key:           # 정부지원일자리정보
    job-seeker-program-auth-key:       # 구직자취업역량 강화프로그램
    occupation-auth-key:               # 직업정보
    duty-auth-key:                     # 직무정보
    small-giant-company-auth-key:      # 강소기업
    common-code-auth-key:              # 공통코드(채용 지역·직종, 훈련 KECO·NCS 등)
```

키 이름은 배포 로그 마스킹이 가리도록 모두 `-auth-key`로 끝냅니다. 응답 계약은 [API 성공 응답](docs/architecture/api-response.md#고용24-open-api-조회)을 읽습니다.

관리자 API는 매일 04:00(Asia/Seoul)에 고용24 채용정보와 일학습병행 훈련과정을 채용공고로, K-디지털 트레이닝 조건의 국민내일배움카드 훈련과정을 부트캠프로 새 항목만 등록합니다. 시각과 켜짐 여부는 `scheduled_jobs`의 `work24DailyCollection` 행으로 바꿉니다. 고용24 Open API는 과정 이미지를 주지 않아, 훈련기관 소개 화면의 로고와 사진을 관리자 API가 이미지 저장소(`cloud.aws.s3.bucket`)로 옮겨 넣습니다. 로고는 로고 칸에, 사진은 상세에서만 보여 주는 `bootcamp_images`에 들어가며 대표 이미지는 비워 두어 클라이언트가 기본 이미지를 그립니다. 저장소 설정이 없거나 옮기지 못하면 이미지를 넣지 않습니다. 훈련목표·교과편성·주차별 커리큘럼과 일학습병행의 학습기업·훈련 편성은 Open API에 없어 고용24 과정 상세 화면과 시간표 엑셀에서 읽으며, 화면을 못 읽으면 Open API 값만으로 등록합니다. 수집한 콘텐츠는 등록 경로 `WORK24`와 고용24 식별값(`external_id`)을 남깁니다. 등록 규칙은 [고용24 일일 수집](docs/architecture/api-response.md#고용24-일일-수집)을 읽습니다.

## Scheduled jobs

스케줄 작업의 실행 주기(cron)와 켜짐 여부는 DB `scheduled_jobs` 테이블에서 SQL로 바꿉니다. 바꾼 cron은 1분 안에 반영되고, 행은 애플리케이션이 기동할 때 없는 작업만 기본값으로 만들어집니다. 규칙과 작업 목록은 [스케줄 작업](docs/architecture/scheduling.md)을 읽습니다.

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

`main` 브랜치에 푸시하면 `ogonggo-api-user`와 `ogonggo-api-admin`을 ECS에 병렬로 배포합니다. 두 서비스 모두 운영 `ddl-auto`가 `none`이라 기동하면서 공유 DB의 스키마를 바꾸지 않기 때문입니다.

운영 설정은 저장소에 없고 GitHub 시크릿(`APPLICATION_SECRET_USER`, `APPLICATION_SECRET_ADMIN`)의 내용을 그대로 `application.yml`로 씁니다. 잘못된 시크릿이 운영 서비스를 죽이지 못하도록, 배포 워크플로는 이미지를 ECR에 올리기 전에 시크릿·AWS 리소스를 확인하고 운영 설정으로 컨테이너를 띄워 `/health` 200을 확인합니다. 그래도 배포가 실패하면 직전 태스크 정의로 되돌립니다.

설정 파일 내용은 배포 전에 따로 검사하지 않습니다. 설정 키를 추가하거나 바꿔도 배포 쪽을 함께 고칠 필요는 없습니다.

자세한 내용은 [오공고 배포 검증](docs/infra/ci-cd-validation.md)을 읽습니다.

## Schema management

The project intentionally follows the current LetsCareer approach and does not include a migration tool.

운영의 두 API는 모두 `spring.jpa.hibernate.ddl-auto=none`으로 띄웁니다. 두 서비스가 하나의 DB를 공유하므로, 어느 한쪽이라도 `update`로 뜨면 병렬 배포 중 동시에 스키마를 바꿀 수 있습니다. 운영 시크릿(`APPLICATION_SECRET_USER`, `APPLICATION_SECRET_ADMIN`)에 `none` 외의 값을 넣지 않습니다.

엔티티를 바꿔 스키마가 달라지면 두 API를 배포하기 전에 운영 DB에 변경을 한 번 적용합니다. 스키마 변경 SQL은 저장소에 두지 않습니다. `none`은 스키마를 검사하지도 않으므로, SQL을 빠뜨리면 기동은 되고 해당 칼럼·테이블을 쓰는 요청에서 오류가 납니다.

로컬과 테스트는 빈 DB에서 시작하므로 `update`나 `create-drop`을 씁니다.

Hibernate `update`는 기존 컬럼의 이름 변경이나 제거를 안전하게 처리하지 않습니다. 기존 DB에 파괴적 스키마 변경을 적용해야 할 때는 백업 후 한 번만 실행합니다. 로컬처럼 `update`로 새로 만든 DB에는 Hibernate가 최종 스키마를 생성하므로 기존 스키마 전환을 실행하지 않습니다.
