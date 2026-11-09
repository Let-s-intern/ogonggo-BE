package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampManagementSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampContentEditDto
import com.ogonggo.core.contentreview.domain.ContentReviewStatus
import com.ogonggo.core.image.implement.ImageAssetManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdminBootcampService(
    private val bootcampReader: BootcampReader,
    private val bootcampManager: BootcampManager,
    private val bootcampMetricReader: BootcampMetricReader,
    private val bootcampContentReader: BootcampContentReader,
    private val imageAssetManager: ImageAssetManager,
    private val clock: Clock,
) {

    fun getBootcamps(
        condition: BootcampManagementSearchCondition,
        sortType: BootcampSortType,
        page: Int,
        size: Int,
    ): AdminBootcampPageResult {
        val result = bootcampReader.readManagementPage(condition, sortType, page, size)
        return AdminBootcampPageResult.from(
            result = result,
            metrics = bootcampMetricReader.readAll(result.bootcamps.map { it.requiredId() }),
        )
    }

    fun getBootcamp(bootcampId: Long): AdminBootcampResult {
        val bootcamp = bootcampReader.read(bootcampId)
        return AdminBootcampResult.from(
            bootcamp = bootcamp,
            metric = bootcampMetricReader.read(bootcampId),
            partners = bootcampContentReader.readPartners(bootcampId),
            curriculums = bootcampContentReader.readCurriculums(bootcampId),
            images = bootcampContentReader.readImages(bootcampId),
        )
    }

    /**
     * 채용공고와 같은 순서로 반영한다. 검수 상태를 먼저 바꾸고 노출을 바꾸며, 같은 검수 상태로는 다시 전이하지 않는다.
     */
    @Transactional
    fun updateBootcamp(bootcampId: Long, command: AdminBootcampUpdateCommand) {
        val now = LocalDateTime.now(clock)
        val bootcamp = bootcampReader.readForUpdate(bootcampId)

        command.reviewStatus
            ?.takeIf { it != bootcamp.reviewStatus }
            ?.let { changeReview(bootcamp, it, now) }
        command.visibility?.let { changeVisibility(bootcamp, it) }
        command.priorityExposed
            ?.takeIf { it != bootcamp.priorityExposed }
            ?.let { bootcampManager.changePriorityExposed(bootcamp, it) }
        if (command.title != null || command.contents.isNotEmpty()) {
            bootcampManager.editContent(
                bootcamp,
                BootcampContentEditDto(title = command.title, contents = command.contents),
            )
        }
    }

    /**
     * 모든 내용과 모집 상태를 바꾼다. 검수와 노출은 그대로 둔다.
     * 이미지는 대리 등록처럼 회원 소유와 운영자 소유를 모두 첨부한다. 수집 부트캠프는 회원이 없어 운영자 소유만 첨부한다.
     */
    @Transactional
    fun replaceBootcamp(bootcampId: Long, command: AdminBootcampReplaceCommand, adminUserId: Long) {
        val bootcamp = bootcampReader.readForUpdate(bootcampId)
        bootcampManager.update(bootcamp, command.bootcamp)
        changeStatus(bootcamp, command.status)

        val imageUrls = listOf(command.bootcamp.logoUrl, command.bootcamp.representativeImageUrl)
        bootcamp.ownerUserId?.let { imageAssetManager.attachImageUrls(it, imageUrls) }
        imageAssetManager.attachImageUrls(adminUserId, imageUrls)
    }

    /**
     * 채용공고 일괄 노출 변경과 같이 한 트랜잭션에서 모두 바꾸고, 하나라도 실패하면 아무것도 바꾸지 않는다.
     * 이미 같은 노출인 부트캠프는 건드리지 않는다.
     */
    @Transactional
    fun changeVisibilities(command: AdminBootcampVisibilityChangeCommand) {
        bootcampReader.readAllForUpdate(command.bootcampIds)
            .filter { bootcamp -> bootcamp.visibility() != command.visibility }
            .forEach { bootcamp -> changeVisibility(bootcamp, command.visibility) }
    }

    /** 반려 기록은 지우지 않는다. 반려 보관에서 "반려하고 지웠다"는 기록으로 남는다. */
    @Transactional
    fun deleteBootcamp(bootcampId: Long) {
        bootcampManager.delete(bootcampReader.readForDelete(bootcampId), LocalDateTime.now(clock))
    }

    private fun Bootcamp.visibility(): AdminContentVisibility =
        AdminContentVisibility.of(publicationStatus == BootcampPublicationStatus.PUBLISHED)

    private fun changeVisibility(bootcamp: Bootcamp, visibility: AdminContentVisibility) {
        when (visibility) {
            AdminContentVisibility.VISIBLE -> bootcampManager.publish(bootcamp)
            AdminContentVisibility.HIDDEN -> bootcampManager.hide(bootcamp)
        }
    }

    /** 마감은 마감 일시가 함께 있어야 하므로 상태를 직접 넣지 않고 도메인 전이로 바꾼다. 같은 상태면 그대로 둔다. */
    private fun changeStatus(bootcamp: Bootcamp, status: BootcampRecruitmentStatus) {
        if (bootcamp.status == status) {
            return
        }
        when (status) {
            BootcampRecruitmentStatus.RECRUITING -> bootcampManager.startRecruitment(bootcamp)
            BootcampRecruitmentStatus.CLOSED -> bootcampManager.close(bootcamp, LocalDateTime.now(clock))
            BootcampRecruitmentStatus.DRAFT -> throw IllegalArgumentException("임시저장으로는 바꾸지 않습니다.")
        }
    }

    private fun changeReview(bootcamp: Bootcamp, reviewStatus: ContentReviewStatus, now: LocalDateTime) {
        when (reviewStatus) {
            ContentReviewStatus.APPROVED -> bootcampManager.approveReview(bootcamp, now)
            ContentReviewStatus.PENDING -> bootcampManager.requestReview(bootcamp, now)
            ContentReviewStatus.REJECTED -> throw IllegalArgumentException("반려는 사유와 함께 검수 화면에서 처리합니다.")
        }
    }
}
