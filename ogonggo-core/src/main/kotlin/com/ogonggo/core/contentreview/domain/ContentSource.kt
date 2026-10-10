package com.ogonggo.core.contentreview.domain

import com.ogonggo.core.enumeration.EnumField

/**
 * 콘텐츠의 등록 경로다. 등록할 때 정해져 `source` 칼럼에 저장하고 바꾸지 않는다.
 *
 * 소유자가 없는 콘텐츠도 크롤러와 고용24처럼 들어온 곳이 달라 소유자 유무만으로는 구별할 수 없다.
 * 대신 소유자와 어긋나지 않도록 [requireConsistent]로 비즈니스 등록만 소유자가 있게 한다.
 */
enum class ContentSource(
    override val code: Int,
    override val desc: String,
) : EnumField {
    CRAWLER(1, "크롤링"),
    COMPANY(2, "비즈니스 등록"),
    WORK24(3, "고용24"),
    ;

    companion object {
        const val EXTERNAL_ID_MAX_LENGTH = 100

        /** 등록 경로를 따로 정하지 않았을 때의 값이다. 소유자가 있으면 비즈니스 등록, 없으면 크롤링이다. */
        fun of(ownerUserId: Long?): ContentSource = if (ownerUserId == null) CRAWLER else COMPANY

        /**
         * 비즈니스 등록만 소유자가 있고, 고용24 수집만 외부 식별값을 가진다.
         * 외부 식별값은 고용24가 공고·과정마다 매기는 번호로, 같은 콘텐츠를 다시 등록하지 않는 데 쓴다.
         */
        fun requireConsistent(source: ContentSource, ownerUserId: Long?, externalId: String?) {
            require((source == COMPANY) == (ownerUserId != null)) { "비즈니스 등록 콘텐츠만 소유자가 있습니다." }
            require((source == WORK24) == (externalId != null)) { "고용24 수집 콘텐츠만 외부 식별값을 가집니다." }
            require(externalId == null || (externalId.isNotBlank() && externalId.length <= EXTERNAL_ID_MAX_LENGTH)) {
                "외부 식별값은 비어 있거나 ${EXTERNAL_ID_MAX_LENGTH}자를 넘을 수 없습니다."
            }
        }
    }
}
