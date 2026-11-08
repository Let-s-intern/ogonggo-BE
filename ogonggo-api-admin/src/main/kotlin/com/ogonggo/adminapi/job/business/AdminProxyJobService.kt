package com.ogonggo.adminapi.job.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.adminapi.member.implement.CompanyMemberValidator
import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractCompanyPostingAppendDto
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 운영자가 비즈니스 회원 대신 채용공고를 등록한다(대리 등록). 공고는 그 회원의 것이 되어 회원이 직접 등록한 공고처럼
 * 마이페이지에 보이고 고칠 수 있다. 회원이 고치면 평소처럼 다시 검수를 기다린다.
 *
 * 운영자가 올린 공고라 검수를 거치지 않고 승인으로 등록한다. 승인 경로와 같이 처음 승인 일시가 남아 사용자 화면의 등록 일시가 된다.
 * 검수 요청 슬랙 알림은 보내지 않는다. 운영자에게 검수할 공고가 들어왔다고 알리는 것인데 이 공고는 운영자가 올려 이미 승인했다.
 */
@Service
class AdminProxyJobService(
    private val companyMemberValidator: CompanyMemberValidator,
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val imageAssetManager: ImageAssetManager,
    private val advertisementContractAppender: AdvertisementContractAppender,
    private val clock: Clock,
) {

    /**
     * 이미지는 기업회원 등록처럼 첨부해 정리 작업이 지우지 않게 한다. 운영자가 콘솔에서 올린 이미지는 운영자 계정 소유라
     * 회원 소유 이미지(회원의 기업 로고 등)와 운영자 소유 이미지를 모두 첨부한다. 소유자는 바꾸지 않는다.
     */
    @Transactional
    fun create(memberId: Long, command: AdminProxyJobCreateCommand, adminUserId: Long): Long {
        companyMemberValidator.validateProxyRegistrable(memberId)
        val now = LocalDateTime.now(clock)
        val job = jobAppender.append(command.job.copy(ownerUserId = memberId), now)
        advertisementContractAppender.appendForCompanyPosting(
            AdvertisementContractCompanyPostingAppendDto(
                userId = memberId,
                promotionTarget = AdvertisementContractPromotionTarget.JOB,
                targetId = job.requiredId(),
                companyName = command.job.companyName,
                recruitmentTitle = command.job.title,
                recruitmentDeadline = command.job.recruitmentEndAt?.toLocalDate(),
            ),
        )
        jobManager.approveReview(job, now)
        if (command.visibility == AdminContentVisibility.HIDDEN) {
            jobManager.hide(job)
        }
        jobManager.markAdminRegistered(job)

        val imageUrls = listOf(command.job.logoUrl, command.job.coverImageUrl)
        imageAssetManager.attachImageUrls(memberId, imageUrls)
        imageAssetManager.attachImageUrls(adminUserId, imageUrls)
        return job.requiredId()
    }
}
