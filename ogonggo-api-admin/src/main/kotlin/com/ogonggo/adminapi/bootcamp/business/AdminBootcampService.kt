package com.ogonggo.adminapi.bootcamp.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampManagementSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.implement.BootcampContentReader
import com.ogonggo.core.bootcamp.implement.BootcampManager
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampContentEditDto
import com.ogonggo.core.review.domain.ReviewStatus
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
        when (command.visibility) {
            AdminContentVisibility.VISIBLE -> bootcampManager.publish(bootcamp)
            AdminContentVisibility.HIDDEN -> bootcampManager.hide(bootcamp)
            null -> Unit
        }
        if (command.title != null || command.contents.isNotEmpty()) {
            bootcampManager.editContent(
                bootcamp,
                BootcampContentEditDto(title = command.title, contents = command.contents),
            )
        }
    }

    /** 반려 기록은 지우지 않는다. 반려 보관에서 "반려하고 지웠다"는 기록으로 남는다. */
    @Transactional
    fun deleteBootcamp(bootcampId: Long) {
        bootcampManager.delete(bootcampReader.readForDelete(bootcampId), LocalDateTime.now(clock))
    }

    private fun changeReview(bootcamp: Bootcamp, reviewStatus: ReviewStatus, now: LocalDateTime) {
        when (reviewStatus) {
            ReviewStatus.APPROVED -> bootcampManager.approveReview(bootcamp, now)
            ReviewStatus.PENDING -> bootcampManager.requestReview(bootcamp, now)
            ReviewStatus.REJECTED -> throw IllegalArgumentException("반려는 사유와 함께 검수 화면에서 처리합니다.")
        }
    }
}
