package com.ogonggo.core.job.domain

import com.ogonggo.core.review.domain.ContentSource
import java.time.LocalDate

/**
 * 공개 목록 최신순의 정렬 키다. 등록할 때 한 번 정하고 바꾸지 않으며, 큰 값이 앞에 온다.
 * 크롤러 공고가 먼저 오고, 그 안에서 등록일이 늦은 공고가 먼저 오며, 같은 날 등록한 공고끼리는 무작위로 섞인다.
 *
 * 크롤러는 한 회사의 직무를 한꺼번에 등록하므로 식별자순으로 보이면 한 페이지가 한 회사로 채워진다.
 * 조회할 때 섞으면 요청마다 조건에 맞는 공고를 전부 읽어 정렬해야 하므로, 등록할 때 섞어 두고 인덱스 순서로 읽는다.
 * 무작위라 같은 회사 공고가 드물게 붙을 수 있지만, 회사별 순번을 세는 쿼리와 인덱스를 등록마다 치르지 않는다.
 *
 * 한 값에 `[크롤러 여부 1비트][등록일 20비트][무작위 32비트]`를 담아 인덱스 하나로 정렬한다.
 */
object JobListSortKey {

    private const val SHUFFLE_BITS = 32
    private const val DAY_BITS = 20
    private const val CRAWLER_FLAG = 1L shl (SHUFFLE_BITS + DAY_BITS)
    private const val SHUFFLE_MASK = (1L shl SHUFFLE_BITS) - 1

    fun of(source: ContentSource, registeredOn: LocalDate, shuffle: Int): Long {
        val epochDay = registeredOn.toEpochDay()
        require(epochDay in 0 until (1L shl DAY_BITS)) { "정렬 키에 담을 수 없는 등록일입니다." }
        val priority = if (source == ContentSource.CRAWLER) CRAWLER_FLAG else 0L
        return priority or (epochDay shl SHUFFLE_BITS) or (shuffle.toLong() and SHUFFLE_MASK)
    }
}
