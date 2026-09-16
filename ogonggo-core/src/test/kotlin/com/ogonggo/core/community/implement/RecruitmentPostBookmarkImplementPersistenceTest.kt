package com.ogonggo.core.community.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.community.persistence.RecruitmentPostBookmarkJpaRepository
import com.ogonggo.core.community.persistence.PostMetricJpaRepository
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.user.domain.User
import com.ogonggo.core.user.persistence.UserJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
    PostMetricManager::class,
    PostMetricRegistrar::class,
)
internal class RecruitmentPostBookmarkImplementPersistenceTest @Autowired constructor(
    private val postAppender: RecruitmentPostAppender,
    private val bookmarkManager: RecruitmentPostBookmarkManager,
    private val bookmarkReader: RecruitmentPostBookmarkReader,
    private val bookmarkRepository: RecruitmentPostBookmarkJpaRepository,
    private val userRepository: UserJpaRepository,
    private val postMetricManager: PostMetricManager,
    private val postMetricRepository: PostMetricJpaRepository,
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
    fun `북마크 수는 활성 북마크만 다시 세어 반영한다`() {
        val postId = appendPost()
        val userId = appendUser()
        val now = LocalDateTime.of(2026, 9, 14, 10, 0)

        bookmarkManager.append(userId = userId, postId = postId, now = now)
        postMetricManager.syncBookmarkCount(postId, now)
        assertEquals(1L, postMetricRepository.findByPostId(postId)?.bookmarkCount)

        bookmarkManager.delete(userId = userId, postId = postId, now = now.plusMinutes(1))
        postMetricManager.syncBookmarkCount(postId, now.plusMinutes(1))
        assertEquals(0L, postMetricRepository.findByPostId(postId)?.bookmarkCount)
    }

    private fun appendPost(): Long = checkNotNull(postAppender.append(
        RecruitmentPostAppendCommand(
            authorUserId = 1L,
            title = "사이드 프로젝트 팀원 모집",
            recruitmentType = RecruitmentType.SIDE_PROJECT,
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
