package com.ogonggo.core.advertisementcontract.implement

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContract
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractCreativeMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractInvoiceMethod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractProduct
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractTarget
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementReportInsightDraftDto
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementReportInsightDto
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractProductPeriod
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPurpose
import com.ogonggo.core.advertisementcontract.error.AdvertisementContractErrorCode
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractExposureQueryRepository
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractJpaRepository
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractProductJpaRepository
import com.ogonggo.core.advertisementcontract.persistence.AdvertisementContractTargetJpaRepository
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposurePeriod
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementexposure.persistence.AdvertisementExposureJpaRepository
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.advertisementproduct.persistence.AdvertisementProductJpaRepository
import com.ogonggo.core.advertisementproduct.persistence.AdvertisementProductQueryRepository
import com.ogonggo.core.bootcamp.implement.BootcampBookmarkReader
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.contentdailystat.implement.ContentDailyStatManager
import com.ogonggo.core.contentdailystat.implement.ContentMetricReader
import com.ogonggo.core.contentdailystat.persistence.ContentDailyStatQueryRepository
import com.ogonggo.core.contentevent.domain.ContentEvent
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.persistence.ContentEventJpaRepository
import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.implement.JobBookmarkReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.user.domain.UserGrade
import com.ogonggo.core.user.domain.UserProfile
import com.ogonggo.core.user.persistence.UserProfileJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    AdvertisementContractReportReader::class,
    AdvertisementContractExposureQueryRepository::class,
    AdvertisementContractTargetReader::class,
    AdvertisementReportInsightReader::class,
    AdvertisementReportInsightManager::class,
    AdvertisementProductReader::class,
    AdvertisementProductQueryRepository::class,
    ContentMetricReader::class,
    ContentDailyStatManager::class,
    ContentDailyStatQueryRepository::class,
)
internal class AdvertisementContractReportPersistenceTest @Autowired constructor(
    private val reportReader: AdvertisementContractReportReader,
    private val dailyStatManager: ContentDailyStatManager,
    private val contractRepository: AdvertisementContractJpaRepository,
    private val contractProductRepository: AdvertisementContractProductJpaRepository,
    private val productRepository: AdvertisementProductJpaRepository,
    private val exposureRepository: AdvertisementExposureJpaRepository,
    private val eventRepository: ContentEventJpaRepository,
    private val userProfileRepository: UserProfileJpaRepository,
    private val contractTargetRepository: AdvertisementContractTargetJpaRepository,
    private val insightManager: AdvertisementReportInsightManager,
) {

    @MockBean
    private lateinit var jobReader: JobReader

    @MockBean
    private lateinit var bootcampReader: BootcampReader

    @MockBean
    private lateinit var jobBookmarkReader: JobBookmarkReader

    @MockBean
    private lateinit var bootcampBookmarkReader: BootcampBookmarkReader

    private lateinit var contract: AdvertisementContract
    private var bannerExposureId = 0L
    private var cardExposureId = 0L
    private var bannerProductId = 0L
    private var cardProductId = 0L

    @BeforeEach
    fun setUp() {
        val job = Mockito.mock(Job::class.java)
        Mockito.`when`(job.title).thenReturn("백엔드 개발자")
        Mockito.`when`(job.companyName).thenReturn("뱅크")
        Mockito.`when`(job.jobField).thenReturn(JobField.IT_DEVELOPMENT)
        Mockito.`when`(jobReader.readIncludingDeleted(JOB_ID)).thenReturn(job)

        contract = contractRepository.save(form())
        val banner = productRepository.save(AdvertisementProduct(AdvertisementProductCategory.BANNER, AdvertisementProductPosition.JOB_HOME_TOP_BANNER, "홈 상단 배너", 500_000))
        val card = productRepository.save(AdvertisementProduct(AdvertisementProductCategory.JOB_POSTING, null, "목록 광고 공고", null))
        bannerProductId = addProduct(banner, START, END)
        cardProductId = addProduct(card, START, END)
        bannerExposureId = checkNotNull(
            exposureRepository.save(
                AdvertisementExposure(
                    product = banner,
                    contractId = contract.id,
                    content = AdvertisementExposureContent(name = "11월 홈 상단", imageUrl = "https://cdn.example.com/a.png", linkUrl = "https://example.com"),
                    adLabel = true,
                    period = AdvertisementExposurePeriod.ALWAYS,
                    exposed = true,
                    displayOrder = 1,
                ),
            ).id,
        )
        cardExposureId = checkNotNull(
            exposureRepository.save(
                AdvertisementExposure(
                    product = card,
                    contractId = contract.id,
                    content = AdvertisementExposureContent(targetType = AdvertisementExposureTargetType.JOB, targetId = JOB_ID),
                    adLabel = true,
                    period = AdvertisementExposurePeriod.ALWAYS,
                    exposed = true,
                    displayOrder = 1,
                ),
            ).id,
        )
    }

    @Test
    fun `기간이 겹치는 배너와 광고 공고 상품은 같은 공고 조회를 들어온 노출의 상품에만 센다`() {
        // given
        event(ContentEventType.VIEW, START.atTime(10, 0), "a", bannerExposureId, ContentEventSource.BANNER)
        event(ContentEventType.VIEW, START.atTime(11, 0), "b", cardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.VIEW, START.atTime(12, 0), "c", null, ContentEventSource.LIST)
        event(ContentEventType.IMPRESSION, START.atTime(9, 0), "a", bannerExposureId, ContentEventSource.BANNER, target = false)

        // when
        val bannerReport = reportReader.read(contract, bannerProductId, null, null, TODAY)
        val cardReport = reportReader.read(contract, cardProductId, null, null, TODAY)

        // then
        assertEquals(500_000, bannerReport.product.price)
        assertEquals(null, cardReport.product.price)
        assertEquals(1L, bannerReport.summary.views)
        assertEquals(1L, bannerReport.summary.impressions)
        assertEquals(1L, cardReport.summary.views)
        assertEquals(0L, cardReport.summary.impressions)
        assertEquals(listOf("11월 홈 상단"), bannerReport.exposures.map { it.name })
        assertEquals(listOf("백엔드 개발자 · 뱅크"), cardReport.exposures.map { it.name })
    }

    @Test
    fun `배너 상품은 스크롤 깊이와 체류 시간을 null로 주고 광고 공고 상품은 방문자별 최대 깊이로 깔때기를 센다`() {
        // given
        event(ContentEventType.VIEW, START.atTime(10, 0), "a", cardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.SCROLL, START.atTime(10, 1), "a", cardExposureId, ContentEventSource.AD_CARD, scroll = 30)
        event(ContentEventType.SCROLL, START.atTime(10, 2), "a", cardExposureId, ContentEventSource.AD_CARD, scroll = 80)
        event(ContentEventType.VIEW, START.plusDays(1).atTime(10, 0), "a", cardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.VIEW, START.atTime(11, 0), "b", cardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.SCROLL, START.atTime(11, 1), "b", cardExposureId, ContentEventSource.AD_CARD, scroll = 50)
        event(ContentEventType.VIEW, START.atTime(12, 0), "c", bannerExposureId, ContentEventSource.BANNER)
        event(ContentEventType.SCROLL, START.atTime(12, 1), "c", bannerExposureId, ContentEventSource.BANNER, scroll = 100)
        dailyStatManager.rebuild(START, TODAY)
        dailyStatManager.rebuild(START.plusDays(1), TODAY)

        // when
        val card = reportReader.read(contract, cardProductId, null, null, TODAY).summary
        val banner = reportReader.read(contract, bannerProductId, null, null, TODAY).summary

        // then
        assertEquals(2L, card.uniqueVisitors)
        assertEquals(listOf(2L, 2L, 2L, 1L, 0L), card.scrollFunnel!!.map { it.visitors })
        assertEquals(listOf(1.0, 1.0, 1.0, 0.5, 0.0), card.scrollFunnel!!.map { it.rate })
        assertNull(banner.scrollFunnel)
        assertNull(banner.avgScrollPercent)
        assertNull(banner.avgDurationSeconds)
    }

    @Test
    fun `스크롤 기록이 없는 날의 평균 스크롤과 체류 시간은 0이 아니라 null이고 CSV 값도 null이다`() {
        // given
        event(ContentEventType.VIEW, START.atTime(10, 0), "a", cardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.VIEW, START.plusDays(1).atTime(10, 0), "a", cardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.SCROLL, START.plusDays(1).atTime(10, 1), "a", cardExposureId, ContentEventSource.AD_CARD, scroll = 80)
        dailyStatManager.rebuild(START, TODAY)
        dailyStatManager.rebuild(START.plusDays(1), TODAY)

        // when
        val report = reportReader.read(contract, cardProductId, START, START.plusDays(1), TODAY)

        // then
        val noScroll = report.daily.single { it.date == START }
        assertNull(noScroll.avgScrollPercent)
        assertNull(noScroll.avgDurationSeconds)
        assertEquals(80.0, report.daily.single { it.date == START.plusDays(1) }.avgScrollPercent)
        val csvNoScroll = report.csvRows.single { it.date == START }
        assertNull(csvNoScroll.avgScrollPercent)
        assertNull(csvNoScroll.avgDurationSeconds)
    }

    @Test
    fun `전날까지는 일별 집계를, 오늘은 원본을 읽어 기간의 모든 날을 준다`() {
        // given
        event(ContentEventType.VIEW, TODAY.minusDays(1).atTime(10, 0), "a", bannerExposureId, ContentEventSource.BANNER)
        dailyStatManager.rebuild(TODAY.minusDays(1), TODAY)
        event(ContentEventType.VIEW, TODAY.atTime(9, 0), "b", bannerExposureId, ContentEventSource.BANNER)
        event(ContentEventType.VIEW, TODAY.atTime(9, 10), "b", bannerExposureId, ContentEventSource.BANNER)

        // when
        val report = reportReader.read(contract, bannerProductId, null, null, TODAY)

        // then
        assertEquals(START, report.from)
        assertEquals(TODAY, report.to)
        assertEquals(START.datesUntil(TODAY.plusDays(1)).toList(), report.daily.map { it.date })
        assertEquals(1L, report.daily.single { it.date == TODAY.minusDays(1) }.views)
        assertEquals(1L, report.daily.single { it.date == TODAY }.views)
        assertEquals(2L, report.summary.views)
        assertEquals(2L, report.summary.uniqueVisitors)
        assertEquals(report.daily.size, report.csvRows.size)
    }

    @Test
    fun `같은 사람이 한 날 두 노출로 들어와도 그날 순 방문자와 CSV의 노출별 순 방문자는 날짜·노출 단위로 센다`() {
        // given
        val card = productRepository.findAll().single { it.category == AdvertisementProductCategory.JOB_POSTING }
        val secondCardExposureId = checkNotNull(
            exposureRepository.save(
                AdvertisementExposure(
                    product = card,
                    contractId = contract.id,
                    content = AdvertisementExposureContent(targetType = AdvertisementExposureTargetType.JOB, targetId = JOB_ID),
                    adLabel = true,
                    period = AdvertisementExposurePeriod.ALWAYS,
                    exposed = true,
                    displayOrder = 2,
                ),
            ).id,
        )
        event(ContentEventType.VIEW, START.atTime(10, 0), "a", cardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.VIEW, START.atTime(12, 0), "a", secondCardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.VIEW, START.atTime(13, 0), "a", cardExposureId, ContentEventSource.LIST)
        event(ContentEventType.VIEW, START.atTime(14, 0), "b", secondCardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.CTA_CLICK, START.atTime(14, 1), "a", cardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.CTA_CLICK, START.atTime(14, 2), "a", secondCardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.VIEW, START.plusDays(1).atTime(10, 0), "a", cardExposureId, ContentEventSource.AD_CARD)
        dailyStatManager.rebuild(START, TODAY)
        dailyStatManager.rebuild(START.plusDays(1), TODAY)

        // when
        val report = reportReader.read(contract, cardProductId, null, null, TODAY)

        // then
        val first = report.daily.single { it.date == START }
        assertEquals(4L, first.views)
        assertEquals(2L, first.uniqueVisitors)
        assertEquals(1L, first.ctaUniqueVisitors)
        assertEquals(1L, report.daily.single { it.date == START.plusDays(1) }.uniqueVisitors)
        assertEquals(2L, report.summary.uniqueVisitors)
        val csv = report.csvRows.filter { it.date == START }.associateBy { it.exposureId }
        assertEquals(1L, csv.getValue(cardExposureId).uniqueVisitors)
        assertEquals(2L, csv.getValue(secondCardExposureId).uniqueVisitors)
        assertEquals(false, report.uniqueCountsApproximate)
        assertEquals(false, report.rawCountsApproximate)
    }

    @Test
    fun `원본 보관 기간을 벗어난 기간은 날짜별 순 방문자를 칸 합으로 두고 근사 표시를 단다`() {
        // given
        event(ContentEventType.VIEW, START.atTime(10, 0), "a", cardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.VIEW, START.atTime(12, 0), "a", cardExposureId, ContentEventSource.LIST)
        dailyStatManager.rebuild(START, TODAY)
        val farFuture = START.plusDays(366)

        // when
        val report = reportReader.read(contract, cardProductId, START, START, farFuture)

        // then
        assertEquals(true, report.uniqueCountsApproximate)
        assertEquals(true, report.rawCountsApproximate)
        assertEquals(2L, report.daily.single().uniqueVisitors)
    }

    @Test
    fun `조회 기간은 상품 기간 안으로 자르고 비면 400, 기간 없는 상품과 채널 상품은 409다`() {
        // given
        val kakao = productRepository.save(AdvertisementProduct(AdvertisementProductCategory.KAKAO_1, null, "카카오 톡방", null))
        val channelProductId = addProduct(kakao, START, END)

        // when
        val clipped = reportReader.read(contract, bannerProductId, START.minusDays(10), END.plusDays(10), END.plusDays(30))
        val narrowed = reportReader.read(contract, bannerProductId, START.plusDays(2), START.plusDays(3), TODAY)
        val outside = assertThrows<BusinessException> { reportReader.read(contract, bannerProductId, END.plusDays(1), END.plusDays(2), END.plusDays(30)) }
        val reversed = assertThrows<BusinessException> { reportReader.read(contract, bannerProductId, START.plusDays(3), START.plusDays(2), TODAY) }
        val channel = assertThrows<BusinessException> { reportReader.read(contract, channelProductId, null, null, TODAY) }

        // then
        assertEquals(START to END, clipped.from to clipped.to)
        assertEquals(START.plusDays(2) to START.plusDays(3), narrowed.from to narrowed.to)
        assertEquals(AdvertisementContractErrorCode.REPORT_PERIOD_INVALID, outside.errorCode)
        assertEquals(AdvertisementContractErrorCode.REPORT_PERIOD_INVALID, reversed.errorCode)
        assertEquals(AdvertisementContractErrorCode.REPORT_NOT_AVAILABLE, channel.errorCode)
    }

    @Test
    fun `희망 직무별 나눔은 로그인 회원만 직군마다 세고 두 직군 회원은 양쪽에, 직무 없는 회원은 null 칸에 센다`() {
        // given
        profile(1L, "백엔드/서버 개발, 마케터")
        profile(2L, null)
        event(ContentEventType.VIEW, START.atTime(10, 0), "a", cardExposureId, ContentEventSource.AD_CARD, userId = 1L)
        event(ContentEventType.VIEW, START.atTime(10, 10), "a", cardExposureId, ContentEventSource.AD_CARD, userId = 1L)
        event(ContentEventType.CTA_CLICK, START.atTime(10, 11), "a", cardExposureId, ContentEventSource.AD_CARD, userId = 1L)
        event(ContentEventType.AD_CLICK, START.atTime(9, 59), "a", cardExposureId, ContentEventSource.AD_CARD, userId = 1L)
        event(ContentEventType.VIEW, START.atTime(11, 0), "b", cardExposureId, ContentEventSource.AD_CARD, userId = 2L)
        event(ContentEventType.VIEW, START.atTime(12, 0), "guest", cardExposureId, ContentEventSource.AD_CARD)

        // when
        val byJobField = reportReader.read(contract, cardProductId, null, null, TODAY).byJobField

        // then
        assertEquals(
            setOf(JobField.IT_DEVELOPMENT, JobField.MARKETING_ADVERTISING, null),
            byJobField.map { it.jobField }.toSet(),
        )
        byJobField.filter { it.jobField != null }.forEach {
            assertEquals(listOf(1L, 1L, 1L, 1L), listOf(it.memberUniqueVisitors, it.memberViews, it.ctaUniqueMembers, it.adClickMembers))
        }
        val none = byJobField.single { it.jobField == null }
        assertEquals("직무 미설정", none.label)
        assertEquals(listOf(1L, 1L, 0L, 0L), listOf(none.memberUniqueVisitors, none.memberViews, none.ctaUniqueMembers, none.adClickMembers))
    }

    @Test
    fun `도달은 기간 동안 광고를 본 서로 다른 사람이고 평균 빈도는 노출수를 도달로 나눈 값이다`() {
        // given
        event(ContentEventType.IMPRESSION, START.atTime(10, 0), "a", cardExposureId, ContentEventSource.AD_CARD, target = false)
        event(ContentEventType.IMPRESSION, START.plusDays(1).atTime(10, 0), "a", cardExposureId, ContentEventSource.AD_CARD, target = false)
        event(ContentEventType.IMPRESSION, START.atTime(11, 0), "b", cardExposureId, ContentEventSource.AD_CARD, target = false)
        event(ContentEventType.IMPRESSION, START.atTime(11, 0), "ignored", cardExposureId, ContentEventSource.AD_CARD, target = false, userId = 5L)
        event(ContentEventType.IMPRESSION, START.plusDays(1).atTime(11, 0), "other-device", cardExposureId, ContentEventSource.AD_CARD, target = false, userId = 5L)
        event(ContentEventType.AD_CLICK, START.atTime(10, 1), "a", cardExposureId, ContentEventSource.AD_CARD, target = false)
        event(ContentEventType.AD_CLICK, START.atTime(10, 2), "a", cardExposureId, ContentEventSource.AD_CARD, target = false)

        // when
        val audience = reportReader.read(contract, cardProductId, null, null, TODAY).audience

        // then
        assertEquals(3L, audience.reach)
        assertEquals(1.7, audience.avgFrequency)
        assertEquals(1L, audience.adClickVisitors)
    }

    @Test
    fun `날짜별 도달은 같은 사람이 하루에 몇 번을 봐도 1이고 두 날에 걸치면 날마다 1이다`() {
        // given
        event(ContentEventType.IMPRESSION, START.atTime(9, 0), "a", cardExposureId, ContentEventSource.AD_CARD, target = false)
        event(ContentEventType.IMPRESSION, START.atTime(12, 0), "a", cardExposureId, ContentEventSource.AD_CARD, target = false)
        event(ContentEventType.IMPRESSION, START.atTime(15, 0), "a", cardExposureId, ContentEventSource.AD_CARD, target = false)
        event(ContentEventType.IMPRESSION, START.plusDays(1).atTime(9, 0), "a", cardExposureId, ContentEventSource.AD_CARD, target = false)
        event(ContentEventType.AD_CLICK, START.plusDays(2).atTime(9, 0), "a", cardExposureId, ContentEventSource.AD_CARD, target = false)

        // when
        val report = reportReader.read(contract, cardProductId, null, null, TODAY)

        // then
        assertEquals(1L, report.daily.single { it.date == START }.reach)
        assertEquals(1L, report.daily.single { it.date == START.plusDays(1) }.reach)
        assertEquals(0L, report.daily.single { it.date == START.plusDays(2) }.reach)
        assertEquals(1L, report.audience.reach)
    }

    @Test
    fun `날짜별 도달은 같은 브라우저의 회원과 비회원 이벤트를 한 사람으로 센다`() {
        // given
        event(ContentEventType.IMPRESSION, START.atTime(9, 0), "a", cardExposureId, ContentEventSource.AD_CARD, target = false)
        event(ContentEventType.IMPRESSION, START.atTime(9, 1), "a", bannerExposureId, ContentEventSource.BANNER, target = false)
        event(ContentEventType.IMPRESSION, START.atTime(10, 0), "a", cardExposureId, ContentEventSource.AD_CARD, target = false, userId = 5L)
        event(ContentEventType.IMPRESSION, START.atTime(11, 0), "b", bannerExposureId, ContentEventSource.BANNER, target = false)

        // when
        val daily = reportReader.read(contract, bannerProductId, null, null, TODAY).daily
        val cardDaily = reportReader.read(contract, cardProductId, null, null, TODAY).daily

        // then
        assertEquals(2L, daily.single { it.date == START }.reach)
        assertEquals(1L, cardDaily.single { it.date == START }.reach)
    }

    @Test
    fun `날짜별 도달은 원본 보관 기간보다 앞날만 null이고 보관 기간 안의 날은 이벤트가 없어도 0이다`() {
        // given
        val firstRetained = TODAY.minusDays(365)
        val oldContract = contractRepository.save(form())
        val card = productRepository.findAll().first { it.category == AdvertisementProductCategory.JOB_POSTING }
        val oldProductId = checkNotNull(
            contractProductRepository.save(
                AdvertisementContractProduct(
                    checkNotNull(oldContract.id),
                    checkNotNull(card.id),
                    AdvertisementContractProductPeriod(firstRetained.minusDays(2), firstRetained.plusDays(2)),
                ),
            ).id,
        )
        val exposure = exposureRepository.save(
            AdvertisementExposure(
                product = card,
                contractId = oldContract.id,
                content = AdvertisementExposureContent(targetType = AdvertisementExposureTargetType.JOB, targetId = JOB_ID),
                adLabel = true,
                period = AdvertisementExposurePeriod.ALWAYS,
                exposed = true,
                displayOrder = 1,
            ),
        )
        event(ContentEventType.IMPRESSION, firstRetained.atTime(9, 0), "a", exposure.id, ContentEventSource.AD_CARD, target = false)

        // when
        val daily = reportReader.read(oldContract, oldProductId, null, null, TODAY).daily

        // then
        assertEquals(
            listOf(null, null, 1L, 0L, 0L),
            daily.sortedBy { it.date }.map { it.reach },
        )
    }

    @Test
    fun `원본 보관 기간보다 앞날이 걸린 기간은 도달과 평균 빈도를 null로 준다`() {
        // given
        val old = TODAY.minusDays(400)
        val oldContract = contractRepository.save(form())
        val card = productRepository.findAll().first { it.category == AdvertisementProductCategory.JOB_POSTING }
        val oldProductId = checkNotNull(
            contractProductRepository.save(
                AdvertisementContractProduct(checkNotNull(oldContract.id), checkNotNull(card.id), AdvertisementContractProductPeriod(old, old.plusDays(10))),
            ).id,
        )

        // when
        val audience = reportReader.read(oldContract, oldProductId, null, null, TODAY).audience

        // then
        assertNull(audience.reach)
        assertNull(audience.avgFrequency)
        assertNull(audience.adClickVisitors)
    }

    @Test
    fun `타깃 직무 일치율은 공고를 연 회원 중 희망 직군이 광고 공고의 직군과 겹치는 회원이고 학년은 공고를 연 회원만 센다`() {
        // given
        profile(1L, "백엔드/서버 개발", UserGrade.FOURTH)
        profile(2L, "마케터", UserGrade.FOURTH)
        profile(3L, null)
        profile(4L, "백엔드/서버 개발", UserGrade.GRADUATE)
        event(ContentEventType.VIEW, START.atTime(10, 0), "a", cardExposureId, ContentEventSource.AD_CARD, userId = 1L)
        event(ContentEventType.VIEW, START.atTime(10, 0), "b", cardExposureId, ContentEventSource.AD_CARD, userId = 2L)
        event(ContentEventType.VIEW, START.atTime(10, 0), "c", cardExposureId, ContentEventSource.AD_CARD, userId = 3L)
        event(ContentEventType.AD_CLICK, START.atTime(10, 0), "d", cardExposureId, ContentEventSource.AD_CARD, target = false, userId = 4L)

        // when
        val audience = reportReader.read(contract, cardProductId, null, null, TODAY).audience

        // then
        assertEquals(listOf(JobField.IT_DEVELOPMENT), audience.targetJobFields)
        assertEquals(1L, audience.targetMatch.matchedMembers)
        assertEquals(3L, audience.targetMatch.members)
        assertEquals(0.3333, audience.targetMatch.rate)
        assertEquals(listOf(UserGrade.FOURTH, null), audience.byGrade.map { it.grade })
        assertEquals(listOf(2L, 1L), audience.byGrade.map { it.memberUniqueVisitors })
        assertEquals("학년 미설정", audience.byGrade.last().label)
    }

    @Test
    fun `타깃 직군이 없는 배너는 일치율이 null이다`() {
        // given
        profile(1L, "백엔드/서버 개발")
        event(ContentEventType.VIEW, START.atTime(10, 0), "a", bannerExposureId, ContentEventSource.BANNER, userId = 1L)

        // when
        val audience = reportReader.read(contract, bannerProductId, null, null, TODAY).audience

        // then
        assertEquals(emptyList<JobField>(), audience.targetJobFields)
        assertNull(audience.targetMatch.rate)
    }

    @Test
    fun `대상 공고의 공유는 공유 이벤트 수로, 저장은 북마크 읽기로 센다`() {
        // given
        contractTargetRepository.save(
            AdvertisementContractTarget(checkNotNull(contract.id), AdvertisementContractPromotionTarget.JOB, JOB_ID),
        )
        event(ContentEventType.SHARE, START.atTime(10, 0), "a", null, ContentEventSource.LIST)
        event(ContentEventType.SHARE, START.plusDays(1).atTime(10, 0), "a", null, ContentEventSource.LIST)
        event(ContentEventType.SHARE, START.minusDays(1).atTime(10, 0), "a", null, ContentEventSource.LIST)
        Mockito.`when`(jobBookmarkReader.countSaved(JOB_ID, START, TODAY)).thenReturn(4L)

        // when
        val total = reportReader.read(contract, cardProductId, null, null, TODAY).targets.single().total

        // then
        assertEquals(2L, total.shares)
        assertEquals(4L, total.saves)
    }

    @Test
    fun `인사이트는 통째로 바꾸고 보낸 것만 기업 회원이 보며 운영자는 임시 저장도 본다`() {
        // given
        val drafts = listOf(
            AdvertisementReportInsightDraftDto(" 첫 제목 ", "첫 본문"),
            AdvertisementReportInsightDraftDto("둘째 제목", "둘째 본문"),
        )

        // when
        insightManager.replace(contract, cardProductId, drafts, publish = false, now = NOW)
        val draftForCompany = reportReader.read(contract, cardProductId, null, null, TODAY).insights
        val draftForAdmin = reportReader.read(contract, cardProductId, null, null, TODAY, includeDraftInsights = true).insights
        insightManager.replace(contract, cardProductId, drafts.take(1), publish = true, now = NOW.plusHours(1))
        val sentForCompany = reportReader.read(contract, cardProductId, null, null, TODAY).insights

        // then
        assertEquals(emptyList<AdvertisementReportInsightDto>(), draftForCompany)
        assertEquals(listOf("첫 제목", "둘째 제목"), draftForAdmin.map { it.title })
        assertEquals(listOf(false, false), draftForAdmin.map { it.published })
        assertEquals(listOf(AdvertisementReportInsightDto("첫 제목", "첫 본문", true)), sentForCompany)
    }

    @Test
    fun `인사이트를 빈 목록으로 저장하면 모두 걷어내고 다른 상품에는 영향이 없다`() {
        // given
        insightManager.replace(contract, cardProductId, listOf(AdvertisementReportInsightDraftDto("카드", "본문")), publish = true, now = NOW)
        insightManager.replace(contract, bannerProductId, listOf(AdvertisementReportInsightDraftDto("배너", "본문")), publish = true, now = NOW)

        // when
        insightManager.replace(contract, cardProductId, emptyList(), publish = true, now = NOW.plusHours(1))

        // then
        assertEquals(emptyList<AdvertisementReportInsightDto>(), reportReader.read(contract, cardProductId, null, null, TODAY).insights)
        assertEquals(listOf("배너"), reportReader.read(contract, bannerProductId, null, null, TODAY).insights.map { it.title })
    }

    @Test
    fun `계약에 붙지 않은 상품의 인사이트 저장은 404이고 여섯 개를 넘기면 거부한다`() {
        // given
        val tooMany = (1..7).map { AdvertisementReportInsightDraftDto("제목$it", "본문$it") }

        // then
        assertEquals(
            AdvertisementContractErrorCode.ADVERTISEMENT_CONTRACT_PRODUCT_NOT_FOUND,
            assertThrows<BusinessException> {
                insightManager.replace(contract, 999_999L, listOf(AdvertisementReportInsightDraftDto("제목", "본문")), publish = true, now = NOW)
            }.errorCode,
        )
        assertThrows<IllegalArgumentException> { insightManager.replace(contract, cardProductId, tooMany, publish = true, now = NOW) }
    }

    @Test
    fun `계약 합계는 리포트를 열 수 있는 상품의 상품 몫을 더한 값이다`() {
        // given
        event(ContentEventType.VIEW, START.atTime(10, 0), "a", bannerExposureId, ContentEventSource.BANNER)
        event(ContentEventType.IMPRESSION, START.atTime(9, 0), "a", bannerExposureId, ContentEventSource.BANNER, target = false)
        event(ContentEventType.IMPRESSION, START.atTime(9, 1), "a", bannerExposureId, ContentEventSource.BANNER, target = false)
        event(ContentEventType.IMPRESSION, START.atTime(9, 2), "b", cardExposureId, ContentEventSource.AD_CARD)
        event(ContentEventType.VIEW, START.atTime(11, 0), "b", cardExposureId, ContentEventSource.AD_CARD)
        val kakao = productRepository.save(AdvertisementProduct(AdvertisementProductCategory.KAKAO_1, null, "카카오 톡방", null))
        addProduct(kakao, START, END)

        // when
        val total = reportReader.readTotal(contract, TODAY)
        val banner = reportReader.read(contract, bannerProductId, null, null, TODAY).summary
        val card = reportReader.read(contract, cardProductId, null, null, TODAY).summary

        // then
        assertEquals(listOf(bannerProductId, cardProductId), total.products.map { it.contractProductId })
        assertEquals(banner.impressions + card.impressions, total.total.impressions)
        assertEquals(banner.uniqueImpressions + card.uniqueImpressions, total.total.uniqueImpressions)
        assertEquals(banner.views + card.views, total.total.views)
        assertEquals(3L to 2L, total.total.impressions to total.total.uniqueImpressions)
    }

    @Test
    fun `진행 중 광고는 오늘이 기간 안인 상품만 끝나는 날이 가까운 순으로 준다`() {
        // given
        val banner2 = productRepository.save(AdvertisementProduct(AdvertisementProductCategory.BANNER, AdvertisementProductPosition.JOB_HOME_BOTTOM_BANNER, "홈 하단 배너", null))
        val endingSoon = addProduct(banner2, START, TODAY.plusDays(2))
        addProduct(banner2, TODAY.plusDays(1), END)
        addProduct(banner2, START, TODAY.minusDays(1))

        // when
        val active = reportReader.readActive(listOf(contract), TODAY)

        // then
        assertEquals(listOf(endingSoon, bannerProductId, cardProductId), active.map { it.contractProductId })
        assertEquals(2L, active.first().daysLeft)
        assertEquals("백엔드 개발자", active.single { it.contractProductId == cardProductId }.target?.title)
        assertNull(active.single { it.contractProductId == bannerProductId }.scroll50Rate)
        assertEquals(START.datesUntil(TODAY.plusDays(1)).toList(), active.first().daily.map { it.date })
    }

    private fun profile(userId: Long, wishJob: String?, grade: UserGrade? = null) {
        val profile = UserProfile(userId = userId, lastSyncedAt = NOW)
        profile.replaceJobInfo(null, null, grade, null, wishJob, null, null, null, null)
        userProfileRepository.save(profile)
    }

    private fun addProduct(product: AdvertisementProduct, start: LocalDate, end: LocalDate): Long = checkNotNull(
        contractProductRepository.save(
            AdvertisementContractProduct(checkNotNull(contract.id), checkNotNull(product.id), AdvertisementContractProductPeriod(start, end)),
        ).id,
    )

    private fun event(
        type: ContentEventType,
        occurredAt: LocalDateTime,
        visitor: String,
        exposureId: Long?,
        source: ContentEventSource,
        scroll: Int? = null,
        target: Boolean = true,
        userId: Long? = null,
    ) {
        eventRepository.save(
            ContentEvent(
                eventType = type,
                targetType = if (target) ContentEventTargetType.JOB else null,
                targetId = if (target) JOB_ID else null,
                exposureId = exposureId,
                userId = userId,
                visitorId = visitor,
                source = source,
                scrollPercent = scroll,
                durationMs = scroll?.let { 10_000L },
                occurredAt = occurredAt,
            ),
        )
    }

    private fun form() = AdvertisementContract.requestByForm(
        userId = 7L,
        promotionTarget = AdvertisementContractPromotionTarget.JOB,
        companyName = "뱅크",
        recruitmentTitle = "백엔드 개발자",
        recruitmentUrl = "https://example.com/recruit",
        recruitmentDeadline = LocalDate.of(2026, 12, 20),
        purpose = AdvertisementContractPurpose.FAST_APPLICANTS,
        budgetAmount = null,
        categories = setOf(AdvertisementProductCategory.BANNER),
        scheduleRequest = null,
        advertisementRequest = null,
        managerName = "김담당",
        email = "marketing@example.com",
        phoneNumber = "010-1234-5678",
        invoiceMethod = AdvertisementContractInvoiceMethod.TAX_INVOICE,
        creativeMethod = AdvertisementContractCreativeMethod.REQUEST_PRODUCTION,
    )

    private companion object {
        const val JOB_ID = 15818L
        val START: LocalDate = LocalDate.of(2026, 11, 10)
        val END: LocalDate = LocalDate.of(2026, 11, 30)
        val TODAY: LocalDate = LocalDate.of(2026, 11, 19)
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 19, 9, 0)
    }
}
