package com.ogonggo.core.region.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegionTest {

    @Test
    fun `시·군·구는 속한 시·도의 행정구역 코드로 시작하고 코드가 겹치지 않는다`() {
        assertTrue(SubRegion.entries.all { it.administrativeCode.startsWith(it.region.administrativeCode!!) })
        assertEquals(SubRegion.entries.size, SubRegion.entries.map { it.administrativeCode }.distinct().size)
    }

    @Test
    fun `도로명코드처럼 긴 코드도 앞 5자리 행정구역 코드로 시·군·구를 찾는다`() {
        assertEquals(SubRegion.SEOUL_GWANAK_GU, SubRegion.fromAdministrativeCode("116204160785"))
        assertEquals(Region.SEOUL, Region.fromAdministrativeCode("116204160785"))
    }

    @Test
    fun `일반구 코드는 소속 시로 찾는다`() {
        assertEquals(SubRegion.GYEONGGI_SEONGNAM_SI, SubRegion.fromAdministrativeCode("41135"))
        assertEquals(SubRegion.GYEONGGI_HWASEONG_SI, SubRegion.fromAdministrativeCode("41597"))
    }

    @Test
    fun `통합·개편 전 코드는 현재 시·도와 시·군·구로 찾는다`() {
        // 전라북도(45)는 전북특별자치도(52)로, 전주시 완산구는 전주시로 찾는다.
        assertEquals(SubRegion.JEONBUK_JEONJU_SI, SubRegion.fromAdministrativeCode("45111"))
        assertEquals(SubRegion.GANGWON_YEONGWOL_GUN, SubRegion.fromAdministrativeCode("42750"))
        // 광주광역시의 구는 전남광주에서 번호가 다르다.
        assertEquals(SubRegion.JEONNAM_GWANGJU_DONG_GU, SubRegion.fromAdministrativeCode("29110"))
        assertEquals(SubRegion.JEONNAM_GWANGJU_MOKPO_SI, SubRegion.fromAdministrativeCode("46110"))
    }

    @Test
    fun `새 구와 대응하지 않는 인천의 개편 전 구는 시·도까지만 찾는다`() {
        assertNull(SubRegion.fromAdministrativeCode("28110"))
        assertEquals(Region.INCHEON, Region.fromAdministrativeCode("28110"))
    }

    @Test
    fun `숫자 5자리로 시작하지 않는 코드는 찾지 않는다`() {
        assertNull(SubRegion.fromAdministrativeCode("1168"))
        assertNull(Region.fromAdministrativeCode("서울"))
    }
}
