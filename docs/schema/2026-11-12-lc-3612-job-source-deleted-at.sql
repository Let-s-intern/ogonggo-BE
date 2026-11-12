-- 기업회원이 채용공고 원문 이미지(선택)를 올리고 뺄 수 있게 한다(LC-3612).
-- 원문 이미지는 job_sources 에 <img> 문단으로 저장하고, 모두 빼면 행을 지우지 않고 deleted_at 을 기록한다.
-- 다시 올리면 같은 행을 되살린다(deleted_at = null).

-- 사용자·관리자 API 배포 전에 적용한다. 새 칼럼이 없으면 원문을 읽는 모든 요청이 실패한다
alter table job_sources add column deleted_at datetime(6) null;
