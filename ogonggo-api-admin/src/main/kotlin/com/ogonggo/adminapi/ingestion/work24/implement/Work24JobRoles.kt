package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.core.job.domain.JobRole

/**
 * 고용24 직종코드(`jobsCd`, 6자리 세분류)를 오공고 직무로 옮기는 표다. 직군은 직무의 [JobRole.jobField]다.
 *
 * 고용24 직종 분류(고용24 공통코드 직종코드)와 오공고 직군·직무 분류는 체계가 달라, 세분류마다 가장 가까운 직무를 골랐다.
 * 세분류 이름으로 정하지 못한 코드는 중분류에 가까운 직무(대개 "기타")를 쓴다.
 * 표에 없는 코드는 직군·직무를 비운다. 고용24가 직종코드를 바꾸면 이 표도 함께 바꾼다.
 */
internal object Work24JobRoles {

    fun of(code: String?): JobRole? = code?.trim()?.let(BY_CODE::get)

    private val BY_CODE: Map<String, JobRole> = mapOf(
        "011100" to JobRole.PUBLIC_ADMINISTRATION, // 의회 의원
        "011200" to JobRole.PUBLIC_ADMINISTRATION, // 고위 공무원 및 정당‧특수 단체 임원
        "011300" to JobRole.PLANNING_MANAGEMENT_SUPPORT, // 기업 대표 및 기업 고위 임원
        "012100" to JobRole.PUBLIC_ADMINISTRATION, // 정부 행정 관리자
        "012200" to JobRole.PLANNING_MANAGEMENT_SUPPORT, // 경영지원 관리자
        "012201" to JobRole.PLANNING_BUSINESS_STRATEGY, // 경영기획 부서장
        "012202" to JobRole.HR_PLANNING, // 인사·노무·교육·총무·감사 부서장
        "012203" to JobRole.TRADE_PURCHASING, // 자재·구매 부서장
        "012204" to JobRole.ACCOUNTING_FINANCE, // 재무·회계·경리 부서장
        "012205" to JobRole.PLANNING_MANAGEMENT_SUPPORT, // 기타 경영지원 서비스 관리자
        "012300" to JobRole.MARKETING_STRATEGY, // 마케팅 및 광고‧홍보 관리자(부서장)
        "012400" to JobRole.BANKING_ETC, // 금융 및 보험 관리자(부서장)
        "013100" to JobRole.ENGINEERING_ETC, // 연구 관리자(부서장)
        "013200" to JobRole.EDUCATION_ETC, // 교육 관리자(부서장)
        "013300" to JobRole.LEGAL_ETC, // 법률‧경찰‧소방 및 교도 관리자
        "013400" to JobRole.MEDICAL_HOSPITAL_ADMIN, // 보건 의료 관련 관리자(부서장)
        "013500" to JobRole.PUBLIC_SOCIAL_SERVICE, // 사회복지 관련 관리자(부서장)
        "013600" to JobRole.MEDIA_ETC, // 문화‧예술 관련 관리자(부서장)
        "013700" to JobRole.IT_DEV_PM, // 정보 통신 관련 관리자(부서장)
        "013900" to JobRole.PLANNING_ETC, // 기타 전문 서비스 관리자
        "013901" to JobRole.SALES_GENERAL, // 부동산·임대업 관리자
        "013902" to JobRole.PRODUCTION_FACILITY_MANAGEMENT, // 아파트 관리소장
        "013903" to JobRole.PRODUCTION_FACILITY_MANAGEMENT, // 빌딩 관리소장
        "013904" to JobRole.HR_RECRUITER, // 조사, 인력공급·알선 및 기타 전문서비스 관리자(부서장)
        "014100" to JobRole.SERVICE_HOTEL, // 숙박·여행·오락 및 스포츠 관련 관리자
        "014101" to JobRole.SERVICE_HOTEL, // 미용·여행·숙박·오락 서비스 관리자
        "014102" to JobRole.SERVICE_ETC, // 스포츠 관리자
        "014200" to JobRole.FOOD_STORE_OPERATION, // 음식 서비스 관련 관리자
        "014300" to JobRole.SERVICE_SECURITY_GUARD, // 환경·청소 및 경비 관련 관리자
        "014301" to JobRole.SERVICE_SECURITY_GUARD, // 경비 관리자
        "014302" to JobRole.SERVICE_SANITATION, // 환경·청소 관리자
        "015100" to JobRole.SALES_MANAGEMENT_SUPPORT, // 영업 및 판매 관련 관리자
        "015101" to JobRole.SALES_MANAGEMENT_SUPPORT, // 영업 관리자(영업소장·지점장)
        "015102" to JobRole.SALES_MANAGEMENT_SUPPORT, // 판매 관리자(소규모 판매점장 제외)
        "015200" to JobRole.TRANSPORT_DISPATCH, // 운송 관련 관리자(부서장)
        "015900" to JobRole.CS_CX_MANAGER, // 기타 판매 및 고객 서비스 관리자
        "016100" to JobRole.CONSTRUCTION_DESIGN_SUPERVISION, // 건설 및 광업 관련 관리자(부서장)
        "016200" to JobRole.ENGINEERING_ENERGY, // 전기·가스 및 수도 관련 관리자(부서장)
        "016300" to JobRole.PRODUCTION_PRODUCTION, // 제품 생산 관련 관리자
        "016301" to JobRole.PRODUCTION_PRODUCTION, // 기계·금속·비금속 제조 관리자(공장장)
        "016302" to JobRole.PRODUCTION_PRODUCTION, // 화학 제조 관리자(공장장)
        "016303" to JobRole.PRODUCTION_PRODUCTION, // 섬유·의류 제조 관리자(공장장)
        "016304" to JobRole.PRODUCTION_PRODUCTION, // 전기·전자 제조 관리자(공장장)
        "016305" to JobRole.FOOD_FOOD_PROCESSING_DEVELOPMENT, // 식품가공 제조 관리자(공장장)
        "016306" to JobRole.PRODUCTION_PRODUCTION, // 기타 제품 제조 관리자(공장장)
        "016900" to JobRole.PRODUCTION_PRODUCTION, // 기타 건설·전기 및 생산 관련 관리자
        "016901" to JobRole.PRODUCTION_EQUIPMENT, // 수리·정비 관리자(부서장)
        "016902" to JobRole.PRODUCTION_PRODUCTION, // 농림어업 및 기타 생산 관리자
        "021100" to JobRole.PUBLIC_ADMINISTRATION, // 정부 행정 전문가
        "021200" to JobRole.LEGAL_ETC, // 행정사
        "022100" to JobRole.PLANNING_CONSULTING, // 경영 및 진단 전문가(경영 컨설턴트, M&A·창업·인증심사·FTA)
        "022201" to JobRole.HR_LABOR_RELATIONS, // 인사·노무 전문가
        "022202" to JobRole.HR_HRD_CULTURE, // HRD·교육·훈련 전문가
        "023100" to JobRole.ACCOUNTING_ACCOUNTING, // 회계사
        "023200" to JobRole.ACCOUNTING_TAX, // 세무사
        "023301" to JobRole.TRADE_CUSTOMS_BROKER, // 관세사
        "023302" to JobRole.TRADE_IMPORT_EXPORT, // FTA 원산지관리사
        "023400" to JobRole.PLANNING_ETC, // 감정 관련 전문가
        "023401" to JobRole.PLANNING_ETC, // 감정평가사
        "023402" to JobRole.PLANNING_ETC, // 감정사(문화재·보석·문서·음식료품 및 조향사)
        "024101" to JobRole.MARKETING_PR, // 광고·홍보 전문가
        "024102" to JobRole.MARKETING_STRATEGY, // 마케팅 전문가
        "024200" to JobRole.PLANNING_BUSINESS_DEVELOPMENT, // 조사 전문가
        "024301" to JobRole.MD_PRODUCT_PLANNING, // 상품 기획자
        "024302" to JobRole.MD_ETC, // 머천다이저(MD)
        "024400" to JobRole.MARKETING_EXHIBITION_EVENT, // 행사·전시 및 회의 기획자
        "024401" to JobRole.MARKETING_EXHIBITION_EVENT, // 행사·이벤트 기획자
        "024402" to JobRole.MARKETING_EXHIBITION_EVENT, // 전시·회의 기획자
        "025100" to JobRole.PUBLIC_ADMINISTRATION, // 조세 행정 사무원
        "025200" to JobRole.TRADE_IMPORT_EXPORT, // 관세 행정 사무원
        "025300" to JobRole.PUBLIC_ADMINISTRATION, // 병무 행정 사무원
        "025900" to JobRole.PUBLIC_ADMINISTRATION, // 기타 정부 행정 사무원
        "025901" to JobRole.PUBLIC_ADMINISTRATION, // 국가·지방행정 사무원
        "025902" to JobRole.PUBLIC_ADMINISTRATION, // 군무원
        "026100" to JobRole.PLANNING_BUSINESS_STRATEGY, // 경영 기획 사무원
        "026200" to JobRole.MARKETING_ETC, // 영업 및 마케팅 사무원
        "026201" to JobRole.MARKETING_ETC, // 마케팅·광고·홍보·상품기획 사무원
        "026202" to JobRole.SALES_MANAGEMENT_SUPPORT, // 영업 기획·관리·지원 사무원
        "026203" to JobRole.SALES_GENERAL, // 분양·임대 사무원
        "026300" to JobRole.HR_PLANNING, // 인사 및 노무 사무원
        "026301" to JobRole.HR_PLANNING, // 인사 사무원
        "026302" to JobRole.HR_LABOR_RELATIONS, // 노무 사무원
        "026400" to JobRole.HR_HRD_CULTURE, // 교육 및 훈련 사무원
        "026500" to JobRole.HR_GENERAL_AFFAIRS_SECRETARY, // 총무 사무원 및 대학 행정조교
        "026501" to JobRole.HR_GENERAL_AFFAIRS_SECRETARY, // 총무 및 일반 사무원
        "026502" to JobRole.MEDICAL_HOSPITAL_ADMIN, // 병원행정 사무원(원무)
        "026503" to JobRole.EDUCATION_STAFF_ASSISTANT, // 학교행정 사무원(교무)
        "026504" to JobRole.EDUCATION_STAFF_ASSISTANT, // 대학 행정조교
        "026505" to JobRole.HR_GENERAL_AFFAIRS_SECRETARY, // 협회·회원단체 사무원
        "026506" to JobRole.SERVICE_ETC, // 기숙사사감 및 독서실·고시원 총무
        "026600" to JobRole.LEGAL_INTERNAL_AUDIT, // 감사 사무원
        "027100" to JobRole.ACCOUNTING_ACCOUNTING, // 회계 사무원
        "027101" to JobRole.ACCOUNTING_FINANCE, // 재무 사무원
        "027102" to JobRole.ACCOUNTING_ACCOUNTING, // 회계 사무원(회계·세무 사무소)
        "027103" to JobRole.ACCOUNTING_ACCOUNTING, // 회계 사무원(일반 사업체)
        "027200" to JobRole.ACCOUNTING_BOOKKEEPING, // 경리 사무원
        "027201" to JobRole.ACCOUNTING_BOOKKEEPING, // 경리 사무원(무역)
        "027202" to JobRole.ACCOUNTING_BOOKKEEPING, // 경리 사무원(운수)
        "027203" to JobRole.ACCOUNTING_BOOKKEEPING, // 경리 사무원(건설)
        "027204" to JobRole.ACCOUNTING_BOOKKEEPING, // 경리 사무원(제조)
        "027205" to JobRole.ACCOUNTING_BOOKKEEPING, // 경리 사무원(회계·세무 사무소)
        "027206" to JobRole.ACCOUNTING_BOOKKEEPING, // 경리 사무원(일반사업체)
        "027207" to JobRole.ACCOUNTING_BOOKKEEPING, // 경리 사무원(아파트·빌딩)
        "027208" to JobRole.ACCOUNTING_BOOKKEEPING, // 단순 경리 사무원
        "028100" to JobRole.TRADE_IMPORT_EXPORT, // 무역 사무원
        "028101" to JobRole.TRADE_IMPORT_EXPORT, // 무역 사무원(영어)
        "028102" to JobRole.TRADE_IMPORT_EXPORT, // 무역 사무원(중국어)
        "028103" to JobRole.TRADE_IMPORT_EXPORT, // 무역 사무원(일본어)
        "028104" to JobRole.TRADE_IMPORT_EXPORT, // 무역 사무원(기타 언어)
        "028105" to JobRole.TRADE_IMPORT_EXPORT, // 관세 사무원
        "028106" to JobRole.TRADE_IMPORT_EXPORT, // FTA 관리 사무원
        "028200" to JobRole.TRANSPORT_DISPATCH, // 도로 및 철도운송 사무원
        "028201" to JobRole.TRANSPORT_DISPATCH, // 도로 운송 사무원(배차사무 포함)
        "028202" to JobRole.TRANSPORT_ETC, // 철도·지하철 운송 사무원
        "028300" to JobRole.TRANSPORT_ETC, // 수상 및 항공운송 사무원
        "028301" to JobRole.TRANSPORT_ETC, // 항공 운송 사무원
        "028302" to JobRole.TRANSPORT_ETC, // 수상 운송 사무원(해상 운송)
        "028400" to JobRole.TRADE_INVENTORY, // 자재관리 사무원
        "028401" to JobRole.TRADE_PURCHASING, // 자재·구매 사무원(건설)
        "028402" to JobRole.TRADE_PURCHASING, // 자재·구매 사무원(기계·자동차·금속)
        "028403" to JobRole.TRADE_PURCHASING, // 자재·구매 사무원(화학·섬유·의류)
        "028404" to JobRole.TRADE_PURCHASING, // 자재·구매 사무원(전기·전자·컴퓨터)
        "028405" to JobRole.TRADE_PURCHASING, // 자재·구매 사무원(음식료품)
        "028406" to JobRole.TRADE_PURCHASING, // 자재·구매 사무원(일반 사업체)
        "028407" to JobRole.TRADE_WAREHOUSE_PACKING, // 창고 관리원(자재 검수원 포함)
        "028408" to JobRole.TRADE_LOGISTICS_SCM, // 물류 사무원(물류 관리사)
        "028500" to JobRole.PRODUCTION_PRODUCTION, // 생산 및 품질관리 사무원
        "028501" to JobRole.CONSTRUCTION_OFFICE_ADMIN, // 생산관리 사무원(건설)
        "028502" to JobRole.PRODUCTION_PRODUCTION, // 생산관리 사무원(기계·자동차·금속)
        "028503" to JobRole.PRODUCTION_PRODUCTION, // 생산관리 사무원(화학·섬유·의료)
        "028504" to JobRole.PRODUCTION_PRODUCTION, // 생산관리 사무원(전기·전자·컴퓨터)
        "028505" to JobRole.PRODUCTION_PRODUCTION, // 생산관리 사무원(음식료품)
        "028506" to JobRole.PRODUCTION_PRODUCTION, // 생산관리 사무원(그 외 분야)
        "028507" to JobRole.CONSTRUCTION_SAFETY_QUALITY_MATERIAL, // 품질관리 사무원(건설)
        "028508" to JobRole.PRODUCTION_QUALITY, // 품질관리 사무원(기계·자동차·금속)
        "028509" to JobRole.PRODUCTION_QUALITY, // 품질관리 사무원(화학·섬유·의료)
        "028510" to JobRole.PRODUCTION_QUALITY, // 품질관리 사무원(전기·전자·컴퓨터)
        "028511" to JobRole.PRODUCTION_QUALITY, // 품질관리 사무원(음식료품)
        "028512" to JobRole.PRODUCTION_QUALITY, // 품질관리 사무원(그 외 분야)
        "028900" to JobRole.TRADE_IMPORT_EXPORT, // 기타 운송 및 무역 사무원
        "029103" to JobRole.SERVICE_HOTEL, // 호텔·콘도·숙박시설 프론트 사무원
        "029202" to JobRole.CS_INBOUND, // 콜센터 상담원(콜센터·고객센터·CS센터)
        "029210" to JobRole.MEDICAL_HOSPITAL_ADMIN, // 병원 코디네이터
        "029212" to JobRole.HR_RECRUITER, // 취업 알선원
        "029902" to JobRole.MEDIA_PUBLISHING, // 출판·자료 편집 사무원
        "110100" to JobRole.PUBLIC_EDUCATION_RESEARCH, // 인문과학 연구원
        "110200" to JobRole.PUBLIC_EDUCATION_RESEARCH, // 사회과학 연구원
        "121100" to JobRole.ENGINEERING_ETC, // 자연과학 연구원
        "121200" to JobRole.ENGINEERING_ETC, // 자연과학 시험원
        "122101" to JobRole.ENGINEERING_BIO_PHARMA, // 생명과학 연구원
        "122102" to JobRole.ENGINEERING_ETC, // 농어업 연구원 및 기술자
        "122103" to JobRole.ENGINEERING_ETC, // 임업 및 산림 연구원 및 기술자
        "122200" to JobRole.ENGINEERING_BIO_PHARMA, // 생명과학 시험원
        "122300" to JobRole.ENGINEERING_ETC, // 농림어업 관련 시험원
        "131100" to JobRole.IT_HARDWARE_ENGINEER, // 컴퓨터 하드웨어 기술자 및 연구원
        "131200" to JobRole.ENGINEERING_TELECOM_NETWORK, // 통신공학 기술자 및 연구원
        "131201" to JobRole.ENGINEERING_TELECOM_NETWORK, // 통신기기·장비 개발자 및 연구원
        "131202" to JobRole.ENGINEERING_TELECOM_NETWORK, // 통신기술 개발자 및 통신망 운영 기술자
        "131203" to JobRole.ENGINEERING_TELECOM_NETWORK, // 통신공사 감리 기술자
        "132000" to JobRole.IT_ETC, // 컴퓨터시스템 전문가
        "132001" to JobRole.PLANNING_CONSULTING, // IT 컨설턴트
        "132002" to JobRole.IT_SOFTWARE_ENGINEER, // 컴퓨터시스템 설계 및 분석가
        "132003" to JobRole.IT_ETC, // IT 감리 전문가(시스템 감리)
        "133100" to JobRole.IT_SYSTEM_SOFTWARE, // 시스템 소프트웨어 개발자
        "133101" to JobRole.IT_SYSTEM_SOFTWARE, // 시스템 소프트웨어 개발자(프로그래머)
        "133102" to JobRole.IT_EMBEDDED, // 펌웨어 및 임베디드 소프트웨어 프로그래머
        "133200" to JobRole.IT_APPLICATION, // 응용 소프트웨어 개발자
        "133201" to JobRole.IT_BACKEND, // JAVA 프로그래밍 언어 전문가
        "133202" to JobRole.IT_SOFTWARE_ENGINEER, // C언어 및 그 외 프로그래밍 언어 전문가
        "133203" to JobRole.IT_ERP_SAP, // 범용 응용 소프트웨어 프로그래머(ERP,정보처리,재무관리 등)
        "133204" to JobRole.IT_APPLICATION, // 산업특화 응용 소프트웨어 프로그래머(국방,항공,교통,에너지,금융,자동차 등)
        "133205" to JobRole.IT_SYSTEM_NETWORK, // 네트워크 프로그래머
        "133206" to JobRole.GAME_CLIENT, // 게임 프로그래머
        "133207" to JobRole.IT_CROSS_PLATFORM, // 모바일 애플리케이션 프로그래머(앱·어플 개발)
        "133300" to JobRole.IT_FULLSTACK, // 웹 개발자
        "133301" to JobRole.IT_FULLSTACK, // 웹 개발자(웹 엔지니어·웹 프로그래머)
        "133302" to JobRole.PLANNING_SERVICE_PRODUCT_PLANNING, // 웹 기획자
        "133900" to JobRole.IT_QA, // 기타 컴퓨터 시스템 및 소프트웨어 전문가(IT 테스터 및 IT QA 전문가 등)
        "134100" to JobRole.IT_SYSTEM_NETWORK, // 네트워크 시스템 개발자
        "134200" to JobRole.IT_SECURITY, // 정보보안 전문가
        "134201" to JobRole.IT_SECURITY, // 정보보호 전문가
        "134202" to JobRole.IT_SECURITY, // 침해사고 대응 전문가
        "134900" to JobRole.IT_SECURITY, // 기타 네트워크 및 정보 보안 전문가(디지털 포렌식 전문가 등)
        "135100" to JobRole.AI_DATA_ENGINEER, // 데이터 시스템 전문가
        "135101" to JobRole.AI_DATA_ENGINEER, // 데이터 설계 및 프로그래머
        "135102" to JobRole.IT_DBA, // 데이터베이스 운영·관리자
        "135200" to JobRole.AI_DATA_ANALYST, // 데이터 분석가(빅데이터 분석가)
        "136101" to JobRole.IT_SYSTEM_NETWORK, // 정보시스템 운영자
        "136102" to JobRole.IT_SYSTEM_NETWORK, // 네트워크 관리자(클라우딩컴퓨터운영관리자)
        "136103" to JobRole.IT_ETC, // IT 기술지원 전문가
        "136200" to JobRole.IT_ETC, // 웹 운영자(홈페이지 관리자)
        "137000" to JobRole.MEDIA_TRANSMISSION_PROGRAMMING, // 통신 및 방송 송출 장비 기사
        "140100" to JobRole.CONSTRUCTION_ARCHITECTURAL_DESIGN, // 건축가(건축설계 포함)
        "140200" to JobRole.CONSTRUCTION_ARCHITECTURAL_DESIGN, // 건축공학 기술자
        "140201" to JobRole.CONSTRUCTION_ARCHITECTURAL_DESIGN, // 건축 현장소장
        "140202" to JobRole.CONSTRUCTION_ARCHITECTURAL_DESIGN, // 건축구조 기술자
        "140203" to JobRole.CONSTRUCTION_ARCHITECTURAL_DESIGN, // 건축시공 기술자(건축견적 포함)
        "140204" to JobRole.CONSTRUCTION_MEP_FIRE, // 건축설비 기술자
        "140205" to JobRole.CONSTRUCTION_ARCHITECTURAL_DESIGN, // 건축감리 기술자
        "140206" to JobRole.CONSTRUCTION_SAFETY_QUALITY_MATERIAL, // 건축안전·환경·품질·에너지관리 기술자
        "140300" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 토목공학 기술자
        "140301" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 토목 현장소장
        "140302" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 토목구조 설계 기술자
        "140303" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 토목시공 기술자(토목견적 포함)
        "140304" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 토목감리 기술자
        "140305" to JobRole.CONSTRUCTION_SAFETY_QUALITY_MATERIAL, // 토목안전·환경·품질 기술자
        "140400" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 조경 기술자
        "140500" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 도시 및 교통 관련 전문가
        "140501" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 도시 계획·설계가
        "140502" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 교통 계획·설계·안전·영향평가 전문가
        "140600" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 측량 및 공간정보 전문가
        "140601" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 측량사
        "140602" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 지도제작 및 공간정보시스템 전문가
        "140700" to JobRole.CONSTRUCTION_SAFETY_QUALITY_MATERIAL, // 건설자재 시험원
        "151100" to JobRole.ENGINEERING_MECHANICAL, // 기계공학 기술자 및 연구원
        "151101" to JobRole.ENGINEERING_MECHANICAL, // 산업기계공학 기술자 및 연구원
        "151102" to JobRole.ENGINEERING_MECHANICAL, // 건설기계공학 기술자 및 연구원
        "151103" to JobRole.ENGINEERING_MECHANICAL, // 금형공학 기술자 및 연구원
        "151104" to JobRole.ENGINEERING_MECHANICAL, // 플랜트공학 기술자 및 연구원
        "151105" to JobRole.ENGINEERING_MECHANICAL, // 냉난방·공조공학 기술자 및 연구원
        "151106" to JobRole.ENGINEERING_SHIPBUILDING_AEROSPACE, // 조선·해양공학 기술자 및 연구원
        "151107" to JobRole.ENGINEERING_SHIPBUILDING_AEROSPACE, // 항공기·철도차량 공학 기술자 및 연구원
        "151108" to JobRole.ENGINEERING_AUTOMOTIVE, // 자동차공학 기술자 및 연구원
        "151109" to JobRole.ENGINEERING_MECHANICAL, // 기타 기계공학 기술자 및 연구원
        "151110" to JobRole.ENGINEERING_MECHANICAL, // 기계감리 기술자
        "151200" to JobRole.ENGINEERING_MECHANICAL, // 로봇공학 기술자 및 연구원
        "151300" to JobRole.ENGINEERING_MECHANICAL, // 기계 및 로봇공학 시험원
        "152100" to JobRole.ENGINEERING_METAL_STEEL, // 금속·재료공학 기술자 및 연구원
        "152200" to JobRole.ENGINEERING_METAL_STEEL, // 금속 및 재료공학 시험원
        "159100" to JobRole.ENGINEERING_MECHANICAL_DESIGN_CAD, // 제도사
        "211100" to JobRole.EDUCATION_PROFESSOR_LECTURER, // 대학 교수
        "211200" to JobRole.EDUCATION_PROFESSOR_LECTURER, // 대학 강사(초빙,겸임,BK,강의전담 교수 등 비전임교원 포함)
        "212100" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 중·고등학교 교사
        "212101" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 국어 교사
        "212102" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 수학 교사
        "212103" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 사회 교사
        "212104" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 과학 교사
        "212105" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 예체능 교사
        "212106" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 실업 교사
        "212107" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 외국어 교사
        "212108" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 진로·교양·기타 교사
        "212109" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 기타 중·고등학교 교사
        "212200" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 초등학교 교사
        "212300" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 특수교육 교사
        "212900" to JobRole.EDUCATION_CONTRACT_PRIVATE_TEACHER, // 기타 교사
        "213000" to JobRole.EDUCATION_KINDERGARTEN_CHILDCARE, // 유치원 교사
        "214100" to JobRole.EDUCATION_LANGUAGE_INSTRUCTOR, // 외국어 강사
        "214101" to JobRole.EDUCATION_LANGUAGE_INSTRUCTOR, // 영어 강사
        "214102" to JobRole.EDUCATION_LANGUAGE_INSTRUCTOR, // 중국어 강사
        "214103" to JobRole.EDUCATION_LANGUAGE_INSTRUCTOR, // 일본어 강사
        "214104" to JobRole.EDUCATION_LANGUAGE_INSTRUCTOR, // 한국어 강사(다문화 언어지도사 포함)
        "214105" to JobRole.EDUCATION_LANGUAGE_INSTRUCTOR, // 그 외 외국어 강사
        "214200" to JobRole.EDUCATION_LANGUAGE_INSTRUCTOR, // 문리 강사
        "214201" to JobRole.EDUCATION_LANGUAGE_INSTRUCTOR, // 국어 강사
        "214202" to JobRole.EDUCATION_ACADEMY_EXAM_INSTRUCTOR, // 수학 강사
        "214203" to JobRole.EDUCATION_ACADEMY_EXAM_INSTRUCTOR, // 사회 강사
        "214204" to JobRole.EDUCATION_ACADEMY_EXAM_INSTRUCTOR, // 과학 강사
        "214205" to JobRole.EDUCATION_ACADEMY_EXAM_INSTRUCTOR, // 성인고시·입사시험 학원 강사
        "214206" to JobRole.EDUCATION_LANGUAGE_INSTRUCTOR, // 독서·논술 강사
        "214300" to JobRole.EDUCATION_TECHNICAL_INSTRUCTOR, // 정보통신기술 강사
        "214301" to JobRole.EDUCATION_TECHNICAL_INSTRUCTOR, // 컴퓨터 기초 강사(OA, 워드, 엑셀, 컴퓨터활용능력 등)
        "214302" to JobRole.EDUCATION_TECHNICAL_INSTRUCTOR, // 프로그래밍·웹·웹디자인·DB 강사
        "214400" to JobRole.EDUCATION_TECHNICAL_INSTRUCTOR, // 기술 및 기능계 강사
        "214401" to JobRole.EDUCATION_TECHNICAL_INSTRUCTOR, // 디자인·캐드 강사
        "214402" to JobRole.EDUCATION_TECHNICAL_INSTRUCTOR, // 이미용·예식 강사
        "214403" to JobRole.EDUCATION_TECHNICAL_INSTRUCTOR, // 요리 강사
        "214404" to JobRole.EDUCATION_TECHNICAL_INSTRUCTOR, // 기계·금속·전기·전자 강사
        "214405" to JobRole.EDUCATION_TECHNICAL_INSTRUCTOR, // 건설토목강사
        "214406" to JobRole.EDUCATION_TECHNICAL_INSTRUCTOR, // 기타 기술·기능 강사
        "214500" to JobRole.EDUCATION_TECHNICAL_INSTRUCTOR, // 간호조무 및 요양보호 강사
        "214600" to JobRole.EDUCATION_TECHNICAL_INSTRUCTOR, // 자동차 운전 강사
        "214700" to JobRole.EDUCATION_ETC, // 예능 강사
        "214701" to JobRole.EDUCATION_ETC, // 미술 강사
        "214702" to JobRole.EDUCATION_ETC, // 음악 강사
        "214703" to JobRole.EDUCATION_ETC, // 무용 강사
        "214704" to JobRole.EDUCATION_ETC, // 기타 예능 강사(바둑·웅변·연기·꽃꽂이·종이접기 등)
        "214900" to JobRole.EDUCATION_ETC, // 기타 문리·기술 및 예능 강사(교육연수기관 및 기업체내 강사 포함)
        "215100" to JobRole.EDUCATION_ETC, // 장학관·연구관 및 교육 관련 전문가
        "215101" to JobRole.EDUCATION_ETC, // 장학관·연구관·입학사정관
        "215102" to JobRole.EDUCATION_INSTRUCTIONAL_DESIGN, // 교재·교구 및 e-learning 교육 전문가
        "215200" to JobRole.EDUCATION_STAFF_ASSISTANT, // 대학 교육 조교(연구 조교 포함)
        "215300" to JobRole.EDUCATION_ETC, // 교사보조 및 관련 종사원
        "215900" to JobRole.PUBLIC_CHILD_YOUTH_WELFARE, // 기타 교사보조 및 아동돌봄 종사원
        "221100" to JobRole.LEGAL_ETC, // 판사 및 검사
        "221200" to JobRole.LEGAL_LAWYER, // 변호사
        "221300" to JobRole.LEGAL_LEGAL_AFFAIRS, // 법무사 및 집행관
        "221400" to JobRole.LEGAL_PATENT_ATTORNEY, // 변리사
        "221900" to JobRole.LEGAL_LEGAL_AFFAIRS, // 기타 법률 전문가(외국 변호사 등)
        "222000" to JobRole.LEGAL_LEGAL_AFFAIRS, // 법률 관련 사무원
        "222001" to JobRole.LEGAL_LEGAL_AFFAIRS, // 법률 사무원(법원, 로펌, 법무사무소 등)
        "222002" to JobRole.LEGAL_PATENT_IP, // 특허·저작권 사무원
        "222003" to JobRole.LEGAL_LEGAL_AFFAIRS, // 법무 사무원(일반 기업체)
        "231100" to JobRole.PUBLIC_SOCIAL_SERVICE, // 사회복지사
        "231101" to JobRole.PUBLIC_SOCIAL_SERVICE, // 사회복지사(사회복지시설)
        "231102" to JobRole.PUBLIC_SOCIAL_SERVICE, // 사회복지사(공공행정)
        "231103" to JobRole.PUBLIC_SOCIAL_SERVICE, // 사회복지프로그램 운영자
        "231104" to JobRole.EDUCATION_AFTER_SCHOOL_PART_TIME, // 방과후 교사 및 지도사
        "231200" to JobRole.PUBLIC_COUNSELING, // 상담 전문가
        "231201" to JobRole.PUBLIC_COUNSELING, // 심리상담 전문가
        "231202" to JobRole.PUBLIC_COUNSELING, // 가족·학교·아동·청소년·노인·중독 등 상담 전문가
        "231300" to JobRole.PUBLIC_CHILD_YOUTH_WELFARE, // 청소년 지도사
        "231400" to JobRole.PUBLIC_COUNSELING, // 직업 관련 상담사
        "231500" to JobRole.PUBLIC_ETC, // 시민사회 활동가
        "232100" to JobRole.EDUCATION_KINDERGARTEN_CHILDCARE, // 보육교사
        "232200" to JobRole.PUBLIC_CHILD_YOUTH_WELFARE, // 보육 관련 시설 돌봄 종사원
        "232900" to JobRole.PUBLIC_SOCIAL_SERVICE, // 기타 사회복지 전문가 및 관련 종사원
        "232901" to JobRole.PUBLIC_ELDERLY_WOMEN_WELFARE, // 노인 생활지도원
        "232902" to JobRole.PUBLIC_CHILD_YOUTH_WELFARE, // 아동 생활지도원
        "232903" to JobRole.PUBLIC_SOCIAL_SERVICE, // 장애인 생활지도원(장애인활동보조원 포함)
        "232904" to JobRole.PUBLIC_SOCIAL_SERVICE, // 그 외 사회복지 종사원
        "233100" to JobRole.PUBLIC_RELIGION, // 성직자
        "233900" to JobRole.PUBLIC_RELIGION, // 기타 종교 관련 종사원
        "240100" to JobRole.PUBLIC_PUBLIC_SAFETY, // 경찰관 및 수사관
        "240200" to JobRole.PUBLIC_PUBLIC_SAFETY, // 소방관
        "240300" to JobRole.PUBLIC_PUBLIC_SAFETY, // 소년원 학교 교사 및 교도관
        "250100" to JobRole.PUBLIC_PUBLIC_SAFETY, // 영관급 이상 장교
        "250200" to JobRole.PUBLIC_PUBLIC_SAFETY, // 위관급 장교
        "250300" to JobRole.PUBLIC_PUBLIC_SAFETY, // 준사관
        "250400" to JobRole.PUBLIC_PUBLIC_SAFETY, // 부사관
        "250900" to JobRole.PUBLIC_PUBLIC_SAFETY, // 기타 군인
        "301100" to JobRole.MEDICAL_DOCTOR, // 전문 의사
        "301200" to JobRole.MEDICAL_DOCTOR, // 일반 의사
        "301300" to JobRole.MEDICAL_KOREAN_MEDICINE_DOCTOR, // 한의사
        "301400" to JobRole.MEDICAL_DOCTOR, // 치과의사
        "302000" to JobRole.MEDICAL_VETERINARIAN, // 수의사
        "303100" to JobRole.MEDICAL_PHARMACIST, // 약사
        "303200" to JobRole.MEDICAL_PHARMACIST, // 한약사
        "304000" to JobRole.MEDICAL_NURSE, // 간호사
        "304001" to JobRole.MEDICAL_NURSE, // 전문 간호사
        "304002" to JobRole.MEDICAL_NURSE, // 일반 간호사
        "304003" to JobRole.MEDICAL_NURSE, // 보건교사
        "305000" to JobRole.MEDICAL_DIETITIAN, // 영양사
        "306100" to JobRole.MEDICAL_RADIOLOGY_CLINICAL_LAB, // 임상병리사
        "306200" to JobRole.MEDICAL_RADIOLOGY_CLINICAL_LAB, // 방사선사
        "306300" to JobRole.MEDICAL_ETC, // 치과기공사
        "306400" to JobRole.MEDICAL_DENTAL_HYGIENIST, // 치과위생사
        "306500" to JobRole.MEDICAL_PHYSICAL_OCCUPATIONAL_THERAPY, // 물리 및 작업치료사
        "306501" to JobRole.MEDICAL_PHYSICAL_OCCUPATIONAL_THERAPY, // 물리치료사
        "306502" to JobRole.MEDICAL_PHYSICAL_OCCUPATIONAL_THERAPY, // 작업치료사
        "306600" to JobRole.MEDICAL_ETC, // 임상심리사(심리치료사)
        "306700" to JobRole.MEDICAL_ETC, // 재활공학 기사(의지보조기기사)
        "306800" to JobRole.MEDICAL_ETC, // 언어 및 청각능력 재활사
        "306900" to JobRole.MEDICAL_RADIOLOGY_CLINICAL_LAB, // 기타 치료·재활사 및 의료기사
        "306901" to JobRole.MEDICAL_ETC, // 놀이·음악·미술·독서 등 치료사
        "306902" to JobRole.MEDICAL_ETC, // 운동처방사
        "307100" to JobRole.MEDICAL_EMT, // 응급 구조사(인명구조원, 119구조대원 포함)
        "307200" to JobRole.MEDICAL_ETC, // 위생사
        "307300" to JobRole.MEDICAL_OPTICIAN, // 안경사
        "307400" to JobRole.MEDICAL_HOSPITAL_ADMIN, // 보건의료정보 관리사(의무기록사)
        "307500" to JobRole.MEDICAL_NURSE_AIDE, // 간호조무사
        "307501" to JobRole.MEDICAL_NURSE_AIDE, // 간호조무사(요양병원 제외)
        "307502" to JobRole.MEDICAL_NURSE_AIDE, // 요양병원 간호조무사
        "307600" to JobRole.MEDICAL_ETC, // 환자안전 관리사
        "307700" to JobRole.MEDICAL_ETC, // 수의사 보조원
        "307800" to JobRole.SERVICE_MASSAGE_BODY_CARE, // 안마사
        "307900" to JobRole.MEDICAL_ETC, // 기타 돌봄 및 보건 서비스 종사원
        "307901" to JobRole.MEDICAL_ETC, // 의료 보조원
        "307902" to JobRole.MEDICAL_POSTPARTUM_CARE, // 산후조리 종사원(산모 도우미)
        "307903" to JobRole.MEDICAL_ETC, // 기타 보건·의료 서비스 종사원
        "411100" to JobRole.MEDIA_CONTENT_PLANNING_EDITOR, // 작가
        "411200" to JobRole.MEDIA_TRANSLATION_INTERPRETATION, // 번역가 및 통역가
        "411201" to JobRole.MEDIA_TRANSLATION_INTERPRETATION, // 번역가(영어)
        "411202" to JobRole.MEDIA_TRANSLATION_INTERPRETATION, // 번역가(중국어)
        "411203" to JobRole.MEDIA_TRANSLATION_INTERPRETATION, // 번역가(일본어)
        "411204" to JobRole.MEDIA_TRANSLATION_INTERPRETATION, // 번역가(기타 언어)
        "411205" to JobRole.MEDIA_TRANSLATION_INTERPRETATION, // 통역가(영어)
        "411206" to JobRole.MEDIA_TRANSLATION_INTERPRETATION, // 통역가(중국어)
        "411207" to JobRole.MEDIA_TRANSLATION_INTERPRETATION, // 통역가(일본어)
        "411208" to JobRole.MEDIA_TRANSLATION_INTERPRETATION, // 통역가(기타 언어)
        "411209" to JobRole.MEDIA_TRANSLATION_INTERPRETATION, // 수화 및 의료 통역가
        "411300" to JobRole.MEDIA_PUBLISHING, // 출판물 전문가
        "412000" to JobRole.MEDIA_REPORTER, // 기자 및 언론 관련 전문가
        "412001" to JobRole.MEDIA_REPORTER, // 신문·방송 기자
        "412002" to JobRole.MEDIA_REPORTER, // 잡지·생활정보지 및 기타 기자
        "413100" to JobRole.MEDIA_EXHIBITION_CURATOR, // 학예사 및 문화재 보존원
        "413101" to JobRole.MEDIA_EXHIBITION_CURATOR, // 학예사(큐레이터)
        "413102" to JobRole.MEDIA_EXHIBITION_CURATOR, // 문화재보존원(컨서베이터)
        "413200" to JobRole.PUBLIC_ETC, // 사서 및 기록물관리사
        "413201" to JobRole.PUBLIC_ETC, // 사서
        "413202" to JobRole.PUBLIC_ETC, // 기록물 관리사(기록물관리전문요원)
        "414100" to JobRole.MEDIA_ETC, // 화가 및 조각가
        "414200" to JobRole.MEDIA_PHOTOGRAPHER, // 사진기자 및 사진사
        "414300" to JobRole.MEDIA_WEBTOON_WEB_NOVEL, // 만화가 및 만화영화 작가
        "414301" to JobRole.MEDIA_WEBTOON_WEB_NOVEL, // 만화가
        "414302" to JobRole.MEDIA_CG_MOTION_GRAPHICS, // 만화영화 작가(애니메이터)
        "414400" to JobRole.MEDIA_MUSIC_RECORDS, // 국악 및 전통 예능인
        "414500" to JobRole.MEDIA_MUSIC_RECORDS, // 지휘자‧작곡가 및 연주가
        "414600" to JobRole.MEDIA_MUSIC_RECORDS, // 가수 및 성악가
        "414700" to JobRole.MEDIA_ETC, // 무용가 및 안무가
        "414900" to JobRole.MEDIA_ETC, // 기타 시각 및 공연 예술가
        "414901" to JobRole.MEDIA_ETC, // 조련사(공연)·마술사 등 기타 시각 및 공연 예술가
        "414902" to JobRole.MEDIA_ETC, // 동화구연사
        "415100" to JobRole.DESIGN_INDUSTRIAL_PRODUCT, // 제품 디자이너
        "415101" to JobRole.DESIGN_INDUSTRIAL_PRODUCT, // 기계·자동차·금속 제품 디자이너
        "415102" to JobRole.DESIGN_INDUSTRIAL_PRODUCT, // 조명·전기·전자·통신 기기 디자이너
        "415103" to JobRole.DESIGN_INDUSTRIAL_PRODUCT, // 가구 디자이너
        "415104" to JobRole.DESIGN_INDUSTRIAL_PRODUCT, // 팬시, 귀금속, 생활잡화 등 기타 제품 디자이너
        "415200" to JobRole.DESIGN_FASHION_TEXTILE, // 패션 디자이너
        "415201" to JobRole.DESIGN_FASHION_TEXTILE, // 의상 디자이너
        "415202" to JobRole.DESIGN_FASHION_TEXTILE, // 직물(텍스타일) 디자이너
        "415203" to JobRole.DESIGN_FASHION_TEXTILE, // 가방·신발·엑세서리 디자이너
        "415300" to JobRole.DESIGN_SPACE_INTERIOR_VMD, // 실내장식 디자이너
        "415301" to JobRole.DESIGN_SPACE_INTERIOR_VMD, // 인테리어 디자이너
        "415302" to JobRole.DESIGN_SPACE_INTERIOR_VMD, // 무대·세트·디스플레이 디자이너
        "415400" to JobRole.DESIGN_GRAPHIC_VISUAL, // 시각 디자이너
        "415401" to JobRole.DESIGN_ADVERTISING_CONTENT, // 광고디자이너
        "415402" to JobRole.DESIGN_PUBLISHING_EDITORIAL, // 책·인쇄물·출판 디자이너
        "415403" to JobRole.DESIGN_PACKAGE, // 포장디자이너
        "415404" to JobRole.DESIGN_ILLUSTRATOR, // 일러스트레이터·삽화가
        "415405" to JobRole.DESIGN_GRAPHIC_VISUAL, // 색채전문가(컬러리스트)
        "415406" to JobRole.DESIGN_GRAPHIC_VISUAL, // 기타 시각 디자이너
        "415500" to JobRole.DESIGN_ADVERTISING_CONTENT, // 미디어 콘텐츠 디자이너
        "415501" to JobRole.DESIGN_WEB, // 웹 디자이너
        "415502" to JobRole.GAME_PLANNING_PM, // 게임 디자이너
        "415503" to JobRole.DESIGN_VFX_3D, // 그래픽아트 기술자
        "415504" to JobRole.DESIGN_UX_UI_PRODUCT, // UX/UI 디자이너
        "415505" to JobRole.DESIGN_ADVERTISING_CONTENT, // 기타 미디어 콘텐츠 디자이너
        "416100" to JobRole.MEDIA_PD_DIRECTOR, // 감독 및 기술 감독
        "416200" to JobRole.MEDIA_MODEL_ACTOR, // 배우 및 모델
        "416301" to JobRole.MEDIA_ANNOUNCER_SHOW_HOST, // 아나운서 및 리포터
        "416302" to JobRole.MEDIA_ANNOUNCER_SHOW_HOST, // 쇼핑호스트
        "416303" to JobRole.MEDIA_ANNOUNCER_SHOW_HOST, // 사내(장내) 방송 아나운서
        "416400" to JobRole.MEDIA_CINEMATOGRAPHER, // 촬영기사
        "416500" to JobRole.MEDIA_BROADCAST_ENGINEERING, // 음향 및 녹음 기사
        "416600" to JobRole.MEDIA_VIDEO_EDITOR, // 영상·녹화 및 편집 기사
        "416700" to JobRole.MEDIA_BROADCAST_ENGINEERING, // 조명기사 및 영사기사
        "416800" to JobRole.MEDIA_CREATOR_INFLUENCER, // 미디어 콘텐츠 창작자
        "416900" to JobRole.MEDIA_ETC, // 기타 연극·영화 및 영상 관련 종사원
        "416901" to JobRole.MEDIA_PD_DIRECTOR, // 방송연출 보조(AD, FD)
        "416902" to JobRole.MEDIA_ETC, // 엑스트라,소품·무대의상 관리 등 기타 연극·영화·방송 종사원
        "417100" to JobRole.MEDIA_DISTRIBUTION_PRODUCTION, // 공연 및 시각예술 기획자
        "417200" to JobRole.MEDIA_DISTRIBUTION_PRODUCTION, // 영화 및 음반 기획자
        "417300" to JobRole.MEDIA_ARTIST_MANAGEMENT, // 연예인 및 스포츠 매니저
        "420100" to JobRole.SERVICE_ETC, // 스포츠 감독 및 코치
        "420200" to JobRole.SERVICE_ETC, // 직업 운동선수
        "420300" to JobRole.SERVICE_ETC, // 경기 심판 및 경기 기록원
        "420400" to JobRole.SERVICE_ETC, // 스포츠 강사 및 트레이너
        "420401" to JobRole.SERVICE_ETC, // 태권도·검도·합기도 등 무술사범
        "420402" to JobRole.SERVICE_ETC, // 수영·골프·테니스·배드민턴·탁구 등 스포츠 강사
        "420403" to JobRole.SERVICE_ETC, // 헬스 트레이너
        "420404" to JobRole.SERVICE_ETC, // 에어로빅, 요가 등 기타 스포츠 강사
        "420500" to JobRole.SERVICE_ETC, // 기타 스포츠 및 레크리에이션 관련 전문가
        "420501" to JobRole.SERVICE_ETC, // 레크리에이션 전문가
        "420502" to JobRole.SERVICE_ETC, // 프로모터, 경주마 조련사 등 기타 스포츠 관련 전문가
        "420900" to JobRole.SERVICE_ETC, // 기타 여가 서비스 종사원
        "420901" to JobRole.SERVICE_ETC, // 골프장 캐디
        "420902" to JobRole.SERVICE_ETC, // 카지노 딜러
        "420903" to JobRole.SERVICE_ETC, // 응원단원 및 기타 스포츠·오락 관련 도우미
        "511100" to JobRole.SERVICE_HAIR_DESIGNER, // 이용사
        "511200" to JobRole.SERVICE_HAIR_DESIGNER, // 미용사
        "511300" to JobRole.SERVICE_MAKEUP_NAIL, // 네일 관리사(손톱 관리사)
        "511400" to JobRole.SERVICE_SKIN_CARE, // 피부 및 체형 관리사
        "511401" to JobRole.SERVICE_SKIN_CARE, // 피부 및 체형 관리사(발관리사 포함)
        "511402" to JobRole.SERVICE_MASSAGE_BODY_CARE, // 목욕관리 및 기타 피부미용 종사원
        "511500" to JobRole.SERVICE_MAKEUP_NAIL, // 메이크업 아티스트 및 분장사
        "511501" to JobRole.SERVICE_MAKEUP_NAIL, // 메이크업아티스트 및 뷰티매니저
        "511502" to JobRole.SERVICE_MAKEUP_NAIL, // 분장사·특수분장사
        "511900" to JobRole.SERVICE_ETC, // 기타 미용 관련 서비스 종사원(패션코디, 이미지컨설턴트 등)
        "512100" to JobRole.SERVICE_WEDDING_PLANNER, // 결혼 상담원 및 웨딩플래너
        "512101" to JobRole.SERVICE_WEDDING_PLANNER, // 결혼 상담원
        "512102" to JobRole.SERVICE_WEDDING_PLANNER, // 웨딩 플래너
        "512200" to JobRole.SERVICE_WEDDING_PLANNER, // 혼례 종사원
        "512300" to JobRole.SERVICE_ETC, // 장례 상담원 및 장례 지도사
        "512400" to JobRole.SERVICE_ETC, // 점술가 및 민속신앙 종사원
        "512900" to JobRole.SERVICE_ETC, // 기타 개인 생활 서비스 종사원(말벗, 노년플래너 등)
        "513100" to JobRole.SERVICE_PET_GROOMING_TRAINING, // 반려동물 훈련사 및 행동 상담사
        "513200" to JobRole.SERVICE_PET_GROOMING_TRAINING, // 반려동물 관리 종사원
        "513201" to JobRole.SERVICE_PET_GROOMING_TRAINING, // 반려동물 미용사
        "513202" to JobRole.SERVICE_PET_GROOMING_TRAINING, // 반려동물 장의사
        "521100" to JobRole.SERVICE_TOURISM, // 여행 상품 개발자
        "521200" to JobRole.SERVICE_TOURISM, // 여행 사무원
        "521300" to JobRole.SERVICE_TOURISM, // 관광 서비스 종사원
        "521301" to JobRole.SERVICE_TOURISM, // 관광통역 안내원
        "521302" to JobRole.SERVICE_TOURISM, // 여행 안내원
        "521303" to JobRole.SERVICE_TOURISM, // 박물관,미술관,문화,역사,자연환경 등 각종 해설사
        "522100" to JobRole.SERVICE_AIRLINE, // 항공기 객실 승무원
        "522200" to JobRole.SERVICE_TOURISM, // 선박 및 열차 객실 승무원
        "523000" to JobRole.SERVICE_HOTEL, // 숙박시설 서비스 종사원
        "524000" to JobRole.SERVICE_ETC, // 오락시설 서비스 종사원
        "524001" to JobRole.SERVICE_ETC, // 오락시설 서비스원(노래방, PC방 등)
        "524002" to JobRole.SERVICE_ETC, // 테마파크 서비스원
        "531100" to JobRole.FOOD_KITCHEN_COOKING, // 주방장 및 요리 연구가(푸드코디네이터 포함)
        "531200" to JobRole.FOOD_KITCHEN_COOKING, // 한식 조리사
        "531300" to JobRole.FOOD_KITCHEN_COOKING, // 중식 조리사
        "531400" to JobRole.FOOD_KITCHEN_COOKING, // 양식 조리사
        "531500" to JobRole.FOOD_KITCHEN_COOKING, // 일식 조리사
        "531600" to JobRole.FOOD_BEVERAGE_LIQUOR, // 바텐터(조주사)
        "531700" to JobRole.FOOD_BEVERAGE_LIQUOR, // 음료 조리원(바리스타 포함)
        "531800" to JobRole.FOOD_KITCHEN_COOKING, // 단체급식 조리사
        "531801" to JobRole.FOOD_KITCHEN_COOKING, // 학교 급식 조리사
        "531802" to JobRole.FOOD_KITCHEN_COOKING, // 유치원·어린이집 급식 조리사
        "531803" to JobRole.FOOD_KITCHEN_COOKING, // 병원 급식 조리사
        "531804" to JobRole.FOOD_KITCHEN_COOKING, // 사업체 구내식당 급식 조리사
        "531900" to JobRole.FOOD_KITCHEN_COOKING, // 기타 조리사
        "531901" to JobRole.FOOD_KITCHEN_COOKING, // 분식 조리사
        "531902" to JobRole.FOOD_KITCHEN_COOKING, // 포장마차·주점 조리사
        "531903" to JobRole.FOOD_KITCHEN_COOKING, // 동남아·남미음식 조리사
        "532100" to JobRole.FOOD_HALL_STAFF, // 패스트푸드 준비원
        "532200" to JobRole.FOOD_HALL_STAFF, // 식음료 서비스 종사원
        "532201" to JobRole.FOOD_HALL_STAFF, // 호텔·레스토랑 웨이터·웨이트리스
        "532202" to JobRole.FOOD_HALL_STAFF, // 일반 음식점 접객서빙원
        "532203" to JobRole.FOOD_HALL_STAFF, // 주점·커피숍 접객서빙원
        "532300" to JobRole.FOOD_KITCHEN_COOKING, // 주방 보조원
        "532301" to JobRole.FOOD_KITCHEN_COOKING, // 주방 보조원(일반 음식점)
        "532302" to JobRole.FOOD_KITCHEN_COOKING, // 단체 급식 보조원
        "532900" to JobRole.FOOD_HALL_STAFF, // 기타 식음료 서비스 종사원(병원 배식원)
        "541100" to JobRole.SERVICE_SECURITY_GUARD, // 경호원
        "541200" to JobRole.SERVICE_SECURITY_GUARD, // 청원경찰
        "541300" to JobRole.SERVICE_SECURITY_GUARD, // 시설 및 특수 경비원
        "541301" to JobRole.SERVICE_SECURITY_GUARD, // 기계·무인 경비원(해당 경비지도사 포함)
        "541302" to JobRole.SERVICE_SECURITY_GUARD, // 시설·호송·특수 경비원(해당 경비지도사 포함)
        "541400" to JobRole.SERVICE_SECURITY_GUARD, // 보안 관제원
        "541900" to JobRole.SERVICE_SECURITY_GUARD, // 기타 경호 및 보안 관련 종사원
        "541901" to JobRole.SERVICE_SECURITY_GUARD, // 유통·매장·창고 감시원
        "541902" to JobRole.SERVICE_SECURITY_GUARD, // 주차 단속원 및 안전 순찰원
        "541903" to JobRole.SERVICE_SECURITY_GUARD, // 레저·테마파크 안전요원
        "541904" to JobRole.SERVICE_TOURISM, // 수학여행 안전요원
        "542000" to JobRole.PRODUCTION_FACILITY_MANAGEMENT, // 건물 관리원
        "542001" to JobRole.SERVICE_SECURITY_GUARD, // 아파트·빌라 경비원
        "542002" to JobRole.SERVICE_SECURITY_GUARD, // 건물 경비원(청사,학교,병원,상가,빌딩,공장 등)
        "542003" to JobRole.SERVICE_SECURITY_GUARD, // 공사현장 경비원
        "542004" to JobRole.PUBLIC_RELIGION, // 기타 건물 관리원(공원, 종교시설 등)
        "550100" to JobRole.MEDICAL_CARE_WORKER, // 요양보호사
        "550101" to JobRole.MEDICAL_CARE_WORKER, // 요양보호사(노인요양사)
        "550102" to JobRole.MEDICAL_CARE_WORKER, // 재가 요양보호사
        "550201" to JobRole.MEDICAL_ETC, // 간병인
        "550202" to JobRole.MEDICAL_ETC, // 재가 간병인
        "550300" to JobRole.PUBLIC_ELDERLY_WOMEN_WELFARE, // 노인 돌봄 종사원
        "550400" to JobRole.PUBLIC_SOCIAL_SERVICE, // 장애인 돌봄 종사원
        "550500" to JobRole.SERVICE_HOUSEKEEPER, // 육아 도우미(베이비시터)
        "561100" to JobRole.SERVICE_SANITATION, // 건물 청소원
        "561101" to JobRole.SERVICE_SANITATION, // 건물 청소원(공공건물,아파트,사무실,병원,상가,공장 등)
        "561102" to JobRole.SERVICE_HOTEL, // 호텔·콘도·숙박시설 청소원(룸메이드,하우스키퍼)
        "561103" to JobRole.SERVICE_SANITATION, // 외벽 및 창문 청소원
        "561200" to JobRole.SERVICE_SANITATION, // 운송 및 시설장비 청소원
        "561201" to JobRole.SERVICE_SANITATION, // 특수 크리닝장비 청소원
        "561202" to JobRole.SERVICE_ETC, // 세차원 및 운송장비 청소원
        "561300" to JobRole.SERVICE_SANITATION, // 거리 및 공공장소 청소원(환경 미화원)
        "561400" to JobRole.SERVICE_SANITATION, // 재활용품 및 쓰레기 수거원
        "561900" to JobRole.SERVICE_SANITATION, // 기타 청소 관련 종사원(놀이공원 등)
        "562100" to JobRole.SERVICE_ETC, // 세정원(물탱크 청소 포함)
        "562200" to JobRole.SERVICE_ETC, // 방역원(해충퇴치원 포함)
        "563100" to JobRole.SERVICE_ETC, // 구두 미화원
        "563200" to JobRole.SERVICE_ETC, // 세탁원 및 다림질원
        "563301" to JobRole.SERVICE_HOUSEKEEPER, // 가사 도우미
        "563302" to JobRole.SERVICE_HOUSEKEEPER, // 입주 가사도우미
        "611000" to JobRole.SALES_GENERAL, // 부동산 컨설턴트 및 중개사
        "612100" to JobRole.SALES_TECH_IT, // 기술 영업원
        "612101" to JobRole.SALES_TECH_IT, // 자동차부품 및 운송장비 기술영업원
        "612102" to JobRole.SALES_TECH_IT, // 기계장비 기술영업원
        "612103" to JobRole.SALES_TECH_IT, // 화학제품 기술영업원
        "612104" to JobRole.SALES_TECH_IT, // 전기·전자장비 기술영업원
        "612105" to JobRole.SALES_PHARMA_MEDICAL, // 의료장비 기술영업원
        "612106" to JobRole.SALES_TECH_IT, // IT 기술영업원(전산장비,소프트웨어)
        "612107" to JobRole.SALES_PHARMA_MEDICAL, // 의약품 영업원(제약 영업원)
        "612108" to JobRole.SALES_TECH_IT, // 기타 기술영업원
        "612200" to JobRole.SALES_OVERSEAS, // 해외 영업원
        "612201" to JobRole.SALES_OVERSEAS, // 해외 영업원(영어)
        "612202" to JobRole.SALES_OVERSEAS, // 해외 영업원(중국어)
        "612203" to JobRole.SALES_OVERSEAS, // 해외 영업원(일본어)
        "612204" to JobRole.SALES_OVERSEAS, // 해외 영업원(기타 언어권)
        "612300" to JobRole.SALES_B2B, // 상품중개인 및 경매사
        "612900" to JobRole.SALES_TECH_IT, // 기타 기술 영업 및 중개 관련 종사원
        "613100" to JobRole.SALES_B2C, // 자동차 영업원
        "613101" to JobRole.SALES_B2C, // 자동차 영업원(자동차 딜러)
        "613102" to JobRole.SALES_B2C, // 중고차 영업원
        "613200" to JobRole.SALES_GENERAL, // 제품 영업원
        "613201" to JobRole.FOOD_FOOD_PROCESSING_DEVELOPMENT, // 식품·주류 영업원
        "613202" to JobRole.SALES_GENERAL, // 건설자재 영업원
        "613203" to JobRole.SALES_B2B, // 건설수주 영업원
        "613204" to JobRole.SALES_GENERAL, // 일반 제품 영업원
        "613900" to JobRole.SALES_GENERAL, // 기타 영업원
        "613901" to JobRole.SALES_B2B, // 광고·출판 영업원
        "613902" to JobRole.SALES_GENERAL, // 체인점 영업원
        "613903" to JobRole.SALES_B2C, // 상조·렌탈·회원모집 영업원
        "613904" to JobRole.SALES_GENERAL, // 기타 영업원
        "614000" to JobRole.CS_OUTBOUND, // 텔레마케터
        "614001" to JobRole.CS_OUTBOUND, // 텔레마케터(콜센터)
        "614002" to JobRole.CS_OUTBOUND, // 텔레마케터(학원, 학습지, 유학)
        "614003" to JobRole.CS_OUTBOUND, // 텔레마케터(금융·보험·대출·부동산)
        "614004" to JobRole.CS_OUTBOUND, // 텔레마케터(쇼핑몰, 백화점, 마트)
        "614005" to JobRole.CS_OUTBOUND, // 텔레마케터(인터넷, 통신)
        "614006" to JobRole.CS_OUTBOUND, // 기타 텔레마케터
        "615100" to JobRole.SERVICE_STORE_MANAGEMENT, // 소규모 상점 경영 및 일선 관리 종사원(매장매니저·매장슈퍼바이저 등)
        "615200" to JobRole.SERVICE_STORE_MANAGEMENT, // 상점 판매원
        "615201" to JobRole.SERVICE_STORE_MANAGEMENT, // 백화점 판매원
        "615202" to JobRole.SERVICE_STORE_MANAGEMENT, // 대형마트 판매원
        "615203" to JobRole.SERVICE_STORE_MANAGEMENT, // 편의점 판매원
        "615204" to JobRole.SERVICE_STORE_MANAGEMENT, // 면세점 판매원
        "615205" to JobRole.SERVICE_STORE_MANAGEMENT, // 일반상점(슈퍼) 판매원
        "615206" to JobRole.SERVICE_STORE_MANAGEMENT, // 의류 판매원
        "615207" to JobRole.SERVICE_STORE_MANAGEMENT, // 화장품 판매원
        "615208" to JobRole.SERVICE_STORE_MANAGEMENT, // 가구 판매원
        "615209" to JobRole.SERVICE_STORE_MANAGEMENT, // 가전 판매원(컴퓨터 포함)
        "615210" to JobRole.SERVICE_STORE_MANAGEMENT, // 음식료품 판매원
        "615211" to JobRole.SERVICE_STORE_MANAGEMENT, // 꽃 판매원(플로리스트, 토피어리어 포함)
        "615212" to JobRole.SERVICE_STORE_MANAGEMENT, // 기타 판매원(사무용품,도서,음반 포함 잡화 등)
        "615300" to JobRole.SERVICE_STORE_MANAGEMENT, // 단말기 및 통신 서비스 판매원
        "615301" to JobRole.SALES_B2C, // 통신기기 판매원(핸드폰 판매 및 가입)
        "615302" to JobRole.SALES_B2C, // 통신서비스 판매원(인터넷,IPTV 가입)
        "615400" to JobRole.MD_ONLINE, // 온라인 쇼핑 판매원
        "615500" to JobRole.SALES_B2C, // 상품 대여원
        "615501" to JobRole.SALES_B2C, // 자동차 대여원(렌터카)
        "615502" to JobRole.SALES_B2C, // 생활용품 대여원(정수기,가전제품,의복,어린이 용품)
        "615503" to JobRole.SALES_B2C, // 기타 대여원(기계장비,오락·스포츠 용품,도서,영상)
        "615600" to JobRole.SERVICE_STORE_MANAGEMENT, // 노점 및 이동 판매원
        "615800" to JobRole.SERVICE_PARKING_FUEL, // 주유원
        "615801" to JobRole.SERVICE_PARKING_FUEL, // 주유원(주유판매원)
        "615802" to JobRole.SERVICE_PARKING_FUEL, // 가스 충전원
        "616100" to JobRole.SERVICE_STORE_MANAGEMENT, // 매장 계산원 및 요금 정산원
        "616101" to JobRole.SERVICE_STORE_MANAGEMENT, // 매장 계산원
        "616102" to JobRole.SERVICE_PARKING_FUEL, // 요금 정산원(주차요금,통행료)
        "616200" to JobRole.SERVICE_RECEPTION, // 매표원 및 복권 판매원
        "617100" to JobRole.MARKETING_EXHIBITION_EVENT, // 홍보 도우미 및 판촉원
        "617101" to JobRole.MARKETING_EXHIBITION_EVENT, // 홍보 도우미 및 판촉원(나레이터 모델 포함)
        "617102" to JobRole.MARKETING_EXHIBITION_EVENT, // 이벤트·행사 진행원
        "617900" to JobRole.SERVICE_ETC, // 기타 판매 관련 단순 종사원
        "617901" to JobRole.SERVICE_STORE_MANAGEMENT, // 매장 정리원(매장 보조원)
        "617902" to JobRole.SERVICE_STORE_MANAGEMENT, // 상품 진열원
        "617903" to JobRole.TRADE_WAREHOUSE_PACKING, // 쇼핑몰택배 준비원
        "617904" to JobRole.SERVICE_ETC, // 기타 판매 단순 종사원(전단지배포, 벽보원, 물품 보관원)
        "621100" to JobRole.TRANSPORT_PASSENGER, // 항공기 조종사
        "621200" to JobRole.TRANSPORT_FREIGHT, // 선장 및 항해사·도선사
        "621300" to JobRole.TRANSPORT_PASSENGER, // 철도 및 전동차 기관사
        "621400" to JobRole.TRANSPORT_ETC, // 관제사(항공,선박,철도 등)
        "621900" to JobRole.TRANSPORT_ETC, // 철도운송 관련 종사원
        "623001" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 크레인 운전원(천정,타워 제외)
        "623002" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 천장·타워 크레인 운전원
        "624901" to JobRole.TRANSPORT_DELIVERY, // 음식점 배달원
        "624902" to JobRole.TRANSPORT_DELIVERY, // 패스트푸드 배달원
        "701100" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 강구조물 가공원 및 건립원(철골공)
        "701200" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 경량 철골공(석고보드공,텍스공,조립주택 건립원)
        "701300" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 철근공
        "701401" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 콘크리트공
        "701402" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 콘크리트건물·피시 조립원
        "701500" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 건축 석공
        "701600" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 건축 목공
        "701601" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 형틀 목공(거푸집 설치공)
        "701602" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 외장 목공(목조주택 건립)
        "701603" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 내장 목공(인테리어 목공 - 나무창호, 주방가구, 싱크대, 인테리어 치장)
        "701604" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 전통 건축원
        "701700" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 조적공 및 석재 부설원
        "701900" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 기타 건설 관련 기능 종사원
        "701901" to JobRole.CONSTRUCTION_DESIGN_SUPERVISION, // 건설현장반장
        "701902" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 비계공
        "701903" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 건물칸막이 설치원
        "701904" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 기타 건설 기능원(건물해체,정화조설치,방음벽설치 등)
        "702100" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 미장공
        "702200" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 방수공
        "702300" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 단열공
        "702400" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 바닥재 시공원
        "702401" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 타일·대리석 시공원
        "702402" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 장판·카펫 시공원
        "702500" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 도배공 및 유리 부착원
        "702501" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 도배공
        "702502" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 유리 부착원(유리공)
        "702600" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 건축 도장공
        "702700" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 새시 조립 및 설치원
        "702701" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 새시 조립·설치원
        "702702" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 새시 보조원(견습공)
        "702900" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 기타 건축 마감 관련 기능 종사원
        "702901" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 건물 보수원 및 영선원(아파트 기계·전기 시설관리 제외)
        "702902" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 지붕잇기원 등 기타 건축 마감 기능원
        "703100" to JobRole.CONSTRUCTION_MEP_FIRE, // 건설 배관공
        "703101" to JobRole.CONSTRUCTION_MEP_FIRE, // 건축 배관공(옥내급수관,상하수배관,위생 배관)
        "703102" to JobRole.CONSTRUCTION_MEP_FIRE, // 가스 배관공(가스관 설치원)
        "703200" to JobRole.PRODUCTION_EQUIPMENT, // 공업 배관공(플랜트,항공,선박,철도차량)
        "703900" to JobRole.CONSTRUCTION_MEP_FIRE, // 기타 배관공
        "703901" to JobRole.CONSTRUCTION_MEP_FIRE, // 기타 배관공(배관누수 탐지원)
        "703902" to JobRole.CONSTRUCTION_MEP_FIRE, // 배관 보조원
        "704000" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 건설 및 채굴기계 운전원
        "704001" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 굴착기(굴삭기,포클레인) 운전원
        "704002" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 로더 운전원(페이로더 운전원)
        "704003" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 레미콘 차량 운전원(콘크리트 믹서 트럭)
        "704004" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 도로정지기, 덤프트럭 등 기타 건설·채굴 기계 운전원
        "704005" to JobRole.PRODUCTION_ETC, // 농림어업 기계 운전원
        "705100" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 광원·채석원 및 석재 절단원
        "705101" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 광원 및 채석원
        "705102" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 석재 가공원(석재 절단, 연마, 조각원 등)
        "705200" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 철로 설치 및 보수원
        "705900" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 기타 채굴 및 토목 관련 종사원
        "705901" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 점화·발파·화약관리원 및 삭구원
        "705902" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 잠수 기능원(잠수부, 구난 잠수요원)
        "706000" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 건설 및 광업 단순 종사원
        "706001" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 조경시설물 설치원
        "706002" to JobRole.CONSTRUCTION_SPECIAL_DAILY_LABOR, // 건설·채굴 단순 종사원
        "811100" to JobRole.PRODUCTION_EQUIPMENT, // 로봇 설치 및 정비원
        "811200" to JobRole.PRODUCTION_EQUIPMENT, // 공업기계 설치 및 정비원
        "811201" to JobRole.PRODUCTION_EQUIPMENT, // 공작기계 설치·정비원
        "811202" to JobRole.PRODUCTION_EQUIPMENT, // 화학기계 설치·정비원
        "811203" to JobRole.PRODUCTION_EQUIPMENT, // 섬유기계 설치·정비원
        "811204" to JobRole.PRODUCTION_EQUIPMENT, // 전자제품 제조기계 설치·정비원
        "811205" to JobRole.FOOD_FOOD_PROCESSING_DEVELOPMENT, // 식품기계 설치·정비원
        "811206" to JobRole.PRODUCTION_EQUIPMENT, // 기타 공업기계 설치·정비원
        "811300" to JobRole.SERVICE_INSTALLATION_REPAIR, // 승강기 설치 및 정비원
        "811301" to JobRole.SERVICE_INSTALLATION_REPAIR, // 엘리베이터 설치·정비원
        "811302" to JobRole.SERVICE_INSTALLATION_REPAIR, // 에스컬레이터 설치·정비원(무빙워크 포함)
        "811303" to JobRole.SERVICE_INSTALLATION_REPAIR, // 기타 승강기 설치·정비원(휠체어리프트, 자동문 포함)
        "811400" to JobRole.PRODUCTION_EQUIPMENT, // 물품 이동 장비 설치 및 정비원
        "811401" to JobRole.PRODUCTION_EQUIPMENT, // 크레인·호이스트 설치·정비원
        "811402" to JobRole.PRODUCTION_EQUIPMENT, // 지게차 정비원
        "811500" to JobRole.PRODUCTION_EQUIPMENT, // 냉동‧냉장‧공조기 설치 및 정비원
        "811501" to JobRole.PRODUCTION_EQUIPMENT, // 산업용 냉동·냉장·공조기 설치·정비원
        "811502" to JobRole.SERVICE_INSTALLATION_REPAIR, // 건물·가정용 냉동·냉장·공조기 설치·정비원
        "811600" to JobRole.PRODUCTION_EQUIPMENT, // 보일러 설치 및 정비원
        "811601" to JobRole.PRODUCTION_EQUIPMENT, // 산업용 보일러 설치·수리원
        "811602" to JobRole.SERVICE_INSTALLATION_REPAIR, // 건물·가정용 보일러 설치·수리원
        "811700" to JobRole.PRODUCTION_EQUIPMENT, // 건설·광업기계 설치 및 정비원
        "811900" to JobRole.PRODUCTION_EQUIPMENT, // 농업용·기타 기계장비 설치 및 정비원
        "812100" to JobRole.PRODUCTION_EQUIPMENT, // 항공기 정비원
        "812200" to JobRole.PRODUCTION_EQUIPMENT, // 선박 정비원
        "812300" to JobRole.PRODUCTION_EQUIPMENT, // 철도 기관차 및 전동차 정비원
        "812400" to JobRole.SERVICE_INSTALLATION_REPAIR, // 자동차 정비원
        "812401" to JobRole.SERVICE_INSTALLATION_REPAIR, // 자동차 엔진·섀시·전기·전자 정비원
        "812402" to JobRole.SERVICE_INSTALLATION_REPAIR, // 자동차 차체·판금·도장 정비원
        "812403" to JobRole.SERVICE_INSTALLATION_REPAIR, // 자동차 경정비원
        "812404" to JobRole.SERVICE_INSTALLATION_REPAIR, // 자동차 튜닝원
        "812900" to JobRole.SERVICE_INSTALLATION_REPAIR, // 기타 운송장비 정비원
        "813100" to JobRole.PRODUCTION_PRODUCTION, // 금형원
        "813101" to JobRole.PRODUCTION_PRODUCTION, // 프레스 금형 제조원
        "813102" to JobRole.PRODUCTION_PRODUCTION, // 플라스틱 금형 제조원
        "813103" to JobRole.PRODUCTION_PRODUCTION, // 다이캐스팅 금형 제조원
        "813104" to JobRole.ENGINEERING_MECHANICAL_DESIGN_CAD, // 캐드캠 기능원
        "813105" to JobRole.PRODUCTION_PRODUCTION, // 금형제조 보조원
        "813200" to JobRole.PRODUCTION_PRODUCTION, // 금속 공작기계 조작원
        "813201" to JobRole.PRODUCTION_PRODUCTION, // CNC 선반 조작원(NC 선반 조작원)
        "813202" to JobRole.PRODUCTION_PRODUCTION, // 범용 선반 조작원
        "813203" to JobRole.PRODUCTION_PRODUCTION, // CNC 밀링기 조작원(NC 밀링기 조작원)
        "813204" to JobRole.PRODUCTION_PRODUCTION, // 범용 밀링기조작원
        "813205" to JobRole.PRODUCTION_PRODUCTION, // 드릴링기 및 보링기 조작원
        "813206" to JobRole.PRODUCTION_PRODUCTION, // 연삭기 및 연마(광택)기 조작원
        "813207" to JobRole.PRODUCTION_PRODUCTION, // 프레스기 및 절단기 조작원
        "813208" to JobRole.PRODUCTION_PRODUCTION, // 톱기계 조작원
        "813209" to JobRole.PRODUCTION_PRODUCTION, // 금속절곡기 조작원(밴딩기 조작원)
        "813210" to JobRole.PRODUCTION_PRODUCTION, // 머시닝센터(MCT) 조작원
        "813211" to JobRole.PRODUCTION_PRODUCTION, // 방전기 및 와이어컷 방전기 조작원
        "813212" to JobRole.PRODUCTION_PRODUCTION, // 레이저 절단원
        "813213" to JobRole.PRODUCTION_PRODUCTION, // 기타 금속공작기계 조작원 및 보조원
        "814000" to JobRole.PRODUCTION_EQUIPMENT, // 냉난방 관련 설비 조작원
        "814001" to JobRole.PRODUCTION_EQUIPMENT, // 산업용 냉난방 설비 조작원
        "814002" to JobRole.PRODUCTION_EQUIPMENT, // 건물용 냉난방 설비 조작원
        "815000" to JobRole.PRODUCTION_EQUIPMENT, // 자동 조립라인 및 산업용로봇 조작원
        "815001" to JobRole.PRODUCTION_EQUIPMENT, // 자동조립라인 조작원
        "815002" to JobRole.PRODUCTION_EQUIPMENT, // 산업용 로봇 조작원
        "816100" to JobRole.PRODUCTION_PRODUCTION, // 일반기계 조립원
        "816101" to JobRole.PRODUCTION_PRODUCTION, // 공작기계 조립·검사원
        "816102" to JobRole.PRODUCTION_PRODUCTION, // 공업기계 조립·검사원(공작기계 제외)
        "816103" to JobRole.PRODUCTION_PRODUCTION, // 건설·광업·농업 기계 조립·검사원
        "816104" to JobRole.PRODUCTION_PRODUCTION, // 공구 조립·검사원
        "816105" to JobRole.PRODUCTION_PRODUCTION, // 기타 기계 조립·검사원
        "816200" to JobRole.PRODUCTION_PRODUCTION, // 기계 부품 조립원
        "817100" to JobRole.PRODUCTION_PRODUCTION, // 자동차 조립원(검사원 포함)
        "817200" to JobRole.PRODUCTION_PRODUCTION, // 자동차 부품 조립원(검사원 포함)
        "817300" to JobRole.PRODUCTION_PRODUCTION, // 운송장비 조립원(항공기·선박·철도기관차·전동차 등 검사원 포함)
        "821100" to JobRole.PRODUCTION_PRODUCTION, // 금속가공 관련 제어 장치 조작원(용광로·용해로·금속가열로)
        "821200" to JobRole.PRODUCTION_PRODUCTION, // 금속가공 기계 조작원
        "821201" to JobRole.PRODUCTION_PRODUCTION, // 압연기 조작원
        "821202" to JobRole.PRODUCTION_PRODUCTION, // 금속 열처리로 조작원
        "821203" to JobRole.PRODUCTION_PRODUCTION, // 인발·신선·연선·압출기 조작원
        "821204" to JobRole.PRODUCTION_QUALITY, // 금속가공 검사원
        "822100" to JobRole.PRODUCTION_PRODUCTION, // 판금원(덕트원 포함)
        "822201" to JobRole.PRODUCTION_PRODUCTION, // 판금기 조작원
        "822202" to JobRole.PRODUCTION_PRODUCTION, // 판금기 보조원(견습공)
        "822301" to JobRole.PRODUCTION_PRODUCTION, // 제관원
        "822302" to JobRole.PRODUCTION_PRODUCTION, // 제관 보조원(견습공)
        "822400" to JobRole.PRODUCTION_PRODUCTION, // 제관기 조작원
        "823101" to JobRole.PRODUCTION_PRODUCTION, // 단조원
        "823102" to JobRole.PRODUCTION_PRODUCTION, // 단조 보조원(견습공)
        "823200" to JobRole.PRODUCTION_PRODUCTION, // 단조기 조작원
        "823300" to JobRole.PRODUCTION_PRODUCTION, // 주조원
        "823301" to JobRole.PRODUCTION_PRODUCTION, // 주조원(목형원, 원형원, 주형원)
        "823302" to JobRole.PRODUCTION_PRODUCTION, // 주조 보조원(견습공)
        "823400" to JobRole.PRODUCTION_PRODUCTION, // 주조기 조작원(다이캐스팅기 조작원 포함)
        "824100" to JobRole.PRODUCTION_PRODUCTION, // 용접원
        "824101" to JobRole.PRODUCTION_PRODUCTION, // 가스용접원(산소용접원)
        "824102" to JobRole.PRODUCTION_PRODUCTION, // 전기용접원(아크,알곤,티그용접원)
        "824103" to JobRole.PRODUCTION_PRODUCTION, // 화염절단원
        "824104" to JobRole.PRODUCTION_PRODUCTION, // 조선용접원(조선취부사 포함)
        "824105" to JobRole.PRODUCTION_PRODUCTION, // 납땜원
        "824106" to JobRole.PRODUCTION_PRODUCTION, // 기타 용접원
        "824107" to JobRole.PRODUCTION_PRODUCTION, // 용접 사상원 및 보조원
        "824200" to JobRole.PRODUCTION_PRODUCTION, // 용접기 조작원
        "825100" to JobRole.PRODUCTION_PRODUCTION, // 도장기 조작원
        "825101" to JobRole.PRODUCTION_PRODUCTION, // 자동차 도장기 조작원
        "825102" to JobRole.PRODUCTION_PRODUCTION, // 금속제품 도장기 조작원
        "825103" to JobRole.PRODUCTION_PRODUCTION, // 가구 및 기타 도장기 조작원
        "825200" to JobRole.PRODUCTION_PRODUCTION, // 도금 및 금속 분무기 조작원
        "831100" to JobRole.PRODUCTION_EQUIPMENT, // 산업 전기공(항공기·선박·철도기관차·전동차 전기공)
        "831200" to JobRole.CONSTRUCTION_MEP_FIRE, // 내선 전기공
        "831201" to JobRole.CONSTRUCTION_MEP_FIRE, // 내선 전기공(건물 내 전기공사원)
        "831202" to JobRole.CONSTRUCTION_MEP_FIRE, // 조명기구 등 전기기기 설치·정비원
        "831203" to JobRole.CONSTRUCTION_MEP_FIRE, // 발전기 설치·정비원
        "831204" to JobRole.CONSTRUCTION_MEP_FIRE, // 전기·전자 제어장치 설치·정비원
        "831300" to JobRole.CONSTRUCTION_MEP_FIRE, // 외선 전기공
        "832100" to JobRole.SERVICE_INSTALLATION_REPAIR, // 사무용 전자기기 설치 및 수리원(컴퓨터 제외)
        "832200" to JobRole.SERVICE_INSTALLATION_REPAIR, // 가전제품 설치 및 수리원
        "832201" to JobRole.SERVICE_INSTALLATION_REPAIR, // TV·비디오·오디오 설치 및 수리원
        "832202" to JobRole.SERVICE_INSTALLATION_REPAIR, // 냉장고·세탁기 설치 및 수리원
        "832203" to JobRole.SERVICE_INSTALLATION_REPAIR, // 에어컨·공기정화기 설치 및 수리원
        "832204" to JobRole.SERVICE_INSTALLATION_REPAIR, // 정수기·냉온수기·비데 설치 및 수리원
        "832205" to JobRole.SERVICE_INSTALLATION_REPAIR, // 그 외 가전제품 설치 및 수리원
        "832900" to JobRole.SERVICE_INSTALLATION_REPAIR, // 기타 전기‧전자기기 설치 및 수리원
        "832901" to JobRole.SERVICE_INSTALLATION_REPAIR, // 감시카메라 및 보안장치 설치·수리원
        "832902" to JobRole.SERVICE_INSTALLATION_REPAIR, // 현금인출기 설치·수리원
        "832903" to JobRole.SERVICE_INSTALLATION_REPAIR, // 포스시스템 설치·수리원(POS 설치 및 수리원)
        "832904" to JobRole.SERVICE_INSTALLATION_REPAIR, // 의료기기·장비 설치·수리원
        "832905" to JobRole.SERVICE_INSTALLATION_REPAIR, // 시계·카메라·광학기구 수리원
        "832906" to JobRole.SERVICE_INSTALLATION_REPAIR, // 영상·음향기기 등 기타 전기·전자 기기 설치·수리원
        "833000" to JobRole.ENGINEERING_ENERGY, // 발전 및 배전 장치 조작원
        "834000" to JobRole.PRODUCTION_EQUIPMENT, // 전기 및 전자설비 조작원
        "834001" to JobRole.PRODUCTION_FACILITY_MANAGEMENT, // 아파트 전기관리원
        "834002" to JobRole.PRODUCTION_FACILITY_MANAGEMENT, // 빌딩 전기관리원
        "834003" to JobRole.PRODUCTION_EQUIPMENT, // 공장 전기관리원
        "834004" to JobRole.PRODUCTION_FACILITY_MANAGEMENT, // 기타 전기·전자 설비 조작원
        "835100" to JobRole.PRODUCTION_PRODUCTION, // 전기 부품 및 제품 제조 기계 조작원
        "835200" to JobRole.PRODUCTION_PRODUCTION, // 일차전지 및 이차전지 제조 기계 조작원
        "835201" to JobRole.PRODUCTION_PRODUCTION, // 일차전지 제조 기계 조작원
        "835202" to JobRole.PRODUCTION_PRODUCTION, // 이차전지 제조 기계 조작원
        "835300" to JobRole.PRODUCTION_PRODUCTION, // 전자 부품 및 제품 제조 기계 조작원
        "836000" to JobRole.PRODUCTION_PRODUCTION, // 전기‧전자 부품 및 제품 조립원
        "836001" to JobRole.PRODUCTION_PRODUCTION, // 전기 부품·제품 조립·검사원
        "836002" to JobRole.PRODUCTION_PRODUCTION, // PCB 부품·제품 조립·검사원
        "836003" to JobRole.PRODUCTION_PRODUCTION, // LCD 부품·제품 조립·검사원
        "836004" to JobRole.PRODUCTION_PRODUCTION, // LED 부품·제품 조립·검사원
        "836005" to JobRole.PRODUCTION_PRODUCTION, // 통신기기 부품·제품 조립원(핸드폰)
        "836006" to JobRole.PRODUCTION_PRODUCTION, // 기타 전자 부품·제품 조립·검사원
        "841100" to JobRole.SERVICE_INSTALLATION_REPAIR, // 컴퓨터 설치 및 수리원(컴퓨터A/S원)
        "841200" to JobRole.SERVICE_INSTALLATION_REPAIR, // 이동전화기 수리원(핸드폰, 휴대폰)
        "841900" to JobRole.SERVICE_INSTALLATION_REPAIR, // 기타 정보 통신기기 설치 및 수리원
        "842100" to JobRole.SERVICE_INSTALLATION_REPAIR, // 방송 관련 장비 설치 및 수리원
        "842200" to JobRole.SERVICE_INSTALLATION_REPAIR, // 통신 관련 장비 설치 및 수리원
        "842300" to JobRole.SERVICE_INSTALLATION_REPAIR, // 방송‧통신‧인터넷 케이블 설치 및 수리원
        "842301" to JobRole.SERVICE_INSTALLATION_REPAIR, // 통신·인터넷 케이블 설치·수리원
        "842302" to JobRole.SERVICE_INSTALLATION_REPAIR, // 방송 케이블 설치·수리원
        "851100" to JobRole.PRODUCTION_PRODUCTION, // 석유 및 천연가스 제조 관련 제어 장치 조작원
        "851101" to JobRole.PRODUCTION_PRODUCTION, // 석유·천연가스 제조 제어장치 조작원
        "851102" to JobRole.PRODUCTION_PRODUCTION, // 폐유·재생유 처리 제어장치 조작원
        "851200" to JobRole.PRODUCTION_PRODUCTION, // 화학물 가공 장치 조작원
        "851900" to JobRole.PRODUCTION_PRODUCTION, // 기타 석유 및 화학물 가공 장치 조작원
        "852100" to JobRole.PRODUCTION_PRODUCTION, // 타이어 및 고무제품 생산기 조작원
        "852101" to JobRole.PRODUCTION_PRODUCTION, // 고무 사출성형기 조작원
        "852102" to JobRole.PRODUCTION_PRODUCTION, // 고무 압출성형기 조작원
        "852103" to JobRole.PRODUCTION_PRODUCTION, // 고무 프레스기 조작원
        "852104" to JobRole.PRODUCTION_PRODUCTION, // 타이어 생산기계 조작원
        "852200" to JobRole.PRODUCTION_PRODUCTION, // 플라스틱제품 생산기 조작원
        "852201" to JobRole.PRODUCTION_PRODUCTION, // 플라스틱 사출성형기 조작원
        "852202" to JobRole.PRODUCTION_PRODUCTION, // 플라스틱 압출성형기 조작원
        "852300" to JobRole.PRODUCTION_PRODUCTION, // 화학제품 생산기 조작원
        "852301" to JobRole.PRODUCTION_PRODUCTION, // 도료·잉크제품 생산기계 조작원
        "852302" to JobRole.PRODUCTION_PRODUCTION, // 화장품·비누제품 생산기계 조작원
        "852303" to JobRole.PRODUCTION_PRODUCTION, // 의약품 생산기계 조작원
        "852304" to JobRole.PRODUCTION_PRODUCTION, // 농약·비료 생산기계 조작원
        "852305" to JobRole.PRODUCTION_PRODUCTION, // 기타 화학제품 생산기계 조작원
        "852400" to JobRole.PRODUCTION_PRODUCTION, // 고무 및 플라스틱제품 조립원
        "852401" to JobRole.PRODUCTION_PRODUCTION, // 고무제품 조립원 및 검사원
        "852402" to JobRole.PRODUCTION_PRODUCTION, // 플라스틱제품 조립원 및 검사원
        "853100" to JobRole.PRODUCTION_ENVIRONMENT_SAFETY, // 상하수도 처리 장치 조작원
        "853101" to JobRole.PRODUCTION_ENVIRONMENT_SAFETY, // 물펌프·정수 처리장치 조작원
        "853102" to JobRole.PRODUCTION_ENVIRONMENT_SAFETY, // 하수·폐수 처리장치 조작원
        "853103" to JobRole.PRODUCTION_ENVIRONMENT_SAFETY, // 기타 상·하수도 처리장치 조작원(댐수문, 저수지)
        "853200" to JobRole.PRODUCTION_ENVIRONMENT_SAFETY, // 재활용 처리 및 소각로 조작원
        "853201" to JobRole.PRODUCTION_ENVIRONMENT_SAFETY, // 소각로 조작원
        "853202" to JobRole.PRODUCTION_ENVIRONMENT_SAFETY, // 재활용 처리장치 조작원
        "861100" to JobRole.PRODUCTION_PRODUCTION, // 섬유 제조 기계 조작원(방적,방사,연사,합사,권사기 등)
        "861200" to JobRole.PRODUCTION_PRODUCTION, // 직조기 및 편직기 조작원
        "861201" to JobRole.PRODUCTION_PRODUCTION, // 제직기(직조기) 조작원
        "861202" to JobRole.PRODUCTION_PRODUCTION, // 편직기 조작원
        "861203" to JobRole.PRODUCTION_PRODUCTION, // 기타 직조기·편직기 조작원(정단,정경,연경,통경기 등)
        "861204" to JobRole.PRODUCTION_PRODUCTION, // 섬유가공 준비 및 후가공 처리원
        "861300" to JobRole.PRODUCTION_PRODUCTION, // 표백 및 염색 관련 기계 조작원
        "861301" to JobRole.PRODUCTION_PRODUCTION, // 염색 준비 및 조색기 조작원
        "861302" to JobRole.PRODUCTION_PRODUCTION, // 정련·표백기 조작원
        "861303" to JobRole.PRODUCTION_PRODUCTION, // 염색기 조작원
        "861304" to JobRole.PRODUCTION_PRODUCTION, // 날염기 조작원(나염기)
        "862100" to JobRole.PRODUCTION_PRODUCTION, // 패턴사
        "862200" to JobRole.PRODUCTION_PRODUCTION, // 재단사
        "862300" to JobRole.PRODUCTION_PRODUCTION, // 재봉사
        "862301" to JobRole.PRODUCTION_PRODUCTION, // 재봉사(의류·직물)
        "862302" to JobRole.PRODUCTION_PRODUCTION, // 가죽·모피·신발 재봉사
        "862303" to JobRole.PRODUCTION_PRODUCTION, // 기계 자수원
        "862900" to JobRole.PRODUCTION_PRODUCTION, // 기타 섬유 및 가죽 관련 기능 종사원(의복·직물 검사 등)
        "863100" to JobRole.PRODUCTION_PRODUCTION, // 한복 제조원
        "863200" to JobRole.PRODUCTION_PRODUCTION, // 양장 및 양복 제조원
        "863300" to JobRole.PRODUCTION_PRODUCTION, // 모피 및 가죽 의복 제조원
        "863400" to JobRole.PRODUCTION_PRODUCTION, // 의복·가죽 및 모피 수선원
        "863900" to JobRole.PRODUCTION_PRODUCTION, // 기타 의복 제조 관련 기능 종사원
        "864100" to JobRole.PRODUCTION_PRODUCTION, // 제화원
        "864200" to JobRole.PRODUCTION_PRODUCTION, // 신발 제조기 조작원
        "864300" to JobRole.PRODUCTION_PRODUCTION, // 세탁 기계 조작원
        "864900" to JobRole.PRODUCTION_PRODUCTION, // 기타 직물·신발 관련 기계 조작원
        "871100" to JobRole.FOOD_BAKERY, // 제과원 및 제빵사
        "871200" to JobRole.FOOD_BAKERY, // 떡 제조원
        "872100" to JobRole.FOOD_FOOD_PROCESSING_DEVELOPMENT, // 정육 가공원 및 도축원
        "872101" to JobRole.FOOD_FOOD_PROCESSING_DEVELOPMENT, // 도축원
        "872102" to JobRole.FOOD_FOOD_PROCESSING_DEVELOPMENT, // 정육원
        "872200" to JobRole.FOOD_FOOD_PROCESSING_DEVELOPMENT, // 김치 및 밑반찬 제조 종사원
        "872300" to JobRole.FOOD_FOOD_PROCESSING_DEVELOPMENT, // 도시락 제조원
        "872400" to JobRole.FOOD_FOOD_PROCESSING_DEVELOPMENT, // 식품 및 담배 등급원
        "872900" to JobRole.FOOD_FOOD_PROCESSING_DEVELOPMENT, // 기타 식품 가공 관련 기능 종사원
        "881100" to JobRole.PRODUCTION_PRODUCTION, // 인쇄 관련 기계 조작원
        "881101" to JobRole.PRODUCTION_PRODUCTION, // 인쇄판·인쇄필름 출력원
        "881102" to JobRole.PRODUCTION_PRODUCTION, // 오프셋·윤전 인쇄기 조작원
        "881103" to JobRole.PRODUCTION_PRODUCTION, // 그라비어 인쇄기 조작원
        "881104" to JobRole.PRODUCTION_PRODUCTION, // 스크린 인쇄기 조작원(실크스크린 포함)
        "881105" to JobRole.PRODUCTION_PRODUCTION, // 디지털 및 기타 인쇄기 조작원
        "881106" to JobRole.PRODUCTION_PRODUCTION, // 인쇄 후가공원
        "881107" to JobRole.PRODUCTION_PRODUCTION, // 제책·제본기 조작원
        "881200" to JobRole.PRODUCTION_PRODUCTION, // 사진 인화 및 현상기 조작원(사진수정 포함)
        "882100" to JobRole.PRODUCTION_PRODUCTION, // 목재 가공 관련 기계 조작원
        "882101" to JobRole.PRODUCTION_PRODUCTION, // 목재 가공기계 조작원
        "882102" to JobRole.PRODUCTION_PRODUCTION, // 합판 제조기계 조작원
        "882200" to JobRole.PRODUCTION_PRODUCTION, // 펄프 및 종이 제조 장치 조작원
        "882300" to JobRole.PRODUCTION_PRODUCTION, // 종이제품 생산기 조작원
        "882900" to JobRole.PRODUCTION_PRODUCTION, // 기타 목재 및 종이 관련 기계 조작원
        "883100" to JobRole.PRODUCTION_PRODUCTION, // 가구 제조 및 수리원
        "883101" to JobRole.PRODUCTION_PRODUCTION, // 가구 제조원
        "883102" to JobRole.PRODUCTION_PRODUCTION, // 가구 수리원
        "883200" to JobRole.PRODUCTION_PRODUCTION, // 가구 조립원
        "883300" to JobRole.PRODUCTION_PRODUCTION, // 목제품 제조 관련 종사원
        "884100" to JobRole.PRODUCTION_ETC, // 공예원
        "884200" to JobRole.PRODUCTION_ETC, // 귀금속 및 보석 세공원
        "885100" to JobRole.PRODUCTION_ETC, // 악기 제조 및 조율사
        "885200" to JobRole.PRODUCTION_ETC, // 간판 제작 및 설치원
        "885300" to JobRole.PRODUCTION_ETC, // 기타 기능 관련 종사원(유리기능, 복사, 수제 제본 등)
        "885900" to JobRole.PRODUCTION_ETC, // 기타 기계 조작원
        "885901" to JobRole.TRANSPORT_ETC, // 드론 조작원
        "885902" to JobRole.PRODUCTION_ETC, // 주입·포장·상표부착기 등 기타 기계 조작원
        "890000" to JobRole.PRODUCTION_PRODUCTION, // 기타 제조 관련 단순 종사원
        "890001" to JobRole.PRODUCTION_PRODUCTION, // 기계·금속 분야 단순 종사원
        "890002" to JobRole.PRODUCTION_PRODUCTION, // 단순 사상원(용접사상 제외-용접원구분)
        "890003" to JobRole.PRODUCTION_PRODUCTION, // 화학·환경·에너지 분야 단순 종사원
        "890004" to JobRole.PRODUCTION_PRODUCTION, // 섬유·의복 분야 단순 종사원
        "890005" to JobRole.PRODUCTION_PRODUCTION, // 전기·전자 분야 단순 종사원
        "890006" to JobRole.FOOD_FOOD_PROCESSING_DEVELOPMENT, // 식품 분야 단순 종사원
        "890007" to JobRole.PRODUCTION_PRODUCTION, // 인쇄, 목재, 가구 및 기타 제조 분야 단순 종사원
        "901100" to JobRole.PRODUCTION_ETC, // 곡식작물 재배원
        "901200" to JobRole.PRODUCTION_ETC, // 채소 및 특용작물 재배원
        "901300" to JobRole.PRODUCTION_ETC, // 과수작물 재배원
        "901400" to JobRole.PRODUCTION_ETC, // 원예작물 재배원
        "901500" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 조경원
        "901501" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 조경 식재원
        "901502" to JobRole.CONSTRUCTION_CIVIL_SURVEY_LANDSCAPE, // 식물 관리원
        "902100" to JobRole.PRODUCTION_ETC, // 낙농 관련 종사원
        "902200" to JobRole.PRODUCTION_ETC, // 한우 및 육우 사육원
        "902300" to JobRole.PRODUCTION_ETC, // 돼지 사육원
        "902400" to JobRole.PRODUCTION_ETC, // 가금 사육원
        "902900" to JobRole.PRODUCTION_ETC, // 기타 축산 및 사육 관련 종사원(양봉·감별·동물원사육·실험동물사육 등)
        "903100" to JobRole.PRODUCTION_ETC, // 조림·산림 경영인 및 벌목원
        "903900" to JobRole.PRODUCTION_ETC, // 기타 임업 관련 종사원(임산물 채취 포함)
        "904100" to JobRole.PRODUCTION_ETC, // 양식원
        "904200" to JobRole.PRODUCTION_ETC, // 어부 및 해녀
        "905100" to JobRole.PRODUCTION_ETC, // 농업 관련 단순 종사원
        "905200" to JobRole.PRODUCTION_ETC, // 임업 관련 단순 종사원(산림보호감시, 산불감시원 등)
        "905300" to JobRole.PRODUCTION_ETC, // 어업 관련 단순 종사원
    )
}
