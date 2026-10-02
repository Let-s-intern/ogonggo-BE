package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.ApplicationMethod
import com.ogonggo.core.bootcamp.domain.BootcampContentField
import com.ogonggo.core.bootcamp.domain.BootcampManagementSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentType
import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.domain.OperationType
import com.ogonggo.core.bootcamp.domain.TuitionType
import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.bootcamp.implement.dto.BootcampAppendDto
import com.ogonggo.core.bootcamp.implement.dto.BootcampContentEditDto
import com.ogonggo.core.bootcamp.persistence.BootcampQueryRepository
import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import com.ogonggo.core.review.error.ReviewErrorCode
import com.ogonggo.core.review.implement.ContentRejectionManager
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    BootcampReader::class,
    BootcampQueryRepository::class,
    BootcampAppender::class,
    BootcampManager::class,
    BootcampBookmarkManager::class,
    BootcampBookmarkReader::class,
    ContentRejectionManager::class,
)
internal class BootcampManagementPersistenceTest @Autowired constructor(
    private val bootcampReader: BootcampReader,
    private val bootcampAppender: BootcampAppender,
    private val bootcampManager: BootcampManager,
    private val bootcampBookmarkManager: BootcampBookmarkManager,
    private val bootcampBookmarkReader: BootcampBookmarkReader,
) {

    @Test
    fun `기업회원 부트캠프는 모집 중이어도 승인되기 전까지 사용자에게 보이지 않는다`() {
        val bootcamp = bootcampAppender.append(command(ownerUserId = OWNER_ID, status = BootcampStatus.RECRUITING))
        val bootcampId = checkNotNull(bootcamp.id)
        bootcampBookmarkManager.append(USER_ID, bootcampId, NOW)

        assertThrows(EntityNotFoundException::class.java) { bootcampReader.readPublic(bootcampId, NOW) }
        assertEquals(0L, publicPage().totalElements)
        assertEquals(0L, bootcampBookmarkReader.readBookmarkedPublicPage(USER_ID, BootcampSearchCondition.NONE, 0, 10, NOW).totalElements)
        val exception = assertThrows(ConflictException::class.java) { bootcampManager.publish(bootcamp) }
        assertEquals(ReviewErrorCode.REVIEW_NOT_APPROVED, exception.errorCode)

        bootcampManager.approveReview(bootcamp, NOW)

        assertEquals(bootcampId, bootcampReader.readPublic(bootcampId, NOW).id)
        assertEquals(listOf(bootcampId), publicPage().bootcamps.map { it.id })
        assertEquals(1L, bootcampBookmarkReader.readBookmarkedPublicPage(USER_ID, BootcampSearchCondition.NONE, 0, 10, NOW).totalElements)

        bootcampManager.hide(bootcamp)
        assertEquals(0L, publicPage().totalElements)
    }

    @Test
    fun `관리 목록은 공개 여부와 무관하게 미삭제 부트캠프를 읽고 필터를 AND로 묶는다`() {
        val crawledRecruiting = bootcampAppender.append(
            command(status = BootcampStatus.RECRUITING, publicationStatus = BootcampPublicationStatus.PUBLISHED),
        )
        val companyPending = bootcampAppender.append(command(ownerUserId = OWNER_ID, status = BootcampStatus.RECRUITING))
        val companyClosed = bootcampAppender.append(
            command(ownerUserId = OWNER_ID, status = BootcampStatus.CLOSED, closedAt = NOW),
        )
        bootcampManager.approveReview(companyClosed, NOW)
        val deleted = bootcampAppender.append(command())
        bootcampManager.delete(deleted, NOW)

        assertEquals(
            listOf(companyClosed.id, companyPending.id, crawledRecruiting.id),
            ids(BootcampManagementSearchCondition.NONE),
        )
        assertEquals(
            listOf(companyClosed.id, crawledRecruiting.id),
            ids(BootcampManagementSearchCondition(published = true)),
        )
        assertEquals(listOf(crawledRecruiting.id), ids(BootcampManagementSearchCondition(source = ContentSource.CRAWLER)))
        assertEquals(
            listOf(companyPending.id),
            ids(BootcampManagementSearchCondition(reviewStatus = ReviewStatus.PENDING)),
        )
        assertEquals(
            listOf(companyPending.id),
            ids(BootcampManagementSearchCondition(source = ContentSource.COMPANY, status = BootcampStatus.RECRUITING)),
        )
    }

    @Test
    fun `운영자는 제목과 본문 칸만 고치고 null이면 비운다`() {
        val bootcamp = bootcampAppender.append(command(eligibilityAndSelectionProcess = "서류 전형"))

        bootcampManager.editContent(
            bootcamp,
            BootcampContentEditDto(
                title = "고친 과정명",
                contents = mapOf(
                    BootcampContentField.CONTENT to "고친 상세 내용",
                    BootcampContentField.ELIGIBILITY_AND_SELECTION_PROCESS to null,
                ),
            ),
        )

        val saved = bootcampReader.read(checkNotNull(bootcamp.id))
        assertEquals("고친 과정명", saved.title)
        assertEquals("고친 상세 내용", saved.content)
        assertNull(saved.eligibilityAndSelectionProcess)
        assertThrows(IllegalArgumentException::class.java) {
            bootcampManager.editContent(
                bootcamp,
                BootcampContentEditDto(contents = mapOf(BootcampContentField.CONTENT to null)),
            )
        }
    }

    @Test
    fun `여러 건 잠금 조회는 같은 식별자를 한 번만 읽고 없거나 삭제된 부트캠프가 섞이면 그 식별자를 알려 거절한다`() {
        val first = checkNotNull(bootcampAppender.append(command()).id)
        val second = checkNotNull(bootcampAppender.append(command()).id)
        val deleted = bootcampAppender.append(command())
        bootcampManager.delete(deleted, NOW)

        assertEquals(
            listOf(first, second),
            bootcampReader.readAllForUpdate(listOf(second, first, second)).map { it.id },
        )
        listOf(checkNotNull(deleted.id), 999_999L).forEach { invalidBootcampId ->
            val exception = assertThrows(EntityNotFoundException::class.java) {
                bootcampReader.readAllForUpdate(listOf(first, invalidBootcampId))
            }
            assertEquals(BootcampErrorCode.BOOTCAMP_NOT_FOUND, exception.errorCode)
            assertEquals("부트캠프를 찾을 수 없습니다. (id: $invalidBootcampId)", exception.message)
        }
    }

    @Test
    fun `모집 종료 일시가 지난 모집 중 부트캠프만 자동 마감하고 종료 일시와 같은 시각은 남긴다`() {
        val expired = bootcampAppender.append(periodCommand(recruitmentEndAt = NOW.minusSeconds(1)))
        val endsNow = bootcampAppender.append(periodCommand(recruitmentEndAt = NOW))
        val alwaysOpen = bootcampAppender.append(command(status = BootcampStatus.RECRUITING))
        val alreadyClosed = bootcampAppender.append(
            periodCommand(recruitmentEndAt = NOW.minusDays(1), status = BootcampStatus.CLOSED, closedAt = CLOSED_AT),
        )
        val draft = bootcampAppender.append(periodCommand(recruitmentEndAt = NOW.minusDays(1), status = BootcampStatus.DRAFT))
        val deleted = bootcampAppender.append(periodCommand(recruitmentEndAt = NOW.minusDays(1)))
        bootcampManager.delete(deleted, NOW)

        val closedCount = bootcampManager.closeExpired(NOW)

        assertEquals(1, closedCount)
        bootcampReader.read(checkNotNull(expired.id)).let {
            assertEquals(BootcampStatus.CLOSED, it.status)
            assertEquals(NOW, it.closedAt)
        }
        assertEquals(BootcampStatus.RECRUITING, bootcampReader.read(checkNotNull(endsNow.id)).status)
        assertEquals(BootcampStatus.RECRUITING, bootcampReader.read(checkNotNull(alwaysOpen.id)).status)
        assertEquals(CLOSED_AT, bootcampReader.read(checkNotNull(alreadyClosed.id)).closedAt)
        assertEquals(BootcampStatus.DRAFT, bootcampReader.read(checkNotNull(draft.id)).status)
    }

    private fun periodCommand(
        recruitmentEndAt: LocalDateTime,
        status: BootcampStatus = BootcampStatus.RECRUITING,
        closedAt: LocalDateTime? = null,
    ): BootcampAppendDto = command(status = status, closedAt = closedAt).copy(
        recruitmentType = BootcampRecruitmentType.PERIOD,
        recruitmentStartAt = recruitmentEndAt.minusDays(30),
        recruitmentEndAt = recruitmentEndAt,
    )

    private fun publicPage() =
        bootcampReader.readPublicPage(BootcampSearchCondition.NONE, BootcampSortType.LATEST, 0, 10, NOW)

    private fun ids(condition: BootcampManagementSearchCondition): List<Long?> =
        bootcampReader.readManagementPage(condition, BootcampSortType.LATEST, 0, 20).bootcamps.map { it.id }

    private fun command(
        ownerUserId: Long? = null,
        status: BootcampStatus = BootcampStatus.DRAFT,
        closedAt: LocalDateTime? = null,
        publicationStatus: BootcampPublicationStatus = BootcampPublicationStatus.DRAFT,
        eligibilityAndSelectionProcess: String? = null,
    ): BootcampAppendDto = BootcampAppendDto(
        ownerUserId = ownerUserId,
        companyName = "오공고 교육사",
        title = "백엔드 부트캠프",
        programType = "개발",
        operationType = OperationType.ONLINE,
        recruitmentType = BootcampRecruitmentType.ALWAYS_OPEN,
        programStartDate = LocalDate.of(2026, 10, 1),
        programEndDate = LocalDate.of(2026, 12, 1),
        tuitionType = TuitionType.FREE,
        representativeImageUrl = "https://example.com/images/bootcamp.png",
        shortDescription = "백엔드 개발자로 성장하는 12주",
        content = "부트캠프 상세 내용",
        eligibilityAndSelectionProcess = eligibilityAndSelectionProcess,
        applicationMethod = ApplicationMethod.EXTERNAL_PAGE,
        applicationUrl = "https://example.com/apply",
        status = status,
        closedAt = closedAt,
        publicationStatus = publicationStatus,
    )

    companion object {
        private const val OWNER_ID = 7L
        private const val USER_ID = 11L
        private val NOW = LocalDateTime.of(2026, 9, 14, 12, 0)
        private val CLOSED_AT = LocalDateTime.of(2026, 9, 1, 0, 0)
    }
}
