package com.ogonggo.userapi.response

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PageResponseTest {

    @Test
    fun `내부 0 기반 페이지를 외부 1 기반 페이지 정보로 변환한다`() {
        val response = PageResponse.fromZeroBased(
            items = listOf("item"),
            page = 2,
            size = 10,
            totalElements = 31,
            totalPages = 4,
        )

        assertEquals(listOf("item"), response.items)
        assertEquals(PageInfo(3, 10, 31, 4), response.pageInfo)
    }
}
