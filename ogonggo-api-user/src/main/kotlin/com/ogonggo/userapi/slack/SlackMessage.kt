package com.ogonggo.userapi.slack

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 슬랙 인커밍 웹훅으로 보내는 Block Kit 메시지다.
 *
 * text는 채널 목록과 푸시 알림에 뜨는 줄이다. 열어보지 않고도 무슨 건인지 알 수 있게 요약을 넣는다.
 */
internal data class SlackMessage(
    val text: String,
    val blocks: List<SlackBlock>,
)

/**
 * section 블록은 text 와 fields 중 쓰는 쪽만 담아야 한다.
 * 쓰지 않는 쪽을 null 로 직렬화해 보내면 슬랙이 400 invalid_blocks 로 거절한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class SlackBlock(
    val type: String = SECTION_TYPE,
    val text: SlackText? = null,
    val fields: List<SlackText>? = null,
) {
    companion object {
        private const val SECTION_TYPE = "section"

        fun text(text: SlackText): SlackBlock = SlackBlock(text = text)

        fun fields(fields: List<SlackText>): SlackBlock = SlackBlock(fields = fields)
    }
}

internal data class SlackText(
    val type: String = MARKDOWN_TYPE,
    val text: String,
) {
    companion object {
        private const val MARKDOWN_TYPE = "mrkdwn"
        private const val EMPTY = "-"

        /** 제목을 굵게 쓰고 값을 다음 줄에 둔다. 슬랙이 필드를 두 열로 배치한다. */
        fun labeled(label: String, value: String?): SlackText =
            SlackText(text = "*$label*\n${escape(value.orEmpty()).ifBlank { EMPTY }}")

        /**
         * 슬랙 mrkdwn은 `&`, `<`, `>`를 제어 문자로 읽는다.
         * 사용자가 넣은 값이 링크나 멘션 문법으로 해석되지 않도록 먼저 치환한다.
         */
        fun escape(value: String): String = value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
