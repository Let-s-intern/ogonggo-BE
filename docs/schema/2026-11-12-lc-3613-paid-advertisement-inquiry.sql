-- 광고 문의에 유료 광고 문의(PAID_ADVERTISEMENT)를 받는다. 배포 전에 적용한다.
-- 1) 희망 홍보 채널과 설문 답변은 무료 홍보 폼에만 있으므로 null을 허용한다. 기존 행은 값이 그대로 남는다.
--    배포 전 서버도 이 두 칸을 항상 채워 저장하므로 먼저 풀어도 문제가 없다.
alter table advertisement_inquiries modify column promotion_channel varchar(40) null;
alter table advertisement_inquiries modify column promotion_answer varchar(2000) null;

-- 2) 유료 광고 문의의 광고 목적(필수)과 희망 예산(원, 선택)이다. 무료 홍보 문의와 기존 행은 값이 없어 nullable이다.
alter table advertisement_inquiries add column purpose varchar(2000) null;
alter table advertisement_inquiries add column budget_amount int null;
