package com.ogonggo.core.community.implement

import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPost
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
    fun `공개된 모집 중과 마감 모집글만 내 지원 목록에 포함한다`() {
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
        publicationStatus: PublicationStatus = PublicationStatus.PUBLISHED,
        recruitmentStatus: RecruitmentStatus = RecruitmentStatus.RECRUITING,
        closedAt: LocalDateTime? = null,
    ): RecruitmentPost = postRepository.saveAndFlush(
        RecruitmentPost(
            authorUserId = userId,
            title = title,
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
