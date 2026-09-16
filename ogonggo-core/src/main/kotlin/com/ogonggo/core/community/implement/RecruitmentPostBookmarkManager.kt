package com.ogonggo.core.community.implement

import com.ogonggo.core.community.domain.RecruitmentPostBookmark
import com.ogonggo.core.community.error.RecruitmentPostErrorCode
import com.ogonggo.core.community.persistence.RecruitmentPostBookmarkJpaRepository
import com.ogonggo.core.error.ConflictException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class RecruitmentPostBookmarkManager internal constructor(
    private val bookmarkRepository: RecruitmentPostBookmarkJpaRepository,
) {

    fun append(userId: Long, postId: Long, now: LocalDateTime): Boolean {
        if (bookmarkRepository.restore(postId = postId, userId = userId, now = now) > 0) {
            return true
        }

        try {
            bookmarkRepository.saveAndFlush(RecruitmentPostBookmark(postId = postId, userId = userId))
            return true
        } catch (exception: DataIntegrityViolationException) {
            throw ConflictException(RecruitmentPostErrorCode.RECRUITMENT_POST_BOOKMARK_ALREADY_EXISTS)
        }
    }

    fun delete(userId: Long, postId: Long, now: LocalDateTime): Boolean =
        bookmarkRepository.softDelete(postId = postId, userId = userId, now = now) > 0
}
