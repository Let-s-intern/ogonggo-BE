package com.ogonggo.userapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReportReader
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.user.implement.requireActive
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

/**
 * 기업 회원이 자기 광고 계약의 성과 리포트를 본다. 광고 신청 폼 계약과 비즈니스 공고 계약을 모두 볼 수 있다.
 * 회사 회원이 아니면 403, 남의 계약은 없는 계약과 같이 404다.
 */
@Service
class MyAdvertisementReportService(
    private val userReader: UserReader,
    private val advertisementContractReader: AdvertisementContractReader,
    private val advertisementContractReportReader: AdvertisementContractReportReader,
    private val clock: Clock,
) {

    fun getReport(userId: Long, contractId: Long, contractProductId: Long, from: LocalDate?, to: LocalDate?): AdvertisementContractReportDto {
        verifyCompany(userId)
        val contract = advertisementContractReader.readOwnedBy(userId, contractId)
        return advertisementContractReportReader.read(contract, contractProductId, from, to, LocalDate.now(clock))
    }

    private fun verifyCompany(userId: Long) {
        val account = userReader.read(userId)
        account.status.requireActive()
        if (account.role != UserRole.COMPANY) {
            throw ForbiddenException(UserErrorCode.COMPANY_ROLE_REQUIRED)
        }
    }
}
