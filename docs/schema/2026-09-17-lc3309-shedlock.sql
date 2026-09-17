-- LC-3309 다중 인스턴스 스케줄러 잠금 테이블.
-- JPA 엔티티가 아니므로 Hibernate ddl-auto가 생성하지 않는다.
-- 현재 운영 버전(f422091e)이 참조하지 않는 테이블이므로 애플리케이션 배포 전에 실행한다.

create table if not exists shedlock
(
    name       varchar(64)  not null,
    lock_until timestamp(3) not null,
    locked_at  timestamp(3) not null default current_timestamp(3),
    locked_by  varchar(255) not null,
    primary key (name)
);
