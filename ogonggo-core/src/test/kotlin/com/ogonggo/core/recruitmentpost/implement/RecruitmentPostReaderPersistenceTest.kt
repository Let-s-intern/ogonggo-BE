package com.ogonggo.core.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostAppendDto
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostContactMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPost
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPublicationStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostPosition
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostSortType
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.core.jpa.CoreJpaConfiguration
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostJpaRepository
import com.ogonggo.core.recruitmentpost.persistence.RecruitmentPostQueryRepository
import com.ogonggo.core.recruitmentpost.error.RecruitmentPostErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
    RecruitmentPostAppender::class,
    RecruitmentPostManager::class,
    RecruitmentPostReader::class,
    RecruitmentPostQueryRepository::class,
    RecruitmentPostMetricManager::class,
    RecruitmentPostMetricRegistrar::class,
)
internal class RecruitmentPostReaderPersistenceTest @Autowired constructor(
    private val postAppender: RecruitmentPostAppender,
    private val postManager: RecruitmentPostManager,
    private val postReader: RecruitmentPostReader,
    private val postRepository: RecruitmentPostJpaRepository,
    private val recruitmentPostMetricManager: RecruitmentPostMetricManager,
) {

    @Test
    fun `공개 모집글을 식별자로 조회한다`() {
        val savedPost = postAppender.append(createCommand(title = "상세 모집글", recruitmentType = RecruitmentPostType.STUDY))

        val result = postReader.readPublished(checkNotNull(savedPost.id))

        assertEquals("상세 모집글", result.title)
        assertEquals(RecruitmentPostPublicationStatus.PUBLISHED, result.publicationStatus)
    }

    @Test
    fun `존재하지 않거나 비공개인 모집글은 찾을 수 없음 예외를 던진다`() {
        val hiddenPost = postRepository.save(createPost(title = "비공개 모집", publicationStatus = RecruitmentPostPublicationStatus.HIDDEN))

        val missingException = assertThrows(EntityNotFoundException::class.java) {
            postReader.readPublished(999L)
        }
        val hiddenException = assertThrows(EntityNotFoundException::class.java) {
            postReader.readPublished(checkNotNull(hiddenPost.id))
        }

        assertEquals(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND, missingException.errorCode)
        assertEquals(RecruitmentPostErrorCode.RECRUITMENT_POST_NOT_FOUND, hiddenException.errorCode)
    }

    @Test
    fun `공개 모집글을 모집 구분으로 필터링하고 페이지로 조회한다`() {
        postAppender.append(createCommand(title = "스터디 모집", recruitmentType = RecruitmentPostType.STUDY))
        postAppender.append(createCommand(title = "사이드 프로젝트 모집", recruitmentType = RecruitmentPostType.SIDE_PROJECT))
        postRepository.save(createPost(title = "비공개 모집", publicationStatus = RecruitmentPostPublicationStatus.HIDDEN))

        val result = postReader.readPublishedPage(
            page = 0,
            size = 1,
            filter = RecruitmentPostListFilter(recruitmentTypes = setOf(RecruitmentPostType.STUDY)),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(1, result.posts.size)
        assertEquals("스터디 모집", result.posts.single().title)
        assertEquals(1, result.totalElements)
        assertEquals(1, result.totalPages)
    }

    @Test
    fun `모집 포지션 필터는 하나라도 일치하는 모집글을 반환한다`() {
        postAppender.append(
            createCommand(
                title = "백엔드 모집",
                recruitmentType = RecruitmentPostType.SIDE_PROJECT,
                positions = listOf(RecruitmentPostPosition.BACKEND, RecruitmentPostPosition.DESIGN),
            ),
        )
        postAppender.append(
            createCommand(
                title = "프론트엔드 모집",
                recruitmentType = RecruitmentPostType.SIDE_PROJECT,
                positions = listOf(RecruitmentPostPosition.FRONTEND),
            ),
        )

        val result = postReader.readPublishedPage(
            page = 0,
            size = 10,
            filter = RecruitmentPostListFilter(positions = setOf(RecruitmentPostPosition.DESIGN)),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(listOf("백엔드 모집"), result.posts.map(RecruitmentPost::title))
    }

    @Test
    fun `필터를 선택하지 않으면 공개 모집글 전체를 반환한다`() {
        postAppender.append(createCommand(title = "스터디 모집", recruitmentType = RecruitmentPostType.STUDY))
        postAppender.append(createCommand(title = "사이드 프로젝트 모집", recruitmentType = RecruitmentPostType.SIDE_PROJECT))
        postRepository.save(createPost(title = "비공개 모집", publicationStatus = RecruitmentPostPublicationStatus.HIDDEN))

        val result = postReader.readPublishedPage(
            page = 0,
            size = 10,
            filter = RecruitmentPostListFilter(),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(setOf("스터디 모집", "사이드 프로젝트 모집"), result.posts.map(RecruitmentPost::title).toSet())
        assertEquals(2, result.totalElements)
        assertEquals(1, result.totalPages)
    }

    @Test
    fun `삭제된 모집글은 공개 단건과 목록에서 조회하지 않는다`() {
        val deletedPost = postAppender.append(createCommand("삭제될 모집글", RecruitmentPostType.STUDY))
        postManager.delete(deletedPost, LocalDateTime.of(2026, 9, 11, 9, 0))
        postRepository.flush()

        assertThrows(EntityNotFoundException::class.java) {
            postReader.readPublished(checkNotNull(deletedPost.id))
        }

        val result = postReader.readPublishedPage(
            page = 0,
            size = 10,
            filter = RecruitmentPostListFilter(),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(emptyList<RecruitmentPost>(), result.posts)
        assertEquals(0, result.totalElements)
        assertEquals(0, result.totalPages)
    }

    @Test
    fun `삭제용 조회는 삭제된 본인 모집글을 찾고 다른 사용자의 글은 찾지 않는다`() {
        val deletedPost = postAppender.append(createCommand("삭제 대상", RecruitmentPostType.STUDY))
        val postId = checkNotNull(deletedPost.id)
        postManager.delete(deletedPost, LocalDateTime.of(2026, 9, 11, 9, 0))
        postRepository.flush()

        assertEquals(postId, postReader.readOwnedForDelete(1L, postId).id)
        assertThrows(EntityNotFoundException::class.java) {
            postReader.readOwnedForDelete(2L, postId)
        }
    }

    @Test
    fun `최신순 페이지로 다음 목록을 중복 없이 조회한다`() {
        postAppender.append(createCommand(title = "첫 번째", recruitmentType = RecruitmentPostType.STUDY))
        postAppender.append(createCommand(title = "두 번째", recruitmentType = RecruitmentPostType.STUDY))
        postAppender.append(createCommand(title = "세 번째", recruitmentType = RecruitmentPostType.STUDY))

        val first = postReader.readPublishedPage(
            page = 0,
            size = 2,
            filter = RecruitmentPostListFilter(),
            sortType = RecruitmentPostSortType.LATEST,
        )
        val second = postReader.readPublishedPage(
            page = 1,
            size = 2,
            filter = RecruitmentPostListFilter(),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(0, first.page)
        assertEquals(3, first.totalElements)
        assertEquals(2, first.totalPages)
        assertEquals(listOf("첫 번째"), second.posts.map(RecruitmentPost::title))
        assertEquals(1, second.page)
        assertEquals(3, second.totalElements)
        assertEquals(2, second.totalPages)
    }

    @Test
    fun `조회수순 목록은 조회수가 높은 모집글부터 중복 없이 반환한다`() {
        // given
        val zeroViewPost = postAppender.append(
            createCommand(
                title = "조회수 0",
                recruitmentType = RecruitmentPostType.STUDY,
                positions = listOf(RecruitmentPostPosition.BACKEND, RecruitmentPostPosition.DESIGN),
            ),
        )
        val oneViewPost = postAppender.append(
            createCommand(
                title = "조회수 1",
                recruitmentType = RecruitmentPostType.STUDY,
                positions = listOf(RecruitmentPostPosition.BACKEND, RecruitmentPostPosition.DESIGN),
            ),
        )
        val twoViewPost = postAppender.append(
            createCommand(
                title = "조회수 2",
                recruitmentType = RecruitmentPostType.STUDY,
                positions = listOf(RecruitmentPostPosition.BACKEND, RecruitmentPostPosition.DESIGN),
            ),
        )
        val now = LocalDateTime.of(2026, 9, 17, 13, 0)
        recruitmentPostMetricManager.initialize(checkNotNull(oneViewPost.id))
        recruitmentPostMetricManager.initialize(checkNotNull(twoViewPost.id))
        recruitmentPostMetricManager.increaseViewCount(checkNotNull(oneViewPost.id), now)
        recruitmentPostMetricManager.increaseViewCount(checkNotNull(twoViewPost.id), now)
        recruitmentPostMetricManager.increaseViewCount(checkNotNull(twoViewPost.id), now.plusSeconds(1))

        // when
        val result = postReader.readPublishedPage(
            page = 0,
            size = 10,
            filter = RecruitmentPostListFilter(
                positions = setOf(RecruitmentPostPosition.BACKEND, RecruitmentPostPosition.DESIGN),
            ),
            sortType = RecruitmentPostSortType.VIEW_COUNT,
        )

        // then
        assertEquals(listOf("조회수 2", "조회수 1", "조회수 0"), result.posts.map(RecruitmentPost::title))
        assertEquals(3, result.posts.map(RecruitmentPost::id).distinct().size)
        assertEquals(checkNotNull(zeroViewPost.id), result.posts.last().id)
    }

    @Test
    fun `댓글수순 목록은 댓글수가 높은 모집글부터 반환한다`() {
        // given
        val zeroCommentPost = postAppender.append(createCommand("댓글수 0", RecruitmentPostType.STUDY))
        val oneCommentPost = postAppender.append(createCommand("댓글수 1", RecruitmentPostType.STUDY))
        val twoCommentPost = postAppender.append(createCommand("댓글수 2", RecruitmentPostType.STUDY))
        val now = LocalDateTime.of(2026, 9, 17, 13, 0)
        recruitmentPostMetricManager.initialize(checkNotNull(oneCommentPost.id))
        recruitmentPostMetricManager.initialize(checkNotNull(twoCommentPost.id))
        recruitmentPostMetricManager.increaseCommentCount(checkNotNull(oneCommentPost.id), now)
        recruitmentPostMetricManager.increaseCommentCount(checkNotNull(twoCommentPost.id), now)
        recruitmentPostMetricManager.increaseCommentCount(checkNotNull(twoCommentPost.id), now.plusSeconds(1))

        // when
        val result = postReader.readPublishedPage(
            page = 0,
            size = 10,
            filter = RecruitmentPostListFilter(),
            sortType = RecruitmentPostSortType.COMMENT_COUNT,
        )

        // then
        assertEquals(listOf("댓글수 2", "댓글수 1", "댓글수 0"), result.posts.map(RecruitmentPost::title))
        assertEquals(checkNotNull(zeroCommentPost.id), result.posts.last().id)
    }

    @Test
    fun `페이지 번호가 음수면 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            postReader.readPublishedPage(
                page = -1,
                size = 10,
                filter = RecruitmentPostListFilter(),
                sortType = RecruitmentPostSortType.LATEST,
            )
        }
    }

    private fun createCommand(
        title: String,
        recruitmentType: RecruitmentPostType,
        positions: List<RecruitmentPostPosition> = listOf(RecruitmentPostPosition.BACKEND),
    ) = RecruitmentPostAppendDto(
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
        positions = positions,
        contactMethod = RecruitmentPostContactMethod.EMAIL,
        contactValue = "team@example.com",
    )

    private fun createPost(
        title: String,
        publicationStatus: RecruitmentPostPublicationStatus,
    ) = RecruitmentPost(
        authorUserId = 1L,
        title = title,
        recruitmentType = RecruitmentPostType.STUDY,
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
        publicationStatus = publicationStatus,
    )
}
