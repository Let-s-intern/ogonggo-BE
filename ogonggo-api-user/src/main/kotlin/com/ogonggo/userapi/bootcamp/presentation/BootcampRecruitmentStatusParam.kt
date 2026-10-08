package com.ogonggo.userapi.bootcamp.presentation

import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.userapi.error.InvalidRequestParameterException

/** 임시저장 부트캠프는 사용자에게 보이지 않아 골라도 늘 0건이므로, 잘못 보낸 값으로 보고 거절한다. */
internal fun requirePublicRecruitmentStatus(recruitmentStatus: BootcampRecruitmentStatus?): BootcampRecruitmentStatus? {
    if (recruitmentStatus == BootcampRecruitmentStatus.DRAFT) {
        throw InvalidRequestParameterException("recruitmentStatus", "RECRUITING 또는 CLOSED만 고를 수 있습니다.")
    }
    return recruitmentStatus
}
