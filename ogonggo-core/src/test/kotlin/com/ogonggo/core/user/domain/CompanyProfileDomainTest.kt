package com.ogonggo.core.user.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CompanyProfileDomainTest {

    @Test
    fun `사업자등록번호는 숫자 10자리, 법인등록번호는 숫자 13자리다`() {
        val business = profile(CompanyBusinessNumberType.BUSINESS_REGISTRATION, "1234567890")
        val corporate = profile(CompanyBusinessNumberType.CORPORATE_REGISTRATION, "1101111234567")

        assertEquals("1234567890", business.businessNumber)
        assertEquals("1101111234567", corporate.businessNumber)
        listOf(
            CompanyBusinessNumberType.BUSINESS_REGISTRATION to "123456789",
            CompanyBusinessNumberType.BUSINESS_REGISTRATION to "1101111234567",
            CompanyBusinessNumberType.BUSINESS_REGISTRATION to "123-45-6789",
            CompanyBusinessNumberType.CORPORATE_REGISTRATION to "1234567890",
            CompanyBusinessNumberType.CORPORATE_REGISTRATION to "110111-1234567",
            CompanyBusinessNumberType.BUSINESS_REGISTRATION to null,
        ).forEach { (type, number) ->
            assertThrows(IllegalArgumentException::class.java, { profile(type, number) }, "$type $number")
        }
    }

    @Test
    fun `해외 기업은 번호 없이 두고 번호가 있으면 거부한다`() {
        val overseas = profile(CompanyBusinessNumberType.OVERSEAS, null)

        assertEquals(CompanyBusinessNumberType.OVERSEAS, overseas.businessNumberType)
        assertNull(overseas.businessNumber)
        assertThrows(IllegalArgumentException::class.java) {
            profile(CompanyBusinessNumberType.OVERSEAS, "1234567890")
        }
    }

    @Test
    fun `번호 종류 없이 번호만 둘 수 없고 둘 다 없으면 이전 가입처럼 비워 둔다`() {
        assertThrows(IllegalArgumentException::class.java) { profile(null, "1234567890") }

        val legacy = profile(null, null)
        assertNull(legacy.businessNumberType)
        assertNull(legacy.businessNumber)
    }

    @Test
    fun `가입 정보의 글자 값은 비워 둘 수 있지만 공백일 수 없다`() {
        listOf<(String) -> CompanyProfile>(
            { profileWith(industry = it) },
            { profileWith(address = it) },
            { profileWith(homepageUrl = it) },
            { profileWith(managerTitle = it) },
            { profileWith(recommender = it) },
        ).forEach { create ->
            assertThrows(IllegalArgumentException::class.java) { create(" ") }
        }
    }

    private fun profile(type: CompanyBusinessNumberType?, number: String?): CompanyProfile = CompanyProfile(
        userId = 1L,
        organizationName = "렛츠커리어",
        managerName = "김담당",
        businessNumberType = type,
        businessNumber = number,
    )

    private fun profileWith(
        industry: String? = null,
        address: String? = null,
        homepageUrl: String? = null,
        managerTitle: String? = null,
        recommender: String? = null,
    ): CompanyProfile = CompanyProfile(
        userId = 1L,
        organizationName = "렛츠커리어",
        managerName = "김담당",
        industry = industry,
        address = address,
        homepageUrl = homepageUrl,
        managerTitle = managerTitle,
        recommender = recommender,
    )
}
