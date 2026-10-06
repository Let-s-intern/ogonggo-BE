package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostAppendDto
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostContactMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostBookmarkSearchCondition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.recruitmentpost.error.RecruitmentPostErrorCode
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostBookmarkJpaRepository
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostJpaRepository
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostQueryRepository
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.persistence.UserJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
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
    RecruitmentPostAppender::class,
    RecruitmentPostBookmarkManager::class,
    RecruitmentPostBookmarkReader::class,
    RecruitmentPostQueryRepository::class,
)
internal class RecruitmentPostBookmarkImplementPersistenceTest @Autowired constructor(
    private val postAppender: RecruitmentPostAppender,
    private val bookmarkManager: RecruitmentPostBookmarkManager,
    private val bookmarkReader: RecruitmentPostBookmarkReader,
    private val bookmarkRepository: RecruitmentPostBookmarkJpaRepository,
    private val postRepository: RecruitmentPostJpaRepository,
    private val userRepository: UserJpaRepository,
) {

    @Test
    fun `북마크를 해제한 뒤 다시 등록하면 기존 행을 복구한다`() {
        val postId = appendPost()
        val userId = appendUser()
        val firstDeletedAt = LocalDateTime.of(2026, 9, 14, 10, 0)

        bookmarkManager.append(userId = userId, postId = postId, now = firstDeletedAt.minusMinutes(1))
        bookmarkManager.delete(userId = userId, postId = postId, now = firstDeletedAt)
        bookmarkManager.append(userId = userId, postId = postId, now = firstDeletedAt.plusMinutes(1))
        bookmarkRepository.flush()

        val bookmark = bookmarkRepository.findByPostIdAndUserId(postId, userId)
        assertEquals(1, bookmarkRepository.count())
        assertNull(bookmark?.deletedAt)
    }

    @Test
    fun `활성 북마크한 공개 모집글만 북마크 목록에 조회한다`() {
        val postId = appendPost()
        val userId = appendUser()
        bookmarkManager.append(userId = userId, postId = postId, now = LocalDateTime.of(2026, 9, 14, 10, 0))

        val page = bookmarkReader.readBookmarkedPublishedPage(userId, page = 0, size = 10)

        assertEquals(listOf(postId), page.items.map { checkNotNull(it.post.id) })
    }

    @Test
    fun `활성 북마크를 중복 등록하면 충돌 예외를 던진다`() {
        val postId = appendPost()
        val userId = appendUser()
        val now = LocalDateTime.of(2026, 9, 14, 10, 0)
        bookmarkManager.append(userId = userId, postId = postId, now = now)

        val exception = assertThrows(ConflictException::class.java) {
            bookmarkManager.append(userId = userId, postId = postId, now = now.plusMinutes(1))
        }

        assertEquals(RecruitmentPostErrorCode.RECRUITMENT_POST_BOOKMARK_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `북마크 등록과 해제는 실제 상태가 변경됐는지 반환한다`() {
        val postId = appendPost()
        val userId = appendUser()
        val now = LocalDateTime.of(2026, 9, 14, 10, 0)

        assertTrue(bookmarkManager.append(userId = userId, postId = postId, now = now))
        assertTrue(bookmarkManager.delete(userId = userId, postId = postId, now = now.plusMinutes(1)))
        assertFalse(bookmarkManager.delete(userId = userId, postId = postId, now = now.plusMinutes(2)))
    }

    @Test
    fun `북마크 목록은 모집 상태와 유형과 검색어로 좁히고 최근 저장순을 유지한다`() {
        // given
        val userId = appendUser()
        val first = appendPublishedPost("Kotlin 스터디", RecruitmentPostType.STUDY, RecruitmentPostRecruitmentStatus.RECRUITING)
        val last = appendPublishedPost("kotlin 코루틴 스터디", RecruitmentPostType.STUDY, RecruitmentPostRecruitmentStatus.RECRUITING)
        val closed = appendPublishedPost("Kotlin 마감 스터디", RecruitmentPostType.STUDY, RecruitmentPostRecruitmentStatus.CLOSED)
        val side = appendPublishedPost("Kotlin 사이드", RecruitmentPostType.SIDE_PROJECT, RecruitmentPostRecruitmentStatus.RECRUITING)
        val otherTitle = appendPublishedPost("Java 스터디", RecruitmentPostType.STUDY, RecruitmentPostRecruitmentStatus.RECRUITING)
        val now = LocalDateTime.of(2026, 9, 14, 10, 0)
        listOf(first, closed, side, otherTitle, last).forEachIndexed { index, postId ->
            bookmarkManager.append(userId = userId, postId = postId, now = now.plusMinutes(index.toLong()))
        }

        // when
        val page = bookmarkReader.readBookmarkedPublishedPage(
            userId = userId,
            page = 0,
            size = 10,
            condition = RecruitmentPostBookmarkSearchCondition(
                recruitmentStatus = RecruitmentPostRecruitmentStatus.RECRUITING,
                recruitmentType = RecruitmentPostType.STUDY,
                keyword = "KOTLIN",
            ),
        )

        // then
        assertEquals(listOf(last, first), page.items.map { checkNotNull(it.post.id) })
        assertEquals(2L, page.totalElements)
    }

    private fun appendPublishedPost(
        title: String,
        recruitmentType: RecruitmentPostType,
        recruitmentStatus: RecruitmentPostRecruitmentStatus,
    ): Long = checkNotNull(
        postRepository.saveAndFlush(
            RecruitmentPost(
                authorUserId = 1L,
                title = title,
                recruitmentType = recruitmentType,
                capacity = 4,
                progressMethod = RecruitmentPostProgressMethod.ONLINE,
                activityDurationMonths = 3,
                technologyStacks = listOf("Kotlin"),
                summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
                content = "{\"root\":{\"children\":[]}}",
                eligibilityAndSelectionProcess = null,
                recruitmentStartDate = LocalDate.of(2026, 9, 1),
                recruitmentEndDate = LocalDate.of(2026, 9, 30),
                positions = listOf(RecruitmentPostPosition.BACKEND),
                contactMethod = RecruitmentPostContactMethod.EMAIL,
                contactValue = "team@example.com",
                publicationStatus = RecruitmentPostPublicationStatus.PUBLISHED,
                recruitmentStatus = recruitmentStatus,
                closedAt = if (recruitmentStatus == RecruitmentPostRecruitmentStatus.CLOSED) LocalDateTime.of(2026, 9, 10, 0, 0) else null,
            ),
        ).id,
    )

    private fun appendPost(): Long = checkNotNull(postAppender.append(
        RecruitmentPostAppendDto(
            authorUserId = 1L,
            title = "사이드 프로젝트 팀원 모집",
            recruitmentType = RecruitmentPostType.SIDE_PROJECT,
            capacity = 4,
            progressMethod = RecruitmentPostProgressMethod.ONLINE,
            activityDurationMonths = 3,
            technologyStacks = listOf("Kotlin"),
            summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
            content = "{\"root\":{\"children\":[]}}",
            eligibilityAndSelectionProcess = null,
            recruitmentStartDate = LocalDate.of(2026, 9, 1),
            recruitmentEndDate = LocalDate.of(2026, 9, 30),
            positions = listOf(RecruitmentPostPosition.BACKEND),
            contactMethod = RecruitmentPostContactMethod.EMAIL,
            contactValue = "team@example.com",
        ),
    ).id)

    private fun appendUser(): Long = checkNotNull(
        userRepository.saveAndFlush(
            User.ofLetsCareer(
                letsCareerUserId = 4821L,
                joinedAt = LocalDateTime.of(2026, 9, 1, 0, 0),
            ),
        ).id,
    )
}
