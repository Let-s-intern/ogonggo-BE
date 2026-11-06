package com.ogonggo.core.user.domain

import com.ogonggo.core.job.domain.JobRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** 관리자가 고른 렛츠커리어 희망 직무와 회원 프로필의 희망 직무 원문이 겹치는지 판단하는 규칙을 확인한다. */
class LetsCareerWishJobCatalogTest {
    @Test
    @DisplayName("회원이 고른 직무 중 하나라도 관리자가 고른 직무와 같으면 대상이다")
    fun `고른 직무가 하나라도 겹치면 대상이다`() {
        assertTrue(LetsCareerWishJobCatalog.matches("마케터, 백엔드/서버 개발", listOf("백엔드/서버 개발")))
        assertFalse(LetsCareerWishJobCatalog.matches("마케터, 콘텐츠 마케터", listOf("백엔드/서버 개발")))
    }

    @Test
    @DisplayName("직군 전체 선택지는 양쪽 모두 그 직군의 모든 직무로 펼쳐 비교한다")
    fun `직군 전체는 그 직군의 모든 직무로 비교한다`() {
        // 회원이 직군 전체를 고른 경우: 관리자가 그 직군의 한 직무만 골라도 대상이다.
        assertTrue(LetsCareerWishJobCatalog.matches("개발 직무 전체", listOf("iOS 개발")))
        // 관리자가 직군 전체를 고른 경우: 그 직군의 직무를 하나라도 고른 회원이 대상이다.
        assertTrue(LetsCareerWishJobCatalog.matches("퍼포먼스 마케터", listOf("마케팅 직무 전체")))
        assertFalse(LetsCareerWishJobCatalog.matches("퍼포먼스 마케터", listOf("개발 직무 전체")))
    }

    @Test
    @DisplayName("직무 무관과 목록에 없는 값은 어떤 직무와도 겹치지 않는다")
    fun `직무 무관과 모르는 값은 대상이 아니다`() {
        assertFalse(LetsCareerWishJobCatalog.matches("직무 무관", listOf("마케팅 직무 전체")))
        assertFalse(LetsCareerWishJobCatalog.matches("옛날 직무 이름", listOf("마케터")))
        assertFalse(LetsCareerWishJobCatalog.matches(null, listOf("마케터")))
    }

    @Test
    @DisplayName("관리자는 직군 전체와 개별 직무만 고를 수 있고 직무 무관이나 목록에 없는 값은 고를 수 없다")
    fun `고를 수 있는 선택지만 허용한다`() {
        assertTrue(LetsCareerWishJobCatalog.isSelectable("마케팅 직무 전체"))
        assertTrue(LetsCareerWishJobCatalog.isSelectable("CS 매니저"))
        assertFalse(LetsCareerWishJobCatalog.isSelectable(LetsCareerWishJobCatalog.ANY_JOB_ROLE))
        assertFalse(LetsCareerWishJobCatalog.isSelectable("마케팅"))
    }

    @Test
    @DisplayName("렛츠커리어의 모든 직무가 오공고 직무 매핑표에 있다")
    fun `모든 직무에 매핑이 있다`() {
        val roles = LetsCareerWishJobCatalog.rolesByField.values.flatten().toSet()
        assertEquals(roles, LetsCareerWishJobCatalog.jobRolesByWishJob.keys)
    }

    @Test
    @DisplayName("희망 직무는 매핑표대로 오공고 직무가 되고 직군 전체는 그 직군 직무를 모두 합친다")
    fun `희망 직무를 오공고 직무로 옮긴다`() {
        assertEquals(setOf(JobRole.IT_BACKEND, JobRole.IT_IOS), LetsCareerWishJobCatalog.jobRolesOf("백엔드/서버 개발, iOS 개발"))
        assertEquals(
            setOf(JobRole.SALES_B2B, JobRole.SALES_TECH_IT, JobRole.SALES_OVERSEAS, JobRole.TRADE_OVERSEAS_TRADING_SALES,
                JobRole.PLANNING_CONSULTING, JobRole.SALES_B2C, JobRole.SALES_GENERAL, JobRole.SALES_MANAGEMENT_SUPPORT,
                JobRole.SALES_ETC, JobRole.SALES_PHARMA_MEDICAL),
            LetsCareerWishJobCatalog.jobRolesOf("영업 직무 전체"),
        )
    }

    @Test
    @DisplayName("직무 무관, 비어 있는 값, 맞는 오공고 직무가 없는 값은 아무 직무도 되지 않는다")
    fun `옮길 직무가 없으면 빈 집합이다`() {
        assertEquals(emptySet<JobRole>(), LetsCareerWishJobCatalog.jobRolesOf(LetsCareerWishJobCatalog.ANY_JOB_ROLE))
        assertEquals(emptySet<JobRole>(), LetsCareerWishJobCatalog.jobRolesOf(null))
        assertEquals(emptySet<JobRole>(), LetsCareerWishJobCatalog.jobRolesOf("그래픽 디자인"))
    }
}
