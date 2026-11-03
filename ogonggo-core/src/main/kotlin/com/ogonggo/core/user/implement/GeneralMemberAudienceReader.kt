package com.ogonggo.core.user.implement

import com.ogonggo.core.user.implement.dto.GeneralMemberAudienceDto
import com.ogonggo.core.user.persistence.UserQueryRepository
import org.springframework.stereotype.Component

/**
 * 관리자 알림 발송의 대상을 고를 때 쓰는 활성 일반 회원 조회다. 희망 직무와 렛츠커리어 회원 ID만 읽는다.
 * 전체를 한 번에 읽으므로 회원이 수만 명으로 늘면 희망 직무를 따로 저장하는 방식으로 바꾼다.
 */
@Component
class GeneralMemberAudienceReader internal constructor(
    private val userQueryRepository: UserQueryRepository,
) {
    fun readActive(): List<GeneralMemberAudienceDto> =
        userQueryRepository.findActiveGeneralMemberAudience().map { (userId, letsCareerUserId, wishJob) ->
            GeneralMemberAudienceDto(userId, letsCareerUserId, wishJob)
        }
}
