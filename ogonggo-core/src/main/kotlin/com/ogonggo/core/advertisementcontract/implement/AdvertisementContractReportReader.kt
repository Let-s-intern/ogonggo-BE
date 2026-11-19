package com.ogonggo.core.advertisementcontract.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractProduct
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractReportUnavailableReason
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractActiveDailyDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractActiveReportDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportCsvRowDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDailyDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportExposureDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportJobFieldDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportPostingDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportProductDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportProductSumDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportScrollStepDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportSourceDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportSummaryDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTargetTotalDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTotalDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractReportTotalSumDto
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
import com.ogonggo.core.contentdailystat.implement.dto.ContentMemberActivityDto
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricCellDto
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricDto
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricScope
import com.ogonggo.core.contentdailystat.implement.dto.ContentMetricSheetDto
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.user.domain.LetsCareerWishJobCatalog
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
        val measure = measure(contract, contractProduct, product, from, to, today)
        val byDateExposure = measure.sheet.cells.groupBy { it.date to it.exposureId }.mapValues { (_, cells) -> cells.sum() }
        val byExposure = measure.sheet.cells.groupBy { it.exposureId }.mapValues { (_, cells) -> cells.sum() }
        val hidesScroll = measure.hidesScroll

        return AdvertisementContractReportDto(
            product = AdvertisementContractReportProductDto(
                contractId = checkNotNull(contract.id),
                contractProductId = contractProductId,
                productId = checkNotNull(product.id),
                category = product.category,
                position = product.position,
                name = product.name,
                startDate = measure.startDate,
                endDate = measure.endDate,
            ),
            from = measure.from,
            to = measure.to,
            collectedSince = contentMetricReader.readCollectedSince(today),
            uniqueCountsApproximate = measure.sheet.uniqueCountsApproximate,
            summary = summaryOf(measure.sheet.period, hidesScroll),
            daily = measure.dailies(),
            exposures = measure.exposures.map {
                val metric = byExposure[it.id] ?: ContentMetricDto.ZERO
                AdvertisementContractReportExposureDto(
                    exposureId = checkNotNull(it.id),
                    name = measure.exposureNames.getValue(checkNotNull(it.id)),
                    impressions = metric.impressions,
                    adClicks = metric.adClicks,
                    adClickRate = ContentMetricDto.rate(metric.adClicks, metric.impressions),
                )
            },
            targets = advertisementContractTargetReader.readTargets(contract).map { targetReportOf(it, measure.from, measure.to, today) },
            byJobField = jobFieldsOf(
                contentMetricReader.readMemberActivities(ContentMetricScope.Exposures(measure.exposureNames.keys), measure.from, measure.to, today),
            ),
            csvRows = measure.dates.flatMap { date ->
                measure.exposures.map { exposure ->
                    val exposureId = checkNotNull(exposure.id)
                    val daily = dailyOf(date, byDateExposure[date to exposureId] ?: ContentMetricDto.ZERO, hidesScroll)
                    AdvertisementContractReportCsvRowDto(
                        date = date,
                        exposureId = exposureId,
                        exposureName = measure.exposureNames.getValue(exposureId),
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

    /**
     * 대시보드의 진행 중 광고다. 오늘이 상품 기간 안이고 리포트를 열 수 있는 상품을 모두, 끝나는 날이 가까운 순으로 준다.
     * 숫자는 상품 시작일부터 오늘까지의 상품 몫이다.
     */
    fun readActive(contracts: List<AdvertisementContract>, today: LocalDate): List<AdvertisementContractActiveReportDto> =
        reportableProductsOf(contracts)
            .filter { (_, contractProduct, _) -> !contractProduct.startDate!!.isAfter(today) && !contractProduct.endDate!!.isBefore(today) }
            .map { (contract, contractProduct, product) ->
                val measure = measure(contract, contractProduct, product, null, null, today)
                val metric = measure.sheet.period
                val summary = summaryOf(metric, measure.hidesScroll)
                val firstExposure = measure.exposures.firstOrNull()
                AdvertisementContractActiveReportDto(
                    contractId = checkNotNull(contract.id),
                    contractProductId = checkNotNull(contractProduct.id),
                    category = product.category,
                    position = product.position,
                    name = product.name,
                    startDate = measure.startDate,
                    endDate = measure.endDate,
                    daysLeft = ChronoUnit.DAYS.between(today, measure.endDate),
                    impressions = metric.impressions,
                    adClicks = metric.adClicks,
                    adClickRate = summary.adClickRate,
                    views = metric.views,
                    ctaClicks = metric.ctaClicks,
                    scroll50Rate = summary.scrollFunnel?.single { it.threshold == 50 }?.rate,
                    target = if (product.category == AdvertisementProductCategory.JOB_POSTING) firstExposure?.let(::postingOf) else null,
                    daily = measure.dailies().map { AdvertisementContractActiveDailyDto(it.date, it.impressions, it.adClicks, it.views) },
                )
            }
            .sortedWith(compareBy({ it.endDate }, { it.contractProductId }))

    /**
     * 계약 전체 합계다. 리포트를 열 수 있는 상품만 각 상품 기간 전체(진행 중이면 오늘까지)로 센 상품 몫을 더한다.
     * 아직 시작하지 않은 상품은 0이다.
     */
    fun readTotal(contract: AdvertisementContract, today: LocalDate): AdvertisementContractReportTotalDto {
        val products = reportableProductsOf(listOf(contract)).map { (_, contractProduct, product) ->
            val metric = if (contractProduct.startDate!!.isAfter(today)) {
                ContentMetricDto.ZERO
            } else {
                measure(contract, contractProduct, product, null, null, today).sheet.period
            }
            AdvertisementContractReportProductSumDto(
                contractProductId = checkNotNull(contractProduct.id),
                category = product.category,
                position = product.position,
                name = product.name,
                startDate = contractProduct.startDate!!,
                endDate = contractProduct.endDate!!,
                impressions = metric.impressions,
                uniqueImpressions = metric.uniqueImpressions,
                adClicks = metric.adClicks,
                views = metric.views,
                ctaClicks = metric.ctaClicks,
            )
        }
        val impressions = products.sumOf { it.impressions }
        val adClicks = products.sumOf { it.adClicks }
        return AdvertisementContractReportTotalDto(
            total = AdvertisementContractReportTotalSumDto(
                impressions = impressions,
                uniqueImpressions = products.sumOf { it.uniqueImpressions },
                adClicks = adClicks,
                adClickRate = ContentMetricDto.rate(adClicks, impressions),
                views = products.sumOf { it.views },
                ctaClicks = products.sumOf { it.ctaClicks },
            ),
            products = products,
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

    /** 계약들에 붙은 상품 중 리포트를 열 수 있는 것을 계약·붙인 순서로 준다. */
    private fun reportableProductsOf(contracts: List<AdvertisementContract>): List<Triple<AdvertisementContract, AdvertisementContractProduct, AdvertisementProduct>> {
        val contractProducts = contracts.flatMap { contract ->
            advertisementContractProductRepository.findAllByContractIdAndDeletedAtIsNullOrderByIdAsc(checkNotNull(contract.id)).map { contract to it }
        }
        val products = advertisementProductReader.readAllByIds(contractProducts.map { it.second.productId })
        return contractProducts.mapNotNull { (contract, contractProduct) ->
            val product = products[contractProduct.productId] ?: return@mapNotNull null
            if (AdvertisementContractReportUnavailableReason.of(product.category, contractProduct.startDate, contractProduct.endDate) != null) {
                null
            } else {
                Triple(contract, contractProduct, product)
            }
        }
    }

    private fun measure(
        contract: AdvertisementContract,
        contractProduct: AdvertisementContractProduct,
        product: AdvertisementProduct,
        from: LocalDate?,
        to: LocalDate?,
        today: LocalDate,
    ): ProductMeasure {
        val startDate = checkNotNull(contractProduct.startDate)
        val endDate = checkNotNull(contractProduct.endDate)
        val (clippedFrom, clippedTo) = clip(startDate, endDate, from, to, today)
        val exposures = advertisementContractExposureQueryRepository.findOverlapping(
            checkNotNull(contract.id),
            checkNotNull(product.id),
            startDate,
            endDate,
        )
        val exposureNames = exposures.associate { checkNotNull(it.id) to nameOf(it) }
        val sheet = contentMetricReader.read(ContentMetricScope.Exposures(exposureNames.keys), clippedFrom, clippedTo, today)
        return ProductMeasure(
            startDate = startDate,
            endDate = endDate,
            from = clippedFrom,
            to = clippedTo,
            exposures = exposures,
            exposureNames = exposureNames,
            sheet = sheet,
            hidesScroll = product.category == AdvertisementProductCategory.BANNER,
        )
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

    private fun targetReportOf(target: AdvertisementContractTargetDto, from: LocalDate, to: LocalDate, today: LocalDate): AdvertisementContractReportTargetDto {
        val sheet = contentMetricReader.read(
            ContentMetricScope.Target(ContentEventTargetType.valueOf(target.targetType.name), target.targetId),
            from,
            to,
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
    private fun nameOf(exposure: AdvertisementExposure): String =
        exposure.name ?: postingOf(exposure)?.let { "${it.title} · ${it.companyName}" } ?: "노출 ${exposure.id}"

    /** 광고 공고 노출이 띄우는 공고다. 대상이 없거나 행이 아예 없으면 null이다. 지운 공고도 보인다. */
    private fun postingOf(exposure: AdvertisementExposure): AdvertisementContractReportPostingDto? {
        val targetType = exposure.targetType ?: return null
        val targetId = exposure.targetId ?: return null
        return try {
            when (targetType) {
                AdvertisementExposureTargetType.JOB -> jobReader.readIncludingDeleted(targetId).let {
                    AdvertisementContractReportPostingDto(targetType, targetId, it.title, it.companyName, it.coverImageUrl, it.logoUrl)
                }
                AdvertisementExposureTargetType.BOOTCAMP -> bootcampReader.readIncludingDeleted(targetId).let {
                    AdvertisementContractReportPostingDto(targetType, targetId, it.title, it.companyName, it.representativeImageUrl, it.logoUrl)
                }
            }
        } catch (exception: EntityNotFoundException) {
            null
        }
    }

    private class ProductMeasure(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val from: LocalDate,
        val to: LocalDate,
        val exposures: List<AdvertisementExposure>,
        val exposureNames: Map<Long, String>,
        val sheet: ContentMetricSheetDto,
        val hidesScroll: Boolean,
    ) {
        val dates: List<LocalDate> = from.datesUntil(to.plusDays(1)).toList()

        fun dailies(): List<AdvertisementContractReportDailyDto> {
            val byDate = sheet.cells.groupBy { it.date }.mapValues { (_, cells) -> cells.sum() }
            return dates.map { dailyOf(it, byDate[it] ?: ContentMetricDto.ZERO, hidesScroll) }
        }
    }

    companion object {
        private const val NO_JOB_FIELD_LABEL = "직무 미설정"

        internal fun summaryOf(metric: ContentMetricDto, hidesScroll: Boolean) = AdvertisementContractReportSummaryDto(
            impressions = metric.impressions,
            uniqueImpressions = metric.uniqueImpressions,
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

        /**
         * 회원을 희망 직군으로 나눈다. 렛츠커리어 희망 직무를 오공고 직무로 옮긴 뒤 그 직무의 직군으로 묶는다.
         * 여러 직군에 걸친 회원은 각 직군에 한 번씩 센다. 옮길 직무가 없으면 null 칸이다. 순 회원 수가 큰 순이다.
         */
        internal fun jobFieldsOf(activities: List<ContentMemberActivityDto>): List<AdvertisementContractReportJobFieldDto> =
            activities
                .flatMap { activity ->
                    val fields: Set<JobField?> = LetsCareerWishJobCatalog.jobRolesOf(activity.wishJob).map { it.jobField }.toSet()
                    fields.ifEmpty { setOf(null) }.map { it to activity }
                }
                .groupBy({ it.first }, { it.second })
                .map { (jobField, members) ->
                    AdvertisementContractReportJobFieldDto(
                        jobField = jobField,
                        label = jobField?.desc ?: NO_JOB_FIELD_LABEL,
                        memberUniqueVisitors = members.count { it.viewed }.toLong(),
                        memberViews = members.sumOf { it.views },
                        ctaUniqueMembers = members.count { it.ctaClicked }.toLong(),
                        adClickMembers = members.count { it.adClicked }.toLong(),
                    )
                }
                .sortedWith(compareByDescending<AdvertisementContractReportJobFieldDto> { it.memberUniqueVisitors }.thenBy { it.jobField == null }.thenBy { it.jobField })

        private fun dailyOf(date: LocalDate, metric: ContentMetricDto, hidesScroll: Boolean) = AdvertisementContractReportDailyDto(
            date = date,
            impressions = metric.impressions,
            uniqueImpressions = metric.uniqueImpressions,
            adClicks = metric.adClicks,
            views = metric.views,
            uniqueVisitors = metric.uniqueVisitors,
            memberViews = metric.memberViews,
            ctaClicks = metric.ctaClicks,
            ctaUniqueVisitors = metric.ctaUniqueVisitors,
            avgScrollPercent = if (hidesScroll) null else metric.avgScrollPercent ?: 0.0,
            avgDurationSeconds = if (hidesScroll) null else metric.avgDurationSeconds ?: 0.0,
        )

        private fun List<ContentMetricCellDto>.sum(): ContentMetricDto = fold(ContentMetricDto.ZERO) { total, cell -> total + cell.metric }
    }
}
