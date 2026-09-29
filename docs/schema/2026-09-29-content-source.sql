-- 2026-09-29 채용공고·부트캠프 등록 경로(source)와 외부 식별값(external_id), 부트캠프 대표 이미지 선택 칸 전환
--
-- 사용자·관리자 API를 새 코드로 배포하기 전에 백업한 뒤 한 번만 실행한다. 새 코드는 두 칼럼에 값을 넣어 저장하므로
-- 칼럼이 없으면 공고·부트캠프 등록이 실패한다. 신규 DB는 Hibernate가 최종 스키마를 만들므로 실행하지 않는다.
-- enum 칼럼 타입은 Hibernate 6의 MySQL 기본 매핑(enum)에 맞췄다.

-- 1. 칼럼을 먼저 null 허용으로 추가한다.
alter table jobs
    add column source      enum ('CRAWLER', 'COMPANY', 'WORK24') null,
    add column external_id varchar(100)                           null;

alter table bootcamps
    add column source      enum ('CRAWLER', 'COMPANY', 'WORK24') null,
    add column external_id varchar(100)                           null;

-- 2. 지금까지의 계산 규칙(소유자가 있으면 비즈니스 등록, 없으면 크롤링)으로 채운다.
update jobs set source = if(owner_user_id is null, 'CRAWLER', 'COMPANY');
update bootcamps set source = if(owner_user_id is null, 'CRAWLER', 'COMPANY');

-- 3. 이미 고용24에서 수집한 행이 있으면 고용24로 바꾸고 원문 URL에서 식별값을 꺼낸다.
--    채용정보: wantedInfoUrl의 wantedAuthNo, 훈련과정: titleLink의 tracseId-tracseTme(과정 ID-회차)
--    먼저 대상 행을 확인한다. 0건이면 이 단계는 건너뛴다.
select 'jobs' as target, count(*)
from jobs
where owner_user_id is null
  and source_url like 'https://www.work24.go.kr/wk/a/b/1500/empDetailAuthView.do?wantedAuthNo=%'
union all
select 'bootcamps', count(*)
from bootcamps
where owner_user_id is null
  and source_url like 'https://www.work24.go.kr/hr/a/a/3100/%tracseId=%';

update jobs
set source      = 'WORK24',
    external_id = substring_index(substring_index(source_url, 'wantedAuthNo=', -1), '&', 1)
where owner_user_id is null
  and source_url like 'https://www.work24.go.kr/wk/a/b/1500/empDetailAuthView.do?wantedAuthNo=%';

update bootcamps
set source      = 'WORK24',
    external_id = concat(
        substring_index(substring_index(source_url, 'tracseId=', -1), '&', 1), '-',
        substring_index(substring_index(source_url, 'tracseTme=', -1), '&', 1)
    )
where owner_user_id is null
  and source_url like 'https://www.work24.go.kr/hr/a/a/3100/%tracseId=%';

-- 4. 빈 값이 없는지 확인한 뒤 not null과 유니크 제약을 건다. 유니크 제약은 NULL을 겹쳐도 허용하므로
--    외부 식별값이 없는 크롤링·비즈니스 등록 행에는 영향이 없다.
alter table jobs
    modify column source enum ('CRAWLER', 'COMPANY', 'WORK24') not null,
    add constraint uk_jobs_source_external_id unique (source, external_id);

alter table bootcamps
    modify column source enum ('CRAWLER', 'COMPANY', 'WORK24') not null,
    add constraint uk_bootcamps_source_external_id unique (source, external_id);

-- 5. 고용24 수집 부트캠프는 대표 이미지가 없어 비워 둔다. 기업회원·크롤러 요청은 여전히 대표 이미지를 받는다.
alter table bootcamps
    modify column representative_image_url varchar(2048) null;
