package com.ogonggo.core.advertisementcontract.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractProduct
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractReportUnavailableReason
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportCsvRowDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDailyDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportExposureDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportProductDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportScrollStepDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportSourceDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportSummaryDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetTotalDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractTargetDto
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractExposureQueryRepository
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractProductJpaRepository
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.contentdailystat.implement.ContentMetricReader
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricDto
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricScope
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.job.implement.JobReader
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class AdvertisementContractReportReader internal constructor(
    private val advertisementContractProductRepository: AdvertisementContractProductJpaRepository,
    private val advertisementContractExposureQueryRepository: AdvertisementContractExposureQueryRepository,
    private val advertisementProductReader: AdvertisementProductReader,
    private val advertisementContractTargetReader: AdvertisementContractTargetReader,
    private val contentMetricReader: ContentMetricReader,
    private val jobReader: JobReader,
    private val bootcampReader: BootcampReader,
) {

    /**
     * 계약 상품 하나의 리포트다. 계약을 볼 권한은 부르는 쪽이 확인한다.
     *
     * - 계약에서 뺐거나 다른 계약의 상품이면 404, 기간이 없거나 채널 상품이면 409 `REPORT_NOT_AVAILABLE`이다.
     * - [from]·[to]는 상품 기간 안으로 자른다. 비우면 상품 기간 전체이고 진행 중이면 [today]까지다. 자른 기간이 비면 400이다.
     * - 상품의 노출은 계약·상품이 같고 기간이 상품 기간과 겹치는 것이다. 같은 노출을 기간이 다른 두 계약 상품이 나눠 쓰면 날짜로 갈린다.
     */
    fun read(contract: AdvertisementContract, contractProductId: Long, from: LocalDate?, to: LocalDate?, today: LocalDate): AdvertisementContractReportDto {
        val (contractProduct, product) = readReportable(contract, contractProductId)
        val startDate = checkNotNull(contractProduct.startDate)
        val endDate = checkNotNull(contractProduct.endDate)
        val period = clip(startDate, endDate, from, to, today)
        val exposures = advertisementContractExposureQueryRepository.findOverlapping(
            checkNotNull(contract.id),
            checkNotNull(product.id),
            startDate,
            endDate,
        )
        val exposureNames = exposures.associate { checkNotNull(it.id) to nameOf(it) }
        val sheet = contentMetricReader.read(ContentMetricScope.Exposures(exposureNames.keys), period.first, period.second, today)
        val dates = period.first.datesUntil(period.second.plusDays(1)).toList()
        val byDate = sheet.cells.groupBy { it.date }.mapValues { (_, cells) -> cells.sum() }
        val byDateExposure = sheet.cells.groupBy { it.date to it.exposureId }.mapValues { (_, cells) -> cells.sum() }
        val byExposure = sheet.cells.groupBy { it.exposureId }.mapValues { (_, cells) -> cells.sum() }
        val hidesScroll = product.category == AdvertisementProductCategory.BANNER

        return AdvertisementContractReportDto(
            product = AdvertisementContractReportProductDto(
                contractId = checkNotNull(contract.id),
                contractProductId = contractProductId,
                productId = checkNotNull(product.id),
                category = product.category,
                position = product.position,
                name = product.name,
                startDate = startDate,
                endDate = endDate,
            ),
            from = period.first,
            to = period.second,
            collectedSince = contentMetricReader.readCollectedSince(today),
            uniqueCountsApproximate = sheet.uniqueCountsApproximate,
            summary = summaryOf(sheet.period, hidesScroll),
            daily = dates.map { dailyOf(it, byDate[it] ?: ContentMetricDto.ZERO, hidesScroll) },
            exposures = exposures.map {
                val metric = byExposure[it.id] ?: ContentMetricDto.ZERO
                AdvertisementContractReportExposureDto(
                    exposureId = checkNotNull(it.id),
                    name = exposureNames.getValue(checkNotNull(it.id)),
                    imageUrl = it.imageUrl,
                    impressions = metric.impressions,
                    adClicks = metric.adClicks,
                    adClickRate = ContentMetricDto.rate(metric.adClicks, metric.impressions),
                )
            },
            targets = advertisementContractTargetReader.readTargets(contract).map { targetReportOf(it, period, today) },
            csvRows = dates.flatMap { date ->
                exposures.map { exposure ->
                    val exposureId = checkNotNull(exposure.id)
                    val daily = dailyOf(date, byDateExposure[date to exposureId] ?: ContentMetricDto.ZERO, hidesScroll)
                    AdvertisementContractReportCsvRowDto(
                        date = date,
                        exposureId = exposureId,
                        exposureName = exposureNames.getValue(exposureId),
                        impressions = daily.impressions,
                        adClicks = daily.adClicks,
                        views = daily.views,
                        uniqueVisitors = daily.uniqueVisitors,
                        memberViews = daily.memberViews,
                        ctaClicks = daily.ctaClicks,
                        ctaUniqueVisitors = daily.ctaUniqueVisitors,
                        avgScrollPercent = daily.avgScrollPercent,
                        avgDurationSeconds = daily.avgDurationSeconds,
                    )
                }
            },
        )
    }

    private fun readReportable(contract: AdvertisementContract, contractProductId: Long): Pair<AdvertisementContractProduct, AdvertisementProduct> {
        val contractProduct = advertisementContractProductRepository.findByIdAndContractIdAndDeletedAtIsNull(
            contractProductId,
            checkNotNull(contract.id),
        ) ?: throw EntityNotFoundException(AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND)
        val product = checkNotNull(advertisementProductReader.readAllByIds(listOf(contractProduct.productId))[contractProduct.productId]) {
            "계약에 붙인 광고 상품이 없습니다."
        }
        if (AdvertisementContractReportUnavailableReason.of(product.category, contractProduct.startDate, contractProduct.endDate) != null) {
            throw ConflictException(AdvertisementContractErrorCode.REPORT_NOT_AVAILABLE)
        }
        return contractProduct to product
    }

    private fun clip(startDate: LocalDate, endDate: LocalDate, from: LocalDate?, to: LocalDate?, today: LocalDate): Pair<LocalDate, LocalDate> {
        if (from != null && to != null && from.isAfter(to)) {
            throw InvalidValueException(AdvertisementContractErrorCode.REPORT_PERIOD_INVALID)
        }
        val last = minOf(endDate, today)
        val clippedFrom = maxOf(from ?: startDate, startDate)
        val clippedTo = minOf(to ?: last, last)
        if (clippedFrom.isAfter(clippedTo)) {
            throw InvalidValueException(AdvertisementContractErrorCode.REPORT_PERIOD_INVALID)
        }
        return clippedFrom to clippedTo
    }

    private fun targetReportOf(
        target: AdvertisementContractTargetDto,
        period: Pair<LocalDate, LocalDate>,
        today: LocalDate,
    ): AdvertisementContractReportTargetDto {
        val sheet = contentMetricReader.read(
            ContentMetricScope.Target(ContentEventTargetType.valueOf(target.targetType.name), target.targetId),
            period.first,
            period.second,
            today,
        )
        val bySource = sheet.cells.groupBy { it.source }.mapValues { (_, cells) -> cells.sum() }
        val total = sheet.period
        return AdvertisementContractReportTargetDto(
            targetType = target.targetType,
            targetId = target.targetId,
            title = target.title,
            total = AdvertisementContractReportTargetTotalDto(
                views = total.views,
                uniqueVisitors = total.uniqueVisitors,
                memberViews = total.memberViews,
                ctaClicks = total.ctaClicks,
                ctaUniqueVisitors = total.ctaUniqueVisitors,
                avgScrollPercent = total.avgScrollPercent,
                avgDurationSeconds = total.avgDurationSeconds,
            ),
            bySource = ContentEventSource.entries.map {
                val metric = bySource[it] ?: ContentMetricDto.ZERO
                AdvertisementContractReportSourceDto(source = it, views = metric.views, ctaClicks = metric.ctaClicks)
            },
        )
    }

    /** 배너는 소재 이름, 광고 공고는 `<대상 제목> · <회사명>`이다. */
    private fun nameOf(exposure: AdvertisementExposure): String {
        exposure.name?.let { return it }
        val targetId = exposure.targetId ?: return "노출 ${exposure.id}"
        return try {
            when (exposure.targetType) {
                AdvertisementExposureTargetType.JOB -> jobReader.readIncludingDeleted(targetId).let { "${it.title} · ${it.companyName}" }
                AdvertisementExposureTargetType.BOOTCAMP -> bootcampReader.readIncludingDeleted(targetId).let { "${it.title} · ${it.companyName}" }
                null -> "노출 ${exposure.id}"
            }
        } catch (exception: EntityNotFoundException) {
            "노출 ${exposure.id}"
        }
    }

    companion object {
        internal fun summaryOf(metric: ContentMetricDto, hidesScroll: Boolean) = AdvertisementContractReportSummaryDto(
            impressions = metric.impressions,
            adClicks = metric.adClicks,
            adClickRate = ContentMetricDto.rate(metric.adClicks, metric.impressions),
            views = metric.views,
            uniqueVisitors = metric.uniqueVisitors,
            memberViews = metric.memberViews,
            memberUniqueVisitors = metric.memberUniqueVisitors,
            ctaClicks = metric.ctaClicks,
            ctaUniqueVisitors = metric.ctaUniqueVisitors,
            ctaUniqueMembers = metric.ctaUniqueMembers,
            ctaClickRate = ContentMetricDto.rate(metric.ctaUniqueVisitors, metric.uniqueVisitors),
            avgScrollPercent = if (hidesScroll) null else metric.avgScrollPercent,
            scrollFunnel = if (hidesScroll) {
                null
            } else {
                ContentMetricDto.SCROLL_THRESHOLDS.map {
                    val visitors = metric.scrollReachAt(it)
                    AdvertisementContractReportScrollStepDto(it, visitors, ContentMetricDto.rate(visitors, metric.uniqueVisitors))
                }
            },
            avgDurationSeconds = if (hidesScroll) null else metric.avgDurationSeconds,
        )

        private fun dailyOf(date: LocalDate, metric: ContentMetricDto, hidesScroll: Boolean) = AdvertisementContractReportDailyDto(
            date = date,
            impressions = metric.impressions,
            adClicks = metric.adClicks,
            views = metric.views,
            uniqueVisitors = metric.uniqueVisitors,
            memberViews = metric.memberViews,
            ctaClicks = metric.ctaClicks,
            ctaUniqueVisitors = metric.ctaUniqueVisitors,
            avgScrollPercent = if (hidesScroll) null else metric.avgScrollPercent ?: 0.0,
            avgDurationSeconds = if (hidesScroll) null else metric.avgDurationSeconds ?: 0.0,
        )

        private fun List<com.ogonggo.core.contentdailystat.implement.dto.ContentMetricCellDto>.sum(): ContentMetricDto =
            fold(ContentMetricDto.ZERO) { total, cell -> total + cell.metric }
    }
}
