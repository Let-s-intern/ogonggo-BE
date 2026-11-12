package com.ogonggo.core.advertisementinquiry.domain

import com.ogonggo.core.user.domain.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AdvertisementInquiryDomainTest {

    @Test
    fun `새 문의는 접수 상태이고 메모가 없다`() {
        val inquiry = inquiry()

        assertEquals(AdvertisementInquiryStatus.RECEIVED, inquiry.status)
        assertNull(inquiry.memo)
    }

    @Test
    fun `회사명 입력 방식은 받은 값 그대로 남긴다`() {
        AdvertisementInquiryCompanyNameSource.entries.forEach { source ->
            assertEquals(source, inquiry(companyNameSource = source).companyNameSource)
        }
    }

    @Test
    fun `처리 상태는 어느 상태로든 바꾸고 되돌릴 수 있다`() {
        val inquiry = inquiry()

        inquiry.changeStatus(AdvertisementInquiryStatus.REJECTED)
        assertEquals(AdvertisementInquiryStatus.REJECTED, inquiry.status)

        inquiry.changeStatus(AdvertisementInquiryStatus.CONTACTED)
        assertEquals(AdvertisementInquiryStatus.CONTACTED, inquiry.status)
    }

    @Test
    fun `메모는 통째로 바꾸고 공백만 있으면 지운 것으로 본다`() {
        val inquiry = inquiry()

        inquiry.changeMemo("10월 30일 통화 예정")
        assertEquals("10월 30일 통화 예정", inquiry.memo)

        inquiry.changeMemo("  ")
        assertNull(inquiry.memo)
    }

    @Test
    fun `메모는 2000자를 넘을 수 없다`() {
        val inquiry = inquiry()

        assertThrows(IllegalArgumentException::class.java) { inquiry.changeMemo("가".repeat(2001)) }
    }

    @Test
    fun `비회원 문의에만 회원 식별자가 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            inquiry(userId = 3L, memberType = AdvertisementInquiryMemberType.GUEST)
        }
        assertThrows(IllegalArgumentException::class.java) {
            inquiry(userId = null, memberType = AdvertisementInquiryMemberType.COMPANY)
        }
    }

    @Test
    fun `회원 구분은 역할로 정하고 멘토·관리자는 일반 회원, 회원이 아니면 비회원이다`() {
        assertEquals(AdvertisementInquiryMemberType.COMPANY, AdvertisementInquiryMemberType.of(UserRole.COMPANY))
        listOf(UserRole.USER, UserRole.MENTOR, UserRole.ADMIN).forEach { role ->
            assertEquals(AdvertisementInquiryMemberType.USER, AdvertisementInquiryMemberType.of(role))
        }
        assertEquals(AdvertisementInquiryMemberType.GUEST, AdvertisementInquiryMemberType.of(null))
    }

    @Test
    fun `무료 홍보 문의는 희망 홍보 채널과 설문 답변이 있어야 한다`() {
        listOf(AdvertisementInquiryType.FREE_PROMOTION, AdvertisementInquiryType.LAUNCH_ALERT).forEach { type ->
            assertThrows(IllegalArgumentException::class.java) { inquiry(inquiryType = type, promotionChannel = null) }
            assertThrows(IllegalArgumentException::class.java) { inquiry(inquiryType = type, promotionAnswer = " ") }
        }
    }

    @Test
    fun `유료 광고 문의는 광고 목적만 있으면 되고 홍보 채널과 설문 답변이 없어도 된다`() {
        val inquiry = paidInquiry(purpose = "신입 마케터 채용 홍보", budgetAmount = null)

        assertEquals(AdvertisementInquiryType.PAID_ADVERTISEMENT, inquiry.inquiryType)
        assertEquals("신입 마케터 채용 홍보", inquiry.purpose)
        assertNull(inquiry.promotionChannel)
        assertNull(inquiry.budgetAmount)
    }

    @Test
    fun `유료 광고 문의는 광고 목적이 비어 있으면 만들 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) { paidInquiry(purpose = null) }
        assertThrows(IllegalArgumentException::class.java) { paidInquiry(purpose = "  ") }
    }

    @Test
    fun `희망 예산은 적었다면 양수여야 한다`() {
        assertEquals(1_500_000, paidInquiry(budgetAmount = 1_500_000).budgetAmount)
        assertThrows(IllegalArgumentException::class.java) { paidInquiry(budgetAmount = 0) }
    }

    private fun paidInquiry(purpose: String? = "신입 마케터 채용 홍보", budgetAmount: Int? = 1_500_000) = inquiry(
        inquiryType = AdvertisementInquiryType.PAID_ADVERTISEMENT,
        promotionChannel = null,
        promotionAnswer = null,
        purpose = purpose,
        budgetAmount = budgetAmount,
    )

    private fun inquiry(
        userId: Long? = null,
        memberType: AdvertisementInquiryMemberType = AdvertisementInquiryMemberType.GUEST,
        companyNameSource: AdvertisementInquiryCompanyNameSource = AdvertisementInquiryCompanyNameSource.MANUAL,
        inquiryType: AdvertisementInquiryType = AdvertisementInquiryType.FREE_PROMOTION,
        promotionChannel: AdvertisementInquiryPromotionChannel? = AdvertisementInquiryPromotionChannel.INSTAGRAM,
        promotionAnswer: String? = "사람인에 올리고 있습니다.",
        purpose: String? = null,
        budgetAmount: Int? = null,
    ) = AdvertisementInquiry(
        inquiryType = inquiryType,
        campaignCode = "launch_free_promo".takeIf { inquiryType.isFreePromotionForm },
        companyName = "렛츠커리어",
        companyNameSource = companyNameSource,
        managerName = "김담당",
        managerTitle = "매니저",
        email = "manager@ogonggo.co.kr",
        phoneNumber = "010-1234-5678",
        promotionChannel = promotionChannel,
        hiringJobName = "마케팅 인턴",
        jobPostingUrl = "https://example.com/jobs/1",
        promotionAnswer = promotionAnswer,
        purpose = purpose,
        budgetAmount = budgetAmount,
        marketingConsent = false,
        userId = userId,
        memberType = memberType,
        inflow = AdvertisementInquiryInflow(
            utmSource = null,
            utmMedium = null,
            utmCampaign = null,
            utmContent = null,
            referrer = null,
            entryHost = null,
            landingPath = null,
            channel = AdvertisementInquiryInflowChannel.DIRECT,
        ),
    )
}
