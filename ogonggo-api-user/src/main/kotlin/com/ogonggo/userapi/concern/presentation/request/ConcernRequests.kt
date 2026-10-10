package com.ogonggo.userapi.concern.presentation.request

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernComment
import com.ogonggo.core.concern.domain.ConcernSortType
import com.ogonggo.userapi.concern.business.ConcernListQuery
import com.ogonggo.userapi.concern.business.CreateConcernCommentCommand
import com.ogonggo.userapi.concern.business.SaveConcernCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

class ConcernListRequest {
    @field:Schema(defaultValue = "1", minimum = "1")
    @field:Min(1)
    var page: Int = 1

    @field:Schema(defaultValue = "10", minimum = "1", maximum = "100")
    @field:Min(1)
    @field:Max(100)
    var size: Int = 10

    @field:Schema(description = "보내지 않으면 전체 카테고리")
    var category: ConcernCategory? = null

    var sort: ConcernSortType = ConcernSortType.LATEST

    fun toQuery(): ConcernListQuery = ConcernListQuery(
        category = category,
        sortType = sort,
        page = page - 1,
        size = size,
    )
}

data class SaveConcernRequest(
    val category: ConcernCategory,
    @field:NotBlank
    @field:Size(max = Concern.TITLE_MAX_LENGTH)
    val title: String,
    @field:NotBlank
    @field:Size(max = Concern.CONTENT_MAX_LENGTH)
    val content: String,
) {
    fun toCommand(): SaveConcernCommand = SaveConcernCommand(
        category = category,
        title = title,
        content = content,
    )
}

data class CreateConcernCommentRequest(
    @field:NotBlank
    @field:Size(max = ConcernComment.CONTENT_MAX_LENGTH)
    val content: String,
    @field:Schema(description = "답글을 달 답변(부모 댓글) 식별자. 보내지 않으면 답변을 작성합니다.")
    @field:Positive
    val parentId: Long? = null,
) {
    fun toCommand(): CreateConcernCommentCommand = CreateConcernCommentCommand(
        parentId = parentId,
        content = content,
    )
}
