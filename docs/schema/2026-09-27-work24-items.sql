-- 2026-09-27 고용24 목록 일일 수집 원본 테이블
--
-- 관리자 API의 고용24 수집 스케줄러가 쓴다. 관리자 API를 배포하기 전에 한 번 실행한다.
-- 이미 Hibernate update가 만들었다면 `if not exists`로 건너뛴다.
-- api 칼럼 타입은 Hibernate 6의 MySQL enum 기본 매핑에 맞췄다. Work24Api에 값을 추가하면 이 enum도 넓혀야 한다.
--
-- 스케줄러 잠금은 사용자 API와 같은 shedlock 테이블(2026-09-17-lc3309-shedlock.sql)을 쓴다.

create table if not exists work24_items
(
    id          bigint       not null auto_increment,
    api         enum ('RECRUITMENTS', 'TOMORROW_LEARNING_CARD_COURSES', 'TOMORROW_LEARNING_CARD_COURSE_DETAIL',
        'TOMORROW_LEARNING_CARD_COURSE_SCHEDULES', 'WORK_STUDY_COURSES', 'WORK_STUDY_COURSE_DETAIL',
        'WORK_STUDY_COURSE_SCHEDULES', 'GOVERNMENT_JOB_RECRUITMENTS', 'GOVERNMENT_JOB_RECRUITMENT_DETAIL',
        'GOVERNMENT_JOB_PROGRAMS', 'GOVERNMENT_JOB_PROGRAM_DETAIL', 'GOVERNMENT_JOB_INSTITUTIONS',
        'GOVERNMENT_JOB_PARTICIPANT_STATISTICS', 'JOB_SEEKER_PROGRAMS', 'OCCUPATIONS', 'OCCUPATION_DETAIL',
        'OCCUPATION_DICTIONARY', 'STANDARD_JOB_DESCRIPTIONS', 'DUTY_DATA_DICTIONARY', 'SMALL_GIANT_COMPANIES',
        'SMALL_GIANT_COMPANY_VISITS', 'YOUTH_SMALL_GIANT_COMPANY_EXPERIENCES',
        'YOUTH_FRIENDLY_SMALL_GIANT_COMPANIES') not null,
    external_id varchar(200) not null,
    payload     longtext     not null,
    created_at  datetime(6)  not null,
    updated_at  datetime(6)  not null,
    primary key (id),
    constraint uk_work24_item_api_external_id unique (api, external_id)
);
