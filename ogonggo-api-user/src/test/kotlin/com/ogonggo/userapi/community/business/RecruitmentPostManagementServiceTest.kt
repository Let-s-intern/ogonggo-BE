package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.RecruitmentPostManagementSortType
import com.ogonggo.core.community.domain.RecruitmentPostManagementStatus
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.PostMetricDto
import com.ogonggo.core.community.implement.PostMetricManager
import com.ogonggo.core.community.implement.PostMetricReader
import com.ogonggo.core.community.implement.RecruitmentPostApplicationReader
import com.ogonggo.core.community.implement.RecruitmentPostManagementPage
import com.ogonggo.core.community.implement.RecruitmentPostManagementReader
import com.ogonggo.core.community.implement.RecruitmentPostManager
import com.ogonggo.core.community.implement.RecruitmentPostReader
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.LocalDateTime

class RecruitmentPostManagementServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val managementReader = Mockito.mock(RecruitmentPostManagementReader::class.java)
    private val postMetricReader = Mockito.mock(PostMetricReader::class.java)
    private val applicationReader = Mockito.mock(RecruitmentPostApplicationReader::class.java)
    private val postReader = Mockito.mock(RecruitmentPostReader::class.java)
    private val postManager = Mockito.mock(RecruitmentPostManager::class.java)
    private val postMetricManager = Mockito.mock(PostMetricManager::class.java)
    private val imageAssetManager = Mockito.mock(ImageAssetManager::class.java)
    private val service = RecruitmentPostManagementService(
        userReader,
        managementReader,
        postMetricReader,
        applicationReader,
        postReader,
        postManager,
        postMetricManager,
        imageAssetManager,
    )

    @Test
    fun `페이지의 모집글 ID를 모아 지표와 지원 수를 일괄 조회한다`() {
        val firstPost = publishedPost(12L)
        val secondPost = publishedPost(13L)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(
            managementReader.readPage(
                ownerUserId = USER_ID,
                status = RecruitmentPostManagementStatus.ALL,
                recruitmentStatus = null,
                applicationStatus = null,
                recruitmentType = null,
                keyword = null,
                page = 0,
                size = 10,
                sort = RecruitmentPostManagementSortType.LATEST_SAVED,
            ),
        ).thenReturn(
            RecruitmentPostManagementPage(
                posts = listOf(firstPost, secondPost),
                page = 0,
                size = 10,
                totalElements = 2,
                totalPages = 1,
            ),
        )
        Mockito.`when`(postMetricReader.readAll(listOf(12L, 13L))).thenReturn(
            mapOf(12L to PostMetricDto(148, 3), 13L to PostMetricDto(2, 0)),
        )
        Mockito.`when`(applicationReader.countByPostIds(listOf(12L, 13L))).thenReturn(
            mapOf(12L to 4L, 13L to 1L),
        )

        val result = service.getPosts(
            userId = USER_ID,
            status = RecruitmentPostManagementStatus.ALL,
            recruitmentStatus = null,
            applicationStatus = null,
            recruitmentType = null,
            keyword = null,
            page = 0,
            size = 10,
            sort = RecruitmentPostManagementSortType.LATEST_SAVED,
        )

        assertEquals(listOf(148L, 2L), result.items.map { it.viewCount })
        assertEquals(listOf(4L, 1L), result.items.map { it.applicationCount })
        Mockito.verify(postMetricReader).readAll(listOf(12L, 13L))
        Mockito.verify(applicationReader).countByPostIds(listOf(12L, 13L))
    }

    @Test
    fun `임시저장 응답은 게시 필드를 null로 만들고 이어 작성하기를 표시한다`() {
        val draft = Mockito.mock(RecruitmentPost::class.java)
        Mockito.`when`(draft.id).thenReturn(15L)
        Mockito.`when`(draft.publicationStatus).thenReturn(PublicationStatus.DRAFT)
        Mockito.`when`(draft.title).thenReturn("작성 중인 모집글")
        Mockito.`when`(draft.updatedAt).thenReturn(LocalDateTime.of(2026, 9, 16, 10, 0))
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(
            managementReader.readPage(
                ownerUserId = USER_ID,
                status = RecruitmentPostManagementStatus.DRAFT,
                recruitmentStatus = null,
                applicationStatus = null,
                recruitmentType = null,
                keyword = null,
                page = 0,
                size = 10,
                sort = RecruitmentPostManagementSortType.LATEST_SAVED,
            ),
        ).thenReturn(RecruitmentPostManagementPage(listOf(draft), 0, 10, 1, 1))
        Mockito.`when`(postMetricReader.readAll(listOf(15L))).thenReturn(emptyMap())
        Mockito.`when`(applicationReader.countByPostIds(listOf(15L))).thenReturn(emptyMap())

        val item = service.getPosts(
            userId = USER_ID,
            status = RecruitmentPostManagementStatus.DRAFT,
            recruitmentStatus = null,
            applicationStatus = null,
            recruitmentType = null,
            keyword = null,
            page = 0,
            size = 10,
            sort = RecruitmentPostManagementSortType.LATEST_SAVED,
        ).items.single()

        assertEquals(RecruitmentPostManagementStatus.DRAFT, item.status)
        assertNull(item.recruitmentType)
        assertNull(item.recruitmentStatus)
        assertEquals(0L, item.applicationCount)
        assertEquals(0L, item.viewCount)
        assertEquals(0L, item.commentCount)
        assertTrue(item.continueWriting)
    }

    @Test
    fun `작성자 모집글을 새 임시저장 글로 복사하고 작성 폼 결과를 반환한다`() {
        val source = publishedPost(12L)
        val copied = formPost(101L, PublicationStatus.DRAFT)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readOwnedForUpdate(USER_ID, 12L)).thenReturn(source)
        Mockito.`when`(postManager.copyAsDraft(source)).thenReturn(copied)
        Mockito.`when`(copied.title).thenReturn("공개 모집글")
        val copiedContent = copied.content
        Mockito.`when`(imageAssetManager.copyPostImages(USER_ID, 12L, 101L, copiedContent))
            .thenReturn(copiedContent)

        val result = service.copy(USER_ID, 12L)

        assertEquals(101L, result.postId)
        assertEquals(RecruitmentPostManagementStatus.DRAFT, result.status)
        assertEquals("공개 모집글", result.title)
        assertEquals(false, result.agreedToPolicy)
        Mockito.verify(postReader).readOwnedForUpdate(USER_ID, 12L)
        Mockito.verify(postManager).copyAsDraft(source)
        Mockito.verify(imageAssetManager).copyPostImages(USER_ID, 12L, 101L, copied.content)
    }

    @Test
    fun `작성자 본인의 임시저장 모집글 작성 폼을 전체 필드로 조회한다`() {
        val draft = formPost(15L, PublicationStatus.DRAFT)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readOwned(USER_ID, 15L)).thenReturn(draft)

        val result = service.getPostForm(USER_ID, 15L)

        assertEquals(15L, result.postId)
        assertEquals(RecruitmentPostManagementStatus.DRAFT, result.status)
        assertEquals(RecruitmentStatus.RECRUITING, result.recruitmentStatus)
        assertEquals("작성 중인 모집글", result.title)
        assertEquals(listOf("Kotlin", "Spring"), result.technologyStacks)
        assertEquals("요약", result.summary)
        assertEquals("{\"root\":{\"children\":[]}}", result.content)
        assertEquals(listOf(com.ogonggo.core.community.domain.RecruitmentPosition.BACKEND), result.positions)
        assertEquals(false, result.agreedToPolicy)
        Mockito.verify(postReader).readOwned(USER_ID, 15L)
        Mockito.verifyNoInteractions(postMetricReader, applicationReader, imageAssetManager, postManager)
    }

    @Test
    fun `작성자 본인의 임시저장 모집글을 게시한다`() {
        // given
        val draft = formPost(15L, PublicationStatus.DRAFT)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readOwnedForUpdate(USER_ID, 15L)).thenReturn(draft)

        // when
        service.publish(USER_ID, 15L)

        // then
        Mockito.verify(postReader).readOwnedForUpdate(USER_ID, 15L)
        Mockito.verify(postManager).publish(draft)
        Mockito.verify(postMetricManager).initialize(15L)
    }

    @Test
    fun `공개 및 비공개 모집글 작성 폼은 원래 게시 상태를 반환한다`() {
        val published = formPost(12L, PublicationStatus.PUBLISHED)
        val hidden = formPost(13L, PublicationStatus.HIDDEN)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(postReader.readOwned(USER_ID, 12L)).thenReturn(published)
        Mockito.`when`(postReader.readOwned(USER_ID, 13L)).thenReturn(hidden)

        assertEquals(RecruitmentPostManagementStatus.PUBLISHED, service.getPostForm(USER_ID, 12L).status)
        assertEquals(RecruitmentPostManagementStatus.HIDDEN, service.getPostForm(USER_ID, 13L).status)
    }

    private fun formPost(postId: Long, status: PublicationStatus): RecruitmentPost =
        Mockito.mock(RecruitmentPost::class.java).also {
            Mockito.`when`(it.id).thenReturn(postId)
            Mockito.`when`(it.publicationStatus).thenReturn(status)
            Mockito.`when`(it.recruitmentStatus).thenReturn(RecruitmentStatus.RECRUITING)
            Mockito.`when`(it.title).thenReturn("작성 중인 모집글")
            Mockito.`when`(it.recruitmentType).thenReturn(RecruitmentType.SIDE_PROJECT)
            Mockito.`when`(it.capacity).thenReturn(5)
            Mockito.`when`(it.progressMethod).thenReturn(com.ogonggo.core.community.domain.ProgressMethod.ONLINE)
            Mockito.`when`(it.activityDurationMonths).thenReturn(3)
            Mockito.`when`(it.technologyStacks).thenReturn(mutableListOf("Kotlin", "Spring"))
            Mockito.`when`(it.summary).thenReturn("요약")
            Mockito.`when`(it.content).thenReturn("{\"root\":{\"children\":[]}}")
            Mockito.`when`(it.eligibilityAndSelectionProcess).thenReturn(null)
            Mockito.`when`(it.recruitmentStartDate).thenReturn(LocalDate.of(2026, 9, 1))
            Mockito.`when`(it.recruitmentEndDate).thenReturn(LocalDate.of(2026, 9, 30))
            Mockito.`when`(it.positions).thenReturn(
                mutableListOf(com.ogonggo.core.community.domain.RecruitmentPosition.BACKEND),
            )
            Mockito.`when`(it.contactMethod).thenReturn(com.ogonggo.core.community.domain.ContactMethod.EMAIL)
            Mockito.`when`(it.contactValue).thenReturn("team@example.com")
        }

    private fun publishedPost(postId: Long): RecruitmentPost = Mockito.mock(RecruitmentPost::class.java).also {
        Mockito.`when`(it.id).thenReturn(postId)
        Mockito.`when`(it.publicationStatus).thenReturn(PublicationStatus.PUBLISHED)
        Mockito.`when`(it.title).thenReturn("공개 모집글")
        Mockito.`when`(it.recruitmentStatus).thenReturn(RecruitmentStatus.RECRUITING)
        Mockito.`when`(it.updatedAt).thenReturn(LocalDateTime.of(2026, 9, 16, 10, 0))
    }

    private fun activeUser() = UserAccountDto(
        userId = USER_ID,
        letsCareerUserId = 100L,
        email = null,
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
        joinedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    )

    companion object {
        private const val USER_ID = 17L
    }
}
