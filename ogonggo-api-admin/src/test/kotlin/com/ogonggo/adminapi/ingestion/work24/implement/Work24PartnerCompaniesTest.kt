package com.ogonggo.adminapi.ingestion.work24.implement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class Work24PartnerCompaniesTest {

    @Test
    fun `과정명 앞 괄호가 표에 있는 기업이면 파트너사 이름을 돌려준다`() {
        assertEquals("kt cloud", Work24PartnerCompanies.of("[kt cloud] 생성형AI 과정"))
        // 괄호 종류, 공백, 대소문자가 달라도 찾는다.
        assertEquals("LG전자", Work24PartnerCompanies.of("[ LG전자 ] LG전자 AX School - 마케팅 과정"))
        assertEquals("코오롱베니트", Work24PartnerCompanies.of("[코오롱 베니트]하이브리드 클라우드 컨테이너 플랫폼 설계 및 구현 과정"))
        assertEquals("MBC플러스", Work24PartnerCompanies.of("(MBC+) 생성형 AI & 언리얼 엔진 활용 미디어 콘텐츠 크리에이터"))
        // 과정 브랜드 이름은 운영 기업 이름으로 바꾼다.
        assertEquals("이스트소프트", Work24PartnerCompanies.of("[이스트캠프] 가디언즈 정보보호 및 보안 인프라 운영 관리"))
    }

    @Test
    fun `직종이나 과정 성격을 적은 괄호와 괄호 없는 과정명은 파트너사가 없다`() {
        assertNull(Work24PartnerCompanies.of("[취업기업확대]AI활용 풀스택(프론트엔드,백엔드)부트캠프"))
        assertNull(Work24PartnerCompanies.of("(스마트웹&콘텐츠개발) AI코딩 어시스턴트 활용 프론트엔드 개발"))
        assertNull(Work24PartnerCompanies.of("프로젝트 기반 AWS 풀스택 웹 개발자 양성"))
        // 괄호가 과정명 중간에 있으면 보지 않는다.
        assertNull(Work24PartnerCompanies.of("AI 아카데미 [LG전자] 과정"))
        assertNull(Work24PartnerCompanies.of(null))
    }
}
