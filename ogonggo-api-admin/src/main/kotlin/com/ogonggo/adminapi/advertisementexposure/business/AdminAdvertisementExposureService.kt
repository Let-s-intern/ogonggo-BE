package com.ogonggo.adminapi.advertisementexposure.business

import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureMoveDirection
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposurePeriod
import com.ogonggo.core.advertisementexposure.error.AdvertisementExposureErrorCode
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureAppender
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureManager
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureAppendDto
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureUpdateDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.implement.ImageAssetManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminAdvertisementExposureService(
    private val advertisementExposureReader: AdvertisementExposureReader,
    private val advertisementExposureAppender: AdvertisementExposureAppender,
    private val advertisementExposureManager: AdvertisementExposureManager,
    private val advertisementProductReader: AdvertisementProductReader,
    private val advertisementContractReader: AdvertisementContractReader,
    private val imageAssetManager: ImageAssetManager,
    private val clock: Clock,
) {

    fun getExposures(productId: Long, contractId: Long?, page: Int, size: Int): AdminAdvertisementExposurePageResult {
        val result = advertisementExposureReader.readByProductPage(productId, contractId, page, size)
        val contracts = advertisementContractReader.readAllByIds(result.items.mapNotNull { it.contractId }.toSet())
        return AdminAdvertisementExposurePageResult.from(
            result,
            contracts.mapValues { it.value.companyName },
        )
    }

    @Transactional
    fun createExposure(command: CreateAdminAdvertisementExposureCommand, adminUserId: Long): Long {
        // A-12의 BANNER별 URL 검증을 위해 상품을 읽으며, 이 잠금은 동일 상품의 동시 등록 순서를 직렬화한다.
        val product = advertisementProductReader.readForUpdate(command.productId)
        if (!product.category.exposable) {
            throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE)
        }

        val exposureId = advertisementExposureAppender.append(
            AdvertisementExposureAppendDto(
                productId = command.productId,
                contractId = command.contractId,
                content = AdvertisementExposureContent(
                    name = command.name,
                    imageUrl = command.imageUrl,
                    mobileImageUrl = command.mobileImageUrl,
                    linkUrl = command.linkUrl,
                    targetType = command.targetType,
                    targetId = command.targetId,
                ),
                adLabel = command.adLabel,
                period = AdvertisementExposurePeriod(command.startAt, command.endAt),
                exposed = command.exposed,
            ),
        )
        // 콘솔에서 올린 배너 이미지는 첨부하지 않으면 정리 작업이 하루 뒤 S3에서 지운다(LC-3623).
        imageAssetManager.attachImageUrls(adminUserId, listOf(command.imageUrl, command.mobileImageUrl))
        return exposureId
    }

    @Transactional
    fun updateExposure(command: UpdateAdminAdvertisementExposureCommand, adminUserId: Long) {
        val target = advertisementExposureReader.readForUpdateWithActiveProduct(command.exposureId)
        val exposure = target.exposure
        val product = target.product
        if (!product.category.exposable) {
            throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE)
        }
        advertisementExposureManager.update(
            exposure = exposure,
            dto = AdvertisementExposureUpdateDto(
                contractId = command.contractId,
                content = AdvertisementExposureContent(
                    name = command.name,
                    imageUrl = command.imageUrl,
                    mobileImageUrl = command.mobileImageUrl,
                    linkUrl = command.linkUrl,
                    targetType = command.targetType,
                    targetId = command.targetId,
                ),
                adLabel = command.adLabel,
                period = AdvertisementExposurePeriod(command.startAt, command.endAt),
            ),
        )
        imageAssetManager.attachImageUrls(adminUserId, listOf(command.imageUrl, command.mobileImageUrl))
    }

    @Transactional
    fun changeExposed(exposureId: Long, exposed: Boolean) {
        advertisementExposureManager.changeExposed(advertisementExposureReader.readForUpdate(exposureId), exposed)
    }

    @Transactional
    fun moveExposure(exposureId: Long, direction: AdvertisementExposureMoveDirection) {
        // readForMove locks the same product row as A-12 before reading sibling order.
        advertisementExposureManager.move(advertisementExposureReader.readForMove(exposureId), direction)
    }

    @Transactional
    fun deleteExposure(exposureId: Long) {
        advertisementExposureManager.delete(
            exposure = advertisementExposureReader.readForUpdate(exposureId),
            now = LocalDateTime.now(clock),
        )
    }

}
