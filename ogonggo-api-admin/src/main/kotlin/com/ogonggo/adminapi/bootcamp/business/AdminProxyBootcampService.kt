package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.member.implement.CompanyMemberValidator
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractCompanyPostingAppendDto
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.implement.BootcampAppender
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.image.implement.ImageAssetManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 운영자가 비즈니스 회원 대신 부트캠프를 등록한다(대리 등록). 채용공고 대리 등록([com.ogonggo.adminapi.job.business.AdminProxyJobService])과 같은 규칙이다.
 * 과정은 그 회원의 것이 되어 회원이 직접 등록한 과정처럼 마이페이지에 보이고 고칠 수 있다.
 * 검수 없이 승인으로 등록하며 검수 요청 슬랙 알림은 보내지 않는다.
 */
@Service
class AdminProxyBootcampService(
    private val companyMemberValidator: CompanyMemberValidator,
    private val bootcampAppender: BootcampAppender,
    private val bootcampManager: BootcampManager,
    private val imageAssetManager: ImageAssetManager,
    private val advertisementContractAppender: AdvertisementContractAppender,
    private val clock: Clock,
) {

    /** 마감으로 등록하면 기업회원 등록처럼 지금을 마감 일시로 남긴다. 이미지는 회원 소유와 운영자 소유를 모두 첨부한다. */
    @Transactional
    fun create(memberId: Long, command: AdminProxyBootcampCreateCommand, adminUserId: Long): Long {
        companyMemberValidator.validateProxyRegistrable(memberId)
        val now = LocalDateTime.now(clock)
        val closedAt = if (command.bootcamp.status == BootcampRecruitmentStatus.CLOSED) now else null
        val bootcamp = bootcampAppender.append(command.bootcamp.copy(ownerUserId = memberId, closedAt = closedAt))
        advertisementContractAppender.appendForCompanyPosting(
            AdvertisementContractCompanyPostingAppendDto(
                userId = memberId,
                promotionTarget = AdvertisementContractPromotionTarget.BOOTCAMP,
                targetId = bootcamp.requiredId(),
                companyName = command.bootcamp.companyName,
                recruitmentTitle = command.bootcamp.title,
                recruitmentDeadline = command.bootcamp.recruitmentEndAt?.toLocalDate(),
            ),
        )
        bootcampManager.approveReview(bootcamp, now)
        if (command.visibility == AdminContentVisibility.HIDDEN) {
            bootcampManager.hide(bootcamp)
        }
        bootcampManager.markAdminRegistered(bootcamp)

        val imageUrls = listOf(command.bootcamp.logoUrl, command.bootcamp.representativeImageUrl)
        imageAssetManager.attachImageUrls(memberId, imageUrls)
        imageAssetManager.attachImageUrls(adminUserId, imageUrls)
        return bootcamp.requiredId()
    }
}
