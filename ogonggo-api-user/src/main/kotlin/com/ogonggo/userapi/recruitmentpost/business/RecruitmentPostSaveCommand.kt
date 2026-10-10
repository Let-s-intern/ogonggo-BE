package com.ogonggo.userapi.recruitmentpost.business

import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostAppendDto
import com.ogonggo.core.recruitmentpost.implement.dto.RecruitmentPostDraftAppendDto

enum class RecruitmentPostSaveMode {
    DRAFT,
    PUBLISH,
}

sealed interface RecruitmentPostSaveCommand {
    data class Draft(
        val command: RecruitmentPostDraftAppendDto,
    ) : RecruitmentPostSaveCommand

    data class Published(
        val command: RecruitmentPostAppendDto,
    ) : RecruitmentPostSaveCommand
}
