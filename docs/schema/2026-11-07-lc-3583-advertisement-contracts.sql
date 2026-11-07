-- LC-3583 광고 상품·계약·노출 테이블을 만들고, 기존 비즈니스 공고마다 광고 계약을 만든다.
-- enum 값은 다른 업무 enum과 같이 이름을 varchar로 저장한다.
--
-- 적용 순서
-- 1. 배포 전에 [1] 테이블 생성을 운영 DB에 적용한다. 두 API 모두 ddl-auto=none이라 테이블이 없으면 공고 등록·검수가 실패한다.
-- 2. 두 API를 배포한다.
-- 3. 배포 뒤 [2] 데이터 이관을 적용한다. 배포 전 구버전이 등록한 공고까지 계약을 만들도록 배포 뒤에 돌린다.
--    이미 계약이 있는 공고는 건너뛰므로 여러 번 돌려도 된다.

-- =========================================================================
-- [1] 테이블 생성
-- =========================================================================

create table advertisement_products (
    id bigint not null auto_increment,
    category varchar(30) not null,
    position varchar(40) null,
    name varchar(100) not null,
    price int null,
    deleted_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id)
) engine = InnoDB;

create table advertisement_contracts (
    id bigint not null auto_increment,
    source varchar(30) not null,
    status varchar(20) not null,
    user_id bigint null,
    promotion_target varchar(20) not null,
    target_id bigint null,
    company_name varchar(150) not null,
    recruitment_title varchar(255) not null,
    recruitment_url varchar(2000) null,
    recruitment_deadline date null,
    purpose varchar(30) null,
    schedule_request varchar(2000) null,
    advertisement_request varchar(2000) null,
    manager_name varchar(50) null,
    email varchar(255) null,
    phone_number varchar(20) null,
    invoice_method varchar(20) null,
    creative_method varchar(30) null,
    payment_amount int null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    constraint uk_advertisement_contracts_source_target unique (source, promotion_target, target_id),
    index idx_advertisement_contracts_status_created_at (status, created_at)
) engine = InnoDB;

create table advertisement_contract_categories (
    contract_id bigint not null,
    category varchar(30) not null,
    primary key (contract_id, category)
) engine = InnoDB;

create table advertisement_contract_products (
    id bigint not null auto_increment,
    contract_id bigint not null,
    product_id bigint not null,
    deleted_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    index idx_advertisement_contract_products_contract (contract_id)
) engine = InnoDB;

create table advertisement_exposures (
    id bigint not null auto_increment,
    product_id bigint not null,
    contract_id bigint null,
    name varchar(100) null,
    image_url varchar(2048) null,
    link_url varchar(2048) null,
    target_type varchar(20) null,
    target_id bigint null,
    ad_label bit not null,
    start_at datetime(6) null,
    end_at datetime(6) null,
    exposed bit not null,
    display_order int not null,
    deleted_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    index idx_advertisement_exposures_product_order (product_id, display_order)
) engine = InnoDB;

-- =========================================================================
-- [2] 데이터 이관: 기존 비즈니스 공고마다 광고 계약
-- =========================================================================
-- 비즈니스 공고 등록 상품을 하나 둔다. 맨 앞 우선 노출은 아직 광고 노출로 옮기지 않아 노출 자리는 비운다.
-- 비즈니스 공고 계약은 경로(source)로 이 상품임을 알 수 있어 계약 상품(advertisement_contract_products)은 붙이지 않는다.
insert into advertisement_products (category, position, name, price, created_at, updated_at)
select 'JOB_POSTING', null, '비즈니스 공고 등록', null, now(6), now(6)
from dual
where not exists (
    select 1 from advertisement_products where category = 'JOB_POSTING' and name = '비즈니스 공고 등록'
);

-- 계약 상태는 지금 검수 상태를 그대로 옮긴다(검수 대기 = 신청). 지운 공고도 판매 기록으로 남긴다.
-- 생성 일시는 공고 등록 일시로 맞춰 계약 목록이 실제 등록 순서대로 보이게 한다.
insert into advertisement_contracts
    (source, status, user_id, promotion_target, target_id, company_name, recruitment_title, recruitment_deadline,
     created_at, updated_at)
select 'COMPANY_POSTING',
       case j.review_status when 'APPROVED' then 'APPROVED' when 'REJECTED' then 'REJECTED' else 'REQUESTED' end,
       j.owner_user_id, 'JOB', j.id, j.company_name, j.title, date(j.recruitment_end_at),
       j.created_at, now(6)
from jobs j
where j.source = 'COMPANY'
  and j.owner_user_id is not null
  and not exists (
      select 1 from advertisement_contracts c
      where c.source = 'COMPANY_POSTING' and c.promotion_target = 'JOB' and c.target_id = j.id
  );

insert into advertisement_contracts
    (source, status, user_id, promotion_target, target_id, company_name, recruitment_title, recruitment_deadline,
     created_at, updated_at)
select 'COMPANY_POSTING',
       case b.review_status when 'APPROVED' then 'APPROVED' when 'REJECTED' then 'REJECTED' else 'REQUESTED' end,
       b.owner_user_id, 'BOOTCAMP', b.id, b.company_name, b.title, date(b.recruitment_end_at),
       b.created_at, now(6)
from bootcamps b
where b.source = 'COMPANY'
  and b.owner_user_id is not null
  and not exists (
      select 1 from advertisement_contracts c
      where c.source = 'COMPANY_POSTING' and c.promotion_target = 'BOOTCAMP' and c.target_id = b.id
  );

-- 확인: 비즈니스 공고 수와 비즈니스 공고 계약 수가 같아야 한다.
-- select (select count(*) from jobs where source = 'COMPANY' and owner_user_id is not null)
--      + (select count(*) from bootcamps where source = 'COMPANY' and owner_user_id is not null) as postings,
--        (select count(*) from advertisement_contracts where source = 'COMPANY_POSTING') as contracts;
