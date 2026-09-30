-- 2026-09-30 부트캠프 상세 사진
--
-- 고용24에서 수집한 부트캠프에 훈련기관 시설 사진을 함께 저장한다. 상세 조회에서만 보여 주며 목록의 대표 이미지
-- (bootcamps.representative_image_url)와 따로 둔다. 사진 주소는 관리자 API가 고용24에서 받아 오공고 이미지 저장소로 옮긴 주소다.
-- bootcamp_images.caption: 훈련기관이 붙인 사진 설명(강의실, 안내데스크 등). 없으면 null.
--
-- 운영은 ddl-auto=none이므로 사용자·관리자 API를 새 코드로 배포하기 전에 한 번 실행한다.
-- 테이블이 없으면 부트캠프 상세 조회와 고용24 부트캠프 등록이 실패한다.

-- 1. 이미 적용됐는지 확인한다. 결과가 있으면 실행하지 않는다.
show tables like 'bootcamp_images';

-- 2. 부트캠프 상세 사진
create table bootcamp_images
(
    id            bigint        not null auto_increment,
    bootcamp_id   bigint        not null,
    image_url     varchar(2048) not null,
    caption       varchar(255),
    display_order int           not null,
    deleted_at    datetime(6),
    created_at    datetime(6)   not null,
    updated_at    datetime(6)   not null,
    primary key (id)
) engine = InnoDB;

create index idx_bootcamp_images_bootcamp on bootcamp_images (bootcamp_id, deleted_at, display_order);
