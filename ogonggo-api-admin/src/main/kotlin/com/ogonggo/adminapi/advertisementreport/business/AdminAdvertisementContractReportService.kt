package com.ogonggo.adminapi.advertisementreport.business

import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementevent.error.AdvertisementEventErrorCode
import com.ogonggo.core.advertisementevent.implement.AdvertisementEventReportReader
import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.error.InvalidValueException
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class AdminAdvertisementContractReportService(
    private val advertisementContractReader: AdvertisementContractReader,
    private val advertisementEventReportReader: AdvertisementEventReportReader,
    private val clock: Clock,
) {

    fun getReport(contractId: Long, from: LocalDate, to: LocalDate): AdvertisementContractReportDto {
        advertisementContractReader.read(contractId)
        validatePeriod(from, to)
        return advertisementEventReportReader.read(contractId, from, to)
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
