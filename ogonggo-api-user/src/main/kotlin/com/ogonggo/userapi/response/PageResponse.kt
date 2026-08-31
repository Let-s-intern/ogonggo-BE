package com.ogonggo.userapi.response

data class PageResponse<T>(
    val items: List<T>,
    val pageInfo: PageInfo,
) {
    companion object {
        fun <T> fromZeroBased(
            items: List<T>,
            page: Int,
            size: Int,
            totalElements: Long,
            totalPages: Int,
        ): PageResponse<T> = PageResponse(
            items = items,
            pageInfo = PageInfo(
                pageNum = page + 1,
                pageSize = size,
                totalElements = totalElements,
                totalPages = totalPages,
            ),
        )
    }
}

data class PageInfo(
    val pageNum: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
)
