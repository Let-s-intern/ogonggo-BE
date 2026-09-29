-- 2026-09-29 일반 회원 프로필에 휴대폰 번호·오늘의 공고 수신 이메일 칼럼 추가
--
-- 마이페이지가 이름·휴대폰 번호·가입 이메일을 보여주고 오늘의 공고 수신 이메일을 받는다.
-- phone_num은 렛츠커리어 값의 사본으로 로그인마다 채워지고, notification_email은 오공고가 소유해
-- PUT /api/v1/users/me/notification-email로만 채운다. 둘 다 null을 허용한다.
--
-- 운영은 ddl-auto=none이므로 사용자·관리자 API를 새 코드로 배포하기 전에 한 번 실행한다.
-- 기존 행의 phone_num은 해당 사용자가 다음에 로그인할 때 채워진다.

-- 1. 이미 추가된 칼럼이 있는지 확인한다. 결과가 있으면 해당 칼럼은 빼고 실행한다.
show columns from user_profiles where Field in ('phone_num', 'notification_email');

-- 2. 칼럼 추가
alter table user_profiles
    add column phone_num          varchar(30)  null,
    add column notification_email varchar(320) null;
