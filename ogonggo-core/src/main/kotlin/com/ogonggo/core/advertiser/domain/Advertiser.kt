package com.ogonggo.core.advertiser.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * 배너·광고 공고를 맡긴 기업이다. 배너·광고 공고·광고 계약·광고주 메모가 이 식별자로 광고주를 가리킨다.
 *
 * 세 길로 들어온다. 비즈니스 회원은 회원마다 한 행이고 이름과 담당자를 회원 정보로 맞춘다. 광고 문의로 들어온 비회원 기업은
 * 회사 이름마다 한 행이고 처음 들어온 문의의 담당자로 만든다. 운영자가 직접 더한 기업은 운영자가 고친다.
 * 같은 기업이 비회원으로 들어왔다가 회원이 되어도 두 행을 합치지 않는다.
 *
 * 비회원 광고주끼리는 공백·대소문자를 뺀 이름(`nameKey`)이 겹치지 않는다. 회원 광고주는 한 회사에 담당자 계정이 여럿일 수 있어
 * 이름이 겹칠 수 있으므로 `nameKey`를 두지 않는다. 지우는 기능이 없어 소프트 삭제 칼럼을 두지 않는다.
 */
@Entity
@Table(
    name = "advertisers",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_advertisers_company_member_user_id", columnNames = ["company_member_user_id"]),
        UniqueConstraint(name = "uk_advertisers_name_key", columnNames = ["name_key"]),
    ],
)
class Advertiser private constructor(
    source: AdvertiserSource,
    name: String,
    companyMemberUserId: Long?,
    adInquiryId: Long?,
    contactName: String?,
    contactEmail: String?,
    contactPhone: String?,
) : BaseTimeEntity() {

    init {
        validateName(name)
        require((source == AdvertiserSource.COMPANY_MEMBER) == (companyMemberUserId != null)) {
            "회원 광고주만 비즈니스 회원 식별자를 가집니다."
        }
        require(companyMemberUserId == null || companyMemberUserId > 0) { "비즈니스 회원 식별자는 양수여야 합니다." }
        require(adInquiryId == null || adInquiryId > 0) { "광고 문의 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 광고주 식별자 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: AdvertiserType = source.type /* 회원 여부. 들어온 길이 정한다 */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val source: AdvertiserSource = source /* 들어온 길 */

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    var name: String = name /* 기업 이름. 회원은 기관명 */
        protected set

    @Column(name = "name_key", length = MAX_NAME_LENGTH)
    var nameKey: String? = if (source.type == AdvertiserType.NON_MEMBER) nameKeyOf(name) else null /* 비회원 광고주 이름 비교 값 */
        protected set

    @Column(name = "company_member_user_id")
    val companyMemberUserId: Long? = companyMemberUserId /* 비즈니스 회원의 사용자 식별자 */

    @Column(name = "ad_inquiry_id")
    var adInquiryId: Long? = adInquiryId /* 이 기업이 남긴 가장 최근 광고 문의 식별자 */
        protected set

    @Column(name = "contact_name", length = MAX_CONTACT_NAME_LENGTH)
    var contactName: String? = contactName /* 담당자 이름 */
        protected set

    @Column(name = "contact_email", length = MAX_CONTACT_EMAIL_LENGTH)
    var contactEmail: String? = contactEmail /* 담당자 이메일 */
        protected set

    @Column(name = "contact_phone", length = MAX_CONTACT_PHONE_LENGTH)
    var contactPhone: String? = contactPhone /* 담당자 연락처 */
        protected set

    /** 회원 광고주의 이름과 담당자를 비즈니스 회원 정보로 맞춘다. 같으면 그대로 둔다. */
    fun followMember(name: String, contact: AdvertiserContact) {
        check(type == AdvertiserType.MEMBER) { "비회원 광고주는 회원 정보를 따르지 않습니다." }
        validateName(name)
        if (this.name != name) this.name = name
        if (contactName != contact.name) contactName = contact.name
        if (contactEmail != contact.email) contactEmail = contact.email
        if (contactPhone != contact.phone) contactPhone = contact.phone
    }

    /** 더 최근 광고 문의를 잇는다. 예전 문의는 무시한다. */
    fun linkInquiry(inquiryId: Long) {
        require(inquiryId > 0) { "광고 문의 식별자는 양수여야 합니다." }
        if ((adInquiryId ?: 0) < inquiryId) adInquiryId = inquiryId
    }

    /** 비회원 광고주의 칸을 바꾼다. null이면 그대로 두고 빈 문자열이면 지운다. */
    fun edit(name: String?, contactName: String?, contactEmail: String?, contactPhone: String?) {
        check(type == AdvertiserType.NON_MEMBER) { "회원 광고주는 비즈니스 회원 정보를 따릅니다." }
        AdvertiserContact(contactName, contactEmail, contactPhone)
        name?.let {
            validateName(it)
            this.name = it
            nameKey = nameKeyOf(it)
        }
        contactName?.let { this.contactName = it.ifBlank { null } }
        contactEmail?.let { this.contactEmail = it.ifBlank { null } }
        contactPhone?.let { this.contactPhone = it.ifBlank { null } }
    }

    companion object {
        /** 회원 기관명(150자)까지 담는다. 운영자가 직접 적는 이름은 관리자 API가 더 짧게 받는다. */
        const val MAX_NAME_LENGTH = 150
        const val MAX_CONTACT_NAME_LENGTH = 100
        const val MAX_CONTACT_EMAIL_LENGTH = 320
        const val MAX_CONTACT_PHONE_LENGTH = 30

        fun member(userId: Long, name: String, contact: AdvertiserContact): Advertiser = Advertiser(
            source = AdvertiserSource.COMPANY_MEMBER,
            name = name,
            companyMemberUserId = userId,
            adInquiryId = null,
            contactName = contact.name,
            contactEmail = contact.email,
            contactPhone = contact.phone,
        )

        fun fromInquiry(inquiryId: Long, name: String, contact: AdvertiserContact): Advertiser = Advertiser(
            source = AdvertiserSource.AD_INQUIRY,
            name = name,
            companyMemberUserId = null,
            adInquiryId = inquiryId,
            contactName = contact.name,
            contactEmail = contact.email,
            contactPhone = contact.phone,
        )

        fun manual(name: String, contact: AdvertiserContact): Advertiser = Advertiser(
            source = AdvertiserSource.MANUAL,
            name = name,
            companyMemberUserId = null,
            adInquiryId = null,
            contactName = contact.name,
            contactEmail = contact.email,
            contactPhone = contact.phone,
        )

        /** 같은 이름인지 볼 때 쓴다. 공백을 모두 빼고 소문자로 바꾼다. */
        fun nameKeyOf(name: String): String = name.replace(WHITESPACE, "").lowercase()

        private val WHITESPACE = Regex("\\s+")
    }
}

/** 광고주 담당자다. 비어 있는 칸은 null이다. */
data class AdvertiserContact(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
) {
    init {
        require((name?.length ?: 0) <= Advertiser.MAX_CONTACT_NAME_LENGTH) { "담당자 이름이 너무 깁니다." }
        require((email?.length ?: 0) <= Advertiser.MAX_CONTACT_EMAIL_LENGTH) { "담당자 이메일이 너무 깁니다." }
        require((phone?.length ?: 0) <= Advertiser.MAX_CONTACT_PHONE_LENGTH) { "담당자 연락처가 너무 깁니다." }
    }
}

private fun validateName(name: String) {
    require(name.isNotBlank() && name.length <= Advertiser.MAX_NAME_LENGTH) {
        "광고주 이름은 1자 이상 ${Advertiser.MAX_NAME_LENGTH}자 이하여야 합니다."
    }
}
