package com.ogonggo.core.job.domain

import com.ogonggo.core.contentreview.domain.ContentSource
import java.time.LocalDate

/**
 * 공개 목록 최신순의 정렬 키다. 큰 값이 앞에 온다.
 * 크롤러 공고가 먼저 오고, 그 안에서 등록일이 늦은 공고가 먼저 온다.
 * 같은 날 등록한 공고끼리는 오늘의 공고(오공고 PICK)와 직군이 같은 공고가 먼저 오고, 그 안에서는 무작위로 섞인다.
 *
 * 크롤러는 한 회사의 직무를 한꺼번에 등록하므로 식별자순으로 보이면 한 페이지가 한 회사로 채워진다.
 * 조회할 때 섞으면 요청마다 조건에 맞는 공고를 전부 읽어 정렬해야 하므로, 등록할 때 섞어 두고 인덱스 순서로 읽는다.
 * 무작위라 같은 회사 공고가 드물게 붙을 수 있지만, 회사별 순번을 세는 쿼리와 인덱스를 등록마다 치르지 않는다.
 *
 * PICK 직군 비트도 같은 이유로 조회할 때가 아니라 PICK을 바꿀 때와 공고를 등록할 때 정해 둔다.
 * PICK은 하루에 한 번쯤 바뀌므로, 바꿀 때마다 공고 전체의 비트를 다시 쓰는 비용이 목록 조회마다 정렬하는 비용보다 작다.
 * 등록일보다 아래 자리에 두어 오래된 PICK 직군 공고가 최신 공고를 밀어내지 않게 한다.
 *
 * 한 값에 `[크롤러 여부 1비트][등록일 20비트][PICK 직군 1비트][무작위 31비트]`를 담아 인덱스 하나로 정렬한다.
 */
object JobListSortKey {

    private const val SHUFFLE_BITS = 31
    private const val DAY_BITS = 20
    private const val SHUFFLE_MASK = (1L shl SHUFFLE_BITS) - 1

    /** PICK과 직군이 같은 공고에 켜는 비트다. 정렬 키를 다시 쓰는 벌크 갱신이 이 값으로 비트를 켜고 끈다. */
    const val PICK_FIELD_BIT = 1L shl SHUFFLE_BITS
    private const val DAY_SHIFT = SHUFFLE_BITS + 1
    private const val CRAWLER_FLAG = 1L shl (DAY_SHIFT + DAY_BITS)

    fun of(source: ContentSource, registeredOn: LocalDate, pickField: Boolean, shuffle: Int): Long {
        val epochDay = registeredOn.toEpochDay()
        require(epochDay in 0 until (1L shl DAY_BITS)) { "정렬 키에 담을 수 없는 등록일입니다." }
        val priority = if (source == ContentSource.CRAWLER) CRAWLER_FLAG else 0L
        val pick = if (pickField) PICK_FIELD_BIT else 0L
        return priority or (epochDay shl DAY_SHIFT) or pick or (shuffle.toLong() and SHUFFLE_MASK)
    }
}
