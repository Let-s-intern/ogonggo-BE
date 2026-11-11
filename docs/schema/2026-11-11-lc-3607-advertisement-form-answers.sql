-- 광고 신청 폼에서 칸이 따로 없는 문항의 답을 [{ key, label, value }] 목록으로 저장하는 JSON 칸을 더한다(LC-3607).
-- 배포 전에 적용한다. 기존 행과 비즈니스 공고 계약은 값이 없어 nullable이며, 기존 행은 바꾸지 않는다.
-- 이 칸 이전 신청의 schedule_request에 섞인 문항 답은 옮기지 않는다.
alter table advertisement_contracts add column form_answers json null;
