package com.ogonggo.adminapi.community.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.community.domain.PublicationStatus
import com.ogonggo.core.community.domain.RecruitmentPost
import com.ogonggo.core.community.domain.RecruitmentPostConsoleSearchCondition
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.implement.PostMetricReader
import com.ogonggo.core.community.implement.RecruitmentPostManager
import com.ogonggo.core.community.implement.RecruitmentPostReader
import com.ogonggo.core.user.implement.UserProfileReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminRecruitmentPostService(
    private val postReader: RecruitmentPostReader,
    private val postManager: RecruitmentPostManager,
    private val postMetricReader: PostMetricReader,
    private val userProfileReader: UserProfileReader,
) {

    /** 포지션·기술 스택은 지연 로딩 컬렉션이라 결과로 옮길 때까지 영속성 컨텍스트를 연다. 사용자 목록과 같다. */
    @Transactional(readOnly = true)
    fun getRecruitmentPosts(
        condition: RecruitmentPostConsoleSearchCondition,
        sortType: RecruitmentPostSortType,
        page: Int,
        size: Int,
    ): AdminRecruitmentPostPageResult {
        val result = postReader.readConsolePage(condition, sortType, page, size)
        return AdminRecruitmentPostPageResult.from(
            result = result,
            metrics = postMetricReader.readAll(result.posts.map { it.requiredId() }),
            profiles = userProfileReader.readAll(result.posts.map { it.authorUserId }.distinct()),
        )
    }

    /**
     * 채용공고·부트캠프 일괄 노출 변경과 같이 한 트랜잭션에서 모두 바꾸고, 하나라도 없으면 아무것도 바꾸지 않는다.
     * 이미 같은 노출인 모집글은 건드리지 않는다. 임시저장은 작성자만 보는 글이라 없는 모집글로 본다.
     */
    @Transactional
    fun changeVisibilities(command: AdminRecruitmentPostVisibilityChangeCommand) {
        postReader.readAllPostedForUpdate(command.postIds)
            .filter { post -> post.visibility() != command.visibility }
            .forEach { post ->
                when (command.visibility) {
                    AdminContentVisibility.VISIBLE -> postManager.unhide(post)
                    AdminContentVisibility.HIDDEN -> postManager.hide(post)
                }
            }
    }

    private fun RecruitmentPost.visibility(): AdminContentVisibility =
        AdminContentVisibility.of(publicationStatus == PublicationStatus.PUBLISHED)
}
