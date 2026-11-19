package com.ogonggo.userapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReportReader
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class MyAdvertisementReportServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val contractReader = Mockito.mock(AdvertisementContractReader::class.java)
    private val reportReader = Mockito.mock(AdvertisementContractReportReader::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-11-19T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = MyAdvertisementReportService(userReader, contractReader, reportReader, clock)

    @Test
    fun `기업 회원이 아니면 계약을 읽기 전에 403이다`() {
        // given
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(account(UserRole.USER))

        // when
        val exception = assertThrows<ForbiddenException> { service.getReport(USER_ID, CONTRACT_ID, 7L, null, null) }

        // then
        assertEquals(UserErrorCode.COMPANY_ROLE_REQUIRED, exception.errorCode)
        Mockito.verifyNoInteractions(contractReader, reportReader)
    }

    @Test
    fun `남의 계약은 없는 계약과 같이 404다`() {
        // given
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(account(UserRole.COMPANY))
        Mockito.`when`(contractReader.readOwnedBy(USER_ID, CONTRACT_ID))
            .thenThrow(EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_NOT_FOUND))

        // when
        val exception = assertThrows<EntityNotFoundException> { service.getReport(USER_ID, CONTRACT_ID, 7L, null, null) }

        // then
        assertEquals(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_NOT_FOUND, exception.errorCode)
        Mockito.verifyNoInteractions(reportReader)
    }

    private fun account(role: UserRole) = UserAccountDto(
        userId = USER_ID,
        letsCareerUserId = null,
        email = "company@example.com",
        status = UserStatus.ACTIVE,
        role = role,
        joinedAt = LocalDateTime.of(2026, 11, 1, 0, 0),
    )

    private companion object {
        const val USER_ID = 42L
        const val CONTRACT_ID = 1L
    }
}
