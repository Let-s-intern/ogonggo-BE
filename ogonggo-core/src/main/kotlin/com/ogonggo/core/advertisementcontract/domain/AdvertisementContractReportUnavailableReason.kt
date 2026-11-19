package com.ogonggo.core.advertisementcontract.domain

import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.enumeration.EnumField
import java.time.LocalDate

/**
 * 계약 상품의 성과 리포트를 열 수 없는 이유다.
 *
 * 리포트는 상품 기간 안에 그 상품의 노출로 들어온 이벤트를 세므로, 오공고 화면에 노출하지 않는 채널 상품과 기간이 없는 상품은 셀 수 없다.
 */
enum class AdvertisementContractReportUnavailableReason(
    override val code: Int,
    override val desc: String,
) : EnumField {
    NO_PERIOD(1, "기간 미정"),
    NOT_EXPOSABLE(2, "집계 대상 아님"),
    ;

    companion object {
        /** 열 수 있으면 null이다. 둘 다 해당하면 채널 상품이 먼저다. */
        fun of(category: AdvertisementProductCategory, startDate: LocalDate?, endDate: LocalDate?): AdvertisementContractReportUnavailableReason? =
            when {
                !category.exposable -> NOT_EXPOSABLE
                startDate == null || endDate == null -> NO_PERIOD
                else -> null
            }
    }
}
