-- LC-3661 광고 리포트가 같은 브라우저(visitor_id)의 회원 이벤트를 찾아 방문자를 하나로 잇는다.
-- 이 조회가 visitor_id와 occurred_at으로 들어오므로 인덱스를 더한다. 없어도 동작은 같지만 리포트 집계가 테이블 전체를 훑어 느려진다.
-- 배포 전에 운영 DB에 적용한다. 인덱스만 더하며 데이터는 바뀌지 않는다.
alter table content_events
    add index idx_content_events_visitor_occurred_at (visitor_id, occurred_at);
