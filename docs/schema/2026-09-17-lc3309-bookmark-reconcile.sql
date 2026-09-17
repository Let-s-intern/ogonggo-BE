-- 2026-09-17-lc3309-bookmark-verify.sql에서 불일치가 확인된 경우에만 실행한다.
-- 실행 중 북마크 변경이 들어오지 않는 시간대를 사용하고, 완료 후 검증 SQL을 다시 실행한다.

start transaction;

-- 활성 북마크가 있지만 지표 행이 없는 게시글을 복구한다.
-- 댓글 수는 원본 행에서 복구하고, 원본이 없는 조회 수만 0으로 초기화한다.
insert into community_post_metrics
    (post_id, view_count, comment_count, bookmark_count, created_at, updated_at)
select
    bookmark.post_id,
    0,
    (
        select count(*)
        from recruitment_post_comments comment
        where comment.post_id = bookmark.post_id
    ),
    count(*),
    current_timestamp(6),
    current_timestamp(6)
from community_post_bookmarks bookmark
left join community_post_metrics metric
    on metric.post_id = bookmark.post_id
where bookmark.deleted_at is null
  and metric.id is null
group by bookmark.post_id;

-- 모든 기존 지표 행을 활성 북마크 수와 일치시킨다.
update community_post_metrics metric
left join (
    select post_id, count(*) as actual_count
    from community_post_bookmarks
    where deleted_at is null
    group by post_id
) bookmark_count on bookmark_count.post_id = metric.post_id
set
    metric.bookmark_count = coalesce(bookmark_count.actual_count, 0),
    metric.updated_at = current_timestamp(6)
where metric.bookmark_count <> coalesce(bookmark_count.actual_count, 0);

commit;
