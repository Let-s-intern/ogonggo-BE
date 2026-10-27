package com.ogonggo.core.advertisementinquiry.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * 기업 담당자가 남긴 광고 문의다. 무료 홍보 신청만이 아니라 광고 문의 전체를 담고 문의 유형과 캠페인 코드로 나눈다.
 *
 * 로그인 없이도 남길 수 있어 회원 식별자가 없을 수 있다. 회원 구분은 문의 시점의 값이다.
 * 영업이 처리 상태와 메모만 바꾸고, 문의 내용과 유입 정보는 남긴 그대로 둔다.
 * 지우는 기능이 없어 소프트 삭제 칼럼을 두지 않는다.
 */
@Entity
@Table(
    name = "advertisement_inquiries",
    indexes = [
        Index(name = "idx_advertisement_inquiries_created_at", columnList = "created_at"),
    ],
)
class AdvertisementInquiry internal constructor(
    inquiryType: AdvertisementInquiryType,
    campaignCode: String?,
    companyName: String,
    managerName: String,
    managerTitle: String?,
    email: String,
    phoneNumber: String,
    promotionChannel: AdvertisementInquiryPromotionChannel,
    hiringJobName: String?,
    jobPostingUrl: String?,
    promotionAnswer: String,
    marketingConsent: Boolean,
    userId: Long?,
    memberType: AdvertisementInquiryMemberType,
    inflow: AdvertisementInquiryInflow,
) : BaseTimeEntity() {

    init {
        require(companyName.isNotBlank() && managerName.isNotBlank() && email.isNotBlank() && phoneNumber.isNotBlank()) {
            "회사명, 담당자 이름, 이메일, 연락처는 비울 수 없습니다."
        }
        require(promotionAnswer.isNotBlank()) { "설문 답변은 비울 수 없습니다." }
        require(userId == null || userId > 0) { "회원 식별자는 양수여야 합니다." }
        require((memberType == AdvertisementInquiryMemberType.GUEST) == (userId == null)) {
            "비회원 문의에만 회원 식별자가 없습니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 광고 문의 식별자 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiry_type", nullable = false, length = 30)
    val inquiryType: AdvertisementInquiryType = inquiryType /* 문의 유형 */

    @Column(name = "campaign_code", length = 50)
    val campaignCode: String? = campaignCode /* 같은 문의 유형 안의 이벤트. 서버가 진입 페이지로 정한다 */

    @Column(name = "company_name", nullable = false, length = 100)
    val companyName: String = companyName /* 회사명 */

    @Column(name = "manager_name", nullable = false, length = 50)
    val managerName: String = managerName /* 담당자 이름 */

    @Column(name = "manager_title", length = 50)
    val managerTitle: String? = managerTitle /* 담당자 직함 */

    @Column(nullable = false, length = 255)
    val email: String = email /* 담당자 이메일 */

    @Column(name = "phone_number", nullable = false, length = 20)
    val phoneNumber: String = phoneNumber /* 담당자 연락처 */

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_channel", nullable = false, length = 40)
    val promotionChannel: AdvertisementInquiryPromotionChannel = promotionChannel /* 희망 홍보 채널 */

    @Column(name = "hiring_job_name", length = 100)
    val hiringJobName: String? = hiringJobName /* 채용 직무명 */

    @Column(name = "job_posting_url", length = 500)
    val jobPostingUrl: String? = jobPostingUrl /* 채용공고 링크 */

    @Column(name = "promotion_answer", nullable = false, length = 2000)
    val promotionAnswer: String = promotionAnswer /* 현재 쓰는 채용 홍보 채널과 어려운 점 */

    @Column(name = "marketing_consent", nullable = false)
    val marketingConsent: Boolean = marketingConsent /* 마케팅 정보 수신 동의 */

    @Column(name = "user_id")
    val userId: Long? = userId /* 문의한 회원 식별자. 비회원이면 없다 */

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false, length = 20)
    val memberType: AdvertisementInquiryMemberType = memberType /* 문의 시점의 회원 구분 */

    @Column(name = "utm_source", length = MAX_UTM_LENGTH)
    val utmSource: String? = inflow.utmSource /* 진입 URL의 utm_source */

    @Column(name = "utm_medium", length = MAX_UTM_LENGTH)
    val utmMedium: String? = inflow.utmMedium /* 진입 URL의 utm_medium */

    @Column(name = "utm_campaign", length = MAX_UTM_LENGTH)
    val utmCampaign: String? = inflow.utmCampaign /* 진입 URL의 utm_campaign */

    @Column(name = "utm_content", length = MAX_UTM_LENGTH)
    val utmContent: String? = inflow.utmContent /* 진입 URL의 utm_content */

    @Column(length = MAX_URL_LENGTH)
    val referrer: String? = inflow.referrer /* 진입 시 document.referrer 원문 */

    @Column(name = "entry_host", length = 255)
    val entryHost: String? = inflow.entryHost /* 처음 들어온 호스트(리다이렉트 전) */

    @Column(name = "landing_path", length = MAX_URL_LENGTH)
    val landingPath: String? = inflow.landingPath /* 진입 URL의 경로와 쿼리 */

    @Enumerated(EnumType.STRING)
    @Column(name = "inflow_channel", nullable = false, length = 30)
    val inflowChannel: AdvertisementInquiryInflowChannel = inflow.channel /* 저장할 때 판정한 유입 경로 */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AdvertisementInquiryStatus = AdvertisementInquiryStatus.RECEIVED /* 처리 상태 */
        protected set

    @Column(length = MAX_MEMO_LENGTH)
    var memo: String? = null /* 영업 담당자 메모 */
        protected set

    /** 영업이 잘못 바꾼 상태를 되돌릴 수 있도록 어느 상태로든 바꾼다. 같은 상태면 그대로 둔다. */
    fun changeStatus(status: AdvertisementInquiryStatus) {
        this.status = status
    }

    /** 메모를 통째로 바꾼다. 공백만 있으면 메모를 지운 것으로 본다. */
    fun changeMemo(memo: String?) {
        require((memo?.length ?: 0) <= MAX_MEMO_LENGTH) { "메모는 ${MAX_MEMO_LENGTH}자 이하여야 합니다." }
        this.memo = memo?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val MAX_MEMO_LENGTH = 2000
        const val MAX_UTM_LENGTH = 100
        const val MAX_URL_LENGTH = 1000
    }
}

/** 페이지에 처음 들어온 순간의 원본 값과 그 값으로 판정한 유입 경로다. */
data class AdvertisementInquiryInflow(
    val utmSource: String?,
    val utmMedium: String?,
    val utmCampaign: String?,
    val utmContent: String?,
    val referrer: String?,
    val entryHost: String?,
    val landingPath: String?,
    val channel: AdvertisementInquiryInflowChannel,
)
