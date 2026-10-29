-- LC-3526 기업 회원가입에서 받는 기업·담당자 정보를 company_profiles에 더한다. 배포 전에 운영 DB에 먼저 적용한다.
-- 기존 기업 회원 행이 있어 모두 nullable이다. enum 값은 다른 업무 enum과 같이 이름을 varchar로 저장한다.
-- 담당자 연락처는 기존 manager_phone을 쓴다.
alter table company_profiles
    add column business_number_type varchar(30) null,
    add column business_number varchar(13) null,
    add column company_type varchar(30) null,
    add column employee_count_range varchar(30) null,
    add column industry varchar(100) null,
    add column address varchar(255) null,
    add column homepage_url varchar(2048) null,
    add column manager_title varchar(50) null,
    add column manager_duty varchar(30) null,
    add column recommender varchar(100) null;
