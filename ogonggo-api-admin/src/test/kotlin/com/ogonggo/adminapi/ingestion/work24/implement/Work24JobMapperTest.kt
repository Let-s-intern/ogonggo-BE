package com.ogonggo.adminapi.ingestion.work24.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.region.domain.Region
import com.ogonggo.core.region.domain.SubRegion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class Work24JobMapperTest {

    private val objectMapper = ObjectMapper()

    @Test
    fun `도로명코드가 없으면 지역명으로 시·도와 시·군·구를 찾는다`() {
        val dto = map("""{"region": "경기도 화성시 동탄구"}""")

        assertEquals(Region.GYEONGGI, dto.region)
        assertEquals(SubRegion.GYEONGGI_HWASEONG_SI, dto.subRegion)
    }

    @Test
    fun `시·군·구를 알 수 없으면 시·도만 남긴다`() {
        val dto = map("""{"region": "인천 중구", "strtnmCd": "281104100001"}""")

        assertEquals(Region.INCHEON, dto.region)
        assertNull(dto.subRegion)
    }

    private fun map(itemFields: String) = Work24JobMapper.toAppendDto(
        item = objectMapper.readTree(itemFields.dropLast(1) + """, "company": "오공고", "title": "백엔드 개발자"}"""),
        detail = objectMapper.readTree("{}"),
        sourceUrl = "https://www.work24.go.kr/wanted/1",
    )
}
