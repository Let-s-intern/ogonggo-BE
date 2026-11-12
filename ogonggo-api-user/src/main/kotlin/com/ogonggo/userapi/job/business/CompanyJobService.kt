package com.ogonggo.userapi.job.business

import com.ogonggo.core.advertisementcontract.domain.AdvertisementContractPromotionTarget
import com.ogonggo.core.advertisementcontract.implement.AdvertisementContractAppender
import com.ogonggo.core.advertisementcontract.implement.dto.AdvertisementContractCompanyPostingAppendDto
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.job.implement.JobAppender
import com.ogonggo.core.job.implement.JobManager
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.JobSourceManager
import com.ogonggo.core.job.implement.JobSourceReader
import com.ogonggo.core.job.implement.dto.JobAppendDto
import com.ogonggo.core.job.implement.dto.JobUpdateDto
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.userapi.companyposting.business.CompanyPostingRegisteredEvent
import com.ogonggo.userapi.companyposting.business.CompanyPostingType
import com.ogonggo.userapi.job.implement.CompanyJobSourceImages
import com.ogonggo.userapi.user.implement.requireActive
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 기업회원이 자기 채용공고를 등록하고 관리한다.
 *
 * 등록과 게시를 나눈다. 등록은 항상 임시저장으로 끝내고 게시는 별도 요청으로 처리한다.
 * 게시 조건이 지금은 기업회원 자격뿐이지만 앞으로 결제 확인이 추가되므로,
 * 그 판단을 publish 한 곳에 모아 두면 요청 계약을 바꾸지 않고 조건만 늘릴 수 있다.
 */
@Service
class CompanyJobService(
    private val userReader: UserReader,
    private val jobReader: JobReader,
    private val jobAppender: JobAppender,
    private val jobManager: JobManager,
    private val jobSourceReader: JobSourceReader,
    private val jobSourceManager: JobSourceManager,
    private val imageAssetManager: ImageAssetManager,
    private val advertisementContractAppender: AdvertisementContractAppender,
    private val clock: Clock,
    private val eventPublisher: ApplicationEventPublisher,
) {

    /** 비즈니스 공고 등록은 광고 상품 중 하나라 공고마다 광고 계약을 함께 만든다. 계약은 검수 상태를 따른다. */
    @Transactional
    fun create(userId: Long, command: JobAppendDto, sourceImageUrls: List<String>? = null): Long {
        val account = verifyCompany(userId)
        val job = jobAppender.append(command.copy(ownerUserId = userId))
        imageAssetManager.attachImageUrls(userId, listOf(command.logoUrl, command.coverImageUrl))
        if (!sourceImageUrls.isNullOrEmpty()) {
            saveSourceImages(userId, job.requiredId(), sourceImageUrls)
        }
        advertisementContractAppender.appendForCompanyPosting(
            AdvertisementContractCompanyPostingAppendDto(
                userId = userId,
                promotionTarget = AdvertisementContractPromotionTarget.JOB,
                targetId = job.requiredId(),
                companyName = command.companyName,
                recruitmentTitle = command.title,
                recruitmentDeadline = command.recruitmentEndAt?.toLocalDate(),
            ),
        )
        eventPublisher.publishEvent(
            CompanyPostingRegisteredEvent(
                type = CompanyPostingType.JOB,
                postingId = job.requiredId(),
                companyName = command.companyName,
                title = command.title,
                accountEmail = account.email,
                jobField = command.jobField,
                jobRole = command.jobRole,
            ),
        )
        return job.requiredId()
    }

    fun getJobs(userId: Long, page: Int, size: Int): CompanyJobPageResult {
        verifyCompany(userId)
        return CompanyJobPageResult.from(jobReader.readOwnedPage(userId, page, size))
    }

    fun getJob(userId: Long, jobId: Long): CompanyJobResult {
        verifyCompany(userId)
        val job = jobReader.readOwned(userId, jobId)
        return CompanyJobResult.from(job, CompanyJobSourceImages.fromHtml(jobSourceReader.readHtml(job.requiredId())))
    }

    /**
     * 내용을 고치면 다시 검수를 기다리며 그동안 노출하지 않는다. 승인받은 뒤 내용을 바꿔 검수를 우회하지 못하게 한다.
     * 원문 이미지는 `null`이면 그대로 두고(이 칸을 모르는 화면이 보낸 수정), 빈 목록이면 원문을 지우고, 값이 있으면 바꾼다.
     */
    @Transactional
    fun update(userId: Long, jobId: Long, command: JobUpdateDto, sourceImageUrls: List<String>? = null) {
        verifyCompany(userId)
        val job = jobReader.readOwnedForUpdate(userId, jobId)
        val now = LocalDateTime.now(clock)
        jobManager.update(job, command, now)
        imageAssetManager.attachImageUrls(userId, listOf(command.logoUrl, command.coverImageUrl))
        when {
            sourceImageUrls == null -> Unit
            sourceImageUrls.isEmpty() -> jobSourceManager.delete(jobId, now)
            else -> saveSourceImages(userId, jobId, sourceImageUrls)
        }
        jobManager.requestReview(job, now)
    }

    /**
     * 게시 조건을 여기 모은다. 결제가 도입되면 확인 절차가 이 자리에 들어간다.
     * 검수 승인 전이면 도메인이 게시를 막는다. 승인은 곧 노출이므로 이 요청은 운영자가 숨긴 공고를 다시 올릴 때 쓴다.
     */
    @Transactional
    fun publish(userId: Long, jobId: Long) {
        verifyCompany(userId)
        jobManager.publish(jobReader.readOwnedForUpdate(userId, jobId))
    }

    @Transactional
    fun close(userId: Long, jobId: Long) {
        verifyCompany(userId)
        jobManager.close(jobReader.readOwnedForUpdate(userId, jobId), LocalDateTime.now(clock))
    }

    @Transactional
    fun delete(userId: Long, jobId: Long) {
        verifyCompany(userId)
        jobManager.delete(jobReader.readOwnedForDelete(userId, jobId), LocalDateTime.now(clock))
    }

    /** 원문 이미지를 원문 HTML로 저장하고, 오공고에 올린 이미지면 첨부해 정리 작업이 지우지 않게 한다. */
    private fun saveSourceImages(userId: Long, jobId: Long, urls: List<String>) {
        imageAssetManager.attachImageUrls(userId, urls)
        jobSourceManager.save(jobId, CompanyJobSourceImages.toHtml(urls))
    }

    private fun verifyCompany(userId: Long): UserAccountDto {
        val account = userReader.read(userId)
        account.status.requireActive()
        if (account.role != UserRole.COMPANY) {
            throw ForbiddenException(UserErrorCode.COMPANY_ROLE_REQUIRED)
        }
        return account
    }
}
