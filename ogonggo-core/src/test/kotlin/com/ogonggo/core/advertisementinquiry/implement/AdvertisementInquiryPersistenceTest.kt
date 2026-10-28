package com.ogonggo.core.advertisementinquiry.implement

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryCompanyNameSource
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflow
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflowChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryMemberType
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryPromotionChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquirySearchCondition
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryStatus
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import com.ogonggo.core.advertisementinquiry.implement.dto.AdvertisementInquiryAppendDto
import com.ogonggo.core.advertisementinquiry.error.AdvertisementInquiryErrorCode
import com.ogonggo.core.advertisementinquiry.persistence.AdvertisementInquiryJpaRepository
import com.ogonggo.core.advertisementinquiry.persistence.AdvertisementInquiryQueryRepository
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.jpa.CoreJpaConfiguration
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(AdvertisementInquiryAppender::class, AdvertisementInquiryQueryRepository::class)
internal class AdvertisementInquiryPersistenceTest @Autowired constructor(
    private val appender: AdvertisementInquiryAppender,
    private val repository: AdvertisementInquiryJpaRepository,
    private val queryRepository: AdvertisementInquiryQueryRepository,
    private val entityManager: EntityManager,
) {

    @Test
    fun `목록은 최근에 들어온 문의부터 페이지로 준다`() {
        // given
        val first = append()
        val second = append()
        val third = append()

        // when
        val firstPage = reader(at(2026, 10, 27, 12, 0)).readPage(AdvertisementInquirySearchCondition(), page = 0, size = 2)

        // then
        assertEquals(listOf(third, second), firstPage.advertisementInquiries.map { it.id })
        assertEquals(3, firstPage.totalElements)
        assertEquals(2, firstPage.totalPages)
        assertEquals(
            listOf(first),
            reader(at(2026, 10, 27, 12, 0)).readPage(AdvertisementInquirySearchCondition(), page = 1, size = 2)
                .advertisementInquiries.map { it.id },
        )
    }

    @Test
    fun `회사명 입력 방식을 저장하고 다시 읽는다`() {
        // given
        val ids = AdvertisementInquiryCompanyNameSource.entries.associateWith { append(companyNameSource = it) }
        entityManager.clear()

        // when & then
        ids.forEach { (source, id) -> assertEquals(source, repository.findById(id).get().companyNameSource) }
    }

    @Test
    fun `회사명 입력 방식으로 거른다`() {
        // given
        val manual = append(companyNameSource = AdvertisementInquiryCompanyNameSource.MANUAL)
        append(companyNameSource = AdvertisementInquiryCompanyNameSource.SEARCH)
        val memberProfile = append(companyNameSource = AdvertisementInquiryCompanyNameSource.MEMBER_PROFILE)
        val reader = reader(at(2026, 10, 28, 12, 0))

        // when & then
        fun ids(source: AdvertisementInquiryCompanyNameSource) =
            reader.readPage(AdvertisementInquirySearchCondition(companyNameSource = source), 0, 20).advertisementInquiries.map { it.id }

        assertEquals(listOf(manual), ids(AdvertisementInquiryCompanyNameSource.MANUAL))
        assertEquals(listOf(memberProfile), ids(AdvertisementInquiryCompanyNameSource.MEMBER_PROFILE))
    }

    @Test
    fun `문의 유형·캠페인·처리 상태·회원 구분·유입 경로로 거른다`() {
        // given
        val coldmailGuest = append()
        val companyAlert = append(
            inquiryType = AdvertisementInquiryType.LAUNCH_ALERT,
            campaignCode = null,
            userId = 3L,
            memberType = AdvertisementInquiryMemberType.COMPANY,
            channel = AdvertisementInquiryInflowChannel.OGONGGO,
        )
        val rejected = append(channel = AdvertisementInquiryInflowChannel.DIRECT)
        repository.findById(rejected).get().changeStatus(AdvertisementInquiryStatus.REJECTED)
        entityManager.flush()
        val reader = reader(at(2026, 10, 27, 12, 0))

        // when & then
        fun ids(condition: AdvertisementInquirySearchCondition) =
            reader.readPage(condition, 0, 20).advertisementInquiries.map { it.id }

        assertEquals(listOf(companyAlert), ids(AdvertisementInquirySearchCondition(inquiryType = AdvertisementInquiryType.LAUNCH_ALERT)))
        assertEquals(listOf(rejected, coldmailGuest), ids(AdvertisementInquirySearchCondition(campaignCode = CAMPAIGN)))
        assertEquals(listOf(rejected), ids(AdvertisementInquirySearchCondition(status = AdvertisementInquiryStatus.REJECTED)))
        assertEquals(listOf(companyAlert), ids(AdvertisementInquirySearchCondition(memberType = AdvertisementInquiryMemberType.COMPANY)))
        assertEquals(
            listOf(coldmailGuest),
            ids(AdvertisementInquirySearchCondition(inflowChannel = AdvertisementInquiryInflowChannel.COLDMAIL)),
        )
        assertEquals(
            listOf(rejected),
            ids(
                AdvertisementInquirySearchCondition(
                    campaignCode = CAMPAIGN,
                    inflowChannel = AdvertisementInquiryInflowChannel.DIRECT,
                    memberType = AdvertisementInquiryMemberType.GUEST,
                ),
            ),
        )
    }

    @Test
    fun `기간은 시작일 0시부터 종료일이 끝날 때까지다`() {
        // given
        append(createdAt = at(2026, 10, 24, 23, 59, 59))
        val start = append(createdAt = at(2026, 10, 25, 0, 0))
        val end = append(createdAt = at(2026, 10, 26, 23, 59, 59))
        append(createdAt = at(2026, 10, 27, 0, 0))

        // when
        val result = reader(at(2026, 10, 27, 12, 0)).readPage(
            AdvertisementInquirySearchCondition(createdFrom = LocalDate.of(2026, 10, 25), createdTo = LocalDate.of(2026, 10, 26)),
            0,
            20,
        )

        // then
        assertEquals(listOf(end, start), result.advertisementInquiries.map { it.id })
        assertEquals(2, result.totalElements)
    }

    @Test
    fun `오늘 건수는 한국 시간 0시부터 센다`() {
        // given
        append(createdAt = at(2026, 10, 26, 23, 59))
        append(createdAt = at(2026, 10, 27, 0, 0))
        val rejected = append(createdAt = at(2026, 10, 27, 9, 30))
        repository.findById(rejected).get().changeStatus(AdvertisementInquiryStatus.REJECTED)
        entityManager.flush()

        // when & then: 23:59에 만든 문의는 빠지고 00:00에 만든 문의는 들어간다. 처리 상태와 무관하다.
        assertEquals(2, reader(at(2026, 10, 27, 10, 0)).countToday())
        // 하루 전 23:59의 시점에서 보면 그날의 문의 하나뿐이다.
        assertEquals(1, reader(at(2026, 10, 26, 23, 59, 30)).countToday())
    }

    @Test
    fun `잠가 읽은 문의의 상태와 메모 변경이 저장되고 없는 문의는 찾을 수 없다`() {
        // given
        val id = append()
        val reader = reader(at(2026, 10, 27, 12, 0))

        // when
        reader.readForUpdate(id).apply {
            changeStatus(AdvertisementInquiryStatus.CONTACTED)
            changeMemo("10월 30일 통화")
        }
        entityManager.flush()
        entityManager.clear()

        // then
        val saved = repository.findById(id).get()
        assertEquals(AdvertisementInquiryStatus.CONTACTED, saved.status)
        assertEquals("10월 30일 통화", saved.memo)
        val exception = assertThrows(EntityNotFoundException::class.java) { reader.readForUpdate(id + 1000) }
        assertEquals(AdvertisementInquiryErrorCode.ADVERTISEMENT_INQUIRY_NOT_FOUND, exception.errorCode)
    }

    private fun reader(now: LocalDateTime) = AdvertisementInquiryReader(
        repository,
        queryRepository,
        Clock.fixed(now.atZone(SEOUL).toInstant(), SEOUL),
    )

    private fun append(
        inquiryType: AdvertisementInquiryType = AdvertisementInquiryType.FREE_PROMOTION,
        campaignCode: String? = CAMPAIGN,
        userId: Long? = null,
        memberType: AdvertisementInquiryMemberType = AdvertisementInquiryMemberType.GUEST,
        channel: AdvertisementInquiryInflowChannel = AdvertisementInquiryInflowChannel.COLDMAIL,
        createdAt: LocalDateTime? = null,
        companyNameSource: AdvertisementInquiryCompanyNameSource = AdvertisementInquiryCompanyNameSource.MANUAL,
    ): Long {
        val id = appender.append(
            AdvertisementInquiryAppendDto(
                inquiryType = inquiryType,
                campaignCode = campaignCode,
                companyName = "렛츠커리어",
                companyNameSource = companyNameSource,
                managerName = "김담당",
                managerTitle = "매니저",
                email = "manager@ogonggo.co.kr",
                phoneNumber = "010-1234-5678",
                promotionChannel = AdvertisementInquiryPromotionChannel.INSTAGRAM,
                hiringJobName = "마케팅 인턴",
                jobPostingUrl = "https://example.com/jobs/1",
                promotionAnswer = "사람인에 올리고 있습니다.",
                marketingConsent = true,
                userId = userId,
                memberType = memberType,
                inflow = AdvertisementInquiryInflow(
                    utmSource = "coldmail",
                    utmMedium = "email",
                    utmCampaign = "free_promo",
                    utmContent = null,
                    referrer = null,
                    entryHost = "biz.ogonggo.co.kr",
                    landingPath = "/biz?utm_source=coldmail",
                    channel = channel,
                ),
            ),
        )
        entityManager.flush()
        if (createdAt != null) {
            entityManager.createQuery("update AdvertisementInquiry a set a.createdAt = :createdAt where a.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", id)
                .executeUpdate()
            entityManager.clear()
        }
        return id
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0) =
        LocalDateTime.of(year, month, day, hour, minute, second)

    private companion object {
        const val CAMPAIGN = "launch_free_promo"
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
