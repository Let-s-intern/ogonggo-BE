package com.ogonggo.core.advertisementevent.implement

import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementContractReportDto
import java.time.LocalDate

/** 기존 DB 이벤트 적재·집계를 그대로 사용하는 단일 원천 provider. */
class MySqlAdvertisementContractReportProvider(
    private val reader: AdvertisementEventReportReader,
) : AdvertisementContractReportProvider {
    override fun read(contractId: Long, from: LocalDate, to: LocalDate, exposureIds: List<Long>): AdvertisementContractReportDto =
        reader.read(contractId, from, to)
}
