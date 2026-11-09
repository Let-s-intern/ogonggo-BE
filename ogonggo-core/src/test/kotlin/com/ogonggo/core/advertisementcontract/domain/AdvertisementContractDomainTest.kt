package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AdvertisementContractDomainTest {

    @Test
    fun `비즈니스 공고 계약은 검수 대기면 신청, 승인이면 승인, 반려면 반려다`() {
        val contract = companyPostingContract()

        contract.followReview(ContentReviewStatus.APPROVED)
        assertEquals(AdvertisementContractStatus.APPROVED, contract.status)

        contract.followReview(ContentReviewStatus.PENDING)
        assertEquals(AdvertisementContractStatus.REQUESTED, contract.status)

        contract.followReview(ContentReviewStatus.REJECTED)
        assertEquals(AdvertisementContractStatus.REJECTED, contract.status)
    }

    @Test
    fun `비즈니스 공고 계약의 상태는 운영자가 바꿀 수 없다`() {
        val exception = assertThrows(ConflictException::class.java) {
            companyPostingContract().changeStatus(AdvertisementContractStatus.APPROVED)
        }

        assertEquals(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_FOLLOWS_REVIEW, exception.errorCode)
    }

    @Test
    fun `신청 폼 계약은 운영자가 어느 상태로든 바꾼다`() {
        val contract = formContract()

        contract.changeStatus(AdvertisementContractStatus.REJECTED)
        contract.changeStatus(AdvertisementContractStatus.NEGOTIATING)

        assertEquals(AdvertisementContractStatus.NEGOTIATING, contract.status)
    }

    @Test
    fun `신청 폼 계약은 검수 상태를 따르지 않는다`() {
        assertThrows(IllegalStateException::class.java) {
            formContract().followReview(ContentReviewStatus.APPROVED)
        }
    }

    @Test
    fun `신청 폼 계약은 광고 카테고리를 하나 이상 골라야 한다`() {
        assertThrows(IllegalArgumentException::class.java) { formContract(categories = emptySet()) }
    }

    @Test
    fun `신청 폼 계약은 예정 예산 구간을 고르지 않아도 된다`() {
        assertEquals(null, formContract(budgetRange = null).budgetRange)
        assertEquals(AdvertisementContractBudgetRange.FROM_1M_TO_3M, formContract().budgetRange)
    }

    @Test
    fun `비즈니스 공고 계약은 예정 예산 구간을 갖지 않는다`() {
        assertEquals(null, companyPostingContract().budgetRange)
        assertThrows(IllegalArgumentException::class.java) {
            AdvertisementContract(
                source = AdvertisementContractSource.COMPANY_POSTING,
                status = AdvertisementContractStatus.REQUESTED,
                userId = 7L,
                promotionTarget = AdvertisementContractPromotionTarget.JOB,
                targetId = 1L,
                companyName = "뱅크",
                recruitmentTitle = "백엔드 개발자",
                recruitmentUrl = null,
                recruitmentDeadline = null,
                purpose = null,
                budgetRange = AdvertisementContractBudgetRange.OVER_3M,
                categories = emptySet(),
                scheduleRequest = null,
                advertisementRequest = null,
                managerName = null,
                email = null,
                phoneNumber = null,
                businessNumberType = null,
                businessNumber = null,
                invoiceMethod = null,
                creativeMethod = null,
            )
        }
    }

    @Test
    fun `신청 폼 계약은 사업자 번호 없이도 접수한다`() {
        val contract = formContract()

        assertEquals(null, contract.businessNumberType)
        assertEquals(null, contract.businessNumber)
    }

    @Test
    fun `신청 폼 계약의 사업자 번호는 기업 회원가입과 같이 종류와 자릿수가 맞아야 한다`() {
        val registration = formContract(
            businessNumberType = CompanyBusinessNumberType.BUSINESS_REGISTRATION,
            businessNumber = "1234567890",
        )
        val overseas = formContract(businessNumberType = CompanyBusinessNumberType.OVERSEAS)

        assertEquals(CompanyBusinessNumberType.BUSINESS_REGISTRATION, registration.businessNumberType)
        assertEquals("1234567890", registration.businessNumber)
        assertEquals(null, overseas.businessNumber)
        assertThrows(IllegalArgumentException::class.java) { formContract(businessNumber = "1234567890") }
        assertThrows(IllegalArgumentException::class.java) {
            formContract(businessNumberType = CompanyBusinessNumberType.CORPORATE_REGISTRATION, businessNumber = "1234567890")
        }
        assertThrows(IllegalArgumentException::class.java) {
            formContract(businessNumberType = CompanyBusinessNumberType.OVERSEAS, businessNumber = "1234567890")
        }
    }

    private fun companyPostingContract() = AdvertisementContract.ofCompanyPosting(
        userId = 7L,
        promotionTarget = AdvertisementContractPromotionTarget.JOB,
        targetId = 1L,
        companyName = "뱅크",
        recruitmentTitle = "백엔드 개발자",
        recruitmentDeadline = null,
    )

    private fun formContract(
        categories: Set<AdvertisementProductCategory> = setOf(AdvertisementProductCategory.BANNER),
        budgetRange: AdvertisementContractBudgetRange? = AdvertisementContractBudgetRange.FROM_1M_TO_3M,
        businessNumberType: CompanyBusinessNumberType? = null,
        businessNumber: String? = null,
    ) =
        AdvertisementContract(
            source = AdvertisementContractSource.APPLICATION_FORM,
            status = AdvertisementContractStatus.REQUESTED,
            userId = null,
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            targetId = null,
            companyName = "렛츠커리어",
            recruitmentTitle = "브랜드 마케팅 인턴 모집",
            recruitmentUrl = "https://example.com/recruit",
            recruitmentDeadline = LocalDate.of(2026, 12, 20),
            purpose = AdvertisementContractPurpose.FAST_APPLICANTS,
            budgetRange = budgetRange,
            categories = categories,
            scheduleRequest = null,
            advertisementRequest = null,
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "010-1234-5678",
            businessNumberType = businessNumberType,
            businessNumber = businessNumber,
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
        )
}
