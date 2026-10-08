package com.ogonggo.userapi.concern.business

import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernSortType

data class ConcernListQuery(
    val category: ConcernCategory?,
    val sortType: ConcernSortType,
    val page: Int,
    val size: Int,
)

data class SaveConcernCommand(
    val category: ConcernCategory,
    val title: String,
    val content: String,
)

data class CreateConcernCommentCommand(
    val parentId: Long?,
    val content: String,
)
