# 오공고 Datadog 로그·APM 연동

- 상태: Proposed
- 결정일: 2026-10-20
- 적용 범위: `ogonggo-api-user`, `ogonggo-api-admin`의 ECS Fargate 운영 환경
- 리뷰 상태: 팀 리뷰 필요

이 문서는 두 API의 로그와 요청 추적(APM)을 Datadog으로 보내는 구성을 설명합니다. 로그 형식, 이미지의 트레이서,
ECS 태스크 정의의 사이드카를 바꾸기 전에 읽습니다.

## 범위

- 이번 범위는 **로그와 APM**입니다.
- 커스텀 메트릭(`NotificationMetrics`, `SchedulerExecutionObserver`가 기록하는 `ogonggo.*`)은 아직 보내지 않습니다.
  나중에 `ogonggo-observability` 모듈을 만들어 보냅니다(확인 필요). 그 전까지 이 값들은 앱 메모리에만 남습니다.

## 구성

```text
ECS Fargate 태스크
┌────────────────────────────────────────────────────────────┐
│ 앱 컨테이너 (Spring Boot + dd-java-agent)                  │
│   ├─ 트레이스 ──> datadog-agent 사이드카 (localhost:8126) ──┼──> Datadog APM
│   └─ stdout JSON 로그 ──> log_router 사이드카 (FireLens)  ──┼──> Datadog Logs
└────────────────────────────────────────────────────────────┘
Secrets Manager의 Datadog API 키 ──> 두 사이드카
```

- Datadog은 SaaS라서 서버를 따로 띄우지 않습니다. 태스크마다 사이드카 두 개만 함께 뜹니다.
- 로그 한 줄마다 `dd.trace_id`·`dd.span_id`가 붙으므로 Datadog에서 트레이스와 그 요청의 로그가 이어집니다.
- 배포 워크플로는 태스크 정의에서 앱 컨테이너의 이미지만 바꿉니다. 사이드카와 환경변수는 콘솔에서 한 번 넣으면
  이후 배포에서도 그대로 유지됩니다.

## 저장소가 맡는 것

| 위치 | 하는 일 |
| --- | --- |
| `ogonggo-logging` 모듈의 `logback-spring.xml` | `LOG_FORMAT=json`이면 한 줄에 JSON 하나로 남기고 MDC 값을 필드로 내보냅니다. 그 밖의 값이나 비어 있으면 Spring Boot 기본 텍스트로 남깁니다. 값을 잘못 넣어도 로그가 사라지지 않게 하려는 것입니다. |
| 각 API의 `Dockerfile` | `dd-java-agent`를 받아 `-javaagent`로 붙이고 아래 환경변수를 기본값으로 둡니다. |
| `deploy-ecs.yml` | 이미지에 커밋 해시(`GIT_SHA`)를 넘기고, 기동 검증 컨테이너에서는 트레이서를 끕니다. |
| `docker-compose.yml` | 로컬에는 에이전트가 없으므로 트레이서를 끕니다. |

로그를 찍는 코드는 바뀌지 않습니다. 모든 모듈이 지금처럼 SLF4J로 남깁니다.

### 이미지에 들어 있는 환경변수

| 이름 | 값 | 설명 |
| --- | --- | --- |
| `DD_SERVICE` | `ogonggo-api-user` / `ogonggo-api-admin` | Datadog의 서비스 이름 |
| `DD_VERSION` | 배포한 커밋 해시 | 배포 전후 비교 기준. 로컬 빌드는 `local` |
| `DD_LOGS_INJECTION` | `true` | 로그 MDC에 트레이스 식별자를 넣음 |
| `LOG_FORMAT` | `json` | 컨테이너에서는 항상 JSON |
| `SPRING_MAIN_BANNER_MODE` | `off` | 여러 줄짜리 시작 배너가 Datadog에서 줄마다 다른 로그로 쪼개지지 않게 함 |

`DD_ENV`는 이미지에 넣지 않고 태스크 정의에서 넣습니다. 같은 이미지를 다른 환경에 띄울 수 있게 하려는 것입니다.

에이전트가 없어도 애플리케이션은 그대로 동작합니다. 트레이서가 연결 실패 경고를 남기고 트레이스를 버릴 뿐입니다.
그래서 이미지를 먼저 배포하고 사이드카는 나중에 붙여도 됩니다.

## 콘솔에서 할 일

저장소 변경을 배포한 뒤 진행합니다. 관리자 API에 먼저 적용하고, 트레이스와 로그가 들어오는지 확인한 다음
사용자 API에 적용합니다.

### 1. API 키

1. Datadog에서 API 키를 만들고 Secrets Manager에 저장합니다.
2. 태스크 실행 역할(execution role)에 그 시크릿의 `secretsmanager:GetSecretValue` 권한을 줍니다.

배포 워크플로의 **태스크 정의 점검** 단계가 태스크 정의에 적힌 시크릿 참조가 실제로 있는지 확인하므로,
ARN을 잘못 넣으면 배포 전에 걸립니다.

### 2. `datadog-agent` 사이드카

```json
{
  "name": "datadog-agent",
  "image": "public.ecr.aws/datadog/agent:<고정한 7.x 버전>",
  "essential": false,
  "environment": [
    { "name": "ECS_FARGATE", "value": "true" },
    { "name": "DD_SITE", "value": "<Datadog 사이트>" },
    { "name": "DD_APM_ENABLED", "value": "true" },
    { "name": "DD_APM_IGNORE_RESOURCES", "value": "GET /health" }
  ],
  "secrets": [
    { "name": "DD_API_KEY", "valueFrom": "<API 키 시크릿 ARN>" }
  ]
}
```

- `essential: false`로 둡니다. 에이전트가 죽어도 앱 태스크까지 내려가지 않게 하려는 것입니다.
- `DD_APM_IGNORE_RESOURCES`는 ALB 헬스 체크 요청이 트레이스로 쌓이지 않게 합니다.

### 3. `log_router` 사이드카와 앱의 로그 드라이버

```json
{
  "name": "log_router",
  "image": "public.ecr.aws/aws-observability/aws-for-fluent-bit:stable",
  "essential": true,
  "firelensConfiguration": {
    "type": "fluentbit",
    "options": { "enable-ecs-log-metadata": "true" }
  }
}
```

앱 컨테이너의 `logConfiguration`을 바꿉니다.

```json
{
  "logDriver": "awsfirelens",
  "options": {
    "Name": "datadog",
    "Host": "http-intake.logs.<Datadog 사이트>",
    "TLS": "on",
    "provider": "ecs",
    "dd_service": "ogonggo-api-user",
    "dd_source": "java",
    "dd_tags": "env:prod"
  },
  "secretOptions": [
    { "name": "apikey", "valueFrom": "<API 키 시크릿 ARN>" }
  ]
}
```

- 로그 드라이버를 바꾸면 CloudWatch에는 로그가 더 이상 쌓이지 않습니다.
- `log_router`를 `essential: true`로 두면, 로그 전송기가 죽었을 때 로그 없이 앱만 도는 상황 대신 태스크가 교체됩니다.

### 4. 앱 컨테이너 환경변수

| 이름 | 값 |
| --- | --- |
| `DD_ENV` | `prod` |

나머지 `DD_*` 값은 이미지에 들어 있습니다.

### 5. 태스크 크기

사이드카 두 개가 합쳐 대략 0.25 vCPU, 메모리 0.5GB 안팎을 씁니다. 지금 태스크의 CPU·메모리에 여유가 없으면
한 단계 올립니다. 태스크 크기는 배포가 바꾸지 않는 값이므로 콘솔에서 관리합니다.

### 6. 비용 관리

- 로그 색인(index)에 **일일 한도(daily quota)**를 겁니다. 로그가 폭증해도 그날 비용이 한도를 넘지 않습니다.
- 검색할 필요 없는 로그는 **제외 필터(exclusion filter)**로 색인에서 뺍니다. 수집 비용만 남습니다.
- 사용량 알림을 걸어 예상 비용이 기준을 넘으면 알게 합니다.

## 트레이서 버전 올리기

두 Dockerfile의 `ADD --checksum=... dd-java-agent-<버전>.jar`를 함께 바꿉니다.
checksum은 받은 파일의 SHA-256입니다. Maven Central이 함께 주는 `.sha1`과 비교해 받은 파일이 맞는지 확인합니다.

```bash
curl -sSLo dd-java-agent.jar https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/<버전>/dd-java-agent-<버전>.jar
```

```bash
shasum -a 256 dd-java-agent.jar
```

## 확인 필요

- **Datadog 사이트**: US1(`datadoghq.com`)인지 AP1(`ap1.datadoghq.com`)인지. `DD_SITE`와 로그 `Host`가 이 값을 따릅니다.
- **CloudWatch 로그 유지 여부**: FireLens로 바꾸면 CloudWatch에 로그가 쌓이지 않습니다. 계속 필요하면 FireLens
  대신 CloudWatch를 유지하고 Datadog Forwarder로 복제하는 방식을 검토합니다.
- **`datadog-agent` 이미지 버전**: 7.x 중 어느 버전으로 고정할지.
- **JVM 런타임 메트릭 과금**: `dd-java-agent`는 JVM 힙·GC 같은 런타임 메트릭도 에이전트로 보냅니다.
  이것이 커스텀 메트릭으로 과금되는지는 Datadog 계약 조건으로 확인합니다.
