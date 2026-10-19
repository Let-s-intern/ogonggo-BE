-- LC-3478 공개 목록 정렬 키에 오늘의 공고(오공고 PICK) 직군 비트(2^31)를 둔다. 스키마는 바뀌지 않는다.
-- 예전 키의 2^31 자리는 무작위 값이었으므로, 배포한 뒤 지금 PICK에 맞춰 한 번 다시 쓴다.
-- 어드민에서 PICK을 한 번 다시 저장해도 같은 결과다. 공고 분석이 수정 일시를 보므로 updated_at은 바꾸지 않는다.

update jobs
set list_sort_key = list_sort_key - 2147483648
where deleted_at is null
  and mod(list_sort_key, 4294967296) >= 2147483648
  and (job_field is null or job_field not in (
      select job_field from (
          select distinct pick.job_field
          from today_jobs today
          join jobs pick on pick.id = today.job_id
          where today.deleted_at is null
            and pick.deleted_at is null
            and pick.publication_status = 'PUBLISHED'
            and pick.job_field is not null
      ) pick_fields
  ));

update jobs
set list_sort_key = list_sort_key + 2147483648
where deleted_at is null
  and mod(list_sort_key, 4294967296) < 2147483648
  and job_field in (
      select job_field from (
          select distinct pick.job_field
          from today_jobs today
          join jobs pick on pick.id = today.job_id
          where today.deleted_at is null
            and pick.deleted_at is null
            and pick.publication_status = 'PUBLISHED'
            and pick.job_field is not null
      ) pick_fields
  );
