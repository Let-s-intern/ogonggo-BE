-- 오늘의 공고(오공고픽) 추천 문구 설명을 50자에서 200자로 늘린다. 배포 전에 운영 DB에 먼저 적용한다.
alter table today_jobs modify column recommendation_description varchar(200) not null;
