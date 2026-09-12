# Lexical 이미지 업로드 API FE 전달용 API 변경사항

- 상태: Implemented
- 작성일: 2026-09-11
- 적용 범위: `ogonggo-api-user` 이미지 업로드 API
- 사용 목적: Lexical EditorState의 `image` 노드에 삽입할 이미지 업로드
- 저장소: Amazon S3
- 구현 상태: API·검증·S3 저장 코드 구현 완료, 운영 버킷 설정 필요

이 문서는 Lexical 에디터에서 사용하는 이미지 파일을 업로드하고, 에디터 본문에 저장할 URL을 반환하는 API 계약 초안이다. Lexical은 파일 업로드나 S3 저장을 담당하지 않으므로, 파일 업로드 API와 EditorState 저장은 별도 요청으로 처리한다.

## 1. API 요약

| 메서드 | 경로 | 목적 | 인증 |
| --- | --- | --- | --- |
| `POST` | `/api/v1/images` | 이미지 파일을 S3에 업로드하고 표시용 URL을 발급한다 | Bearer 인증 필수 |

## 2. [신규 API] 이미지 업로드

```http
POST /api/v1/images
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}
```

로그인한 사용자가 에디터에 삽입할 이미지를 업로드한다. 업로드가 성공하면 반환된 `data.url`을 Lexical 이미지 노드의 `src`에 저장한다.

### 2.1 요청

JSON 요청 본문은 사용하지 않는다. `multipart/form-data`의 `file` 파트로 파일을 전달한다.

| 이름 | 위치 | 타입 | 필수 | 기본값 | 허용 값 | 설명 |
| --- | --- | --- | --- | --- | --- | --- |
| `file` | multipart part | `File` | 예 | 없음 | JPEG, PNG, WebP | 업로드할 이미지 파일 |

요청 예시는 다음과 같다.

```bash
curl -X POST 'https://api.example.com/api/v1/images' \
  -H 'Authorization: Bearer {accessToken}' \
  -F 'file=@project-image.webp;type=image/webp'
```

### 2.2 업로드 검증

파일명과 클라이언트가 전달한 `Content-Type`만 믿지 않고 실제 파일 형식을 확인한다.

| 항목 | 규칙 |
| --- | --- |
| 허용 MIME 타입 | `image/jpeg`, `image/png`, `image/webp` |
| 최대 파일 크기 | 10 MiB 이하 |
| 확장자 | 서버가 실제 파일 형식으로 결정하며 원본 파일명을 S3 key로 사용하지 않음 |
| SVG | 허용하지 않음. SVG 내부 스크립트 실행 가능성을 차단하기 위함 |
| 저장 위치 | 서버가 생성한 UUID 기반 S3 key 사용 |

S3의 실제 bucket/key는 외부 응답에 노출하지 않는다. `data.url`은 에디터와 게시글 조회 화면에서 사용할 표시용 URL이며, 운영 환경에서는 S3 직접 URL보다 CDN 또는 이미지 도메인 URL을 사용한다.

### 2.2.1 런타임 설정

S3 저장 구현은 다음 설정을 사용한다. `public-base-url`은 경로가 포함되지 않은 URL prefix로 설정한다. 비어 있으면 `https://{bucket}.s3.{region}.amazonaws.com/{key}` 형식의 S3 URL을 반환한다.

```yaml
ogonggo:
  storage:
    s3:
      bucket: ogonggo-images
      region: ap-northeast-2
      public-base-url: https://cdn.example.com

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 11MB
```

AWS 자격 증명은 AWS SDK 기본 자격 증명 체인을 사용한다. 운영에서는 ECS task role 또는 실행 환경의 IAM role을 사용하고, 액세스 키를 코드·요청·응답에 저장하지 않는다.

multipart 용량 제한은 `UserImageConfiguration` 같은 코드 설정이 아니라 Spring Servlet multipart 설정으로 관리한다. 운영 시크릿 설정 파일에도 같은 `spring.servlet.multipart` 값을 포함해야 한다.

### 2.3 성공 응답

```http
HTTP/1.1 201 Created
Content-Type: application/json
```

```json
{
  "status": 201,
  "message": "요청이 성공했습니다.",
  "data": {
    "id": "01J_IMAGE_01",
    "url": "https://cdn.example.com/images/01J_IMAGE_01.webp",
    "mimeType": "image/webp",
    "size": 245760
  }
}
```

#### 응답 필드

| JSON 경로 | 타입 | nullable | 설명 |
| --- | --- | --- | --- |
| `data.id` | `String` | 아니오 | 업로드 이미지 식별자. S3 내부 key 자체는 아니다 |
| `data.url` | `String` | 아니오 | 이미지 표시용 URL. Lexical `image.src`에 저장한다 |
| `data.mimeType` | `String` | 아니오 | 서버가 확인한 실제 이미지 MIME 타입 |
| `data.size` | `Long` | 아니오 | 바이트 단위 파일 크기 |

### 2.4 예외

| 상황 | status | code | message |
| --- | ---: | --- | --- |
| 인증 토큰이 없거나 유효하지 않음 | 401 | `UNAUTHORIZED` | `리소스 접근 권한이 없습니다.` |
| `file` 파트가 없음 | 400 | `IMAGE_FILE_REQUIRED` | `이미지 파일은 필수입니다.` |
| 허용하지 않는 이미지 형식 | 400 | `IMAGE_FILE_TYPE_NOT_SUPPORTED` | `지원하지 않는 이미지 형식입니다.` |
| 파일 크기가 10 MiB 초과 | 400 | `IMAGE_FILE_TOO_LARGE` | `이미지 파일은 10 MiB 이하여야 합니다.` |
| S3 업로드 실패 | 500 | `IMAGE_UPLOAD_FAILED` | `이미지 업로드에 실패했습니다.` |

`IMAGE_FILE_REQUIRED`, `IMAGE_FILE_TYPE_NOT_SUPPORTED`, `IMAGE_FILE_TOO_LARGE`, `IMAGE_UPLOAD_FAILED`는 사용자 API가 소유하는 이미지 업로드 전용 ErrorCode다. S3 SDK의 bucket, key, credential, stack trace 등 내부 정보는 응답에 포함하지 않는다.

## 3. Lexical EditorState 연동

이미지 업로드 API는 Lexical EditorState를 받거나 수정하지 않는다. 프론트는 파일 업로드 성공 후 반환된 `id`와 URL을 Lexical 이미지 노드에 넣고, 그 결과인 EditorState JSON을 모집글 생성·수정 API에 전달한다.

```text
파일 선택
  → POST /api/v1/images
  → data.id, data.url 수신
  → Lexical image 노드의 imageId, src에 설정
  → 모집글 생성·수정 API에 EditorState JSON 저장
```

저장되는 EditorState의 이미지 노드는 다음 형태를 사용한다. `imageId`는 업로드 응답의 `data.id`다.

```json
{
  "type": "image",
  "imageId": "01J_IMAGE_01",
  "src": "https://cdn.example.com/images/01J_IMAGE_01.webp",
  "altText": "서비스 구조 이미지",
  "width": 1280,
  "height": 720
}
```

필드 규칙은 다음과 같다.

| JSON 경로 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `root.children[].imageId` | `String` | 업로드 이미지에 필수 | 이미지 업로드 API가 반환한 식별자 |
| `root.children[].src` | `String` | 예 | 이미지 업로드 API가 반환한 표시용 URL |
| `root.children[].altText` | `String` | 권장 | 접근성을 위한 대체 텍스트 |
| `root.children[].width` | `Int` | 아니오 | 편집기 표시 너비. 원본 이미지 너비와 다를 수 있음 |
| `root.children[].height` | `Int` | 아니오 | 편집기 표시 높이. 원본 이미지 높이와 다를 수 있음 |

게시글 저장 시 이미지 ID가 포함된 노드는 이미지 업로드 응답의 URL과 일치해야 하며, 현재 사용자 소유의 사용 가능한 이미지여야 한다. 이미지 ID가 없는 외부 이미지 URL은 허용할 수 있지만, 업로드 API로 생성한 이미지는 반드시 `imageId`를 함께 저장해야 한다.

## 4. 이미지 생명주기와 이번 범위

업로드 성공 시 S3 객체와 `image_assets` 메타데이터가 생성되고 `TEMPORARY` 상태가 된다. 모집글 저장 트랜잭션이 성공하면 `ATTACHED`로 전환한다.

모집글 수정으로 참조가 빠지거나 모집글이 삭제되면 `UNREFERENCED`로 전환한다. 일정 시간 이상 참조되지 않은 이미지와 업로드 중단 이미지는 스케줄러가 S3에서 삭제하고 `DELETED`로 표시한다.

- Presigned URL 기반 브라우저 직접 업로드
- SVG, GIF, 동영상 업로드

## 5. 구현 경계

- HTTP multipart 바인딩과 인증은 `ogonggo-api-user`가 소유한다.
- 이미지 파일 형식·크기 검증은 업로드 유스케이스에서 수행한다.
- S3 key 생성과 S3 SDK 호출은 HTTP Controller에서 직접 수행하지 않는다.
- 이미지 메타데이터와 S3 저장 구현은 `ogonggo-core`에서 공통 관리한다.
- Lexical EditorState 검증기는 현재처럼 `ogonggo-core/editor/lexical`에 둔다. 이미지 업로드 자체와 EditorState 검증은 서로 다른 책임이다.

---

## 운영 설정

- 기본 정리 주기: 1시간
- 기본 보존 시간: 24시간
- `ogonggo.storage.s3.cleanup.fixed-delay-ms`로 실행 주기를 변경한다.
- `ogonggo.storage.s3.cleanup.retention-hours`로 임시·미참조 이미지 보존 시간을 변경한다.
