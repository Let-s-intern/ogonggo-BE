package com.ogonggo.core.advertiser.implement

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryCompanyNameSource
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflow
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflowChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryMemberType
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryPromotionChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryStatus
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import com.ogonggo.core.advertisementinquiry.implement.AdvertisementInquiryAppender
import com.ogonggo.core.advertisementinquiry.implement.AdvertisementInquiryReader
import com.ogonggo.core.advertisementinquiry.implement.dto.AdvertisementInquiryAppendDto
import com.ogonggo.core.advertisementinquiry.persistence.AdvertisementInquiryJpaRepository
import com.ogonggo.core.advertisementinquiry.persistence.AdvertisementInquiryQueryRepository
import com.ogonggo.core.advertiser.domain.AdvertiserContact
import com.ogonggo.core.advertiser.domain.AdvertiserSearchCondition
import com.ogonggo.core.advertiser.domain.AdvertiserSource
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertiser.error.AdvertiserErrorCode
import com.ogonggo.core.advertiser.implement.dto.AdvertiserAppendDto
import com.ogonggo.core.advertiser.implement.dto.AdvertiserUpdateDto
import com.ogonggo.core.advertiser.persistence.AdvertiserQueryRepository
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.user.implement.CompanyProfileAppender
import com.ogonggo.core.user.implement.UserAppender
import com.ogonggo.core.user.implement.UserManagementReader
import com.ogonggo.core.user.implement.dto.CompanyAccountAppendDto
import com.ogonggo.core.user.implement.dto.CompanyProfileAppendDto
import com.ogonggo.core.user.persistence.UserJpaRepository
import com.ogonggo.core.user.persistence.UserQueryRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDateTime

@DataJpaTest(showSql = false)
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    AdvertiserReader::class,
    AdvertiserAppender::class,
    AdvertiserManager::class,
    AdvertiserQueryRepository::class,
    UserManagementReader::class,
    UserQueryRepository::class,
    UserAppender::class,
    CompanyProfileAppender::class,
    AdvertisementInquiryReader::class,
    AdvertisementInquiryAppender::class,
    AdvertisementInquiryQueryRepository::class,
)
internal class AdvertiserPersistenceTest @Autowired constructor(
    private val advertiserReader: AdvertiserReader,
    private val advertiserAppender: AdvertiserAppender,
    private val advertiserManager: AdvertiserManager,
    private val userAppender: UserAppender,
    private val companyProfileAppender: CompanyProfileAppender,
    private val userRepository: UserJpaRepository,
    private val inquiryAppender: AdvertisementInquiryAppender,
    private val inquiryRepository: AdvertisementInquiryJpaRepository,
    private val entityManager: EntityManager,
) {

    @Test
    fun `목록을 맞추면 탈퇴하지 않은 비즈니스 회원마다 광고주를 하나 만들고 다시 맞춰도 늘지 않으며 회원 정보를 따른다`() {
        // given
        val memberId = companyMember("hr@ogonggo.kr", "오공고")
        val withdrawnId = companyMember("bye@ogonggo.kr", "탈퇴한 회사")
        userRepository.findById(withdrawnId).get().withdraw(NOW)
        sync()

        // when
        entityManager.createQuery("update CompanyProfile p set p.organizationName = '오공고랩스' where p.userId = :userId")
            .setParameter("userId", memberId)
            .executeUpdate()
        entityManager.clear()
        sync()

        // then
        val members = advertiserReader.readAll(AdvertiserSearchCondition(type = AdvertiserType.MEMBER))
        assertEquals(1, members.size)
        val member = members.single()
        assertEquals(memberId, member.companyMemberUserId)
        assertEquals(AdvertiserSource.COMPANY_MEMBER, member.source)
        assertEquals("오공고랩스", member.name)
        assertEquals("김담당", member.contactName)
        assertEquals("hr@ogonggo.kr", member.contactEmail)
    }

    @Test
    fun `회원이 탈퇴하면 목록에서 빠지지만 한 건 조회는 된다`() {
        // given
        val memberId = companyMember("hr@ogonggo.kr", "오공고")
        sync()
        val advertiserId = checkNotNull(advertiserReader.readAll(AdvertiserSearchCondition()).single().id)

        // when
        userRepository.findById(memberId).get().withdraw(NOW)
        entityManager.flush()

        // then
        assertTrue(advertiserReader.readAll(AdvertiserSearchCondition()).isEmpty())
        assertEquals(memberId, advertiserReader.read(advertiserId).companyMemberUserId)
    }

    @Test
    fun `광고 문의는 회사 이름마다 비회원 광고주 하나로 묶고 가장 최근 문의를 잇는다`() {
        // given
        val first = inquiry("그로우랩", managerName = "정하늘")
        val second = inquiry("그로우 랩", managerName = "김도윤")
        inquiry("기업회원 회사", memberType = AdvertisementInquiryMemberType.COMPANY, userId = 99)
        val rejected = inquiry("스팸 회사")
        inquiryRepository.findById(rejected).get().changeStatus(AdvertisementInquiryStatus.REJECTED)
        entityManager.flush()

        // when
        sync()

        // then
        val advertisers = advertiserReader.readAll(AdvertiserSearchCondition())
        assertEquals(listOf("그로우랩"), advertisers.map { it.name })
        val advertiser = advertisers.single()
        assertEquals(AdvertiserSource.AD_INQUIRY, advertiser.source)
        assertEquals(second, advertiser.adInquiryId)
        assertEquals("정하늘", advertiser.contactName)
        assertTrue(first < second)
    }

    @Test
    fun `이미 있는 비회원 광고주와 같은 이름의 문의는 새로 만들지 않고 이으며 운영자가 고친 담당자를 지킨다`() {
        // given
        val manual = advertiserAppender.appendManual(AdvertiserAppendDto("Grow Lab", AdvertiserContact(name = "운영자가 적은 담당자")))
        val inquiryId = inquiry("growlab", managerName = "문의 담당자")

        // when
        sync()
        sync()

        // then
        val advertiser = advertiserReader.read(checkNotNull(manual.id))
        assertEquals(inquiryId, advertiser.adInquiryId)
        assertEquals("운영자가 적은 담당자", advertiser.contactName)
        assertEquals(1, advertiserReader.readAll(AdvertiserSearchCondition()).size)
    }

    @Test
    fun `목록은 종류와 이름·담당자 이름·메일로 대소문자를 가리지 않고 거른다`() {
        // given
        companyMember("hr@ogonggo.kr", "오공고")
        sync()
        advertiserAppender.appendManual(AdvertiserAppendDto("새싹", AdvertiserContact(email = "Sprout@Seoul.go.kr")))
        advertiserAppender.appendManual(AdvertiserAppendDto("코드스테이션", AdvertiserContact(name = "김도윤")))

        fun names(condition: AdvertiserSearchCondition) = advertiserReader.readAll(condition).map { it.name }

        // then
        assertEquals(listOf("새싹", "코드스테이션"), names(AdvertiserSearchCondition(type = AdvertiserType.NON_MEMBER)))
        assertEquals(listOf("새싹"), names(AdvertiserSearchCondition(keyword = "sprout")))
        assertEquals(listOf("코드스테이션"), names(AdvertiserSearchCondition(keyword = "도윤")))
        assertEquals(listOf("오공고"), names(AdvertiserSearchCondition(keyword = "HR@")))
    }

    @Test
    fun `같은 이름은 공백·대소문자를 빼고 회원 광고주까지 보며 수정할 때는 자기 자신을 뺀다`() {
        // given
        companyMember("hr@ogonggo.kr", "Ogonggo Labs")
        sync()
        val manual = advertiserAppender.appendManual(AdvertiserAppendDto("새싹", AdvertiserContact()))

        // then
        assertTrue(advertiserReader.existsSameName("ogonggolabs"))
        assertTrue(advertiserReader.existsSameName("새 싹"))
        assertFalse(advertiserReader.existsSameName("새싹", exceptAdvertiserId = manual.id))
    }

    @Test
    fun `비회원 광고주끼리 같은 이름 비교 값은 DB가 막는다`() {
        advertiserAppender.appendManual(AdvertiserAppendDto("새싹", AdvertiserContact()))

        assertThrows(DataIntegrityViolationException::class.java) {
            advertiserAppender.appendManual(AdvertiserAppendDto("새 싹", AdvertiserContact()))
            entityManager.flush()
        }
    }

    @Test
    fun `잠가 읽은 광고주의 수정이 저장되고 없는 광고주는 찾을 수 없다`() {
        // given
        val manual = advertiserAppender.appendManual(AdvertiserAppendDto("새싹", AdvertiserContact(name = "담당")))
        val advertiserId = checkNotNull(manual.id)

        // when
        advertiserManager.update(advertiserReader.readForUpdate(advertiserId), AdvertiserUpdateDto(contactName = ""))
        entityManager.flush()
        entityManager.clear()

        // then
        assertEquals(null, advertiserReader.read(advertiserId).contactName)
        val exception = assertThrows(EntityNotFoundException::class.java) { advertiserReader.read(advertiserId + 1000) }
        assertEquals(AdvertiserErrorCode.ADVERTISER_NOT_FOUND, exception.errorCode)
    }

    private fun sync() {
        advertiserManager.syncSources()
        entityManager.flush()
    }

    private fun companyMember(email: String, organizationName: String): Long {
        val userId = userAppender.appendCompany(
            CompanyAccountAppendDto(email = email, encodedPassword = "encoded", joinedAt = NOW),
        ).userId
        companyProfileAppender.append(
            CompanyProfileAppendDto(userId = userId, organizationName = organizationName, managerName = "김담당"),
        )
        entityManager.flush()
        return userId
    }

    private fun inquiry(
        companyName: String,
        managerName: String = "김담당",
        memberType: AdvertisementInquiryMemberType = AdvertisementInquiryMemberType.GUEST,
        userId: Long? = null,
    ): Long {
        val id = inquiryAppender.append(
            AdvertisementInquiryAppendDto(
                inquiryType = AdvertisementInquiryType.FREE_PROMOTION,
                campaignCode = null,
                companyName = companyName,
                companyNameSource = AdvertisementInquiryCompanyNameSource.MANUAL,
                managerName = managerName,
                managerTitle = null,
                email = "manager@example.com",
                phoneNumber = "010-1234-5678",
                promotionChannel = AdvertisementInquiryPromotionChannel.INSTAGRAM,
                hiringJobName = null,
                jobPostingUrl = null,
                promotionAnswer = "사람인에 올리고 있습니다.",
                marketingConsent = false,
                userId = userId,
                memberType = memberType,
                inflow = AdvertisementInquiryInflow(
                    utmSource = null,
                    utmMedium = null,
                    utmCampaign = null,
                    utmContent = null,
                    referrer = null,
                    entryHost = null,
                    landingPath = null,
                    channel = AdvertisementInquiryInflowChannel.DIRECT,
                ),
            ),
        )
        entityManager.flush()
        return id
    }

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 10, 0)
    }
}
