-- 2026-09-28 기업 정보에 로고·담당자 연락처·정보 수신용 이메일 칼럼 추가
--
-- 마이페이지 기업/기관 정보 화면이 로고, 담당자 연락처, 정보 수신용 이메일을 받는다.
-- 가입 때는 받지 않고 PUT /api/v1/users/me/company-profile로만 채우므로 모두 null을 허용한다.
--
-- 운영은 ddl-auto=none이므로 사용자·관리자 API를 새 코드로 배포하기 전에 한 번 실행한다. 기존 행은 바뀌지 않는다.

-- 1. 이미 추가된 칼럼이 있는지 확인한다. 결과가 있으면 해당 칼럼은 빼고 실행한다.
show columns from company_profiles where Field in ('logo_url', 'manager_phone', 'notification_email');

-- 2. 칼럼 추가
alter table company_profiles
    add column logo_url           varchar(2048) null,
    add column manager_phone      varchar(20)   null,
    add column notification_email varchar(320)  null;
