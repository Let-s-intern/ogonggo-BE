-- LC-3309 북마크 카운터 증감 방식 전환 전후에 실행하는 읽기 전용 검증 SQL.
-- 두 조회가 모두 0건이어야 한다.

-- 지표 행은 있지만 활성 북마크 수와 bookmark_count가 다른 게시글.
select
    metric.post_id,
    metric.bookmark_count as metric_count,
    count(bookmark.id) as actual_count
from community_post_metrics metric
left join community_post_bookmarks bookmark
    on bookmark.post_id = metric.post_id
    and bookmark.deleted_at is null
group by metric.post_id, metric.bookmark_count
having metric.bookmark_count <> count(bookmark.id);

-- 활성 북마크가 있지만 지표 행이 없는 게시글.
select
    bookmark.post_id,
    count(*) as actual_count
from community_post_bookmarks bookmark
left join community_post_metrics metric
    on metric.post_id = bookmark.post_id
where bookmark.deleted_at is null
  and metric.id is null
group by bookmark.post_id;
