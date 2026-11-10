package com.ogonggo.userapi.advertisementcontract.implement.dto

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.user.domain.CompanyBusinessNumberType
import com.ogonggo.userapi.advertisementcontract.business.CreateAdvertisementContractCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AdvertisementContractNotificationDtoTest {

    @Test
    fun `사업자 번호는 번호 종류와 함께 알림 값에 싣는다`() {
        // when
        val parameters = AdvertisementContractNotificationDto.from(
            CONTRACT_ID,
            COMMAND.copy(businessNumberType = CompanyBusinessNumberType.BUSINESS_REGISTRATION, businessNumber = "1234567890"),
        ).templateParameters()

        // then
        assertEquals("사업자등록번호 1234567890", parameters["businessNumber"])
    }

    @Test
    fun `해외 기업은 번호 종류만 싣는다`() {
        // when
        val parameters = AdvertisementContractNotificationDto.from(
            CONTRACT_ID,
            COMMAND.copy(businessNumberType = CompanyBusinessNumberType.OVERSEAS),
        ).templateParameters()

        // then
        assertEquals("해외 기업", parameters["businessNumber"])
    }

    @Test
    fun `사업자 번호를 보내지 않은 신청은 알림 값에 사업자 번호가 없다`() {
        // when
        val parameters = AdvertisementContractNotificationDto.from(CONTRACT_ID, COMMAND).templateParameters()

        // then
        assertFalse(parameters.containsKey("businessNumber"))
    }

    private companion object {
        const val CONTRACT_ID = 1001L
        val COMMAND = CreateAdvertisementContractCommand(
            promotionTarget = AdvertisementContractPromotionTarget.JOB,
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentUrl = "https://example.com/recruit/backend",
            recruitmentDeadline = LocalDate.of(2026, 12, 31),
            purpose = AdvertisementContractPurpose.FIT_APPLICANTS,
            budgetAmount = null,
            categories = setOf(AdvertisementProductCategory.KAKAO_CRM),
            scheduleRequest = null,
            advertisementRequest = null,
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "02-1234-5678",
            invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
            creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
        )
    }
}
