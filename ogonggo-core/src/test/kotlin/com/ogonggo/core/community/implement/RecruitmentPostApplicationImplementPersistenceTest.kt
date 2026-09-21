package com.ogonggo.core.community.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.RecruitmentApplicationProgressStatus
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.persistence.RecruitmentPostApplicationJpaRepository
import com.ogonggo.core.community.persistence.RecruitmentPostApplicationQueryRepository
import com.ogonggo.core.community.persistence.RecruitmentPostJpaRepository
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.persistence.UserJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
    RecruitmentPostApplicationManager::class,
    RecruitmentPostApplicationReader::class,
    RecruitmentPostApplicationQueryRepository::class,
)
internal class RecruitmentPostApplicationImplementPersistenceTest @Autowired constructor(
    private val applicationManager: RecruitmentPostApplicationManager,
    private val applicationReader: RecruitmentPostApplicationReader,
    private val applicationRepository: RecruitmentPostApplicationJpaRepository,
    private val postRepository: RecruitmentPostJpaRepository,
    private val userRepository: UserJpaRepository,
) {

    @Test
    fun `같은 사용자와 모집글을 다시 기록하면 한 행의 최근 시각만 갱신한다`() {
        val userId = appendUser()
        val postId = checkNotNull(appendPost(userId).id)
        val firstClickedAt = LocalDateTime.of(2026, 9, 16, 9, 0)
        val lastClickedAt = firstClickedAt.plusMinutes(10)

        applicationManager.recordClick(postId, userId, firstClickedAt)
        applicationManager.recordClick(postId, userId, lastClickedAt)
        applicationRepository.flush()

        val application = applicationRepository.findByPostIdAndUserId(postId, userId)
        assertNotNull(application)
        assertEquals(1L, applicationRepository.count())
        assertEquals(firstClickedAt, application?.firstClickedAt)
        assertEquals(lastClickedAt, application?.lastClickedAt)
    }

    @Test
    fun `공개된 모집 중과 마감 모집글만 포함하고 최초 저장순으로 정렬한다`() {
        val userId = appendUser()
        val recruitingPostId = checkNotNull(appendPost(userId, title = "모집 중 Kotlin").id)
        val closedPostId = checkNotNull(appendPost(
            userId = userId,
            title = "마감 Java",
            recruitmentStatus = RecruitmentStatus.CLOSED,
            closedAt = CLICKED_AT,
        ).id)
        val hiddenPostId = checkNotNull(appendPost(
            userId = userId,
            title = "비공개 모집글",
            publicationStatus = PublicationStatus.HIDDEN,
        ).id)
        val deletedPost = appendPost(userId, title = "삭제 모집글")
        deletedPost.delete(CLICKED_AT)
        postRepository.saveAndFlush(deletedPost)

        applicationManager.recordClick(recruitingPostId, userId, CLICKED_AT)
        applicationManager.recordClick(closedPostId, userId, CLICKED_AT.plusMinutes(1))
        applicationManager.recordClick(recruitingPostId, userId, CLICKED_AT.plusMinutes(10))
        applicationManager.recordClick(hiddenPostId, userId, CLICKED_AT.plusMinutes(2))
        applicationManager.recordClick(checkNotNull(deletedPost.id), userId, CLICKED_AT.plusMinutes(3))

        val result = applicationReader.readPage(
            userId = userId,
            recruitmentStatus = null,
            recruitmentType = null,
            keyword = null,
            page = 0,
            size = 10,
        )

        assertEquals(listOf(closedPostId, recruitingPostId), result.items.map { it.postId })

        val keywordResult = applicationReader.readPage(
            userId = userId,
            recruitmentStatus = null,
            recruitmentType = null,
            keyword = "kotlin",
            page = 0,
            size = 10,
        )

        assertEquals(listOf(recruitingPostId), keywordResult.items.map { it.postId })
    }

    @Test
    fun `지원 상태 필터를 목록과 유형별 건수에 함께 적용한다`() {
        val userId = appendUser()
        val preparingPostId = checkNotNull(appendPost(userId, title = "준비 중 모집글").id)
        val completedPostId = checkNotNull(appendPost(userId, title = "지원 완료 모집글").id)

        applicationManager.recordClick(preparingPostId, userId, CLICKED_AT)
        applicationManager.recordClick(completedPostId, userId, CLICKED_AT.plusMinutes(1))
        applicationManager.changeStatus(
            postId = completedPostId,
            userId = userId,
            status = RecruitmentApplicationProgressStatus.COMPLETED,
        )

        val result = applicationReader.readPage(
            userId = userId,
            recruitmentStatus = null,
            recruitmentType = null,
            keyword = null,
            page = 0,
            size = 10,
            applicationStatus = RecruitmentApplicationProgressStatus.COMPLETED,
        )

        assertEquals(listOf(completedPostId), result.items.map { it.postId })
        assertEquals(1L, result.countsByRecruitmentType[RecruitmentType.SIDE_PROJECT])
        assertEquals(0L, result.countsByRecruitmentType[RecruitmentType.STUDY])
    }

    @Test
    fun `지원 상태 변경과 soft delete가 목록과 applicationCount에 반영된다`() {
        val userId = appendUser()
        val postId = checkNotNull(appendPost(userId).id)

        applicationManager.recordClick(postId, userId, CLICKED_AT)
        applicationManager.changeStatus(postId, userId, RecruitmentApplicationProgressStatus.COMPLETED)
        applicationManager.delete(postId, userId, CLICKED_AT.plusMinutes(1))
        applicationRepository.flush()

        val deleted = applicationRepository.findByPostIdAndUserId(postId, userId)
        assertEquals(RecruitmentApplicationProgressStatus.COMPLETED, deleted?.applicationStatus)
        assertNotNull(deleted?.deletedAt)
        assertEquals(emptyList<Long>(), applicationReader.readPage(userId, null, null, null, 0, 10).items.map { it.postId })
        assertEquals(emptyMap<Long, Long>(), applicationReader.countByPostIds(listOf(postId)))
    }

    @Test
    fun `지원 목록의 유형별 건수는 사이드 프로젝트와 스터디만 검색 조건으로 집계한다`() {
        val userId = appendUser()
        val sideProjectId = checkNotNull(
            appendPost(userId, title = "Kotlin 사이드 프로젝트", recruitmentType = RecruitmentType.SIDE_PROJECT).id,
        )
        val studyId = checkNotNull(
            appendPost(userId, title = "Kotlin 스터디", recruitmentType = RecruitmentType.STUDY).id,
        )
        val closedSideProjectId = checkNotNull(
            appendPost(
                userId,
                title = "Kotlin 마감 사이드 프로젝트",
                recruitmentType = RecruitmentType.SIDE_PROJECT,
                recruitmentStatus = RecruitmentStatus.CLOSED,
                closedAt = CLICKED_AT,
            ).id,
        )

        applicationManager.recordClick(sideProjectId, userId, CLICKED_AT)
        applicationManager.recordClick(studyId, userId, CLICKED_AT.plusMinutes(1))
        applicationManager.recordClick(closedSideProjectId, userId, CLICKED_AT.plusMinutes(2))

        val allCounts = applicationReader.readPage(userId, null, null, null, 0, 10).countsByRecruitmentType
        assertEquals(2L, allCounts[RecruitmentType.SIDE_PROJECT])
        assertEquals(1L, allCounts[RecruitmentType.STUDY])

        val recruitingCounts = applicationReader.readPage(
            userId = userId,
            recruitmentStatus = RecruitmentStatus.RECRUITING,
            recruitmentType = RecruitmentType.SIDE_PROJECT,
            keyword = "Kotlin",
            page = 0,
            size = 10,
        ).countsByRecruitmentType
        assertEquals(1L, recruitingCounts[RecruitmentType.SIDE_PROJECT])
        assertEquals(1L, recruitingCounts[RecruitmentType.STUDY])
    }

    @Test
    fun `스크랩에서 옮기면 옮긴 시각으로 지원 준비 중 이력을 만든다`() {
        // given
        val userId = appendUser()
        val postId = checkNotNull(appendPost(userId).id)

        // when
        applicationManager.startPreparation(postId, userId, CLICKED_AT)

        // then
        val application = applicationRepository.findByPostIdAndUserIdAndDeletedAtIsNull(postId, userId)
        assertEquals(RecruitmentApplicationProgressStatus.PREPARING, application?.applicationStatus)
        assertEquals(CLICKED_AT, application?.firstClickedAt)
        assertEquals(RecruitmentApplicationProgressStatus.PREPARING, applicationReader.readActiveStatus(postId, userId))
    }

    @Test
    fun `지운 지원 이력을 스크랩에서 다시 옮기면 한 행을 되살려 지원 준비 중부터 시작한다`() {
        // given
        val userId = appendUser()
        val postId = checkNotNull(appendPost(userId).id)
        applicationManager.recordClick(postId, userId, CLICKED_AT)
        applicationManager.changeStatus(postId, userId, RecruitmentApplicationProgressStatus.IN_PROGRESS)
        applicationManager.delete(postId, userId, CLICKED_AT.plusMinutes(1))
        assertEquals(null, applicationReader.readActiveStatus(postId, userId))

        // when
        applicationManager.startPreparation(postId, userId, CLICKED_AT.plusMinutes(2))
        applicationRepository.flush()

        // then
        assertEquals(1L, applicationRepository.count())
        assertEquals(RecruitmentApplicationProgressStatus.PREPARING, applicationReader.readActiveStatus(postId, userId))
    }

    private fun appendUser(): Long = checkNotNull(
        userRepository.saveAndFlush(
            User.ofLetsCareer(
                letsCareerUserId = nextLetsCareerUserId++,
                joinedAt = CLICKED_AT,
            ),
        ).id,
    )

    private fun appendPost(
        userId: Long,
        title: String = "Kotlin 팀원 모집",
        recruitmentType: RecruitmentType = RecruitmentType.SIDE_PROJECT,
        publicationStatus: PublicationStatus = PublicationStatus.PUBLISHED,
        recruitmentStatus: RecruitmentStatus = RecruitmentStatus.RECRUITING,
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
        private var nextLetsCareerUserId = 4800L
        private val CLICKED_AT = LocalDateTime.of(2026, 9, 16, 9, 0)
    }
}
