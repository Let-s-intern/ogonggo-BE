package com.ogonggo.userapi.advertisement.business

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryCompanyNameSource
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflow
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflowChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryMemberType
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryPromotionChannel
import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryType
import com.ogonggo.core.advertisementinquiry.implement.AdvertisementInquiryAppender
import com.ogonggo.core.advertisementinquiry.implement.dto.AdvertisementInquiryAppendDto
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.advertisement.implement.AdvertisementInquiryMailer
import com.ogonggo.userapi.advertisement.implement.AdvertisementInquiryNotifier
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * B2B 광고 문의 접수.
 *
 * 문의를 저장한 뒤 영업 슬랙 채널과 담당자 확인 메일로 알린다. 저장이 곧 접수 결과이고 알림은 부가 작업이다.
 * 슬랙 실패는 로그만 남기고 접수를 실패로 만들지 않는다. 문의는 어드민에서 다시 볼 수 있다.
 *
 * 저장은 한 행이라 Repository의 트랜잭션으로 충분하다. 유스케이스 전체에 트랜잭션을 열면
 * 슬랙 응답을 기다리는 동안 커넥션을 붙잡고, 커밋 전에 알림이 나간다.
 */
@Service
class AdvertisementInquiryService(
    private val userReader: UserReader,
    private val advertisementInquiryAppender: AdvertisementInquiryAppender,
    private val advertisementInquiryNotifier: AdvertisementInquiryNotifier,
    private val advertisementInquiryMailer: AdvertisementInquiryMailer,
) {

    /**
     * 회원 식별자와 회원 구분은 토큰으로만 정한다. 비로그인이면 userId가 없다.
     * 저장한 문의의 식별자를 돌려준다.
     */
    fun createInquiry(userId: Long?, command: CreateAdvertisementInquiryCommand): Long {
        // 토큰은 유효한데 회원 행이 없으면 비회원으로 받는다. 공개 폼이라 문의를 거절하지 않는다.
        val role = userId?.let { userReader.readRoles(setOf(it))[it] }
        val memberType = AdvertisementInquiryMemberType.of(role)
        val inflow = command.inflow.toDomain()

        val inquiryId = advertisementInquiryAppender.append(
            AdvertisementInquiryAppendDto(
                inquiryType = command.inquiryType,
                // 지금 이 API를 부르는 진입 페이지는 출시 기념 무료 홍보(`/biz`) 하나뿐이다.
                campaignCode = LAUNCH_FREE_PROMO_CAMPAIGN,
                companyName = command.companyName,
                companyNameSource = command.companyNameSource,
                managerName = command.managerName,
                managerTitle = command.managerTitle,
                email = command.email,
                phoneNumber = command.phoneNumber,
                promotionChannel = command.promotionChannel,
                hiringJobName = command.hiringJobName,
                jobPostingUrl = command.jobPostingUrl,
                promotionAnswer = command.promotionAnswer,
                marketingConsent = command.marketingConsent,
                userId = if (role == null) null else userId,
                memberType = memberType,
                inflow = inflow,
            ),
        )
        log.info(
            "B2B 광고 문의를 저장했습니다. inquiryId={}, inquiryType={}, memberType={}, inflowChannel={}",
            inquiryId,
            command.inquiryType,
            memberType,
            inflow.channel,
        )

        val notification = AdvertisementInquiryNotification.from(command, memberType, inflow)
        try {
            advertisementInquiryNotifier.notify(notification)
        } catch (exception: RuntimeException) {
            log.error("광고 문의 슬랙 알림 발송에 실패했습니다. 문의는 저장돼 있습니다. inquiryId={}", inquiryId, exception)
        }
        advertisementInquiryMailer.sendConfirmation(notification)
        return inquiryId
    }

    companion object {
        /** 출시 기념 무료 홍보 이벤트 페이지(`/biz`)의 캠페인 코드 */
        const val LAUNCH_FREE_PROMO_CAMPAIGN = "launch_free_promo"

        private val log = LoggerFactory.getLogger(AdvertisementInquiryService::class.java)
    }
}

data class CreateAdvertisementInquiryCommand(
    val companyName: String,
    val companyNameSource: AdvertisementInquiryCompanyNameSource,
    val managerName: String,
    val managerTitle: String?,
    val email: String,
    val phoneNumber: String,
    val inquiryType: AdvertisementInquiryType,
    val promotionChannel: AdvertisementInquiryPromotionChannel,
    val hiringJobName: String?,
    val jobPostingUrl: String?,
    val promotionAnswer: String,
    val marketingConsent: Boolean,
    val inflow: AdvertisementInquiryInflowCommand,
)

/** 페이지에 처음 들어온 순간에 잡은 원본 값이다. 유입 경로는 서버가 이 값으로 판정한다. */
data class AdvertisementInquiryInflowCommand(
    val utmSource: String?,
    val utmMedium: String?,
    val utmCampaign: String?,
    val utmContent: String?,
    val referrer: String?,
    val entryHost: String?,
    val landingPath: String?,
) {
    fun toDomain(): AdvertisementInquiryInflow = AdvertisementInquiryInflow(
        utmSource = utmSource,
        utmMedium = utmMedium,
        utmCampaign = utmCampaign,
        utmContent = utmContent,
        referrer = referrer,
        entryHost = entryHost,
        landingPath = landingPath,
        channel = AdvertisementInquiryInflowChannel.classify(utmSource, referrer),
    )
}

/** 알림 어댑터에 전달하는 값이다. Command와 필드가 같아도 변경 이유가 달라 따로 둔다. */
data class AdvertisementInquiryNotification(
    val companyName: String,
    val managerName: String,
    val email: String,
    val phoneNumber: String,
    val inquiryType: AdvertisementInquiryType,
    val promotionChannel: AdvertisementInquiryPromotionChannel,
    val promotionAnswer: String,
    val memberType: AdvertisementInquiryMemberType,
    val inflowChannel: AdvertisementInquiryInflowChannel,
    val utmSource: String?,
    val utmCampaign: String?,
    val utmContent: String?,
    val referrer: String?,
) {
    companion object {
        fun from(
            command: CreateAdvertisementInquiryCommand,
            memberType: AdvertisementInquiryMemberType,
            inflow: AdvertisementInquiryInflow,
        ): AdvertisementInquiryNotification =
            AdvertisementInquiryNotification(
                companyName = command.companyName,
                managerName = command.managerName,
                email = command.email,
                phoneNumber = command.phoneNumber,
                inquiryType = command.inquiryType,
                promotionChannel = command.promotionChannel,
                promotionAnswer = command.promotionAnswer,
                memberType = memberType,
                inflowChannel = inflow.channel,
                utmSource = inflow.utmSource,
                utmCampaign = inflow.utmCampaign,
                utmContent = inflow.utmContent,
                referrer = inflow.referrer,
            )
    }
}
