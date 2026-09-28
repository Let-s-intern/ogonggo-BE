package com.ogonggo.core.region.domain

/**
 * 5자리 행정구역 코드를 [Region]·[SubRegion]과 맞출 수 있게 다듬는다.
 *
 * 외부 원천(고용24 도로명코드 등)은 행정구역이 바뀐 뒤에도 예전 코드를 보내므로 현재 코드로 바꾼다.
 * - 강원(42 → 51), 전북(45 → 52), 전남(46 → 12)은 앞 두 자리만 바뀌었다.
 * - 광주(29)의 구는 전남광주(12)에서 번호가 달라져 하나씩 옮긴다.
 * 인천의 개편 전 중구·동구·서구는 새 구와 일대일로 대응하지 않아 옮기지 않는다. 이 코드는 시·도까지만 찾는다.
 */
internal object AdministrativeCodes {

    private val LEGACY_PREFIXES = mapOf("42" to "51", "45" to "52", "46" to "12")
    private val LEGACY_GWANGJU = mapOf(
        "29110" to "12210",
        "29140" to "12240",
        "29155" to "12270",
        "29170" to "12300",
        "29200" to "12330",
    )

    /** 앞 5자리를 현재 행정구역 코드로 바꾼다. 5자리 숫자로 시작하지 않으면 null이다. */
    fun normalize(code: String): String? {
        val head = code.trim().take(CODE_LENGTH)
        if (head.length != CODE_LENGTH || !head.all(Char::isDigit)) return null
        LEGACY_GWANGJU[head]?.let { return it }
        return LEGACY_PREFIXES[head.take(2)]?.let { it + head.drop(2) } ?: head
    }

    /** 일반구 코드(41135 성남시 분당구)의 소속 시 코드(41130)다. */
    fun cityOf(code: String): String = code.dropLast(1) + "0"

    private const val CODE_LENGTH = 5
}
