-- MySQL 8. 사용자 토큰 등록·해제와 FCM 발송을 중단한 뒤 한 번만 실행한다.
-- 기존 users.fcm_token 컬럼이 있어야 한다. 운영 DB에 자동 적용되지 않는다.
-- 배포 순서와 롤백 주의사항: docs/architecture/notification-extension-guide.md

create table user_fcm_tokens (
    id bigint not null auto_increment,
    user_id bigint not null,
    device_id varchar(36) null,
    token varchar(4096) null,
    token_hash varchar(64) character set ascii collate ascii_bin null,
    active_slot varchar(64) character set ascii collate ascii_bin null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    deleted_at datetime(6) null,
    primary key (id),
    constraint uk_user_fcm_token_hash unique (token_hash),
    constraint uk_user_fcm_token_slot unique (active_slot),
    index idx_user_fcm_token_user (user_id, deleted_at, updated_at, id)
) engine = InnoDB;

alter table notifications modify column recipient_address varchar(4096) null;

-- 원문을 출력하지 않고 중복으로 제외할 토큰 수를 확인한다.
select count(*) as excluded_duplicate_token_count
from (
    select sha2(fcm_token, 256)
    from users
    where fcm_token is not null and char_length(trim(fcm_token)) > 0
    group by sha2(fcm_token, 256)
    having count(*) > 1
) duplicates;

-- 여러 회원에게 저장된 토큰은 누구의 것인지 추측하지 않고 전부 제외한다.
-- 다음 클라이언트 PUT으로 소유자를 확정한다. users.fcm_token은 수정·제거하지 않는다.
start transaction;
insert into user_fcm_tokens (user_id, device_id, token, token_hash, active_slot, created_at, updated_at, deleted_at)
select u.id, null, u.fcm_token, sha2(u.fcm_token, 256), concat('legacy:', u.id),
       u.created_at, u.updated_at, null
from users u
join (
    select sha2(fcm_token, 256) as token_hash
    from users
    where fcm_token is not null and char_length(trim(fcm_token)) > 0
    group by sha2(fcm_token, 256)
    having count(*) = 1
) unique_tokens on unique_tokens.token_hash = sha2(u.fcm_token, 256)
where u.fcm_token is not null and char_length(trim(u.fcm_token)) > 0;
commit;

-- 이관 후 확인. 주소·payload·토큰은 출력하지 않는다.
select count(*) as migrated_token_count from user_fcm_tokens where deleted_at is null;
