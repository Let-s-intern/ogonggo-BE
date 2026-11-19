package com.ogonggo.adminapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposurePageDto
import java.time.LocalDateTime

data class AdminAdvertisementExposureResult(
    val id: Long,
    val productId: Long,
    val contractId: Long?,
    val name: String?,
    val imageUrl: String?,
    val mobileImageUrl: String?,
    val linkUrl: String?,
    val targetType: AdvertisementExposureTargetType?,
    val targetId: Long?,
    val adLabel: Boolean,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val exposed: Boolean,
    val advertiserCompanyName: String?,
    val targetTitle: String?,
    val targetCompanyName: String?,
    val targetPublished: Boolean?,
    val displayOrder: Int,
    val registeredAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        internal fun from(
            exposure: AdvertisementExposure,
            advertiserCompanyName: String?,
            target: AdminAdvertisementExposureTarget?,
        ) = AdminAdvertisementExposureResult(
            id = checkNotNull(exposure.id) { "광고 노출 식별자가 없습니다." },
            productId = exposure.productId,
            contractId = exposure.contractId,
            name = exposure.name,
            imageUrl = exposure.imageUrl,
            mobileImageUrl = exposure.mobileImageUrl,
            linkUrl = exposure.linkUrl,
            targetType = exposure.targetType,
            targetId = exposure.targetId,
            adLabel = exposure.adLabel,
            startAt = exposure.startAt,
            endAt = exposure.endAt,
            exposed = exposure.exposed,
            advertiserCompanyName = advertiserCompanyName,
            targetTitle = target?.title,
            targetCompanyName = target?.companyName,
            // 광고 공고인데 대상 행이 없으면 게시 중이 아니다. 배너는 대상이 없어 null이다.
            targetPublished = target?.published ?: exposure.targetType?.let { false },
            displayOrder = exposure.displayOrder,
            registeredAt = exposure.createdAt,
            updatedAt = exposure.updatedAt,
        )
    }
}

data class AdminAdvertisementExposurePageResult(
    val items: List<AdminAdvertisementExposureResult>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(
            page: AdvertisementExposurePageDto,
            companyNames: Map<Long, String>,
            targets: Map<Pair<AdvertisementExposureTargetType, Long>, AdminAdvertisementExposureTarget>,
        ) = AdminAdvertisementExposurePageResult(
            items = page.items.map { exposure ->
                AdminAdvertisementExposureResult.from(
                    exposure,
                    exposure.contractId?.let(companyNames::get),
                    exposure.targetType?.let { type -> exposure.targetId?.let { targets[type to it] } },
                )
            },
            page = page.page,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}

/** 광고 공고가 띄우는 채용공고·부트캠프를 목록에서 알아보게 하는 값이다. 지운 대상은 게시 중이 아닌 것으로 본다. */
data class AdminAdvertisementExposureTarget(
    val title: String,
    val companyName: String,
    val published: Boolean,
)
