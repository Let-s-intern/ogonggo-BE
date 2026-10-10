package com.ogonggo.adminapi.concern.business

import com.ogonggo.adminapi.content.business.AdminContentVisibility
import com.ogonggo.core.concern.domain.ConcernConsoleSearchCondition
import com.ogonggo.core.concern.domain.ConcernSortType
import com.ogonggo.core.concern.implement.ConcernCommentReader
import com.ogonggo.core.concern.implement.ConcernManager
import com.ogonggo.core.concern.implement.ConcernMetricReader
import com.ogonggo.core.concern.implement.ConcernReader
import com.ogonggo.core.concern.implement.dto.ConcernMetricDto
import com.ogonggo.core.user.implement.UserProfileReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminConcernService(
    private val concernReader: ConcernReader,
    private val concernManager: ConcernManager,
    private val concernMetricReader: ConcernMetricReader,
    private val commentReader: ConcernCommentReader,
    private val userProfileReader: UserProfileReader,
) {

    fun getConcerns(
        condition: ConcernConsoleSearchCondition,
        sortType: ConcernSortType,
        page: Int,
        size: Int,
    ): AdminConcernPageResult {
        val result = concernReader.readConsolePage(condition, sortType, page, size)
        val concernIds = result.concerns.map { it.requiredId() }
        val metrics = concernMetricReader.readAll(concernIds)
        val officialCommentedIds = commentReader.readConcernIdsWithOfficialComment(concernIds)
        val profiles = userProfileReader.readAll(result.concerns.map { it.authorUserId }.toSet())

        return AdminConcernPageResult(
            items = result.concerns.map { concern ->
                val concernId = concern.requiredId()
                AdminConcernSummary.from(
                    concern = concern,
                    metric = metrics[concernId] ?: ConcernMetricDto.EMPTY,
                    hasOfficialComment = concernId in officialCommentedIds,
                    authorProfile = profiles[concern.authorUserId],
                )
            },
            page = result.page,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    /** 숨긴 고민글도 읽는다. 사용자 상세와 달리 조회 수를 올리지 않는다. */
    fun getConcern(concernId: Long): AdminConcernDetail {
        val concern = concernReader.readIncludingHidden(concernId)
        return AdminConcernDetail.from(
            concern = concern,
            metric = concernMetricReader.read(concernId),
            hasOfficialComment = concernId in commentReader.readConcernIdsWithOfficialComment(listOf(concernId)),
            authorProfile = userProfileReader.read(concern.authorUserId),
        )
    }

    /**
     * 사이드·스터디 일괄 노출 변경과 같이 한 트랜잭션에서 모두 바꾸고, 하나라도 없으면 아무것도 바꾸지 않는다.
     * 이미 같은 노출인 고민글은 건드리지 않는다.
     */
    @Transactional
    fun changeVisibilities(command: AdminConcernVisibilityChangeCommand) {
        concernReader.readAllIncludingHiddenForUpdate(command.concernIds)
            .filter { concern -> concern.visibility() != command.visibility }
            .forEach { concern ->
                when (command.visibility) {
                    AdminContentVisibility.VISIBLE -> concernManager.unhide(concern)
                    AdminContentVisibility.HIDDEN -> concernManager.hide(concern)
                }
            }
    }
}
