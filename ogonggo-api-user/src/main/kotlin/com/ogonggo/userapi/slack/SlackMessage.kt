package com.ogonggo.userapi.slack

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 슬랙 인커밍 웹훅으로 보내는 Block Kit 메시지다.
 *
 * text는 푸시 알림과 채널 목록 미리보기에만 쓰인다. blocks가 있으면 슬랙이 text를 본문에 그리지 않으므로,
 * 본문에도 요약이 보이도록 [withHeadline]으로 같은 줄을 첫 블록에 굵게 넣는다.
 */
internal data class SlackMessage(
    val text: String,
    val blocks: List<SlackBlock>,
) {
    companion object {
        /** [headline]은 이스케이프를 마친 값이어야 한다. 알림 미리보기와 본문 첫 줄에 같이 쓴다. */
        fun withHeadline(headline: String, blocks: List<SlackBlock>): SlackMessage =
            SlackMessage(
                text = headline,
                blocks = listOf(SlackBlock.text(SlackText(text = "*$headline*"))) + blocks,
            )
    }
}

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

        /** 누르면 [url]로 가는 링크다. [url]은 서버가 만든 주소만 넣는다. 사용자 값이 들어가면 `|`·`>`로 링크가 깨진다. */
        fun link(url: String, label: String): SlackText = SlackText(text = "<$url|${escape(label)}>")

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
