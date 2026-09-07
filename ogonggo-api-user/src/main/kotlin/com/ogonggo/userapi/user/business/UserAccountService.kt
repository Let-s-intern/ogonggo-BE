package com.ogonggo.userapi.user.business

import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.implement.CompanyProfileReader
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import org.springframework.stereotype.Service

@Service
class UserAccountService(
    private val userReader: UserReader,
    private val userProfileReader: UserProfileReader,
    private val companyProfileReader: CompanyProfileReader,
) {

    /**
     * 자기 자신을 보는 조회이므로 정지·탈퇴 상태여도 막지 않고 상태를 그대로 담아 응답한다.
     * 액세스 토큰은 상태가 바뀌어도 만료까지 유효하므로, 클라이언트가 왜 다른 요청이 막히는지 알 수 있어야 한다.
     *
     * 역할은 토큰에 담지 않으므로 여기서 계정을 읽어 확인한다.
     * 계정 종류에 따라 있는 프로필만 조회해 없는 쪽 테이블은 건드리지 않는다.
     */
    fun getMyAccount(userId: Long): MyAccountResult {
        val account = userReader.read(userId)
        val isCompany = account.role == UserRole.COMPANY
        return MyAccountResult.from(
            account = account,
            // 기업 회원만 렛츠커리어 프로필이 없다. ADMIN은 부여 경로가 없지만 생기면 일반 계정과 같은 프로필을 쓴다.
            profile = if (isCompany) null else userProfileReader.read(userId),
            companyProfile = if (isCompany) companyProfileReader.read(userId) else null,
        )
    }
}
