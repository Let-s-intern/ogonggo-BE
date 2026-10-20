package com.ogonggo.userapi.notification.channel.email

/**
 * 메일은 알림톡과 달리 provider에 템플릿을 등록하지 않으므로 제목과 본문을 애플리케이션이 소유한다.
 * 알림 행에는 template code와 변수만 저장하고, 발송 시점에 이 템플릿으로 본문을 만든다.
 */
internal enum class EmailTemplate(
    val templateCode: String,
) {
    SIGN_UP_WELCOME("sign_up_welcome") {
        override fun render(parameters: Map<String, String>): EmailContent {
            // 이름은 렛츠커리어가 주지 않을 수 있다. 이름이 없다고 환영 메일을 빼지 않고 인사만 줄인다.
            val greeting = parameters["name"]?.takeIf(String::isNotBlank)?.let { "안녕하세요, ${it}님." } ?: "안녕하세요."
            return EmailContent(
                subject = "[오공고] 오공고에 오신 것을 환영합니다",
                body = buildString {
                    append(greeting).append('\n')
                    append("오공고에 가입해 주셔서 감사합니다.\n\n")
                    append("오공고에서는 이런 것들을 할 수 있어요.\n")
                    append("  - 채용공고와 부트캠프를 직군·지역·고용 형태로 골라 보기\n")
                    append("  - 마음에 드는 공고를 북마크하고 지원 단계를 관리하기\n")
                    append("  - 매일 운영자가 고른 오늘의 공고 받아 보기\n")
                    append("  - 사이드 프로젝트·스터디 팀원을 모으고 취업 고민 나누기\n\n")
                    append("감사합니다.\n")
                    append("오공고 드림\n")
                },
            )
        }
    },
    ;

    abstract fun render(parameters: Map<String, String>): EmailContent

    companion object {
        fun findByTemplateCode(templateCode: String): EmailTemplate? = entries.firstOrNull { it.templateCode == templateCode }
    }
}

internal data class EmailContent(
    val subject: String,
    val body: String,
)
