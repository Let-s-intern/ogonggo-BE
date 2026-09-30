-- 2026-09-30 일반 회원 프로필 이미지와 기업 로고 변경
--
-- 일반 회원이 오공고 마이페이지에서 프로필 이미지를 바꿀 수 있다. 오공고가 소유하며 렛츠커리어에는 보내지 않는다.
-- user_profiles.profile_image_url은 렛츠커리어에서 복제하는 값이라 로그인 동기화가 덮어쓰므로 칸을 따로 둔다.
-- user_profiles.ogonggo_profile_image_id: 오공고에서 올린 프로필 이미지(image_assets.id). 바꾸거나 지우면 이전 이미지의 참조를 푼다.
-- user_profiles.ogonggo_profile_image_url: 그 이미지의 URL. 있으면 렛츠커리어 이미지 대신 보인다.
-- company_profiles.logo_image_id: 기업 로고로 연결한 이미지(image_assets.id). 로고를 바꾸거나 지우면 이전 이미지의 참조를 푼다.
--   기존 logo_url만 있는 행은 식별자가 null이라, 기본 정보를 고칠 때 logoImageId를 보내지 않으면 로고가 지워진다.
--
-- 운영은 ddl-auto=none이므로 사용자·관리자 API를 새 코드로 배포하기 전에 한 번 실행한다.
-- user_profiles의 기존 행은 두 칸이 null이며 지금처럼 렛츠커리어 이미지가 보인다.

-- 1. 이미 적용됐는지 확인한다. 결과가 있으면 실행하지 않는다.
show columns from user_profiles where Field in ('ogonggo_profile_image_id', 'ogonggo_profile_image_url');
show columns from company_profiles where Field = 'logo_image_id';

-- 2. 오공고 프로필 이미지
alter table user_profiles
    add column ogonggo_profile_image_id  varchar(36)   null,
    add column ogonggo_profile_image_url varchar(2048) null;

-- 3. 기업 로고 이미지 식별자
alter table company_profiles
    add column logo_image_id varchar(36) null;
