#!/usr/bin/env bash
set -euo pipefail
set +x
umask 077

: "${POSTHOG_ENDPOINT_KEY:?Set endpoint:read key in the environment}"
: "${POSTHOG_PROJECT_ID:?Set the PostHog environment/project ID}"
: "${USER_API_BASE_URL:?Set user API base URL}"
: "${ADMIN_API_BASE_URL:?Set admin API base URL}"
: "${ADVERTISEMENT_REPORT_CONTRACT_ID:?Set an owned/test contract ID}"
: "${ADVERTISEMENT_REPORT_FROM:?Set YYYY-MM-DD}"
: "${ADVERTISEMENT_REPORT_TO:?Set YYYY-MM-DD}"
: "${ADVERTISEMENT_REPORT_EXPOSURE_IDS:?Set comma-separated contract-owned exposure IDs}"
: "${USER_API_BEARER_TOKEN:?Set a company-member token for the user API}"
: "${ADMIN_API_BEARER_TOKEN:?Set an admin token for the admin API}"

POSTHOG_API_HOST="${POSTHOG_API_HOST:-https://us.posthog.com}"
POSTHOG_ENDPOINT_NAME="${POSTHOG_ENDPOINT_NAME:-advertisement-contract-report}"
POSTHOG_ENDPOINT_VERSION="${POSTHOG_ENDPOINT_VERSION:-1}"
USER_API_BASE_URL="${USER_API_BASE_URL%/}"
ADMIN_API_BASE_URL="${ADMIN_API_BASE_URL%/}"
PAGE_SIZE=1000
MAX_ROWS=20000

if ! [[ "$ADVERTISEMENT_REPORT_CONTRACT_ID" =~ ^[1-9][0-9]*$ ]]; then
  echo "ADVERTISEMENT_REPORT_CONTRACT_ID must be a positive integer" >&2
  exit 2
fi
if ! [[ "$ADVERTISEMENT_REPORT_FROM" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ && "$ADVERTISEMENT_REPORT_TO" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  echo "ADVERTISEMENT_REPORT_FROM and _TO must be YYYY-MM-DD" >&2
  exit 2
fi
if ! [[ "$ADVERTISEMENT_REPORT_EXPOSURE_IDS" =~ ^[1-9][0-9]*(,[1-9][0-9]*)*$ ]]; then
  echo "ADVERTISEMENT_REPORT_EXPOSURE_IDS must be comma-separated positive integers" >&2
  exit 2
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required" >&2
  exit 2
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT
request_file="$tmp_dir/request.json"
response_file="$tmp_dir/response"
expected_columns='["category","event_date","event_type","job_role_code","position","record_type","total","unique_clickers","unique_users"]'
posthog_auth_config="$tmp_dir/posthog-auth-config"
printf 'header = "Authorization: Bearer %s"\n' "$POSTHOG_ENDPOINT_KEY" > "$posthog_auth_config"

post_with_bearer() {
  local token="$1"
  local output="$2"
  shift 2
  local auth_config="$tmp_dir/auth-config"
  printf 'header = "Authorization: Bearer %s"\n' "$token" > "$auth_config"
  curl --silent --show-error --output "$output" --write-out '%{http_code}' \
    --config "$auth_config" \
    "$@"
}

check_status() {
  local label="$1"
  local status="$2"
  if [[ "$status" != "200" ]]; then
    echo "$label returned HTTP $status" >&2
    exit 1
  fi
}

echo "Checking PostHog endpoint pages"
offset=0
total_rows=0
while :; do
  jq -cn \
    --arg from "$ADVERTISEMENT_REPORT_FROM" \
    --arg to "$ADVERTISEMENT_REPORT_TO" \
    --arg exposure_ids "$ADVERTISEMENT_REPORT_EXPOSURE_IDS" \
    --argjson limit "$PAGE_SIZE" \
    --argjson offset "$offset" \
    '{variables:{from:$from,to:$to,exposure_ids:$exposure_ids},limit:$limit,offset:$offset}' > "$request_file"

  status="$(curl --silent --show-error --output "$response_file" --write-out '%{http_code}' \
    --config "$posthog_auth_config" \
    --header 'Content-Type: application/json' \
    --header 'Accept: application/json' \
    --request POST \
    --data-binary "@$request_file" \
    "$POSTHOG_API_HOST/api/environments/$POSTHOG_PROJECT_ID/endpoints/$POSTHOG_ENDPOINT_NAME/run?version=$POSTHOG_ENDPOINT_VERSION")"
  check_status "PostHog endpoint" "$status"
  jq -e --argjson columns "$expected_columns" \
    '(.columns|type=="array") and ((.columns|sort)==$columns) and (.results|type=="array") and all(.results[]; (type=="array") and (length==9))' \
    "$response_file" >/dev/null
  page_rows="$(jq '.results|length' "$response_file")"
  total_rows=$((total_rows + page_rows))
  if (( total_rows > MAX_ROWS )); then
    echo "PostHog endpoint exceeded the configured $MAX_ROWS-row safety limit" >&2
    exit 1
  fi
  if (( page_rows < PAGE_SIZE )); then
    break
  fi
  offset=$((offset + page_rows))
done
echo "PostHog endpoint OK ($total_rows rows across pages)"

user_url="$USER_API_BASE_URL/api/v1/advertisement-contracts/$ADVERTISEMENT_REPORT_CONTRACT_ID/report?from=$ADVERTISEMENT_REPORT_FROM&to=$ADVERTISEMENT_REPORT_TO"
status="$(post_with_bearer "$USER_API_BEARER_TOKEN" "$response_file" --header 'Accept: application/json' "$user_url")"
check_status "User report API" "$status"
jq -e '.data.summary and (.data.daily|type=="array") and (.data.positions|type=="array") and (.data.jobRoleDistribution|type=="array")' "$response_file" >/dev/null
echo "User report API OK"

admin_url="$ADMIN_API_BASE_URL/api/v1/admin/advertisement-contracts/$ADVERTISEMENT_REPORT_CONTRACT_ID/report?from=$ADVERTISEMENT_REPORT_FROM&to=$ADVERTISEMENT_REPORT_TO"
status="$(post_with_bearer "$ADMIN_API_BEARER_TOKEN" "$response_file" --header 'Accept: application/json' "$admin_url")"
check_status "Admin report API" "$status"
jq -e '.data.summary and (.data.daily|type=="array") and (.data.positions|type=="array") and (.data.jobRoleDistribution|type=="array")' "$response_file" >/dev/null
echo "Admin report JSON OK"

csv_url="$ADMIN_API_BASE_URL/api/v1/admin/advertisement-contracts/$ADVERTISEMENT_REPORT_CONTRACT_ID/report.csv?from=$ADVERTISEMENT_REPORT_FROM&to=$ADVERTISEMENT_REPORT_TO"
status="$(post_with_bearer "$ADMIN_API_BEARER_TOKEN" "$response_file" --header 'Accept: text/csv' "$csv_url")"
check_status "Admin report CSV" "$status"
grep -aq '구분,날짜,카테고리,지면,지표' "$response_file"
echo "Admin report CSV OK"
