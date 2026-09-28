-- 2026-09-14 크롤러 채용공고 등록 계약: 쓰지 않는 채용공고 칼럼 제거
--
-- 기존 DB에 한 번만 실행한다. 신규 DB는 Hibernate가 최종 스키마를 만들므로 실행하지 않는다.
-- Hibernate update는 칼럼을 지우지 않으므로 이 SQL이 없으면 두 칼럼이 남는다. 둘 다 null 허용이라 남아도 동작은 깨지지 않는다.
-- 사용자·관리자 API를 새 코드로 모두 배포한 뒤 백업하고 실행한다. 이전 코드가 떠 있는 동안 지우면 그 코드가 칼럼을 읽다 실패한다.
--
-- 기업 로고는 공고 대표 이미지(cover_image_url)로 합쳤고, 최대 경력 연수는 쓰지 않기로 했다.

-- 1. 지울 값이 있는지 먼저 확인한다. 0건이 아니면 값을 옮길지 담당자와 정한 뒤 진행한다.
select id, company_logo_url, cover_image_url, experience_min_years, experience_max_years
from jobs
where company_logo_url is not null
   or experience_max_years is not null;

-- 2. 칼럼 제거
alter table jobs
    drop column company_logo_url,
    drop column experience_max_years;
