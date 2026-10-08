package com.ogonggo.userapi.user.business

import com.ogonggo.core.error.ForbiddenException
import com.ogonggo.core.image.implement.ImageAssetManager
import com.ogonggo.core.user.domain.UserRole
import com.ogonggo.core.user.error.UserErrorCode
import com.ogonggo.core.user.implement.CompanyProfileManager
import com.ogonggo.core.user.implement.CompanyProfileReader
import com.ogonggo.core.user.implement.LetsCareerJobProfileOutboxManager
import com.ogonggo.core.user.implement.UserProfileManager
import com.ogonggo.core.user.implement.UserProfileReader
import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.core.user.implement.dto.CompanyBasicInfoUpdateDto
import com.ogonggo.core.user.implement.dto.CompanyLogoDto
import com.ogonggo.core.user.implement.dto.CompanyManagerInfoUpdateDto
import com.ogonggo.core.user.implement.dto.UserProfileJobInfoDto
import com.ogonggo.userapi.user.implement.requireActive
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserAccountService(
    private val userReader: UserReader,
    private val userProfileReader: UserProfileReader,
    private val companyProfileReader: CompanyProfileReader,
    private val userProfileManager: UserProfileManager,
    private val companyProfileManager: CompanyProfileManager,
    private val letsCareerJobProfileOutboxManager: LetsCareerJobProfileOutboxManager,
    private val imageAssetManager: ImageAssetManager,
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
     * 이름·닉네임은 렛츠커리어가 소유해 로그인마다 갱신되므로 여기서 바꾸지 않는다.
     *
     * 학력과 희망 조건은 렛츠커리어와 양쪽에서 고칠 수 있어 렛츠커리어 계정이면 같은 트랜잭션에서 보낼 변경을 적재한다.
     * 렛츠커리어 호출은 아웃박스가 나중에 하므로 렛츠커리어 장애가 이 요청을 실패시키지 않는다.
     */
    @Transactional
    fun replaceMyProfile(userId: Long, command: UserProfileJobInfoDto) {
        val now = LocalDateTime.now(clock)
        userProfileManager.replaceJobInfo(userId, command, now)
        if (userReader.read(userId).letsCareerUserId != null) {
            letsCareerJobProfileOutboxManager.enqueue(userId, now)
        }
    }

    /**
     * 오늘의 공고를 받을 이메일은 오공고가 소유하므로 렛츠커리어에 보내지 않는다.
     * 기업 회원은 담당자 정보의 수신 이메일(PUT /api/v1/users/me/company-profile/manager-info)을 쓰므로 막는다.
     */
    @Transactional
    fun changeMyNotificationEmail(userId: Long, notificationEmail: String?) {
        verifyGeneralMember(userId)
        userProfileManager.changeNotificationEmail(userId, notificationEmail, LocalDateTime.now(clock))
    }

    /**
     * 이미지 업로드(POST /api/v1/images)로 올린 이미지를 프로필 이미지로 쓴다.
     * 오공고가 소유하는 값이라 렛츠커리어에 보내지 않고, 재로그인해도 렛츠커리어 이미지로 돌아가지 않는다.
     * 바꾸기 전에 쓰던 이미지는 참조를 풀어 보존 기간이 지나면 정리되게 한다.
     */
    @Transactional
    fun replaceMyProfileImage(userId: Long, imageId: String) {
        verifyGeneralMember(userId)
        val now = LocalDateTime.now(clock)
        val url = imageAssetManager.attachProfileImage(userId, imageId)
        val previousImageId = userProfileManager.changeOgonggoProfileImage(userId, imageId, url, now)
        if (previousImageId != null && previousImageId != imageId) {
            imageAssetManager.unreferenceProfileImage(userId, previousImageId, now)
        }
    }

    /** 오공고에서 올린 프로필 이미지를 지워 렛츠커리어 이미지로 되돌린다. 지울 것이 없어도 성공으로 본다. */
    @Transactional
    fun deleteMyProfileImage(userId: Long) {
        verifyGeneralMember(userId)
        val removedImageId = userProfileManager.removeOgonggoProfileImage(userId) ?: return
        imageAssetManager.unreferenceProfileImage(userId, removedImageId, LocalDateTime.now(clock))
    }

    /**
     * 기관명과 로고를 함께 교체한다. 로고는 이미지 업로드(POST /api/v1/images)로 올린 이미지를 연결하며 null이면 지운다.
     * 바꾸거나 지운 이전 로고 이미지는 참조를 풀어 보존 기간이 지나면 정리되게 한다.
     * 다른 기업 회원 기능과 같이 정지·탈퇴한 계정과 일반 회원은 막는다.
     */
    @Transactional
    fun replaceMyCompanyBasicInfo(userId: Long, command: CompanyBasicInfoCommand) {
        verifyCompany(userId)
        val logo = command.logoImageId?.let { imageId ->
            CompanyLogoDto(imageId = imageId, url = imageAssetManager.attachProfileImage(userId, imageId))
        }
        val previousLogoImageId = companyProfileManager.replaceBasicInfo(
            userId,
            CompanyBasicInfoUpdateDto(organizationName = command.organizationName, logo = logo),
        )
        if (previousLogoImageId != null && previousLogoImageId != command.logoImageId) {
            imageAssetManager.unreferenceProfileImage(userId, previousLogoImageId, LocalDateTime.now(clock))
        }
    }

    /** 담당자 이름·연락처·수신 이메일을 함께 교체한다. 선택 값을 빼면 비운다. */
    @Transactional
    fun replaceMyCompanyManagerInfo(userId: Long, command: CompanyManagerInfoUpdateDto) {
        verifyCompany(userId)
        companyProfileManager.replaceManagerInfo(userId, command)
    }

    /** 오늘의 공고 수신 이메일과 프로필 이미지는 일반 회원의 값이다. 기업 회원은 담당자 정보의 수신 이메일과 기업 로고를 쓴다. */
    private fun verifyGeneralMember(userId: Long) {
        if (userReader.read(userId).letsCareerUserId == null) {
            throw ForbiddenException(UserErrorCode.GENERAL_MEMBER_REQUIRED)
        }
    }

    private fun verifyCompany(userId: Long) {
        val account = userReader.read(userId)
        account.status.requireActive()
        if (account.role != UserRole.COMPANY) {
            throw ForbiddenException(UserErrorCode.COMPANY_ROLE_REQUIRED)
        }
    }
}
