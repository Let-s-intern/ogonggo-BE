package com.ogonggo.adminapi.advertisementexposure.business

import com.ogonggo.adminapi.error.InvalidRequestFieldException
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.net.URISyntaxException
import java.time.Clock
import java.time.LocalDateTime
import java.util.Locale

@Service
class AdminAdvertisementExposureService(
    private val advertisementExposureReader: AdvertisementExposureReader,
    private val advertisementExposureAppender: AdvertisementExposureAppender,
    private val advertisementExposureManager: AdvertisementExposureManager,
    private val advertisementProductReader: AdvertisementProductReader,
    private val advertisementContractReader: AdvertisementContractReader,
    private val clock: Clock,
) {

    fun getExposures(productId: Long, contractId: Long?): List<AdminAdvertisementExposureResult> {
        val exposures = advertisementExposureReader.readByProduct(productId, contractId)
        val contracts = advertisementContractReader.readAllByIds(exposures.mapNotNull { it.contractId }.toSet())
        return exposures.map { exposure ->
            AdminAdvertisementExposureResult.from(
                exposure = exposure,
                advertiserCompanyName = exposure.contractId?.let { contracts[it]?.companyName },
            )
        }
    }

    @Transactional
    fun createExposure(command: CreateAdminAdvertisementExposureCommand): CreateAdminAdvertisementExposureResult {
        // A-12의 BANNER별 URL 검증을 위해 상품을 읽으며, 이 잠금은 동일 상품의 동시 등록 순서를 직렬화한다.
        val product = advertisementProductReader.readForUpdate(command.productId)
        if (!product.category.exposable) {
            throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE)
        }

        if (product.category == AdvertisementProductCategory.BANNER) {
            validateImageUrl(command.imageUrl)
            validateLinkUrl(command.linkUrl)
        }

        val exposureId = advertisementExposureAppender.append(
            AdvertisementExposureAppendDto(
                productId = command.productId,
                contractId = command.contractId,
                content = AdvertisementExposureContent(
                    name = command.name,
                    imageUrl = command.imageUrl,
                    linkUrl = command.linkUrl,
                    targetType = command.targetType,
                    targetId = command.targetId,
                ),
                adLabel = command.adLabel,
                period = AdvertisementExposurePeriod(command.startAt, command.endAt),
                exposed = command.exposed,
            ),
        )
        return CreateAdminAdvertisementExposureResult(exposureId)
    }

    @Transactional
    fun updateExposure(command: UpdateAdminAdvertisementExposureCommand) {
        val target = advertisementExposureReader.readForUpdateWithActiveProduct(command.exposureId)
        val exposure = target.exposure
        val product = target.product
        if (!product.category.exposable) {
            throw InvalidValueException(AdvertisementExposureErrorCode.ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE)
        }
        if (product.category == AdvertisementProductCategory.BANNER) {
            validateImageUrl(command.imageUrl)
            validateLinkUrl(command.linkUrl)
        }

        advertisementExposureManager.update(
            exposure = exposure,
            dto = AdvertisementExposureUpdateDto(
                contractId = command.contractId,
                content = AdvertisementExposureContent(
                    name = command.name,
                    imageUrl = command.imageUrl,
                    linkUrl = command.linkUrl,
                    targetType = command.targetType,
                    targetId = command.targetId,
                ),
                adLabel = command.adLabel,
                period = AdvertisementExposurePeriod(command.startAt, command.endAt),
            ),
        )
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

    private fun validateImageUrl(value: String?) {
        if (value.isNullOrBlank()) return // core domain reports the required banner content error.
        val uri = parseUri("imageUrl", value)
        if (!uri.isHttpUrl()) {
            throw InvalidRequestFieldException("imageUrl", "HTTP 또는 HTTPS 절대 URL이어야 합니다.")
        }
    }

    private fun validateLinkUrl(value: String?) {
        if (value.isNullOrBlank()) return // core domain reports the required banner content error.
        val uri = parseUri("linkUrl", value)
        val isInternalRoute = value.startsWith('/') &&
            !value.startsWith("//") &&
            uri.rawAuthority == null &&
            !value.contains('\\')
        if (!uri.isHttpUrl() && !isInternalRoute) {
            throw InvalidRequestFieldException("linkUrl", "HTTP(S) URL 또는 단일 슬래시로 시작하는 내부 경로여야 합니다.")
        }
    }

    private fun parseUri(fieldName: String, value: String): URI = try {
        URI(value)
    } catch (_: URISyntaxException) {
        throw InvalidRequestFieldException(fieldName, "URL 형식이 올바르지 않습니다.")
    }

    private fun URI.isHttpUrl(): Boolean =
        isAbsolute && scheme?.lowercase(Locale.ROOT) in setOf("http", "https") && !host.isNullOrBlank()
}
