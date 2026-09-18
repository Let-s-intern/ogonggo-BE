-- 2026-09-18 채용공고 근무 지역을 비울 수 있게 한다
--
-- 기존 DB에 한 번만 실행한다. 신규 DB는 Hibernate가 최종 스키마를 만들므로 실행하지 않는다.
-- 엔티티는 region을 null 허용으로 바꿨지만 Hibernate update는 기존 NOT NULL 제약을 풀지 않는다.
-- 이 SQL이 없으면 근무 지역 없이 오는 크롤러 공고 등록이 `Column 'region' cannot be null`로 500이 된다.
-- 제약만 푸는 변경이라 기존 값은 바뀌지 않는다.

-- 1. 현재 상태를 확인한다. Null이 YES면 이미 적용된 DB이므로 실행하지 않는다.
show columns from jobs like 'region';

-- 2. 제약 해제. 길이와 문자셋은 그대로 둔다.
alter table jobs
    modify column region varchar(100) null;
