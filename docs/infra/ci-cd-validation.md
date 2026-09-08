# 오공고 배포 검증

이 문서는 잘못된 시크릿이나 설정이 운영 서비스를 죽이지 못하게 막는 장치를 설명합니다.
배포 워크플로(`.github/workflows/deploy-ecs.yml`)를 고치기 전에 읽습니다.

## 문제

운영 설정은 저장소에 없습니다. `application.yml`은 `.gitignore` 대상이고, 배포 시
GitHub 시크릿 `APPLICATION_SECRET_USER` / `APPLICATION_SECRET_ADMIN`의 내용을 그대로
파일로 써서 jar에 넣습니다. 사람이 편집기에서 붙여 넣는 값이라 다음 사고가 실제로 납니다.

- 시크릿을 등록하지 않았는데 배포가 그대로 진행됩니다. GitHub은 없는 시크릿을 빈 문자열로
  바꾸고, `workflow_call`의 `secrets.<이름>.required: true`는 호출자가 값을 넘겼는지만
  확인할 뿐 그 값이 비었는지는 보지 않습니다.
- 들여쓰기가 한 단계 밀려 키가 엉뚱한 부모 아래로 들어갑니다. YAML로는 멀쩡해서 파싱도 되고
  애플리케이션도 뜨지만, 해당 설정은 기본값으로 동작합니다.
- 같은 키를 두 번 써서 뒤의 블록이 앞을 통째로 덮습니다.
- 값이 잘리거나 앞뒤에 공백이 붙습니다.

이런 파일도 jar에는 잘 들어가고 이미지도 잘 만들어집니다. 문제는 ECS에서 컨테이너가 뜬 다음
드러나고, 그때는 이미 운영 서비스가 죽은 뒤입니다.

## 검증 순서

되돌리기 어려운 단계일수록 뒤에 둡니다. 앞 단계가 실패하면 ECR에 이미지를 올리지도,
ECS를 건드리지도 않습니다.

| 순서 | 단계 | 잡는 것 |
| --- | --- | --- |
| 1 | 필수 시크릿 확인 | 비어 있음, 값 잘림, 공백·줄바꿈 혼입, 사용자·관리자 시크릿 뒤바뀜 |
| 2 | 설정 파일 계약 검증 | 필수 키 누락, 들여쓰기 어긋남, 키 중복, 값 형식, 로컬 값 잔존 |
| 3 | AWS 자격 증명·리소스 확인 | 만료·오타 난 자격 증명, 없는 ECR 리포지토리·ECS 클러스터·서비스 |
| 4 | 태스크 정의 점검 | 태스크 정의에 없는 컨테이너 이름, 없는 SSM·Secrets Manager 참조 |
| 5 | jar·이미지 빌드 | 컴파일 오류 (여기까지 AWS 상태를 바꾸지 않음) |
| 6 | 컨테이너 기동 검증 | 스프링 컨텍스트가 실제로 뜨지 않는 모든 원인 |
| 7 | ECR 푸시 → ECS 배포 → 스모크 테스트 | 실제 트래픽 처리 여부 |
| 8 | 실패 시 자동 롤백 | 위를 다 통과하고도 실패한 경우의 피해 |

### 6번이 핵심입니다

1~4번은 "우리가 이미 아는 실수"만 잡습니다. 6번은 방금 만든 이미지를 실제로 띄워
`/health`가 200을 줄 때까지 기다리므로, 빈 생성 실패나 `@ConfigurationProperties` 바인딩
실패처럼 미리 목록화하지 못한 원인까지 잡습니다.

예를 들어 `ogonggo.auth.jwt.secret`이 HS512 최소 길이에 못 미치면 jjwt가
`OgonggoTokenProvider` 생성자에서 `WeakKeyException`을 던집니다. 빌드는 통과하고
컨테이너만 죽는 종류의 오류인데, 6번이 이것을 이미지 푸시 전에 잡습니다.

러너는 VPC 안의 RDS·ElastiCache에 닿을 수 없으므로, 기동 검증은 CI가 띄운 MySQL·Redis
컨테이너를 보게 하고 접속 정보만 환경변수로 덮어씁니다. `ddl-auto`도 `update`로 덮어씁니다.
운영이 `validate`여도 빈 CI DB에는 스키마가 없기 때문입니다. 덕분에 JPA 매핑으로 스키마를
만들 수 있는지까지 함께 확인됩니다.

**덮어쓰는 값은 검증 대상에서 빠집니다.** 운영 DB 주소·계정처럼 덮어쓰는 값은 2번이 형식만
확인하고, 실제 연결 가능 여부는 7번 스모크 테스트에서야 드러납니다. 이 구간이 자동 롤백을
남겨 둔 이유입니다.

### 8번 자동 롤백

1~6번을 모두 통과하고도 배포나 스모크 테스트가 실패했다면, 러너에서 재현할 수 없는 원인
(보안 그룹, 태스크 역할 권한, VPC 안의 DB 접속 실패 등)입니다. 새 리비전을 그대로 두면
서비스가 죽은 채로 남으므로, 배포 직전에 서비스가 실행 중이던 태스크 정의로 되돌리고
헬스 체크가 회복되는지 확인합니다.

롤백에 성공해도 워크플로는 실패로 남깁니다. 배포는 이루어지지 않았기 때문입니다.

## 설정 계약

계약은 모듈마다 하나씩 `.github/config-schema/<모듈>.yml`에 있습니다.
검사기는 `.github/scripts/validate_application_yml.py`입니다.

```yaml
module: ogonggo-api-user

# 잎 키가 이 접두사 중 하나로 시작하지 않으면 오타이거나 들여쓰기가 어긋난 것으로 본다.
allowed-prefixes:
  - spring.datasource
  - spring.data.redis

rules:
  - path: ogonggo.auth.jwt.secret
    sensitive: true          # ::add-mask:: 로 등록해 이후 로그에서 가린다
    type: base64
    min-decoded-bytes: 64
    reason: 'HS512 서명 키입니다.'   # 오류 메시지에 함께 나온다

forbidden-paths:
  - path: spring.mail.from
    reason: 'Spring 표준 키가 아니라 무시됩니다.'
```

규칙 필드는 `required`, `sensitive`, `type`(`int`·`bool`·`duration`·`base64`),
`equals`, `one-of`, `pattern`, `forbid-substrings`, `min-length`,
`min-decoded-bytes`, `reason`입니다.

### allowed-prefixes를 좁게 유지합니다

`spring.jpa` 전체를 허용하면 `spring.jpa.properties` 아래로 밀려 들어간 키를 잡을 수
없습니다. 그래서 `spring.jpa.open-in-view`, `spring.jpa.hibernate`,
`spring.jpa.properties.hibernate`처럼 실제로 쓰는 가지만 나열합니다.
`spring.mail`도 같은 이유로 쪼개 두었습니다.

모듈이 쓰지 않는 설정은 일부러 넣지 않습니다. 관리자 모듈에 `spring.data.redis`가
허용되지 않는 이유는 관리자 모듈이 Redis 스타터를 의존성에 두지 않기 때문입니다.
그런 키가 들어왔다면 사용자 설정을 잘못 붙여 넣은 것입니다.

### 계약을 고쳐야 하는 때

- 코드에 새 설정 키를 추가했을 때. 같은 작업에서 계약에도 규칙을 추가합니다.
- 설정 키 이름을 바꿨을 때. 옛 이름은 `forbidden-paths`에 이유와 함께 남겨 두면,
  옛 이름을 그대로 둔 시크릿이 조용히 무시되는 대신 배포가 멈춥니다.
- 모듈을 추가했을 때. 계약 파일을 만들지 않으면 PR 빌드가 실패합니다.

계약에 없는 키가 설정 파일에 있으면 배포가 멈춥니다. 들여쓰기 사고를 잡기 위한 의도된
동작이므로, 의도한 키라면 계약에 추가하고 배포합니다.

## 시크릿 값이 로그에 남지 않게 하는 방법

검사기는 `sensitive: true`로 표시된 값을 `::add-mask::`로 등록합니다. 이후 단계의 로그에서
그 값은 `***`로 가려집니다. 기동 검증이 실패하면 컨테이너 로그를 200줄까지 출력하는데,
DB 비밀번호나 JWT 키가 그대로 남지 않는 것은 이 등록 덕분입니다.

`APPLICATION_SECRET` 자체는 GitHub이 통째로 하나의 문자열로만 가립니다. 그 안에 들어 있는
개별 값은 자동으로 가려지지 않으므로, 로그에 나올 수 있는 값은 계약에서 `sensitive`로
표시해야 합니다.

검사기의 오류 메시지도 `sensitive` 값은 내용 대신 "민감한 값이라 표시하지 않습니다"로
바꿔서 출력합니다.

## PR에서 도는 검사

배포를 막는 장치도 코드입니다. 검사기가 조용히 전부 통과시키면 없느니만 못하므로
`.github/workflows/test.yml`의 `Deploy checks` 잡이 PR마다 다음을 확인합니다.

- 계약 파일 자체의 문법과 모순 (`--check-spec`)
- 검사기가 실제로 잘못된 설정을 거르는지 (`.github/scripts/test_validate_application_yml.py`)
- 워크플로 YAML의 문법·표현식 (actionlint)
- 모듈마다 계약 파일이 있는지

운영 시크릿 없이 도는 검사라 PR 빌드에서 안전하게 실행됩니다.

로컬에서 돌리려면 PyYAML이 필요합니다.

```bash
python3 -m pip install pyyaml
python3 .github/scripts/test_validate_application_yml.py
```

시크릿을 고치기 전에 손에 있는 설정 파일을 미리 검사할 수도 있습니다.

```bash
python3 .github/scripts/validate_application_yml.py \
  --spec .github/config-schema/ogonggo-api-user.yml \
  --file ogonggo-api-user/src/main/resources/application.yml
```

## 아직 잡지 못하는 것

- **운영 DB·Redis·SES에 실제로 붙는지.** 러너는 VPC 안에 들어갈 수 없습니다.
  주소와 계정의 형식만 검사하고, 실제 연결은 배포 후 스모크 테스트에서 드러납니다.
- **렛츠커리어 서버와의 연동.** `ogonggo.letscareer.internal-api-key`가 렛츠커리어 쪽 값과
  같은지는 확인하지 않습니다. 형식과 길이만 봅니다.
- **ECS 태스크 정의의 CPU·메모리·네트워크 설정.** 배포는 이미지만 교체하므로 이 값들은
  콘솔에서 관리합니다.
- **두 서비스가 공유하는 스키마의 정합성.** `deploy-main.yml`이 user → admin 순서로
  배포해 동시 스키마 갱신을 피하는 것이 현재의 대응입니다.

## 검증을 끄는 방법

기동 검증과 자동 롤백은 재사용 워크플로의 입력으로 끌 수 있습니다.
긴급 배포처럼 예외 상황에서만 쓰고, 이유를 PR이나 배포 기록에 남깁니다.

```yaml
uses: ./.github/workflows/deploy-ecs.yml
with:
  module: ogonggo-api-user
  startup-verification: false
  rollback-on-failure: false
```
