package com.ogonggo.adminapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReportReader
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDto
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

/** 운영자가 어느 계약이든 상품 성과 리포트를 본다. 숫자는 기업 회원이 보는 것과 같다. */
@Service
class AdminAdvertisementReportService(
    private val advertisementContractReader: AdvertisementContractReader,
    private val advertisementContractReportReader: AdvertisementContractReportReader,
    private val clock: Clock,
) {

    fun getReport(contractId: Long, contractProductId: Long, from: LocalDate?, to: LocalDate?): AdvertisementContractReportDto {
        val contract = advertisementContractReader.read(contractId)
        return advertisementContractReportReader.read(contract, contractProductId, from, to, LocalDate.now(clock))
    }
}
