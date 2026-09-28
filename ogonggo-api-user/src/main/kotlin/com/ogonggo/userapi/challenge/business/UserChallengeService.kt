package com.ogonggo.userapi.challenge.business

import com.ogonggo.core.user.implement.UserReader
import com.ogonggo.userapi.challenge.implement.LetsCareerChallengeClient
import com.ogonggo.userapi.challenge.implement.dto.RecommendedChallengeDto
import org.springframework.stereotype.Service

@Service
class UserChallengeService(
    private val userReader: UserReader,
    private val letsCareerChallengeClient: LetsCareerChallengeClient,
) {

    /**
     * 렛츠커리어 챌린지 중 이 사용자에게 보여줄 것을 가져온다.
     * 렛츠커리어 호출을 기다리는 동안 DB 커넥션을 잡지 않도록 트랜잭션을 열지 않는다.
     */
    fun getRecommendedChallenges(userId: Long?): List<RecommendedChallengeDto> {
        // 기업 회원은 렛츠커리어 계정이 없으므로 비로그인과 같이 보낸다.
        val letsCareerUserId = userId?.let { userReader.read(it).letsCareerUserId }
        return letsCareerChallengeClient.readRecommended(letsCareerUserId)
    }
}
