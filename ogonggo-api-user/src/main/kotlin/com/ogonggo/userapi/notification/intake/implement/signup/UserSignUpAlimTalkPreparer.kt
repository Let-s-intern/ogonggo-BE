package com.ogonggo.userapi.notification.intake.implement.signup

import com.ogonggo.userapi.notification.channel.alimtalk.AlimTalkRecipientNumberPolicy
import com.ogonggo.userapi.auth.business.UserSignedUpEvent
import com.ogonggo.userapi.notification.channel.alimtalk.dto.SignUpAlimTalkParametersDto
import com.ogonggo.userapi.notification.intake.implement.signup.dto.SignUpAlimTalkPreparationDto
import com.ogonggo.userapi.notification.intake.implement.signup.dto.SignUpAlimTalkSkipReason
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

/**
 * 가입 이벤트 스냅샷을 템플릿 변수로 변환하고 발송 불가능한 입력은 사유 코드로 돌려준다.
 * 변환 규칙을 listener의 비동기·예외 처리와 분리해 입력별 동작을 독립적으로 테스트할 수 있다.
 */
@Component
internal class UserSignUpAlimTalkPreparer {

    fun prepare(event: UserSignedUpEvent): SignUpAlimTalkPreparationDto {
        // 누락된 개인정보 원문은 반환하거나 로그에 남기지 않고 어떤 입력이 부족했는지만 구분한다.
        val name = event.name?.takeIf(String::isNotBlank)
            ?: return SignUpAlimTalkPreparationDto.Skipped(SignUpAlimTalkSkipReason.MISSING_NAME)
        val email = event.email?.takeIf(String::isNotBlank)
            ?: return SignUpAlimTalkPreparationDto.Skipped(SignUpAlimTalkSkipReason.MISSING_EMAIL)
        val phoneNumber = event.phoneNum?.takeIf(String::isNotBlank)
            ?: return SignUpAlimTalkPreparationDto.Skipped(SignUpAlimTalkSkipReason.MISSING_PHONE_NUMBER)
        val recipientNo = AlimTalkRecipientNumberPolicy.normalize(phoneNumber)
        if (recipientNo == null) {
            return SignUpAlimTalkPreparationDto.Skipped(SignUpAlimTalkSkipReason.INVALID_PHONE_NUMBER)
        }
        val provider = event.authProvider
            ?: return SignUpAlimTalkPreparationDto.Skipped(SignUpAlimTalkSkipReason.MISSING_AUTH_PROVIDER)
        return SignUpAlimTalkPreparationDto.Ready(
            templateCode = SIGN_UP_TEMPLATE_CODE,
            recipientNo = recipientNo,
            templateParameters = SignUpAlimTalkParametersDto(
                name = name,
                userEmail = email,
                loginType = "${provider.desc} 로그인",
                createDate = event.joinedAt.toLocalDate().format(DATE_FORMATTER),
            ),
        )
    }

    private companion object {
        const val SIGN_UP_TEMPLATE_CODE = "sign_up_confirm"
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
