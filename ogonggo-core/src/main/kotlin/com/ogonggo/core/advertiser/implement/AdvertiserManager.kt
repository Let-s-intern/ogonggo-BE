package com.ogonggo.core.advertiser.implement

import com.ogonggo.core.advertiser.domain.Advertiser
import com.ogonggo.core.advertiser.domain.AdvertiserContact
import com.ogonggo.core.advertiser.domain.AdvertiserType
import com.ogonggo.core.advertiser.implement.dto.AdvertiserUpdateDto
import com.ogonggo.core.advertiser.persistence.AdvertiserJpaRepository
import com.ogonggo.core.advertisementinquiry.implement.AdvertisementInquiryReader
import com.ogonggo.core.user.implement.UserManagementReader
import org.springframework.stereotype.Component

@Component
class AdvertiserManager internal constructor(
    private val advertiserRepository: AdvertiserJpaRepository,
    private val userManagementReader: UserManagementReader,
    private val advertisementInquiryReader: AdvertisementInquiryReader,
) {

    /** 넘어온 칸만 바꾼다. 비회원 광고주만 바꿀 수 있다. */
    fun update(advertiser: Advertiser, dto: AdvertiserUpdateDto) {
        advertiser.edit(
            name = dto.name,
            contactName = dto.contactName,
            contactEmail = dto.contactEmail,
            contactPhone = dto.contactPhone,
        )
        advertiserRepository.save(advertiser)
    }

    /**
     * 비즈니스 회원과 광고 문의 기업을 광고주로 맞춘다. 광고주 목록을 볼 때마다 호출한 서비스의 트랜잭션 안에서 부른다.
     *
     * - 탈퇴하지 않은 비즈니스 회원마다 광고주 행을 하나 두고, 이름·담당자를 회원 정보로 맞춘다. 기업 정보가 없는 회원은 이름이
     *   없어 건너뛴다.
     * - 아직 옮기지 않은 광고 문의(반려·기업 회원 문의 제외)를 회사 이름(공백·대소문자 무시)으로 묶어 비회원 광고주에 잇고,
     *   같은 이름의 비회원 광고주가 없으면 그 문의로 만든다. 이미 있는 광고주의 이름·담당자는 운영자가 고쳤을 수 있어 바꾸지 않는다.
     */
    fun syncSources() {
        syncMembers()
        syncInquiries()
    }

    private fun syncMembers() {
        val advertisers = advertiserRepository.findAllByType(AdvertiserType.MEMBER).associateBy { it.companyMemberUserId }
        userManagementReader.readCompanyMembersNotWithdrawn().forEach { member ->
            val profile = member.companyProfile ?: return@forEach
            val contact = AdvertiserContact(
                name = profile.managerName,
                email = profile.notificationEmail ?: member.email,
                phone = profile.managerPhone,
            )
            val advertiser = advertisers[member.userId]
            if (advertiser == null) {
                advertiserRepository.save(Advertiser.member(member.userId, profile.organizationName, contact))
            } else {
                advertiser.followMember(profile.organizationName, contact)
            }
        }
    }

    private fun syncInquiries() {
        val inquiries = advertisementInquiryReader.readAdvertiserCandidatesAfter(advertiserRepository.findMaxAdInquiryId() ?: 0)
        if (inquiries.isEmpty()) return
        val advertisers = advertiserRepository.findAllByType(AdvertiserType.NON_MEMBER).associateByTo(mutableMapOf()) { it.nameKey }
        inquiries.forEach { inquiry ->
            val inquiryId = checkNotNull(inquiry.id) { "광고 문의 식별자가 없습니다." }
            val name = inquiry.companyName.trim()
            val nameKey = Advertiser.nameKeyOf(name)
            val advertiser = advertisers[nameKey]
            if (advertiser == null) {
                val contact = AdvertiserContact(name = inquiry.managerName, email = inquiry.email, phone = inquiry.phoneNumber)
                advertisers[nameKey] = advertiserRepository.save(Advertiser.fromInquiry(inquiryId, name, contact))
            } else {
                advertiser.linkInquiry(inquiryId)
            }
        }
    }
}
