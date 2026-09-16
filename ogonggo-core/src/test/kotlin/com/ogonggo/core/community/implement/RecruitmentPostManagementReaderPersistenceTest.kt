package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.RecruitmentPostApplicationStatus
import com.ogonggo.core.community.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.community.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.persistence.RecruitmentPostApplicationQueryRepository
import com.ogonggo.core.community.persistence.RecruitmentPostJpaRepository
import com.ogonggo.core.community.persistence.RecruitmentPostQueryRepository
import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.persistence.UserJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(
    RecruitmentPostManagementReader::class,
    RecruitmentPostQueryRepository::class,
    RecruitmentPostApplicationManager::class,
    RecruitmentPostApplicationReader::class,
    RecruitmentPostApplicationQueryRepository::class,
)
internal class RecruitmentPostManagementReaderPersistenceTest @Autowired constructor(
    private val managementReader: RecruitmentPostManagementReader,
    private val applicationManager: RecruitmentPostApplicationManager,
    private val applicationReader: RecruitmentPostApplicationReader,
    private val postRepository: RecruitmentPostJpaRepository,
    private val userRepository: UserJpaRepository,
) {

    @Test
    fun `작성자의 삭제되지 않은 임시저장 공개 비공개 글만 최신 저장순으로 조회한다`() {
        val ownerId = appendUser()
        val otherOwnerId = appendUser()
        val publishedId = checkNotNull(appendPost(ownerId, title = "공개 글").id)
        val hiddenId = checkNotNull(appendPost(ownerId, title = "비공개 글", publicationStatus = PublicationStatus.HIDDEN).id)
        val draftId = checkNotNull(appendDraft(ownerId, title = "임시저장 글").id)
        appendPost(ownerId, title = "삭제 글").also {
            it.delete(NOW)
            postRepository.saveAndFlush(it)
        }
        appendPost(otherOwnerId, title = "다른 작성자 글")

        val result = managementReader.readPage(
            ownerUserId = ownerId,
            status = RecruitmentPostManagementStatus.ALL,
            recruitmentStatus = null,
            applicationStatus = null,
            recruitmentType = null,
            keyword = null,
            page = 0,
            size = 10,
            sort = RecruitmentPostManagementSortType.LATEST_SAVED,
        )

        assertEquals(listOf(draftId, hiddenId, publishedId), result.posts.map { it.id })
        assertEquals(3L, result.totalElements)
    }

    @Test
    fun `모집 상태 지원 이력 유형 제목 필터는 임시저장 글을 제외하고 적용한다`() {
        val ownerId = appendUser()
        val applicantId = appendUser()
        val recruitingId = checkNotNull(appendPost(ownerId, title = "Kotlin 사이드 프로젝트").id)
        val closedId = checkNotNull(
            appendPost(
                ownerId,
                title = "Java 스터디",
                recruitmentType = RecruitmentType.STUDY,
                recruitmentStatus = RecruitmentStatus.CLOSED,
                closedAt = NOW,
            ).id,
        )
        appendDraft(ownerId, title = "Kotlin 임시저장")

        applicationManager.recordClick(recruitingId, applicantId, NOW)
        applicationManager.recordClick(recruitingId, applicantId, NOW.plusMinutes(1))

        val hasApplications = managementReader.readPage(
            ownerUserId = ownerId,
            status = RecruitmentPostManagementStatus.ALL,
            recruitmentStatus = RecruitmentStatus.RECRUITING,
            applicationStatus = RecruitmentPostApplicationStatus.HAS_APPLICATIONS,
            recruitmentType = RecruitmentType.SIDE_PROJECT,
            keyword = "kotlin",
            page = 0,
            size = 10,
            sort = RecruitmentPostManagementSortType.LATEST_SAVED,
        )
        assertEquals(listOf(recruitingId), hasApplications.posts.map { it.id })

        val closedWithoutApplications = managementReader.readPage(
            ownerUserId = ownerId,
            status = RecruitmentPostManagementStatus.ALL,
            recruitmentStatus = RecruitmentStatus.CLOSED,
            applicationStatus = RecruitmentPostApplicationStatus.NO_APPLICATIONS,
            recruitmentType = RecruitmentType.STUDY,
            keyword = "JAVA",
            page = 0,
            size = 10,
            sort = RecruitmentPostManagementSortType.LATEST_SAVED,
        )
        assertEquals(listOf(closedId), closedWithoutApplications.posts.map { it.id })
        assertEquals(mapOf(recruitingId to 1L), applicationReader.countByPostIds(listOf(recruitingId)))
    }

    @Test
    fun `상태 필터로 임시저장 글만 조회할 수 있고 모집 상태 필터에서는 제외된다`() {
        val ownerId = appendUser()
        val draftId = checkNotNull(appendDraft(ownerId, title = "작성 중").id)

        val drafts = managementReader.readPage(
            ownerUserId = ownerId,
            status = RecruitmentPostManagementStatus.DRAFT,
            recruitmentStatus = null,
            applicationStatus = null,
            recruitmentType = null,
            keyword = null,
            page = 0,
            size = 10,
            sort = RecruitmentPostManagementSortType.LATEST_SAVED,
        )
        val draftsWithRecruitmentFilter = managementReader.readPage(
            ownerUserId = ownerId,
            status = RecruitmentPostManagementStatus.ALL,
            recruitmentStatus = RecruitmentStatus.RECRUITING,
            applicationStatus = null,
            recruitmentType = null,
            keyword = null,
            page = 0,
            size = 10,
            sort = RecruitmentPostManagementSortType.LATEST_SAVED,
        )

        assertEquals(listOf(draftId), drafts.posts.map { it.id })
        assertTrue(draftsWithRecruitmentFilter.posts.isEmpty())
    }

    private fun appendUser(): Long = checkNotNull(
        userRepository.saveAndFlush(
            User.ofLetsCareer(
                letsCareerUserId = nextLetsCareerUserId++,
                joinedAt = NOW,
            ),
        ).id,
    )

    private fun appendDraft(userId: Long, title: String): RecruitmentPost = postRepository.saveAndFlush(
        RecruitmentPost(
            authorUserId = userId,
            title = title,
            recruitmentType = null,
            capacity = null,
            progressMethod = null,
            activityDurationMonths = null,
            technologyStacks = emptyList(),
            summary = null,
            content = null,
            eligibilityAndSelectionProcess = null,
            recruitmentStartDate = null,
            recruitmentEndDate = null,
            positions = emptyList(),
            contactMethod = null,
            contactValue = null,
            publicationStatus = PublicationStatus.DRAFT,
        ),
    )

    private fun appendPost(
        userId: Long,
        title: String,
        publicationStatus: PublicationStatus = PublicationStatus.PUBLISHED,
        recruitmentStatus: RecruitmentStatus = RecruitmentStatus.RECRUITING,
        recruitmentType: RecruitmentType = RecruitmentType.SIDE_PROJECT,
        closedAt: LocalDateTime? = null,
    ): RecruitmentPost = postRepository.saveAndFlush(
        RecruitmentPost(
            authorUserId = userId,
            title = title,
            recruitmentType = recruitmentType,
            capacity = 4,
            progressMethod = ProgressMethod.ONLINE,
            activityDurationMonths = 3,
            technologyStacks = listOf("Kotlin"),
            summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
            content = "{\"root\":{\"children\":[]}}",
            eligibilityAndSelectionProcess = null,
            recruitmentStartDate = LocalDate.of(2026, 9, 1),
            recruitmentEndDate = LocalDate.of(2026, 9, 30),
            positions = listOf(RecruitmentPosition.BACKEND),
            contactMethod = ContactMethod.EMAIL,
            contactValue = "team@example.com",
            publicationStatus = publicationStatus,
            recruitmentStatus = recruitmentStatus,
            closedAt = closedAt,
        ),
    )

    companion object {
        private var nextLetsCareerUserId = 5100L
        private val NOW = LocalDateTime.of(2026, 9, 16, 9, 0)
    }
}
