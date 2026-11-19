-- LC-3647 광고 성과 리포트가 전날까지의 숫자를 읽는 일별 집계 테이블을 만든다.
-- 배포 전에 운영 DB에 적용한다. 사용자 API의 contentDailyStatAggregation 작업과 리포트 API가 이 테이블을 쓴다.
-- content_events에서 다시 만들 수 있는 파생 기록이다. 하루를 다시 모으면 그날 행을 지우고 새로 쓴다.
-- 대상 없는 배너 노출·클릭은 target_type·target_id가, 광고로 들어오지 않은 조회는 exposure_id가 비어 있다.
create table content_daily_stats (
    id bigint not null auto_increment,
    stat_date date not null,
    target_type varchar(20) null,
    target_id bigint null,
    exposure_id bigint null,
    source varchar(20) not null,
    views bigint not null,
    unique_visitors bigint not null,
    member_views bigint not null,
    member_unique_visitors bigint not null,
    cta_clicks bigint not null,
    cta_unique_visitors bigint not null,
    cta_unique_members bigint not null,
    scroll_count bigint not null,
    scroll_percent_sum bigint not null,
    duration_ms_sum bigint not null,
    scroll_reach_25 bigint not null,
    scroll_reach_50 bigint not null,
    scroll_reach_75 bigint not null,
    scroll_reach_100 bigint not null,
    impressions bigint not null,
    ad_clicks bigint not null,
    primary key (id),
    index idx_content_daily_stats_stat_date (stat_date),
    index idx_content_daily_stats_exposure_date (exposure_id, stat_date),
    index idx_content_daily_stats_target_date (target_type, target_id, stat_date)
) engine = InnoDB;
