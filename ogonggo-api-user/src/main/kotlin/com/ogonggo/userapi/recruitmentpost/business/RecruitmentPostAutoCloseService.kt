package com.ogonggo.userapi.recruitmentpost.business

import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/** 모집 종료일이 지난 모집글을 마감하고 마감한 건수를 돌려준다. 스케줄러가 주기적으로 부른다. */
@Service
class RecruitmentPostAutoCloseService(
    private val recruitmentPostManager: RecruitmentPostManager,
    private val clock: Clock,
) {

    @Transactional
    fun closeExpired(): Int {
        val now = LocalDateTime.now(clock)
        return recruitmentPostManager.closeExpired(today = now.toLocalDate(), closedAt = now)
    }
}
