#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
BASE_URL="${BASE_URL%/}"
ACCESS_TOKEN="${ACCESS_TOKEN:-}"
TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/ogonggo-recruitment-post.XXXXXX")"

die() {
    printf '실패: %s\n' "$*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || die "필수 명령을 찾을 수 없습니다: $1"
}

cleanup_request() {
    local method="$1"
    local path="$2"

    curl --silent --show-error \
        --request "$method" \
        --header "Authorization: Bearer $ACCESS_TOKEN" \
        --output /dev/null \
        "$BASE_URL$path" || true
}

cleanup() {
    local exit_code=$?
    trap - EXIT

    if [[ -n "${COMMENT_REPLY_ID:-}" ]]; then
        cleanup_request DELETE "/api/v1/recruitment-posts/${POST_ID:-0}/comments/$COMMENT_REPLY_ID"
    fi
    if [[ -n "${COMMENT_ROOT_ID:-}" ]]; then
        cleanup_request DELETE "/api/v1/recruitment-posts/${POST_ID:-0}/comments/$COMMENT_ROOT_ID"
    fi
    if [[ -n "${APPLICATION_POST_ID:-}" ]]; then
        cleanup_request DELETE "/api/v1/users/me/recruitment/applications/$APPLICATION_POST_ID"
    fi
    if [[ -n "${APPLICATION_SORT_POST_ID:-}" ]]; then
        cleanup_request DELETE "/api/v1/users/me/recruitment/applications/$APPLICATION_SORT_POST_ID"
    fi
    if [[ -n "${BOOKMARK_POST_ID:-}" ]]; then
        cleanup_request DELETE "/api/v1/recruitment-post-bookmarks/$BOOKMARK_POST_ID"
    fi
    if [[ -n "${COPY_ID:-}" ]]; then
        cleanup_request DELETE "/api/v1/recruitment-posts/$COPY_ID"
    fi
    if [[ -n "${DRAFT_ID:-}" ]]; then
        cleanup_request DELETE "/api/v1/recruitment-posts/$DRAFT_ID"
    fi
    if [[ -n "${POST_ID:-}" ]]; then
        cleanup_request DELETE "/api/v1/recruitment-posts/$POST_ID"
    fi
    if [[ -n "${SORT_POST_ID:-}" ]]; then
        cleanup_request DELETE "/api/v1/recruitment-posts/$SORT_POST_ID"
    fi

    rm -rf "$TMP_DIR"
    exit "$exit_code"
}

require_command curl
require_command jq
[[ -n "$ACCESS_TOKEN" ]] || die 'ACCESS_TOKEN 환경변수가 필요합니다.'
trap cleanup EXIT

RESPONSE_STATUS=''
RESPONSE_BODY=''

call_api() {
    local expected_status="$1"
    local label="$2"
    local method="$3"
    local path="$4"
    local token="$5"
    local payload="${6:-}"
    local raw
    local actual_status
    local actual_body
    local response_path="$TMP_DIR/$label.json"
    local -a curl_args=(
        --silent
        --show-error
        --request "$method"
        --header 'Accept: application/json'
    )

    if [[ -n "$token" ]]; then
        curl_args+=(--header "Authorization: Bearer $token")
    fi
    if [[ -n "$payload" ]]; then
        curl_args+=(--header 'Content-Type: application/json' --data "$payload")
    fi

    if ! raw="$(curl "${curl_args[@]}" "$BASE_URL$path" --write-out $'\n%{http_code}')"; then
        die "$label 요청이 네트워크 단계에서 실패했습니다: $method $path"
    fi

    actual_status="${raw##*$'\n'}"
    actual_body="${raw%$'\n'*}"
    RESPONSE_STATUS="$actual_status"
    RESPONSE_BODY="$actual_body"
    printf '%s\n' "$actual_body" > "$response_path"

    if [[ "$actual_status" != "$expected_status" ]]; then
        printf '%s\n' "$actual_body" | jq . >&2 || true
        die "$label 상태 코드가 다릅니다. 기대=$expected_status 실제=$actual_status"
    fi
    if ! printf '%s' "$actual_body" | jq -e . >/dev/null 2>&1; then
        die "$label 응답이 JSON이 아닙니다. 저장 위치=$response_path"
    fi

    printf '\n[%s] %s %s -> %s\n' "$label" "$method" "$path" "$actual_status"
    printf '%s\n' "$actual_body" | jq .
}

assert_json() {
    local expression="$1"
    local message="$2"

    if ! printf '%s' "$RESPONSE_BODY" | jq -e "$expression" >/dev/null; then
        printf '%s\n' "$RESPONSE_BODY" | jq . >&2 || true
        die "$message"
    fi
}

expect_success() {
    local expected_status="$1"
    local label="$2"
    local method="$3"
    local path="$4"
    local token="$5"
    local payload="${6:-}"

    call_api "$expected_status" "$label" "$method" "$path" "$token" "$payload"
    assert_json \
        ".status == ${expected_status} and .message == \"요청이 성공했습니다.\"" \
        "$label 공통 성공 응답 검증에 실패했습니다."
}

expect_error() {
    local expected_status="$1"
    local expected_code="$2"
    local label="$3"
    local method="$4"
    local path="$5"
    local token="$6"
    local payload="${7:-}"

    call_api "$expected_status" "$label" "$method" "$path" "$token" "$payload"
    assert_json \
        ".status == ${expected_status} and .code == \"${expected_code}\"" \
        "$label 오류 응답 검증에 실패했습니다."
}

extract_id() {
    local expression="$1"
    local value

    value="$(printf '%s' "$RESPONSE_BODY" | jq -r "$expression")"
    [[ "$value" =~ ^[1-9][0-9]*$ ]] || die "응답에서 양의 정수 식별자를 추출할 수 없습니다: $expression"
    printf '%s' "$value"
}

check_health() {
    local status

    if ! status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' "$BASE_URL/health")"; then
        die "health 요청이 네트워크 단계에서 실패했습니다."
    fi
    [[ "$status" == '200' ]] || die "health 상태 코드가 200이 아닙니다: $status"
    printf '[health] GET /health -> 200\n'
}

wait_for_bookmark_state() {
    local expected_bookmarked="$1"
    local expected_count="$2"
    local raw
    local actual_status
    local actual_body

    for _ in {1..20}; do
        if raw="$(curl --silent --show-error \
            --header 'Accept: application/json' \
            --header "Authorization: Bearer $ACCESS_TOKEN" \
            "$BASE_URL/api/v1/recruitment-posts/$POST_ID" \
            --write-out $'\n%{http_code}')"; then
            actual_status="${raw##*$'\n'}"
            actual_body="${raw%$'\n'*}"
            if [[ "$actual_status" == '200' ]] && printf '%s' "$actual_body" | jq -e \
                ".data.bookmarked == ${expected_bookmarked} and .data.bookmarkCount == ${expected_count}" \
                >/dev/null; then
                RESPONSE_STATUS="$actual_status"
                RESPONSE_BODY="$actual_body"
                return
            fi
        fi
        sleep 0.5
    done

    printf '%s\n' "${actual_body:-}" | jq . >&2 || true
    die "비동기 북마크 지표가 기대 상태에 도달하지 않았습니다. bookmarked=$expected_bookmarked bookmarkCount=$expected_count"
}

EDITOR_STATE='{"root":{"children":[{"children":[{"detail":0,"format":0,"mode":"normal","style":"","text":"curl smoke 본문","type":"text","version":1}],"direction":null,"format":"","indent":0,"textFormat":0,"type":"paragraph","version":1}],"direction":null,"format":"","indent":0,"type":"root","version":1}}'

DRAFT_BODY="$(cat <<JSON
{"title":"Curl smoke draft","recruitmentType":"SIDE_PROJECT","capacity":4,"progressMethod":"ONLINE","activityDurationMonths":3,"technologyStacks":["Kotlin","Spring"],"summary":"curl 실행 검증용 임시저장 모집글입니다.","content":$EDITOR_STATE,"eligibilityAndSelectionProcess":"주 1회 회의에 참여할 수 있는 분","recruitmentStartDate":"2026-10-01","recruitmentEndDate":"2026-12-31","positions":["BACKEND"],"contactMethod":"EMAIL","contactValue":"curl-smoke@example.com"}
JSON
)"

PUBLIC_BODY="$(cat <<JSON
{"title":"Curl smoke public 모집글","recruitmentType":"SIDE_PROJECT","capacity":4,"progressMethod":"ONLINE","activityDurationMonths":3,"technologyStacks":["Kotlin","Spring"],"summary":"curl 실행 검증용 공개 모집글입니다.","content":$EDITOR_STATE,"eligibilityAndSelectionProcess":"주 1회 회의에 참여할 수 있는 분","recruitmentStartDate":"2026-10-01","recruitmentEndDate":"2026-12-31","positions":["BACKEND"],"contactMethod":"OPEN_KAKAO","contactValue":"https://open.kakao.com/o/curl-smoke","agreedToPolicy":true}
JSON
)"
SORT_PUBLIC_BODY="$PUBLIC_BODY"

PUBLIC_UPDATE_BODY="$(cat <<JSON
{"title":"Curl smoke public updated","recruitmentType":"SIDE_PROJECT","capacity":5,"progressMethod":"HYBRID","activityDurationMonths":4,"technologyStacks":["Kotlin","Spring","Docker"],"summary":"curl 실행 검증용 수정된 공개 모집글입니다.","content":$EDITOR_STATE,"eligibilityAndSelectionProcess":"수정된 지원 자격과 전형 절차입니다.","recruitmentStartDate":"2026-10-02","recruitmentEndDate":"2027-01-31","positions":["BACKEND","FRONTEND"],"contactMethod":"OPEN_KAKAO","contactValue":"https://open.kakao.com/o/curl-smoke-updated"}
JSON
)"

check_health

expect_error 401 UNAUTHORIZED auth-management-list GET \
    '/api/v1/me/recruitment-posts' '' ''
expect_error 401 UNAUTHORIZED auth-bookmark-list GET \
    '/api/v1/recruitment-post-bookmarks' '' ''
expect_error 401 UNAUTHORIZED auth-public-create POST \
    '/api/v1/recruitment-posts' '' "$PUBLIC_BODY"
expect_error 401 UNAUTHORIZED auth-draft-create POST \
    '/api/v1/me/recruitment-posts/drafts' '' "$DRAFT_BODY"

expect_success 201 draft-create POST \
    '/api/v1/me/recruitment-posts/drafts' "$ACCESS_TOKEN" "$DRAFT_BODY"
DRAFT_ID="$(extract_id '.data.id')"
expect_error 401 UNAUTHORIZED auth-draft-copy POST \
    "/api/v1/me/recruitment-posts/$DRAFT_ID/copy" '' ''
expect_error 401 UNAUTHORIZED auth-draft-publish POST \
    "/api/v1/me/recruitment-posts/$DRAFT_ID/publish" '' '{"agreedToPolicy":true}'

expect_success 200 draft-form GET \
    "/api/v1/me/recruitment-posts/$DRAFT_ID" "$ACCESS_TOKEN"
assert_json ".data.postId == $DRAFT_ID and .data.status == \"DRAFT\" and .data.title == \"Curl smoke draft\"" \
    '임시저장 작성 폼 응답 검증에 실패했습니다.'

expect_success 200 draft-update PUT \
    "/api/v1/recruitment-posts/$DRAFT_ID" "$ACCESS_TOKEN" "$PUBLIC_UPDATE_BODY"
expect_success 200 draft-form-after-update GET \
    "/api/v1/me/recruitment-posts/$DRAFT_ID" "$ACCESS_TOKEN"
assert_json ".data.postId == $DRAFT_ID and .data.status == \"DRAFT\" and .data.title == \"Curl smoke public updated\" and .data.recruitmentType == \"SIDE_PROJECT\"" \
    '임시저장 전체 수정 결과 검증에 실패했습니다.'

expect_success 200 draft-publish POST \
    "/api/v1/me/recruitment-posts/$DRAFT_ID/publish" "$ACCESS_TOKEN" '{"agreedToPolicy":true}'
expect_success 200 draft-publish-idempotent POST \
    "/api/v1/me/recruitment-posts/$DRAFT_ID/publish" "$ACCESS_TOKEN" '{"agreedToPolicy":true}'
expect_success 200 published-form GET \
    "/api/v1/me/recruitment-posts/$DRAFT_ID" "$ACCESS_TOKEN"
assert_json ".data.postId == $DRAFT_ID and .data.status == \"PUBLISHED\"" \
    '게시 전환 후 작성 폼 상태 검증에 실패했습니다.'

expect_success 200 authored-list GET \
    '/api/v1/me/recruitment-posts?page=1&size=100&status=ALL&recruitmentStatus=RECRUITING&recruitmentType=SIDE_PROJECT&keyword=Curl&sort=LATEST_SAVED' \
    "$ACCESS_TOKEN"
assert_json ".data.items | type == \"array\"" '작성한 모집글 관리 목록의 items 배열 검증에 실패했습니다.'
assert_json ".data.items | any(.[]; .postId == $DRAFT_ID and .status == \"PUBLISHED\")" \
    '작성한 모집글 관리 목록에 게시한 글이 없습니다.'
assert_json '.data.pageInfo.pageNum == 1 and .data.pageInfo.pageSize == 100' \
    '작성한 모집글 관리 목록의 페이지 정보 검증에 실패했습니다.'

expect_success 201 public-create POST \
    '/api/v1/recruitment-posts' "$ACCESS_TOKEN" "$PUBLIC_BODY"
POST_ID="$(extract_id '.data.id')"
[[ "$POST_ID" != "$DRAFT_ID" ]] || die '공개 모집글 생성 ID가 임시저장 ID와 중복되었습니다.'

expect_success 200 public-list-anonymous GET \
    '/api/v1/recruitment-posts?page=1&size=100&sort=LATEST&recruitmentTypes=SIDE_PROJECT&progressMethods=ONLINE&recruitmentStatuses=RECRUITING&positions=BACKEND' \
    ''
assert_json ".data.items | any(.[]; .id == $POST_ID)" '공개 모집글 목록에 생성한 글이 없습니다.'
assert_json ".data.items | any(.[]; .id == $POST_ID and .bookmarked == false and .bookmarkCount == 0)" \
    '비로그인 목록에서 생성한 글의 북마크 상태·수 검증에 실패했습니다.'
assert_json '.data.pageInfo.pageNum == 1 and (.data.pageInfo.totalElements | type) == "number"' \
    '공개 모집글 목록의 페이지 정보 검증에 실패했습니다.'

expect_success 200 public-detail-anonymous GET \
    "/api/v1/recruitment-posts/$POST_ID" ''
assert_json ".data.id == $POST_ID and .data.recruitmentStatus == \"RECRUITING\" and .data.bookmarked == false and .data.bookmarkCount == 0" \
    '비로그인 공개 상세의 식별자·모집 상태·북마크 검증에 실패했습니다.'
assert_json '.data.author.userId > 0 and (.data.content | type) == "object" and (.data.viewCount | type) == "number" and (.data.commentCount | type) == "number"' \
    '공개 상세의 작성자·본문·지표 필드 검증에 실패했습니다.'

expect_success 200 public-update PUT \
    "/api/v1/recruitment-posts/$POST_ID" "$ACCESS_TOKEN" "$PUBLIC_UPDATE_BODY"
expect_success 200 public-detail-after-update GET \
    "/api/v1/recruitment-posts/$POST_ID" "$ACCESS_TOKEN"
assert_json ".data.id == $POST_ID and .data.title == \"Curl smoke public updated\" and .data.progressMethod == \"HYBRID\" and .data.capacity == 5" \
    '게시된 모집글 전체 수정 결과 검증에 실패했습니다.'

expect_success 200 authored-list-filtered GET \
    '/api/v1/me/recruitment-posts?page=1&size=100&status=PUBLISHED&recruitmentStatus=RECRUITING&applicationStatus=NO_APPLICATIONS&recruitmentType=SIDE_PROJECT&keyword=Curl&sort=LATEST_SAVED' \
    "$ACCESS_TOKEN"
assert_json ".data.items | any(.[]; .postId == $POST_ID and .applicationCount == 0)" \
    '작성한 모집글 필터 목록의 공개 글 검증에 실패했습니다.'

expect_success 201 copy POST \
    "/api/v1/me/recruitment-posts/$POST_ID/copy" "$ACCESS_TOKEN"
COPY_ID="$(extract_id '.data.postId')"
[[ "$COPY_ID" != "$POST_ID" ]] || die '모집글 복사 ID가 원본과 같습니다.'
assert_json ".data.postId == $COPY_ID and .data.status == \"DRAFT\"" \
    '모집글 복사 응답의 임시저장 상태 검증에 실패했습니다.'
expect_success 200 copy-form GET \
    "/api/v1/me/recruitment-posts/$COPY_ID" "$ACCESS_TOKEN"
assert_json ".data.postId == $COPY_ID and .data.status == \"DRAFT\"" \
    '복사된 모집글 작성 폼 검증에 실패했습니다.'
expect_success 200 copy-delete DELETE \
    "/api/v1/recruitment-posts/$COPY_ID" "$ACCESS_TOKEN"
COPY_ID=''

expect_success 200 close PATCH \
    "/api/v1/recruitment-posts/$POST_ID/close" "$ACCESS_TOKEN"
expect_success 200 closed-detail GET \
    "/api/v1/recruitment-posts/$POST_ID" "$ACCESS_TOKEN"
assert_json '.data.recruitmentStatus == "CLOSED"' '모집글 마감 상태 검증에 실패했습니다.'
expect_success 200 reopen PATCH \
    "/api/v1/recruitment-posts/$POST_ID/reopen" "$ACCESS_TOKEN"
expect_success 200 reopened-detail GET \
    "/api/v1/recruitment-posts/$POST_ID" "$ACCESS_TOKEN"
assert_json '.data.recruitmentStatus == "RECRUITING"' '모집글 재모집 상태 검증에 실패했습니다.'

expect_error 401 UNAUTHORIZED auth-application-create POST \
    "/api/v1/recruitment-posts/$POST_ID/applications" '' ''
expect_error 401 UNAUTHORIZED auth-application-status PATCH \
    "/api/v1/users/me/recruitment/applications/$POST_ID" '' '{"applicationStatus":"COMPLETED"}'
expect_error 401 UNAUTHORIZED auth-application-delete DELETE \
    "/api/v1/users/me/recruitment/applications/$POST_ID" '' ''
expect_success 200 application-create POST \
    "/api/v1/recruitment-posts/$POST_ID/applications" "$ACCESS_TOKEN"
APPLICATION_POST_ID="$POST_ID"
assert_json ".data.postId == $POST_ID and .data.contactMethod == \"OPEN_KAKAO\" and (.data.clickedAt | type) == \"string\"" \
    '외부 지원 링크 접근 기록 응답 검증에 실패했습니다.'
expect_success 200 application-create-idempotent POST \
    "/api/v1/recruitment-posts/$POST_ID/applications" "$ACCESS_TOKEN"
assert_json ".data.postId == $POST_ID" '외부 지원 링크 접근 멱등 응답 검증에 실패했습니다.'

expect_success 200 application-list GET \
    '/api/v1/users/me/recruitment/applications?page=1&size=100&recruitmentStatus=RECRUITING&recruitmentType=SIDE_PROJECT&applicationStatus=PREPARING&keyword=Curl&sort=LATEST' \
    "$ACCESS_TOKEN"
assert_json ".data.items | any(.[]; .postId == $POST_ID and .recruitmentType == \"SIDE_PROJECT\" and .progressMethod == \"HYBRID\" and .activityDurationMonths == 4 and .applicationStatus == \"PREPARING\")" \
    '내 지원 목록의 모집글·진행 방식·활동 기간·지원 상태 검증에 실패했습니다.'
assert_json '(.data.countsByRecruitmentType.SIDE_PROJECT | type) == "number" and (.data.countsByRecruitmentType.STUDY | type) == "number"' \
    '내 지원 목록의 유형별 건수 검증에 실패했습니다.'
assert_json '.data.pageInfo.pageNum == 1 and .data.pageInfo.pageSize == 100' \
    '내 지원 목록의 페이지 정보 검증에 실패했습니다.'
expect_error 400 BAD_REQUEST application-list-invalid-status GET \
    '/api/v1/users/me/recruitment/applications?page=1&size=100&applicationStatus=INVALID&keyword=Curl&sort=LATEST' \
    "$ACCESS_TOKEN" ''

for application_status in PREPARING COMPLETED IN_PROGRESS ENDED; do
    expect_success 200 "application-status-$application_status" PATCH \
        "/api/v1/users/me/recruitment/applications/$POST_ID" "$ACCESS_TOKEN" \
        "{\"applicationStatus\":\"$application_status\"}"
    expect_success 200 "application-list-filter-$application_status" GET \
        "/api/v1/users/me/recruitment/applications?page=1&size=100&applicationStatus=$application_status&keyword=Curl&sort=LATEST" \
        "$ACCESS_TOKEN"
    assert_json ".data.items | (all(.[]; .applicationStatus == \"$application_status\") and any(.[]; .postId == $POST_ID))" \
        "지원 상태 필터 $application_status 결과 검증에 실패했습니다."
    assert_json '.data.countsByRecruitmentType.SIDE_PROJECT == 1 and .data.countsByRecruitmentType.STUDY == 0' \
        "지원 상태 필터 ${application_status}의 유형별 건수 검증에 실패했습니다."
done
expect_success 200 application-list-after-status GET \
    '/api/v1/users/me/recruitment/applications?page=1&size=100&keyword=Curl&sort=LATEST' \
    "$ACCESS_TOKEN"
assert_json ".data.items | any(.[]; .postId == $POST_ID and .applicationStatus == \"ENDED\")" \
    '내 지원 상태 변경 결과 검증에 실패했습니다.'

expect_success 201 sort-public-create POST \
    '/api/v1/recruitment-posts' "$ACCESS_TOKEN" "$SORT_PUBLIC_BODY"
SORT_POST_ID="$(extract_id '.data.id')"
[[ "$SORT_POST_ID" != "$POST_ID" ]] || die '정렬 검증용 모집글 ID가 원본과 중복되었습니다.'
sleep 1
expect_success 200 sort-application-create POST \
    "/api/v1/recruitment-posts/$SORT_POST_ID/applications" "$ACCESS_TOKEN"
APPLICATION_SORT_POST_ID="$SORT_POST_ID"
expect_success 200 application-list-sorted GET \
    '/api/v1/users/me/recruitment/applications?page=1&size=100&keyword=Curl&sort=LATEST' \
    "$ACCESS_TOKEN"
assert_json ".data.items | map(select(.postId == $POST_ID or .postId == $SORT_POST_ID)) | map(.postId) == [$SORT_POST_ID, $POST_ID]" \
    '지원 목록이 firstClickedAt 최신순으로 정렬되지 않았습니다.'

expect_success 200 application-delete DELETE \
    "/api/v1/users/me/recruitment/applications/$POST_ID" "$ACCESS_TOKEN"
APPLICATION_POST_ID=''
expect_error 404 RECRUITMENT_POST_APPLICATION_NOT_FOUND application-delete-again DELETE \
    "/api/v1/users/me/recruitment/applications/$POST_ID" "$ACCESS_TOKEN" ''
expect_success 200 sort-application-delete DELETE \
    "/api/v1/users/me/recruitment/applications/$SORT_POST_ID" "$ACCESS_TOKEN"
APPLICATION_SORT_POST_ID=''
expect_success 200 application-list-after-delete GET \
    '/api/v1/users/me/recruitment/applications?page=1&size=100&keyword=Curl&sort=LATEST' \
    "$ACCESS_TOKEN"
assert_json ".data.items | all(.[]; .postId != $POST_ID)" '삭제한 지원 이력이 목록에 남아 있습니다.'
expect_success 200 public-list-after-application-delete GET \
    '/api/v1/recruitment-posts?page=1&size=100&sort=LATEST&recruitmentTypes=SIDE_PROJECT' \
    "$ACCESS_TOKEN"
assert_json ".data.items | any(.[]; .id == $POST_ID and .applicationCount == 0)" \
    '지원 이력 삭제 후 공개 목록의 applicationCount가 감소하지 않았습니다.'

expect_success 200 application-reapply POST \
    "/api/v1/recruitment-posts/$POST_ID/applications" "$ACCESS_TOKEN"
APPLICATION_POST_ID="$POST_ID"
expect_success 200 application-list-after-reapply GET \
    '/api/v1/users/me/recruitment/applications?page=1&size=100&applicationStatus=ENDED&keyword=Curl&sort=LATEST' \
    "$ACCESS_TOKEN"
assert_json "(.data.items | any(.[]; .postId == $POST_ID and .applicationStatus == \"ENDED\")) and .data.countsByRecruitmentType.SIDE_PROJECT == 1" \
    '삭제한 지원 이력이 재신청 후 기존 상태로 복구되지 않았습니다.'
expect_success 200 public-list-after-application-reapply GET \
    '/api/v1/recruitment-posts?page=1&size=100&sort=LATEST&recruitmentTypes=SIDE_PROJECT' \
    "$ACCESS_TOKEN"
assert_json ".data.items | any(.[]; .id == $POST_ID and .applicationCount == 1)" \
    '지원 이력 재신청 후 applicationCount가 복구되지 않았습니다.'
expect_success 200 application-delete-after-reapply DELETE \
    "/api/v1/users/me/recruitment/applications/$POST_ID" "$ACCESS_TOKEN"
APPLICATION_POST_ID=''

expect_success 201 bookmark-create POST \
    "/api/v1/recruitment-post-bookmarks/$POST_ID" "$ACCESS_TOKEN"
BOOKMARK_POST_ID="$POST_ID"
assert_json '.data == null' '북마크 등록의 data가 null이 아닙니다.'
expect_error 409 RECRUITMENT_POST_BOOKMARK_ALREADY_EXISTS bookmark-create-again POST \
    "/api/v1/recruitment-post-bookmarks/$POST_ID" "$ACCESS_TOKEN" ''
expect_success 200 bookmark-list GET \
    '/api/v1/recruitment-post-bookmarks?page=1&size=100' "$ACCESS_TOKEN"
assert_json ".data.items | any(.[]; .id == $POST_ID and .bookmarked == true)" \
    '내 북마크 목록에 등록한 모집글이 없습니다.'
wait_for_bookmark_state true 1
assert_json ".data.id == $POST_ID and .data.bookmarked == true and .data.bookmarkCount == 1" \
    '북마크 등록 후 공개 상세 지표 검증에 실패했습니다.'

expect_success 200 bookmark-delete DELETE \
    "/api/v1/recruitment-post-bookmarks/$POST_ID" "$ACCESS_TOKEN"
expect_success 200 bookmark-delete-again DELETE \
    "/api/v1/recruitment-post-bookmarks/$POST_ID" "$ACCESS_TOKEN"
BOOKMARK_POST_ID=''
expect_success 200 bookmark-list-after-delete GET \
    '/api/v1/recruitment-post-bookmarks?page=1&size=100' "$ACCESS_TOKEN"
assert_json ".data.items | all(.[]; .id != $POST_ID)" '해제한 북마크가 목록에 남아 있습니다.'
wait_for_bookmark_state false 0
assert_json ".data.id == $POST_ID and .data.bookmarked == false and .data.bookmarkCount == 0" \
    '북마크 해제 후 공개 상세 지표 검증에 실패했습니다.'

expect_error 401 UNAUTHORIZED auth-comment-create POST \
    "/api/v1/recruitment-posts/$POST_ID/comments" '' '{"content":"curl smoke unauthorized"}'
expect_success 200 public-comment-list-empty GET \
    "/api/v1/recruitment-posts/$POST_ID/comments?page=1&size=30" ''
assert_json '.data.items | (type == "array")' \
    '공개 댓글 목록의 페이지 응답 검증에 실패했습니다.'
assert_json '.data.pageInfo.pageNum == 1' '공개 댓글 목록의 페이지 번호 검증에 실패했습니다.'

expect_success 201 comment-create POST \
    "/api/v1/recruitment-posts/$POST_ID/comments" "$ACCESS_TOKEN" '{"content":"curl smoke root"}'
COMMENT_ROOT_ID="$(extract_id '.data.id')"
assert_json ".data.id == $COMMENT_ROOT_ID" '부모 댓글 생성 응답 검증에 실패했습니다.'
expect_success 200 comment-list GET \
    "/api/v1/recruitment-posts/$POST_ID/comments?page=1&size=30" ''
assert_json ".data.items | any(.[]; .id == $COMMENT_ROOT_ID and .parentId == null and .content == \"curl smoke root\")" \
    '댓글 목록에 생성한 부모 댓글이 없습니다.'

expect_success 201 reply-create POST \
    "/api/v1/recruitment-posts/$POST_ID/comments" "$ACCESS_TOKEN" \
    "{\"content\":\"curl smoke reply\",\"parentId\":$COMMENT_ROOT_ID}"
COMMENT_REPLY_ID="$(extract_id '.data.id')"
expect_success 200 reply-list GET \
    "/api/v1/recruitment-posts/$POST_ID/comments/$COMMENT_ROOT_ID/replies?page=1&size=30" ''
assert_json ".data.items | any(.[]; .id == $COMMENT_REPLY_ID and .parentId == $COMMENT_ROOT_ID)" \
    '대댓글 목록에 생성한 대댓글이 없습니다.'

expect_success 201 comment-report POST \
    "/api/v1/recruitment-posts/$POST_ID/comments/$COMMENT_ROOT_ID/reports" "$ACCESS_TOKEN" \
    '{"reason":"curl smoke 신고 사유"}'
assert_json '.data == null' '댓글 신고의 data가 null이 아닙니다.'
expect_success 200 reply-delete DELETE \
    "/api/v1/recruitment-posts/$POST_ID/comments/$COMMENT_REPLY_ID" "$ACCESS_TOKEN"
COMMENT_REPLY_ID=''
expect_success 200 comment-delete DELETE \
    "/api/v1/recruitment-posts/$POST_ID/comments/$COMMENT_ROOT_ID" "$ACCESS_TOKEN"
COMMENT_ROOT_ID=''

expect_success 200 public-delete DELETE \
    "/api/v1/recruitment-posts/$POST_ID" "$ACCESS_TOKEN"
DELETED_POST_ID="$POST_ID"
POST_ID=''
expect_error 404 RECRUITMENT_POST_NOT_FOUND public-detail-after-delete GET \
    "/api/v1/recruitment-posts/$DELETED_POST_ID" '' ''

expect_success 200 sort-public-delete DELETE \
    "/api/v1/recruitment-posts/$SORT_POST_ID" "$ACCESS_TOKEN"
SORT_POST_ID=''

expect_success 200 draft-delete DELETE \
    "/api/v1/recruitment-posts/$DRAFT_ID" "$ACCESS_TOKEN"
DRAFT_ID=''

printf '\n성공: 모집글 관련 curl 실행 테스트와 응답 검증을 완료했습니다.\n'
