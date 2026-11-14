-- 계약 상품에 게시 시작일·종료일(하루 단위, 종료일 포함)을 더한다(LC-3599).
-- 배포 전에 적용한다. 이미 붙어 있는 계약 상품은 기간이 없어 nullable이며, 기존 행은 바꾸지 않는다.
-- 새로 붙이는 상품은 관리자 API가 두 값을 필수로 받는다.
alter table advertisement_contract_products
    add column start_date date null after product_id,
    add column end_date date null after start_date;
