-- LC-3647 광고 성과 리포트의 대상 공고를 계약에 연결하는 테이블을 만든다.
-- 배포 전에 운영 DB에 적용한다. 관리자 계약 상세(GET /api/v1/admin/advertisement-contracts/{contractId})가 이 테이블을 읽는다.
-- 비즈니스 공고 계약의 공고(advertisement_contracts.target_id)는 자동 대상이라 이 테이블에 넣지 않는다.
-- 연결을 풀면 deleted_at을 남기고, 같은 대상을 다시 연결하면 그 행을 되살린다.
create table advertisement_contract_targets (
    id bigint not null auto_increment,
    contract_id bigint not null,
    target_type varchar(20) not null,
    target_id bigint not null,
    deleted_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    constraint uk_advertisement_contract_targets_contract_target unique (contract_id, target_type, target_id)
) engine = InnoDB;
