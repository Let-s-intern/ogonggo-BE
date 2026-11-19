/*
 * Saved PostHog SQL Endpoint: advertisement-contract-report, version 1
 *
 * Register required Endpoint variables in PostHog Data management → Variables:
 *   from         String, required, YYYY-MM-DD, code_name "from"
 *   to           String, required, YYYY-MM-DD, code_name "to"
 *   exposure_ids String, required, comma-separated positive DB exposure IDs, code_name "exposure_ids"
 *
 * The BE sends limit/offset in the request body and passes only contract-owned exposure IDs.
 * Response columns and row kinds are fixed by StrictPosthogAdvertisementReportEndpointMapper.
 *
 * Counting semantics:
 *   - impressions: one per (exposure_id, Seoul event date, viewer_id)
 *   - clicks: every click event; repeats are retained
 *   - job-role/audience breakdown: earliest event timestamp per exposure/date/viewer; concurrent out-of-order delivery can differ from MySQL's first inserted daily-viewer row
 *   - identified events: person_id; anonymous events: distinct_id, even when person_id is null
 *   - job roles: normalized enum codes captured at click time; raw wish-job labels are never read here
 */
WITH source_events AS (
    SELECT
        uuid,
        timestamp,
        distinct_id,
        person_id,
        event,
        properties,
        toDate(toTimeZone(timestamp, 'Asia/Seoul')) AS event_date,
        toString(properties.exposure_id) AS exposure_id,
        if(event IN ('banner_impression', 'ad_posting_impression'), 'IMPRESSION', 'CLICK') AS event_type,
        if(
            event IN ('banner_impression', 'banner_click'),
            'BANNER',
            toString(properties.category)
        ) AS category,
        if(
            event IN ('banner_impression', 'banner_click'),
            multiIf(
                toString(properties.position) != '', toString(properties.position),
                toString(properties.banner_placement) = 'HOME_TOP', 'JOB_HOME_TOP_BANNER',
                toString(properties.banner_placement) = 'HOME_BOTTOM', 'JOB_HOME_BOTTOM_BANNER',
                toString(properties.banner_placement) = 'JOB_DETAIL_SIDE', 'JOB_DETAIL_BANNER',
                toString(properties.banner_placement) = 'BOOTCAMP_TOP', 'BOOTCAMP_LIST_TOP_BANNER',
                toString(properties.banner_placement) = 'BOOTCAMP_BOTTOM', 'BOOTCAMP_LIST_BOTTOM_BANNER',
                toString(properties.banner_placement) = 'BOOTCAMP_DETAIL_SIDE', 'BOOTCAMP_DETAIL_BANNER',
                ''
            ),
            ifNull(toString(properties.position), '')
        ) AS position,
        ifNull(toString(properties.audience_type), '') AS raw_audience_type,
        ifNull(properties.job_role_codes, []) AS job_role_codes
    FROM events
    WHERE event IN (
        'banner_impression', 'banner_click',
        'ad_posting_impression', 'ad_posting_click'
    )
      AND timestamp >= toDateTime(concat({variables.from}, ' 00:00:00'), 'Asia/Seoul')
      AND timestamp < addDays(toDateTime(concat({variables.to}, ' 00:00:00'), 'Asia/Seoul'), 1)
      AND has(splitByChar(',', {variables.exposure_ids}), toString(properties.exposure_id))
),
typed_events AS (
    SELECT
        uuid,
        timestamp,
        event_date,
        exposure_id,
        category,
        nullIf(position, '') AS position,
        event_type,
        if(
            raw_audience_type IN ('MEMBER', 'NON_MEMBER', 'UNSPECIFIED'),
            raw_audience_type,
            if(isNotNull(person_id), 'UNSPECIFIED', 'NON_MEMBER')
        ) AS audience_type,
        if(
            raw_audience_type = 'NON_MEMBER',
            concat('ANON:', toString(distinct_id)),
            if(
                isNotNull(person_id),
                concat('PERSON:', toString(person_id)),
                concat('DISTINCT:', toString(distinct_id))
            )
        ) AS viewer_id,
        job_role_codes
    FROM source_events
),
ranked_impressions AS (
    SELECT
        uuid,
        timestamp,
        event_date,
        exposure_id,
        category,
        position,
        event_type,
        audience_type,
        viewer_id,
        job_role_codes,
        row_number() OVER (
            PARTITION BY exposure_id, event_date, viewer_id
            ORDER BY timestamp, uuid
        ) AS impression_rank
    FROM typed_events
    WHERE event_type = 'IMPRESSION'
),
report_events AS (
    SELECT
        event_date, exposure_id, category, position, event_type, audience_type, viewer_id, job_role_codes
    FROM ranked_impressions
    WHERE impression_rank = 1
    UNION ALL
    SELECT
        event_date, exposure_id, category, position, event_type, audience_type, viewer_id, job_role_codes
    FROM typed_events
    WHERE event_type = 'CLICK'
),
ranked_daily_click_snapshots AS (
    SELECT
        event_date,
        exposure_id,
        category,
        position,
        event_type,
        audience_type,
        viewer_id,
        job_role_codes,
        row_number() OVER (
            PARTITION BY exposure_id, event_date, viewer_id
            ORDER BY timestamp, uuid
        ) AS click_snapshot_rank
    FROM typed_events
    WHERE event_type = 'CLICK'
),
daily_click_snapshots AS (
    SELECT
        event_date,
        exposure_id,
        category,
        position,
        event_type,
        audience_type,
        viewer_id,
        job_role_codes
    FROM ranked_daily_click_snapshots
    WHERE click_snapshot_rank = 1
),
summary_rows AS (
    SELECT
        'SUMMARY' AS record_type,
        CAST(NULL, 'Nullable(Date)') AS event_date,
        CAST(NULL, 'Nullable(String)') AS category,
        CAST(NULL, 'Nullable(String)') AS position,
        requested.event_type AS event_type,
        countIf(report_events.event_type = requested.event_type) AS total,
        uniqExactIf(report_events.viewer_id, report_events.event_type = requested.event_type) AS unique_users,
        CAST(NULL, 'Nullable(String)') AS job_role_code,
        CAST(NULL, 'Nullable(UInt64)') AS unique_clickers
    FROM (SELECT arrayJoin(['IMPRESSION', 'CLICK']) AS event_type) AS requested
    LEFT JOIN report_events ON report_events.event_type = requested.event_type
    GROUP BY requested.event_type
),
daily_rows AS (
    SELECT
        'DAILY' AS record_type,
        event_date,
        category,
        position,
        event_type,
        count() AS total,
        uniqExact(viewer_id) AS unique_users,
        CAST(NULL, 'Nullable(String)') AS job_role_code,
        CAST(NULL, 'Nullable(UInt64)') AS unique_clickers
    FROM report_events
    GROUP BY event_date, category, position, event_type
),
position_rows AS (
    SELECT
        'POSITION' AS record_type,
        CAST(NULL, 'Nullable(Date)') AS event_date,
        category,
        position,
        CAST(NULL, 'Nullable(String)') AS event_type,
        count() AS total,
        uniqExact(viewer_id) AS unique_users,
        CAST(NULL, 'Nullable(String)') AS job_role_code,
        CAST(NULL, 'Nullable(UInt64)') AS unique_clickers
    FROM report_events
    WHERE event_type = 'CLICK'
    GROUP BY category, position
),
member_role_rows AS (
    SELECT
        'JOB_ROLE' AS record_type,
        CAST(NULL, 'Nullable(Date)') AS event_date,
        CAST(NULL, 'Nullable(String)') AS category,
        CAST(NULL, 'Nullable(String)') AS position,
        CAST(NULL, 'Nullable(String)') AS event_type,
        CAST(NULL, 'Nullable(UInt64)') AS total,
        CAST(NULL, 'Nullable(UInt64)') AS unique_users,
        role_code AS job_role_code,
        uniqExact(viewer_id) AS unique_clickers
    FROM daily_click_snapshots
    ARRAY JOIN job_role_codes AS role_code
    WHERE audience_type = 'MEMBER'
    GROUP BY role_code
),
audience_role_rows AS (
    SELECT
        'JOB_ROLE' AS record_type,
        CAST(NULL, 'Nullable(Date)') AS event_date,
        CAST(NULL, 'Nullable(String)') AS category,
        CAST(NULL, 'Nullable(String)') AS position,
        CAST(NULL, 'Nullable(String)') AS event_type,
        CAST(NULL, 'Nullable(UInt64)') AS total,
        CAST(NULL, 'Nullable(UInt64)') AS unique_users,
        audience_role AS job_role_code,
        uniqExact(viewer_id) AS unique_clickers
    FROM (
        SELECT
            viewer_id,
            if(audience_type = 'NON_MEMBER', 'NON_MEMBER', 'UNSPECIFIED') AS audience_role
        FROM daily_click_snapshots
        WHERE audience_type IN ('NON_MEMBER', 'UNSPECIFIED') OR empty(job_role_codes)
    )
    GROUP BY audience_role
)
SELECT * FROM summary_rows
UNION ALL SELECT * FROM daily_rows
UNION ALL SELECT * FROM position_rows
UNION ALL SELECT * FROM member_role_rows
UNION ALL SELECT * FROM audience_role_rows
ORDER BY record_type, event_date, category, position, event_type, job_role_code
