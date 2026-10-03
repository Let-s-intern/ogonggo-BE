-- 2026-10-03 채용공고 고용 형태(employment_type)에 미래내일 일경험(WORK_EXPERIENCE) 추가
--
-- 기존 DB의 employment_type 칼럼은 Hibernate 6의 MySQL 기본 매핑(enum)으로 만들어져 있어, 목록에 없는 값을 넣으면
-- 저장이 실패한다(크롤러 등록이 500). 백업한 뒤 한 번만 실행한다. 신규 DB는 Hibernate가 최종 스키마를 만들므로
-- 실행하지 않는다. 2026-09-30에 추가한 일학습병행(WORK_STUDY)도 칼럼에 없으면 함께 들어간다.

-- 1. 지금 칼럼 정의를 확인한다.
show columns from jobs like 'employment_type';

-- 2. EmploymentType의 값 전부로 바꾼다. 기존 값은 그대로 남는다.
alter table jobs
    modify column employment_type
        enum ('FULL_TIME', 'CONTRACT', 'INTERN', 'PART_TIME', 'WORK_STUDY', 'WORK_EXPERIENCE', 'ETC') not null;
