package com.ogonggo.core.advertisementexposure.implement

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementexposure.error.AdvertisementExposureErrorCode
import com.ogonggo.core.advertisementexposure.persistence.AdvertisementExposureQueryRepository
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.implement.JobReader
import org.springframework.stereotype.Component

/**
 * 광고 공고 노출이 띄울 공고를 확인한다. 등록과 수정이 같은 규칙을 쓴다.
 *
 * 대상은 지워지지 않았으면 된다. 게시·모집 중이 아니어도 미리 세팅해 둘 수 있고, 공개 조회가 그때그때 거른다.
 * 같은 자리(같은 position의 상품들, 자리가 없는 상품은 그 상품)에 같은 공고를 두 번 띄우지 않는다.
 * 꺼졌거나 기간이 지난 노출도 지우기 전까지는 중복으로 본다.
 * 대상 종류와 자리가 맞는지, 대상이 비었는지는 [com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure]가 본다.
 */
@Component
class AdvertisementExposureTargetValidator internal constructor(
    private val advertisementExposureQueryRepository: AdvertisementExposureQueryRepository,
    private val advertisementProductReader: AdvertisementProductReader,
    private val jobReader: JobReader,
    private val bootcampReader: BootcampReader,
) {

    fun validate(product: AdvertisementProduct, content: AdvertisementExposureContent, excludedExposureId: Long? = null) {
        if (product.category != AdvertisementProductCategory.JOB_POSTING) {
            return
        }
        val targetType = content.targetType ?: return
        val targetId = content.targetId?.takeIf { it > 0 } ?: return
        when (targetType) {
            AdvertisementExposureTargetType.JOB -> jobReader.read(targetId)
            AdvertisementExposureTargetType.BOOTCAMP -> bootcampReader.read(targetId)
        }
        val productIds = product.position
            ?.let { position -> advertisementProductReader.readByPosition(position).mapNotNull { it.id } }
            ?: listOfNotNull(product.id)
        if (advertisementExposureQueryRepository.existsTarget(productIds, targetType, targetId, excludedExposureId)) {
            throw ConflictException(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_TARGET_DUPLICATED)
        }
    }
}
