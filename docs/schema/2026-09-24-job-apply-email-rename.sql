-- 2026-09-24 채용공고 지원 접수 이메일 칼럼 이름 변경: application_email -> apply_email
--
-- application이 앱으로도 읽혀 '지원'이라는 뜻이 드러나도록 이름을 바꿨다. 엔티티 속성과 API 필드도 applyEmail로 바뀌었다.
-- 기존 DB에 한 번만 실행한다. 신규 DB는 Hibernate가 최종 스키마를 만들므로 실행하지 않는다.
--
-- Hibernate update는 이름 변경을 모른다. 새 코드가 뜨면 빈 apply_email을 새로 만들고, 기존 값은 application_email에 남는다.
-- 사용자·관리자 API를 새 코드로 모두 배포한 뒤 백업하고 실행한다. 이전 코드가 떠 있는 동안 옛 칼럼을 지우면 그 코드가 칼럼을 읽다 실패한다.
-- 배포와 실행 사이에는 기존 공고의 지원 이메일이 비어 보이므로 배포 직후 바로 실행한다.
-- DDL_AUTO=validate로 운영하는 DB라면 이 절차 대신 배포 직전에 `alter table jobs rename column application_email to apply_email;`만 실행한다.

-- 1. 옮길 값을 확인한다. 두 칼럼에 서로 다른 값이 있는 행이 있으면 어느 값을 남길지 담당자와 정한 뒤 진행한다.
select id, application_email, apply_email
from jobs
where application_email is not null;

-- 2. 값 이관. 배포 뒤 새로 들어온 값은 덮어쓰지 않는다.
update jobs
set apply_email = application_email
where application_email is not null
  and apply_email is null;

-- 3. 옛 칼럼 제거
alter table jobs
    drop column application_email;
