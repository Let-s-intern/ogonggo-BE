-- LC-3646 광고 성과 리포트가 셀 콘텐츠 이벤트(조회·CTA 클릭·스크롤·광고 노출·클릭) 테이블을 만든다.
-- 배포 전에 운영 DB에 적용한다. 사용자 API가 ddl-auto=none이라 테이블이 없으면 POST /api/v1/events가 실패한다.
-- 고치지 않는 기록이라 created_at·updated_at 없이 서버가 받은 시각(occurred_at, KST)만 둔다.
-- enum 값은 다른 업무 enum과 같이 이름을 varchar로 저장한다.
create table content_events (
    id bigint not null auto_increment,
    event_type varchar(20) not null,
    target_type varchar(20) null,
    target_id bigint null,
    exposure_id bigint null,
    user_id bigint null,
    visitor_id varchar(36) not null,
    source varchar(20) not null,
    scroll_percent int null,
    duration_ms bigint null,
    occurred_at datetime(6) not null,
    primary key (id),
    index idx_content_events_target_occurred_at (target_type, target_id, occurred_at),
    index idx_content_events_exposure_occurred_at (exposure_id, occurred_at),
    index idx_content_events_occurred_at (occurred_at)
) engine = InnoDB;
