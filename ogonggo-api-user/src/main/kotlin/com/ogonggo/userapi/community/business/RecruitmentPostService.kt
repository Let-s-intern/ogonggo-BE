package com.ogonggo.userapi.community.business

import com.ogonggo.core.community.implement.PostAppendCommand
import com.ogonggo.core.community.implement.PostAppender
import com.ogonggo.core.community.implement.PostReader
import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.user.domain.UserStatus
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecruitmentPostService(
    private val userReader: UserReader,
    private val postAppender: PostAppender,
    private val postReader: PostReader,
) {

    @Transactional(readOnly = true)
    fun getRecruitmentPosts(query: RecruitmentPostListQuery): RecruitmentPostPageResult =
        postReader.readPublishedPage(query.page, query.size, query.filter, query.sortType).toResult()

    @Transactional
    fun create(userId: Long, command: PostAppendCommand): Long {
        verifyActiveUser(userId)
        val post = postAppender.append(command.copy(authorUserId = userId))
        return checkNotNull(post.id) { "저장된 모집글 식별자가 없습니다." }
    }

    private fun verifyActiveUser(userId: Long) {
        when (userReader.read(userId).status) {
            UserStatus.ACTIVE -> Unit
            UserStatus.SUSPENDED -> throw ForbiddenException(UserErrorCode.USER_SUSPENDED)
            UserStatus.WITHDRAWN -> throw ForbiddenException(UserErrorCode.USER_WITHDRAWN)
        }
    }
}
