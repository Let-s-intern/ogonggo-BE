package com.ogonggo.core.jpa

import com.querydsl.jpa.impl.JPAQuery
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

/** 목록 쿼리에 [pageable]의 범위를 건다. */
fun <T> JPAQuery<T>.paged(pageable: Pageable): JPAQuery<T> =
    offset(pageable.offset).limit(pageable.pageSize.toLong())

/**
 * 조회한 내용과 전체 개수 쿼리로 페이지를 만든다.
 * 첫 페이지가 다 차지 않았거나 마지막 페이지처럼 내용만으로 전체 개수를 알 수 있으면 [countQuery]를 실행하지 않는다.
 */
fun <T> pageOf(content: List<T>, pageable: Pageable, countQuery: JPAQuery<Long>): Page<T> =
    PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
