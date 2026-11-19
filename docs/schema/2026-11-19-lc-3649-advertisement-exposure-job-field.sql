-- 광고 노출에 채용공고 목록 자리(JOB_LIST)의 직군 탭을 더한다(LC-3649).
-- 배포 전에 적용한다. 값은 JobField enum 이름이고, JOB_LIST 노출만 채우며 다른 자리와 배너는 null이다.
-- 운영의 광고 공고 상품은 모두 자리가 없어 기존 행은 바꾸지 않는다.
alter table advertisement_exposures
    add column job_field varchar(40) null after target_id;
