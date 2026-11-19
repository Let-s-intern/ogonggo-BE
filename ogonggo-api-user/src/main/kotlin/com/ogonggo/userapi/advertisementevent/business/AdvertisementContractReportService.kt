package com.ogonggo.userapi.advertisementevent.business

import com.ogonggo.core.advertisementevent.error.AdvertisementEventErrorCode
import com.ogonggo.core.advertisementevent.implement.AdvertisementContractReportProvider
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.userapi.advertisementevent.implement.AdvertisementReportAccess
import com.ogonggo.userapi.advertisementevent.implement.AdvertisementReportAccessReader
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class AdvertisementContractReportService(
    private val advertisementReportAccessReader: AdvertisementReportAccessReader,
    private val advertisementContractReportProvider: AdvertisementContractReportProvider,
    private val clock: Clock,
    transactionManager: PlatformTransactionManager,
) {

    /** Keeps the business-owned DB snapshot short and closes it before the external report request. */
    private val accessReadTransaction = TransactionTemplate(transactionManager).apply {
        isReadOnly = true
    }

    fun getForCompanyMember(userId: Long, contractId: Long, from: LocalDate, to: LocalDate): AdvertisementContractReportDto {
        val access = checkNotNull(
            accessReadTransaction.execute { advertisementReportAccessReader.read(userId, contractId) },
        )
        if (access.userRole != UserRole.COMPANY || access.contractOwnerId != userId) {
            throw ForbiddenException(AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_FORBIDDEN)
        }
        validatePeriod(from, to)
        // Access and allowlist DB reads have completed before report I/O begins.
        return advertisementContractReportProvider.read(contractId, from, to, access.exposureIds)
    }

    private fun validatePeriod(from: LocalDate, to: LocalDate) {
        val today = LocalDate.now(clock.withZone(SEOUL))
        val valid = !from.isAfter(to) && !to.isAfter(today) && !from.isBefore(today.minusYears(1)) &&
            ChronoUnit.DAYS.between(from, to) <= MAX_REPORT_RANGE_DAYS
        if (!valid) {
            throw InvalidValueException(AdvertisementEventErrorCode.ADVERTISEMENT_REPORT_PERIOD_INVALID)
        }
    }

    companion object {
        private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        private const val MAX_REPORT_RANGE_DAYS = 365L
    }
}
