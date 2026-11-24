-- LC-3660 기능 플래그 테이블을 만들고 첫 플래그(광고 리포트 비용 효율 섹션)를 켠 채로 넣는다.
-- 배포 전에 운영 DB에 적용한다. 사용자·관리자 API가 ddl-auto=none이라 테이블이 없으면 GET /api/v1/feature-flags가 실패한다.
-- 플래그 하나가 행 하나다. 새 플래그는 그 플래그를 쓰는 기능의 배포 SQL에 insert로 넣고, 등록 API는 없다.
-- flag_key는 소문자 snake_case다. updated_by는 켜짐 여부를 마지막으로 바꾼 관리자의 회원 id이고 바꾼 적이 없으면 null이다.
-- 저장 시각은 KST다. 운영 DB의 now()는 KST가 아니어서 utc_timestamp(6) + interval 9 hour를 쓴다.
create table feature_flags (
    id bigint not null auto_increment primary key,
    flag_key varchar(100) not null,
    enabled boolean not null default false,
    description varchar(500) not null,
    updated_by bigint null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    constraint uk_feature_flags_flag_key unique (flag_key)
);

insert into feature_flags (flag_key, enabled, description, created_at, updated_at)
values ('ad_report_cost_efficiency', true, '비즈니스 광고 리포트의 비용 효율 섹션을 보인다',
        utc_timestamp(6) + interval 9 hour, utc_timestamp(6) + interval 9 hour);
