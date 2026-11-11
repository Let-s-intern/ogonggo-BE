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
    ADVERTISEMENT_CONTRACT_RECEIVED(
        templateCode = "advertisement_contract_received",
        subject = "[오공고] 광고 계약 신청이 접수되었습니다",
        bodyPath = "mail/advertisement-contract-received",
    ),

    /** 운영자가 광고 계약 상세에서 비즈니스 계정을 발급하면 관리자 API가 적재한다. */
    COMPANY_ACCOUNT_ISSUED(
        templateCode = "company_account_issued",
        subject = "[오공고] 비즈니스 계정이 발급되었습니다",
        bodyPath = "mail/company-account-issued",
    ),

    /** 비즈니스 회원이 비밀번호 찾기를 하면 사용자 API가 적재한다. */
    COMPANY_PASSWORD_RESET(
        templateCode = "company_password_reset",
        subject = "[오공고] 비즈니스 계정 임시 비밀번호를 보내드립니다",
        bodyPath = "mail/company-password-reset",
    ),
    ;

    // 발송마다 파일을 읽지 않도록 처음 쓸 때 한 번만 읽는다. 파일이 없으면 발송 시점에 예외로 드러난다.
    private val text by lazy { read("$bodyPath.txt") }
    private val html by lazy { read("$bodyPath.html") }

    /** 변수가 생기면 parameters로 받아 본문에 채운다. 지금 템플릿은 모든 수신자에게 같은 본문을 보낸다. */
    fun render(parameters: Map<String, String>): EmailContent = EmailContent(
        subject = subject,
        text = text.render(parameters, escapeHtml = false),
        html = html.render(parameters, escapeHtml = true),
    )

    private fun read(path: String): String = ClassPathResource(path).getContentAsString(Charsets.UTF_8)

    private fun String.render(parameters: Map<String, String>, escapeHtml: Boolean): String =
        PLACEHOLDER.replace(this) { match ->
            val value = parameters[match.groupValues[1]].orEmpty()
            if (escapeHtml) value.escapeHtml() else value
        }

    private fun String.escapeHtml(): String = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    companion object {
        private val PLACEHOLDER = Regex("\\{\\{([A-Za-z][A-Za-z0-9]*)}}")
        fun findByTemplateCode(templateCode: String): EmailTemplate? = entries.firstOrNull { it.templateCode == templateCode }
    }
}

internal data class EmailContent(
    val subject: String,
    val text: String,
    val html: String,
)
