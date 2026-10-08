package com.ogonggo.userapi.concern.business

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.error.ConcernErrorCode
import com.ogonggo.core.concern.implement.ConcernAppender
import com.ogonggo.core.concern.implement.ConcernCommentReader
import com.ogonggo.core.concern.implement.ConcernManager
import com.ogonggo.core.concern.implement.ConcernMetricReader
import com.ogonggo.core.concern.implement.ConcernReader
import com.ogonggo.core.concern.implement.dto.ConcernAppendDto
import com.ogonggo.core.concern.implement.dto.ConcernUpdateDto
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.UserProfileDto
import com.ogonggo.userapi.user.implement.requireActive
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class ConcernService(
    private val userReader: UserReader,
    private val userProfileReader: UserProfileReader,
    private val concernReader: ConcernReader,
    private val concernAppender: ConcernAppender,
    private val concernManager: ConcernManager,
    private val concernMetricReader: ConcernMetricReader,
    private val commentReader: ConcernCommentReader,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {

    fun readConcerns(query: ConcernListQuery): ConcernPageResult {
        val result = concernReader.readPage(query.category, query.sortType, query.page, query.size)
        val concernIds = result.concerns.map { it.requiredId() }
        val profiles = userProfileReader.readAll(result.concerns.map { it.authorUserId }.toSet())
        val metrics = concernMetricReader.readAll(concernIds)
        val officialCommentedIds = commentReader.readConcernIdsWithOfficialComment(concernIds)

        return ConcernPageResult(
            items = result.concerns.map { concern ->
                val concernId = concern.requiredId()
                val metric = checkNotNull(metrics[concernId]) { "고민글 지표가 없습니다." }
                ConcernSummaryResult(
                    id = concernId,
                    category = concern.category,
                    title = concern.title,
                    content = concern.content,
                    author = profiles[concern.authorUserId].toAuthor(),
                    createdAt = concern.createdAt,
                    viewCount = metric.viewCount,
                    commentCount = metric.commentCount,
                    hasOfficialComment = concernId in officialCommentedIds,
                )
            },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    /** 조회 수는 비동기로 올리므로 응답의 `viewCount`에는 이번 조회가 들어가지 않는다. */
    fun readConcern(viewerUserId: Long?, concernId: Long): ConcernDetailResult {
        val concern = concernReader.read(concernId)
        val metric = concernMetricReader.read(concernId)
        val hasOfficialComment = concernId in commentReader.readConcernIdsWithOfficialComment(listOf(concernId))

        return ConcernDetailResult(
            id = concernId,
            category = concern.category,
            title = concern.title,
            content = concern.content,
            author = userProfileReader.read(concern.authorUserId).toAuthor(),
            createdAt = concern.createdAt,
            updatedAt = concern.updatedAt,
            viewCount = metric.viewCount,
            commentCount = metric.commentCount,
            hasOfficialComment = hasOfficialComment,
            mine = viewerUserId == concern.authorUserId,
        ).also { eventPublisher.publishEvent(ConcernViewedEvent(concernId)) }
    }

    @Transactional
    fun create(userId: Long, command: SaveConcernCommand): Long {
        userReader.read(userId).status.requireActive()
        val concern = concernAppender.append(
            ConcernAppendDto(
                authorUserId = userId,
                category = command.category,
                title = command.title,
                content = command.content,
            ),
        )
        return concern.requiredId()
    }

    @Transactional
    fun update(userId: Long, concernId: Long, command: SaveConcernCommand) {
        userReader.read(userId).status.requireActive()
        val concern = concernReader.readForUpdate(concernId)
        verifyAuthor(concern, userId)
        concernManager.update(
            concern,
            ConcernUpdateDto(category = command.category, title = command.title, content = command.content),
        )
    }

    /** 이미 지운 내 고민글을 다시 지워도 같은 결과로 본다. 남의 고민글은 지워졌든 아니든 지울 수 없다. */
    @Transactional
    fun delete(userId: Long, concernId: Long) {
        val concern = concernReader.readIncludingDeletedForUpdate(concernId)
        verifyAuthor(concern, userId)
        if (concern.isDeleted()) return
        concernManager.delete(concern, LocalDateTime.now(clock))
    }

    private fun verifyAuthor(concern: Concern, userId: Long) {
        if (!concern.isWrittenBy(userId)) {
            throw ForbiddenException(ConcernErrorCode.CONCERN_PERMISSION_DENIED)
        }
    }

    private fun Concern.requiredId(): Long = checkNotNull(id) { "고민글 식별자가 없습니다." }
}

internal fun UserProfileDto?.toAuthor(): ConcernAuthorResult = ConcernAuthorResult(
    nickname = this?.nickname,
    profileImageUrl = this?.profileImageUrl,
)
