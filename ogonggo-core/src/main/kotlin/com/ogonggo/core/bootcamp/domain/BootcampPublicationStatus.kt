package com.ogonggo.core.bootcamp.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 부트캠프를 사용자에게 노출하는지 나타낸다. 칼럼 이름과 값은 채용공고의 게시 상태와 맞춘다.
 * 모집 상태(`BootcampStatus`)와는 다른 축이라, 모집이 끝난 과정을 남겨 두거나 모집 중인 과정을 내릴 수 있다.
 * 보관은 채용공고와 값을 맞추려고 두었으며 아직 보관하는 경로는 없다.
 */
enum class BootcampPublicationStatus(
    override val code: Int,
    override val desc: String,
) : EnumField {
    DRAFT(1, "초안"),
    PUBLISHED(2, "게시"),
    HIDDEN(3, "숨김"),
    ARCHIVED(4, "보관"),
}
