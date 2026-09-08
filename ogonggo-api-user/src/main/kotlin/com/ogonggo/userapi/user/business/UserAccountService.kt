package com.ogonggo.userapi.user.business

import com.ogonggo.core.user.implement.CompanyProfileReader
import com.ogonggo.core.user.implement.UserProfileJobInfoCommand
import com.ogonggo.core.user.implement.UserProfileManager
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class UserAccountService(
    private val userReader: UserReader,
    private val userProfileReader: UserProfileReader,
    private val companyProfileReader: CompanyProfileReader,
    private val userProfileManager: UserProfileManager,
    private val clock: Clock,
) {

    /**
     * 자기 자신을 보는 조회이므로 정지·탈퇴 상태여도 막지 않고 상태를 그대로 담아 응답한다.
     * 액세스 토큰은 상태가 바뀌어도 만료까지 유효하므로, 클라이언트가 왜 다른 요청이 막히는지 알 수 있어야 한다.
     *
     * 역할은 토큰에 담지 않으므로 여기서 계정을 읽어 확인한다.
     */
    fun getMyAccount(userId: Long): MyAccountResult = MyAccountResult.from(
        account = userReader.read(userId),
        // 역할로 나누지 않고 행이 있는지로 판단한다.
        // 프로필은 렛츠커리어 로그인에서, 기업 정보는 기업 회원가입에서 생기므로 한쪽만 있는 것이 정상이다.
        profile = userProfileReader.read(userId),
        companyProfile = companyProfileReader.read(userId),
    )

    /**
     * 사용자가 직접 입력하는 학력과 희망 조건만 교체한다.
     * 이름·닉네임·프로필 이미지는 렛츠커리어가 소유해 로그인마다 갱신되므로 여기서 바꾸지 않는다.
     */
    @Transactional
    fun replaceMyProfile(userId: Long, command: UserProfileJobInfoCommand) {
        userProfileManager.replaceJobInfo(userId, command, LocalDateTime.now(clock))
    }
}
