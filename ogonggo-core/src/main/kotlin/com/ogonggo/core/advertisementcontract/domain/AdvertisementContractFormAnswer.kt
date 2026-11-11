package com.ogonggo.core.advertisementcontract.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 광고 신청 폼의 문항 하나에 대한 답이다. 칸이 따로 없는 문항(희망 상품, 희망 기간 등)을 받은 순서대로 남긴다.
 * 폼 문항은 자주 바뀌므로 [key]의 뜻을 서버가 정하지 않는다. [label]은 신청 당시 화면에 보인 문항 이름이다.
 *
 * `advertisement_contracts.form_answers` JSON 칸에 목록으로 저장한다. Hibernate가 Jackson으로 읽으므로
 * Kotlin 모듈이 없는 환경에서도 만들 수 있게 생성자를 직접 알려 준다.
 */
data class AdvertisementContractFormAnswer @JsonCreator constructor(
    @JsonProperty("key") val key: String,
    @JsonProperty("label") val label: String,
    @JsonProperty("value") val value: String,
) {
    init {
        require(key.isNotBlank() && key.length <= MAX_KEY_LENGTH) { "문항 키는 비울 수 없고 ${MAX_KEY_LENGTH}자 이하여야 합니다." }
        require(label.isNotBlank() && label.length <= MAX_LABEL_LENGTH) { "문항 이름은 비울 수 없고 ${MAX_LABEL_LENGTH}자 이하여야 합니다." }
        require(value.isNotBlank() && value.length <= MAX_VALUE_LENGTH) { "답은 비울 수 없고 ${MAX_VALUE_LENGTH}자 이하여야 합니다." }
    }

    companion object {
        const val MAX_COUNT = 30
        const val MAX_KEY_LENGTH = 50
        const val MAX_LABEL_LENGTH = 50
        const val MAX_VALUE_LENGTH = 1000
    }
}
