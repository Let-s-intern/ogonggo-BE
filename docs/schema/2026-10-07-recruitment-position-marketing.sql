-- RecruitmentPosition enum 이름 변경에 맞춰 기존 문자열 저장값을 변경한다.
update community_post_positions
set position = 'MARKETING'
where position = 'MOBILE';
