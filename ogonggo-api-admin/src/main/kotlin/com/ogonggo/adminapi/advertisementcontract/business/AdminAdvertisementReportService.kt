package com.ogonggo.adminapi.advertisementcontract.business

import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReportReader
import com.ogonggo.core.advertisementcontract.implement.AdvertisementReportInsightManager
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTotalDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementReportInsightDraftDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementReportInsightDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/** 운영자가 어느 계약이든 상품 성과 리포트를 본다. 숫자는 기업 회원이 보는 것과 같다. */
@Service
class AdminAdvertisementReportService(
    private val advertisementContractReader: AdvertisementContractReader,
    private val advertisementContractReportReader: AdvertisementContractReportReader,
    private val advertisementReportInsightManager: AdvertisementReportInsightManager,
    private val clock: Clock,
) {

    fun getReport(contractId: Long, contractProductId: Long, from: LocalDate?, to: LocalDate?): AdvertisementContractReportDto {
        val contract = advertisementContractReader.read(contractId)
        return advertisementContractReportReader.read(contract, contractProductId, from, to, LocalDate.now(clock), includeDraftInsights = true)
    }

    /** 상품의 인사이트를 통째로 바꾼다. [published] 가 참이면 기업 회원의 리포트에 보낸다. */
    @Transactional
    fun replaceInsights(
        contractId: Long,
        contractProductId: Long,
        drafts: List<AdvertisementReportInsightDraftDto>,
        published: Boolean,
    ): List<AdvertisementReportInsightDto> {
        val contract = advertisementContractReader.readForUpdate(contractId)
        return advertisementReportInsightManager.replace(contract, contractProductId, drafts, published, LocalDateTime.now(clock))
    }

    fun getTotal(contractId: Long): AdvertisementContractReportTotalDto =
        advertisementContractReportReader.readTotal(advertisementContractReader.read(contractId), LocalDate.now(clock))
}
