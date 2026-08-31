package com.ogonggo.userapi.response

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class SuccessResponseTest {

    @Test
    fun `ok 응답은 200 상태와 성공 메시지와 데이터를 보존한다`() {
        val data = mapOf("id" to 1L)

        val response = SuccessResponse.ok(data)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(200, response.body?.status)
        assertEquals("요청이 성공했습니다.", response.body?.message)
        assertEquals(data, response.body?.data)
    }

    @Test
    fun `created 응답은 201 상태와 null 데이터를 반환할 수 있다`() {
        val response = SuccessResponse.created()

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(201, response.body?.status)
        assertEquals("요청이 성공했습니다.", response.body?.message)
        assertNull(response.body?.data)
    }
}
