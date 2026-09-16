package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.Post
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.common.CoreJpaConfiguration
import com.ogonggo.core.community.persistence.PostJpaRepository
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.error.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate

@DataJpaTest
@ContextConfiguration(classes = [CoreJpaConfiguration::class])
@Import(PostAppenderImpl::class, PostReaderImpl::class)
internal class PostReaderPersistenceTest @Autowired constructor(
    private val postAppender: PostAppender,
    private val postReader: PostReader,
    private val postRepository: PostJpaRepository,
) {

    @Test
    fun `공개 모집글을 식별자로 조회한다`() {
        val savedPost = postAppender.append(createCommand(title = "상세 모집글", recruitmentType = RecruitmentType.STUDY))

        val result = postReader.readPublished(checkNotNull(savedPost.id))

        assertEquals("상세 모집글", result.title)
        assertEquals(PublicationStatus.PUBLISHED, result.publicationStatus)
    }

    @Test
    fun `존재하지 않거나 비공개인 모집글은 찾을 수 없음 예외를 던진다`() {
        val hiddenPost = postRepository.save(createPost(title = "비공개 모집", publicationStatus = PublicationStatus.HIDDEN))

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
        postAppender.append(createCommand(title = "스터디 모집", recruitmentType = RecruitmentType.STUDY))
        postAppender.append(createCommand(title = "사이드 프로젝트 모집", recruitmentType = RecruitmentType.SIDE_PROJECT))
        postRepository.save(createPost(title = "비공개 모집", publicationStatus = PublicationStatus.HIDDEN))

        val result = postReader.readPublishedPage(
            page = 0,
            size = 1,
            filter = RecruitmentPostListFilter(recruitmentTypes = setOf(RecruitmentType.STUDY)),
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
                recruitmentType = RecruitmentType.SIDE_PROJECT,
                positions = listOf(RecruitmentPosition.BACKEND, RecruitmentPosition.DESIGN),
            ),
        )
        postAppender.append(
            createCommand(
                title = "프론트엔드 모집",
                recruitmentType = RecruitmentType.SIDE_PROJECT,
                positions = listOf(RecruitmentPosition.FRONTEND),
            ),
        )

        val result = postReader.readPublishedPage(
            page = 0,
            size = 10,
            filter = RecruitmentPostListFilter(positions = setOf(RecruitmentPosition.DESIGN)),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(listOf("백엔드 모집"), result.posts.map(Post::title))
    }

    @Test
    fun `필터를 선택하지 않으면 공개 모집글 전체를 반환한다`() {
        postAppender.append(createCommand(title = "스터디 모집", recruitmentType = RecruitmentType.STUDY))
        postAppender.append(createCommand(title = "사이드 프로젝트 모집", recruitmentType = RecruitmentType.SIDE_PROJECT))
        postRepository.save(createPost(title = "비공개 모집", publicationStatus = PublicationStatus.HIDDEN))

        val result = postReader.readPublishedPage(
            page = 0,
            size = 10,
            filter = RecruitmentPostListFilter(),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(setOf("스터디 모집", "사이드 프로젝트 모집"), result.posts.map(Post::title).toSet())
        assertEquals(2, result.totalElements)
    }

    @Test
    fun `마지막 페이지를 넘어가면 빈 목록과 전체 개수를 반환한다`() {
        postAppender.append(createCommand(title = "스터디 모집", recruitmentType = RecruitmentType.STUDY))

        val result = postReader.readPublishedPage(
            page = 1,
            size = 10,
            filter = RecruitmentPostListFilter(),
            sortType = RecruitmentPostSortType.LATEST,
        )

        assertEquals(emptyList<Post>(), result.posts)
        assertEquals(1, result.totalElements)
        assertEquals(1, result.totalPages)
    }

    private fun createCommand(
        title: String,
        recruitmentType: RecruitmentType,
        positions: List<RecruitmentPosition> = listOf(RecruitmentPosition.BACKEND),
    ) = PostAppendCommand(
        authorUserId = 1L,
        title = title,
        recruitmentType = recruitmentType,
        capacity = 4,
        progressMethod = ProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = "<p>모집 상세 내용입니다.</p>",
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = positions,
        contactMethod = ContactMethod.EMAIL,
        contactValue = "team@example.com",
    )

    private fun createPost(
        title: String,
        publicationStatus: PublicationStatus,
    ) = Post(
        authorUserId = 1L,
        title = title,
        recruitmentType = RecruitmentType.STUDY,
        capacity = 4,
        progressMethod = ProgressMethod.ONLINE,
        activityDurationMonths = 3,
        technologyStacks = listOf("Kotlin"),
        summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
        content = "<p>모집 상세 내용입니다.</p>",
        eligibilityAndSelectionProcess = null,
        recruitmentStartDate = LocalDate.of(2026, 9, 1),
        recruitmentEndDate = LocalDate.of(2026, 9, 30),
        positions = listOf(RecruitmentPosition.BACKEND),
        contactMethod = ContactMethod.EMAIL,
        contactValue = "team@example.com",
        publicationStatus = publicationStatus,
    )
}
