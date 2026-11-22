-- 고용24 재등록 중복 정리 (LC-3645, 2026-11-18). 배포 뒤 한 번 돌린다
-- 같은 회사·같은 제목으로 구인인증번호만 새로 받아 다시 올린 고용24 공고 가운데
-- 가장 최근 것(고용24 등록일, 같으면 구인인증번호가 큰 것)만 남기고 나머지를 마감한다.
-- 서버 배포 뒤 새로 들어오는 재등록은 수집기가 마감한다. 이 SQL은 이미 쌓인 것만 한 번 정리한다.
-- 시각은 앱과 같은 한국 시각으로 적는다.

-- 1) 마감될 공고 미리 보기 (2026-11-18 사용자 API 기준 약 32쌍)
SELECT old.id, old.company_name, old.title, old.recruitment_start_at, old.recruitment_end_at,
       old.external_id, new.id AS kept_id, new.external_id AS kept_external_id
  FROM jobs old
  JOIN jobs new
    ON new.source = 'WORK24'
   AND new.company_name = old.company_name
   AND new.title = old.title
   AND new.recruitment_status = 'RECRUITING'
   AND new.deleted_at IS NULL
   AND new.id <> old.id
   AND (new.recruitment_start_at > old.recruitment_start_at
        OR (new.recruitment_start_at = old.recruitment_start_at AND new.external_id > old.external_id))
 WHERE old.source = 'WORK24'
   AND old.recruitment_status = 'RECRUITING'
   AND old.deleted_at IS NULL
 ORDER BY old.company_name, old.title, old.id;

-- 2) 마감
UPDATE jobs old
  JOIN jobs new
    ON new.source = 'WORK24'
   AND new.company_name = old.company_name
   AND new.title = old.title
   AND new.recruitment_status = 'RECRUITING'
   AND new.deleted_at IS NULL
   AND new.id <> old.id
   AND (new.recruitment_start_at > old.recruitment_start_at
        OR (new.recruitment_start_at = old.recruitment_start_at AND new.external_id > old.external_id))
   SET old.recruitment_status = 'CLOSED',
       old.closed_at = COALESCE(old.closed_at, CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00')),
       old.updated_at = CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00')
 WHERE old.source = 'WORK24'
   AND old.recruitment_status = 'RECRUITING'
   AND old.deleted_at IS NULL;
