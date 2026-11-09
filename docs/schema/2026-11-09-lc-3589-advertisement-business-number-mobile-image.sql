-- LC-3589 광고 신청에 사업자 번호를, 배너 노출에 모바일 이미지를 더한다. 배포 전에 운영 DB에 먼저 적용한다.
-- 기존 행이 있고 값이 선택이라 모두 nullable이다. 사업자 번호는 company_profiles와 같은 타입이며,
-- enum 값은 다른 업무 enum과 같이 이름을 varchar로 저장한다. 모바일 이미지가 없으면 모바일에서도 image_url을 쓴다.
-- 카카오톡 CRM 카테고리(KAKAO_CRM)는 카테고리를 CHECK 제약 없는 varchar(30)에 저장하므로 스키마 변경이 없다.
alter table advertisement_contracts
    add column business_number_type varchar(30) null,
    add column business_number varchar(13) null;

alter table advertisement_exposures
    add column mobile_image_url varchar(2048) null;
