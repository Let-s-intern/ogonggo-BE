package com.ogonggo.userapi.notification.channel.email

import org.springframework.core.io.ClassPathResource

/**
 * 메일은 알림톡과 달리 provider에 템플릿을 등록하지 않으므로 제목과 본문을 애플리케이션이 소유한다.
 * 알림 행에는 template code와 변수만 저장하고, 발송 시점에 이 템플릿으로 본문을 만든다.
 *
 * 본문은 기획이 문구를 바로 고칠 수 있도록 클래스패스 `mail/` 아래 파일로 둔다.
 * HTML을 막아 둔 메일 클라이언트도 있어 같은 내용의 텍스트 본문을 함께 보낸다.
 */
internal enum class EmailTemplate(
    val templateCode: String,
    private val subject: String,
    private val bodyPath: String,
) {
    SIGN_UP_WELCOME(
        templateCode = "sign_up_welcome",
        subject = "[오늘의 공고] 가입 선물로 AI 자소서 프롬프트를 보내드려요 🎁",
        bodyPath = "mail/sign-up-welcome",
    ),
    ;

    // 발송마다 파일을 읽지 않도록 처음 쓸 때 한 번만 읽는다. 파일이 없으면 발송 시점에 예외로 드러난다.
    private val text by lazy { read("$bodyPath.txt") }
    private val html by lazy { read("$bodyPath.html") }

    /** 변수가 생기면 parameters로 받아 본문에 채운다. 지금 템플릿은 모든 수신자에게 같은 본문을 보낸다. */
    fun render(@Suppress("UNUSED_PARAMETER") parameters: Map<String, String>): EmailContent =
        EmailContent(subject = subject, text = text, html = html)

    private fun read(path: String): String = ClassPathResource(path).getContentAsString(Charsets.UTF_8)

    companion object {
        fun findByTemplateCode(templateCode: String): EmailTemplate? = entries.firstOrNull { it.templateCode == templateCode }
    }
}

internal data class EmailContent(
    val subject: String,
    val text: String,
    val html: String,
)
