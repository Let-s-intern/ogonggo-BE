-- 광고 성과 리포트 DB 배포 스크립트
--
-- 현재 DB 상태에 맞는 경로 하나만 실행한다. 파일 전체를 한 번에 실행하지 않는다.
-- 1) 신규 DB: A. 신규 DB 스키마
-- 2) 기존 3-table 스키마(contract_key 없음): B. 기존 3-table 마이그레이션
-- 3) 기존 4-table 스키마(별도 job_roles 테이블 있음): C를 실행한 뒤 B 실행
--
-- 기존 데이터의 contract_key는 현재 저장된 contract_id 기준으로 채운다.
-- 이미 같은 날짜에 계약이 바뀌어 한 행에 섞인 집계는 계약별로 복구할 수 없다.
-- 원본 이벤트와 고유 사용자 데이터는 1년 뒤 정리하고, 일별 합계는 유지한다.

-- ============================================================================
-- A. 신규 DB 스키마
-- ============================================================================

CREATE TABLE advertisement_events
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_date DATE NOT NULL,
    exposure_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    contract_id BIGINT NULL,
    contract_key BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    position VARCHAR(40) NULL,
    target_type VARCHAR(20) NULL,
    target_id BIGINT NULL,
    event_type VARCHAR(20) NOT NULL,
    event_at DATETIME(6) NOT NULL,
    member_id BIGINT NULL,
    anonymous_id VARCHAR(128) NOT NULL,
    viewer_key VARCHAR(140) NOT NULL,
    deduplication_key VARCHAR(140) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_advertisement_event_daily_impression (
        exposure_id,
        event_date,
        contract_key,
        event_type,
        deduplication_key
    ),
    KEY idx_advertisement_event_contract_date (contract_id, event_date),
    KEY idx_advertisement_event_exposure_date (exposure_id, event_date),
    KEY idx_advertisement_event_event_date (event_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE advertisement_daily_metrics
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_date DATE NOT NULL,
    exposure_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    contract_id BIGINT NULL,
    contract_key BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    position VARCHAR(40) NULL,
    event_type VARCHAR(20) NOT NULL,
    total_count BIGINT NOT NULL,
    unique_viewer_count BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_advertisement_daily_metric_exposure_type (
        event_date,
        exposure_id,
        contract_key,
        event_type
    ),
    KEY idx_advertisement_daily_metric_contract_date (contract_id, event_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE advertisement_event_daily_viewers
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_date DATE NOT NULL,
    exposure_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    contract_id BIGINT NULL,
    contract_key BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    position VARCHAR(40) NULL,
    target_type VARCHAR(20) NULL,
    event_type VARCHAR(20) NOT NULL,
    viewer_key VARCHAR(140) NOT NULL,
    member_id BIGINT NULL,
    anonymous_id VARCHAR(128) NOT NULL,
    audience_type VARCHAR(20) NOT NULL,
    job_roles TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_advertisement_daily_viewer (
        event_date,
        exposure_id,
        contract_key,
        event_type,
        viewer_key
    ),
    KEY idx_advertisement_daily_viewer_contract_date (contract_id, event_date),
    KEY idx_advertisement_daily_viewer_type_date (event_type, event_date),
    KEY idx_advertisement_daily_viewer_event_date (event_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ============================================================================
-- B. 기존 3-table 스키마 마이그레이션
--    기존 테이블에 contract_key와 event_date 정리 인덱스가 없는 경우 실행한다.
-- ============================================================================

ALTER TABLE advertisement_events
    ADD COLUMN contract_key BIGINT NULL AFTER contract_id;

UPDATE advertisement_events
SET contract_key = COALESCE(contract_id, 0);

ALTER TABLE advertisement_events
    MODIFY COLUMN contract_key BIGINT NOT NULL,
    DROP INDEX uk_advertisement_event_daily_impression,
    ADD UNIQUE KEY uk_advertisement_event_daily_impression (
        exposure_id,
        event_date,
        contract_key,
        event_type,
        deduplication_key
    ),
    ADD KEY idx_advertisement_event_event_date (event_date);

ALTER TABLE advertisement_daily_metrics
    ADD COLUMN contract_key BIGINT NULL AFTER contract_id;

UPDATE advertisement_daily_metrics
SET contract_key = COALESCE(contract_id, 0);

ALTER TABLE advertisement_daily_metrics
    MODIFY COLUMN contract_key BIGINT NOT NULL,
    DROP INDEX uk_advertisement_daily_metric_exposure_type,
    ADD UNIQUE KEY uk_advertisement_daily_metric_exposure_type (
        event_date,
        exposure_id,
        contract_key,
        event_type
    );

ALTER TABLE advertisement_event_daily_viewers
    ADD COLUMN contract_key BIGINT NULL AFTER contract_id;

UPDATE advertisement_event_daily_viewers
SET contract_key = COALESCE(contract_id, 0);

ALTER TABLE advertisement_event_daily_viewers
    MODIFY COLUMN contract_key BIGINT NOT NULL,
    DROP INDEX uk_advertisement_daily_viewer,
    ADD UNIQUE KEY uk_advertisement_daily_viewer (
        event_date,
        exposure_id,
        contract_key,
        event_type,
        viewer_key
    ),
    ADD KEY idx_advertisement_daily_viewer_event_date (event_date);

-- ============================================================================
-- C. 기존 4-table 스키마 변환
--    별도 job_roles 테이블을 고유 사용자 테이블에 합친다.
--    C 섹션은 한 연결에서 실행하고, 완료 후 B 섹션을 실행한다.
-- ============================================================================

ALTER TABLE advertisement_event_daily_viewers
    ADD COLUMN job_roles TEXT NULL;

SET @previous_group_concat_max_len = @@SESSION.group_concat_max_len;
SET SESSION group_concat_max_len = 8192;

UPDATE advertisement_event_daily_viewers AS viewers
JOIN (
    SELECT
        event_date,
        exposure_id,
        event_type,
        viewer_key,
        GROUP_CONCAT(DISTINCT job_role ORDER BY job_role SEPARATOR ',') AS job_roles
    FROM advertisement_event_daily_viewer_job_roles
    GROUP BY event_date, exposure_id, event_type, viewer_key
) AS roles
    ON roles.event_date = viewers.event_date
    AND roles.exposure_id = viewers.exposure_id
    AND roles.event_type = viewers.event_type
    AND roles.viewer_key = viewers.viewer_key
SET viewers.job_roles = roles.job_roles;

DROP TABLE advertisement_event_daily_viewer_job_roles;

SET SESSION group_concat_max_len = @previous_group_concat_max_len;
