-- LC-3309 다중 인스턴스 스케줄러 잠금 테이블.
-- 애플리케이션은 이 테이블을 자동 생성하지 않으므로 운영 배포 전에 적용한다.

create table if not exists shedlock
(
    name       varchar(64)  not null,
    lock_until timestamp(3) not null,
    locked_at  timestamp(3) not null default current_timestamp(3),
    locked_by  varchar(255) not null,
    primary key (name)
);
