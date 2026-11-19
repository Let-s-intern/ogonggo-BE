-- 오공고 PICK 자리(JOB_PICK) 광고 공고에 운영자 PICK(today_jobs)과 같은 추천 문구 제목·설명을 더한다(LC-3650).
-- 배포 전에 적용한다. 길이는 today_jobs.recommendation_title(30)·recommendation_description(200)과 같다.
-- JOB_PICK 노출만 채우며 다른 자리와 배너는 null이다. 운영의 광고 공고 상품은 모두 자리가 없어 기존 행은 바꾸지 않는다.
alter table advertisement_exposures
    add column recommendation_title varchar(30) null after job_field,
    add column recommendation_description varchar(200) null after recommendation_title;
