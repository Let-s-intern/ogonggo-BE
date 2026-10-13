package com.ogonggo.userapi.concern.business

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernPopularSortType
import com.ogonggo.core.concern.implement.ConcernAppender
import com.ogonggo.core.concern.implement.ConcernCommentReader
import com.ogonggo.core.concern.implement.ConcernImageManager
import com.ogonggo.core.concern.implement.ConcernImageReader
import com.ogonggo.core.concern.implement.ConcernManager
import com.ogonggo.core.concern.implement.ConcernMetricReader
import com.ogonggo.core.concern.implement.ConcernReader
import com.ogonggo.core.concern.implement.dto.ConcernAppendDto
import com.ogonggo.core.concern.implement.dto.ConcernImageDto
import com.ogonggo.core.concern.implement.dto.ConcernMetricDto
import com.ogonggo.core.editor.lexical.LexicalEditorStateValidator
import com.ogonggo.core.error.BusinessException
import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.image.implement.dto.AttachedImageDto
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserAccountDto
import com.ogonggo.userapi.error.InvalidRequestFieldException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ConcernServiceTest {

    private val userReader = Mockito.mock(UserReader::class.java)
    private val userProfileReader = Mockito.mock(UserProfileReader::class.java)
    private val concernReader = Mockito.mock(ConcernReader::class.java)
    private val concernAppender = Mockito.mock(ConcernAppender::class.java)
    private val concernManager = Mockito.mock(ConcernManager::class.java)
    private val concernMetricReader = Mockito.mock(ConcernMetricReader::class.java)
    private val commentReader = Mockito.mock(ConcernCommentReader::class.java)
    private val concernImageReader = Mockito.mock(ConcernImageReader::class.java)
    private val concernImageManager = Mockito.mock(ConcernImageManager::class.java)
    private val imageAssetManager = Mockito.mock(ImageAssetManager::class.java)
    private val contentValidator = LexicalEditorStateValidator(ObjectMapper())
    private val eventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-10-08T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = ConcernService(
        userReader,
        userProfileReader,
        concernReader,
        concernAppender,
        concernManager,
        concernMetricReader,
        commentReader,
        concernImageReader,
        concernImageManager,
        imageAssetManager,
        contentValidator,
        eventPublisher,
        clock,
    )

    @Test
    fun `상세를 조회하면 조회 사실을 발행하고 내가 쓴 글인지 알려 준다`() {
        // given
        val concern = concern(authorUserId = USER_ID)
        Mockito.`when`(concernReader.read(CONCERN_ID)).thenReturn(concern)
        Mockito.`when`(concernMetricReader.read(CONCERN_ID)).thenReturn(ConcernMetricDto(viewCount = 7, commentCount = 2))
        Mockito.`when`(commentReader.readConcernIdsWithOfficialComment(listOf(CONCERN_ID))).thenReturn(setOf(CONCERN_ID))

        // when
        val result = service.readConcern(USER_ID, CONCERN_ID)

        // then
        assertTrue(result.mine)
        assertTrue(result.hasOfficialComment)
        assertEquals(7L, result.viewCount)
        Mockito.verify(eventPublisher).publishEvent(ConcernViewedEvent(CONCERN_ID))
    }

    @Test
    fun `지금 가장 핫한 고민은 조회 시각부터 7일 전까지 등록한 글에서 3건을 고른다`() {
        // given
        val createdFrom = LocalDateTime.now(clock).minusDays(7)
        Mockito.`when`(concernReader.readPopular(createdFrom, ConcernPopularSortType.COMMENT_COUNT, 3)).thenReturn(emptyList())

        // when
        val result = service.readPopularConcerns(ConcernPopularSortType.COMMENT_COUNT)

        // then
        assertEquals(emptyList<ConcernSummaryResult>(), result)
        Mockito.verify(concernReader).readPopular(createdFrom, ConcernPopularSortType.COMMENT_COUNT, 3)
    }

    @Test
    fun `남의 고민글은 수정할 수 없다`() {
        // given
        val concern = concern(authorUserId = OTHER_USER_ID)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(concernReader.readForUpdate(CONCERN_ID)).thenReturn(concern)

        // when
        val exception = assertThrows(BusinessException::class.java) {
            service.update(USER_ID, CONCERN_ID, SaveConcernCommand(ConcernCategory.ETC, "제목", CONTENT))
        }

        // then
        assertEquals("CONCERN_PERMISSION_DENIED", exception.errorCode.code)
        Mockito.verifyNoInteractions(concernManager, imageAssetManager)
    }

    @Test
    fun `고민글을 쓰면 첨부한 사진을 순서대로 붙인다`() {
        // given
        val now = LocalDateTime.now(clock)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        val saved = concern(authorUserId = USER_ID)
        Mockito.`when`(concernAppender.append(ConcernAppendDto(USER_ID, ConcernCategory.ETC, "제목", CONTENT))).thenReturn(saved)
        Mockito.`when`(
            imageAssetManager.syncImages(USER_ID, emptyList(), listOf("b", "a"), ImageAssetUsage.CONCERN, now),
        ).thenReturn(listOf(AttachedImageDto("b", "https://cdn/b.png"), AttachedImageDto("a", "https://cdn/a.png")))

        // when
        service.create(USER_ID, SaveConcernCommand(ConcernCategory.ETC, "제목", CONTENT, imageIds = listOf("b", "a")))

        // then
        Mockito.verify(concernImageManager).replace(
            CONCERN_ID,
            listOf(ConcernImageDto("b", "https://cdn/b.png"), ConcernImageDto("a", "https://cdn/a.png")),
            now,
        )
    }

    @Test
    fun `고민글을 고치면 전에 붙인 사진과 비교해 사진을 바꾼다`() {
        // given
        val now = LocalDateTime.now(clock)
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        val concern = concern(authorUserId = USER_ID)
        Mockito.`when`(concernReader.readForUpdate(CONCERN_ID)).thenReturn(concern)
        Mockito.`when`(concernImageReader.read(CONCERN_ID)).thenReturn(listOf(ConcernImageDto("old", "https://cdn/old.png")))
        Mockito.`when`(
            imageAssetManager.syncImages(USER_ID, listOf("old"), listOf("new"), ImageAssetUsage.CONCERN, now),
        ).thenReturn(listOf(AttachedImageDto("new", "https://cdn/new.png")))

        // when
        service.update(USER_ID, CONCERN_ID, SaveConcernCommand(ConcernCategory.ETC, "제목", CONTENT, imageIds = listOf("new")))

        // then
        Mockito.verify(concernImageManager).replace(CONCERN_ID, listOf(ConcernImageDto("new", "https://cdn/new.png")), now)
    }

    @Test
    fun `본문이 에디터 JSON이 아니면 content 필드 오류이고 저장하지 않는다`() {
        // given
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())

        // when
        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            service.create(USER_ID, SaveConcernCommand(ConcernCategory.ETC, "제목", "일반 텍스트 본문"))
        }

        // then
        assertEquals("content", exception.fieldName)
        Mockito.verifyNoInteractions(concernAppender, concernImageManager)
    }

    @Test
    fun `본문에 사진을 넣으면 content 필드 오류이고 저장하지 않는다`() {
        // given
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())
        Mockito.`when`(imageAssetManager.containsImages(CONTENT)).thenReturn(true)

        // when
        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            service.create(USER_ID, SaveConcernCommand(ConcernCategory.ETC, "제목", CONTENT))
        }

        // then
        assertEquals("content", exception.fieldName)
        Mockito.verifyNoInteractions(concernAppender, concernImageManager)
    }

    @Test
    fun `같은 사진을 두 번 첨부하면 imageIds 필드 오류이고 저장하지 않는다`() {
        // given
        Mockito.`when`(userReader.read(USER_ID)).thenReturn(activeUser())

        // when
        val exception = assertThrows(InvalidRequestFieldException::class.java) {
            service.create(USER_ID, SaveConcernCommand(ConcernCategory.ETC, "제목", CONTENT, imageIds = listOf("a", "a")))
        }

        // then
        assertEquals("imageIds", exception.fieldName)
        Mockito.verifyNoInteractions(concernAppender, imageAssetManager, concernImageManager)
    }

    @Test
    fun `내 고민글을 지우면 첨부 사진의 참조를 푼다`() {
        // given
        val concern = concern(authorUserId = USER_ID)
        Mockito.`when`(concernReader.readIncludingDeletedForUpdate(CONCERN_ID)).thenReturn(concern)
        Mockito.`when`(concernImageReader.read(CONCERN_ID)).thenReturn(listOf(ConcernImageDto("a", "https://cdn/a.png")))

        // when
        service.delete(USER_ID, CONCERN_ID)

        // then
        Mockito.verify(imageAssetManager).unreferenceImages(USER_ID, listOf("a"), LocalDateTime.now(clock))
    }

    @Test
    fun `이미 지운 내 고민글을 다시 지우면 아무것도 바꾸지 않고 성공한다`() {
        // given
        val concern = concern(authorUserId = USER_ID, deleted = true)
        Mockito.`when`(concernReader.readIncludingDeletedForUpdate(CONCERN_ID)).thenReturn(concern)

        // when
        service.delete(USER_ID, CONCERN_ID)

        // then
        Mockito.verifyNoInteractions(concernManager, imageAssetManager)
    }

    @Test
    fun `지워진 남의 고민글도 지울 수 없다`() {
        // given
        val concern = concern(authorUserId = OTHER_USER_ID, deleted = true)
        Mockito.`when`(concernReader.readIncludingDeletedForUpdate(CONCERN_ID)).thenReturn(concern)

        // when
        val exception = assertThrows(BusinessException::class.java) { service.delete(USER_ID, CONCERN_ID) }

        // then
        assertEquals("CONCERN_PERMISSION_DENIED", exception.errorCode.code)
    }

    private fun concern(authorUserId: Long, deleted: Boolean = false): Concern {
        val concern = Mockito.mock(Concern::class.java)
        Mockito.`when`(concern.id).thenReturn(CONCERN_ID)
        Mockito.`when`(concern.authorUserId).thenReturn(authorUserId)
        Mockito.`when`(concern.isWrittenBy(USER_ID)).thenReturn(authorUserId == USER_ID)
        Mockito.`when`(concern.isDeleted()).thenReturn(deleted)
        Mockito.`when`(concern.category).thenReturn(ConcernCategory.CAREER)
        Mockito.`when`(concern.title).thenReturn("제목")
        Mockito.`when`(concern.content).thenReturn(CONTENT)
        Mockito.`when`(concern.createdAt).thenReturn(NOW)
        Mockito.`when`(concern.updatedAt).thenReturn(NOW)
        return concern
    }

    private fun activeUser() = UserAccountDto(
        userId = USER_ID,
        letsCareerUserId = 100L,
        email = null,
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
        joinedAt = NOW,
    )

    companion object {
        private const val USER_ID = 17L
        private const val OTHER_USER_ID = 18L
        private const val CONCERN_ID = 5L
        private const val CONTENT = """{"root":{"children":[{"type":"paragraph"}]}}"""
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 8, 12, 0)
    }
}
