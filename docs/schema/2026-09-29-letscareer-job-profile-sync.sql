-- 2026-09-29 렛츠커리어와 학력·희망 조건 양방향 동기화
--
-- 학력·희망 조건은 렛츠커리어와 오공고 양쪽에서 고칠 수 있고, 나중에 고친 쪽의 값으로 맞춘다.
-- user_profiles.job_info_updated_at: 학력·희망 조건의 최종 수정 일시. 렛츠커리어에서 받은 값은 렛츠커리어의 일시를 그대로 남긴다.
-- letscareer_job_profile_outbox: 렛츠커리어로 보낼 변경. 사용자당 한 행이며 전송 작업(letsCareerJobProfileSync)이 보내고 성공하면 지운다.
-- scheduled_jobs 행은 사용자 API가 기동할 때 없으면 기본값(30초마다)으로 만든다.
--
-- 운영은 ddl-auto=none이므로 사용자·관리자 API를 새 코드로 배포하기 전에 한 번 실행한다.
-- 기존 행의 job_info_updated_at은 null이며, 어느 쪽에서든 다음에 고칠 때 채워진다.

-- 1. 이미 적용됐는지 확인한다. 결과가 있으면 해당 부분은 빼고 실행한다.
show columns from user_profiles where Field = 'job_info_updated_at';
show tables like 'letscareer_job_profile_outbox';

-- 2. 학력·희망 조건 수정 일시
alter table user_profiles
    add column job_info_updated_at datetime(6) null;

-- 3. 렛츠커리어 전송 아웃박스
create table letscareer_job_profile_outbox
(
    id            bigint      not null auto_increment,
    user_id       bigint      not null,
    requested_at  datetime(6) not null,
    attempt_count int         not null,
    primary key (id),
    constraint uk_letscareer_job_profile_outbox_user_id unique (user_id)
);
