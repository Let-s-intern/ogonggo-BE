package com.ogonggo.core.advertisementevent.implement

import com.ogonggo.core.advertisementevent.implement.dto.AdvertisementContractReportDto
import java.time.LocalDate

/** 사용자·관리자 JSON·CSV가 동일한 단일 집계 원천을 선택해 쓰는 경계다. */
interface AdvertisementContractReportProvider {
    fun read(
        contractId: Long,
        from: LocalDate,
        to: LocalDate,
        exposureIds: List<Long>,
    ): AdvertisementContractReportDto
}
