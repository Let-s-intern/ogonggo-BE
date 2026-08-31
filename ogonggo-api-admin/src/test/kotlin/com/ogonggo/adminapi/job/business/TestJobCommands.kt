package com.ogonggo.adminapi.job.business

import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.ExperienceType
import com.ogonggo.core.job.domain.JobRecruitmentType
import com.ogonggo.core.job.implement.JobAppendCommand
import com.ogonggo.core.job.implement.JobUpdateCommand

internal object TestJobCommands {
    fun append(): JobAppendCommand = JobAppendCommand(
        companyName = "오공고",
        title = "백엔드 개발자",
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.EXPERIENCED,
        region = "서울",
        recruitmentType = JobRecruitmentType.PERIOD,
        responsibilities = "주요 업무",
    )

    fun update(): JobUpdateCommand = JobUpdateCommand(
        companyName = "변경된 오공고",
        title = "변경된 백엔드 개발자",
        employmentType = EmploymentType.FULL_TIME,
        experienceType = ExperienceType.EXPERIENCED,
        region = "서울",
        recruitmentType = JobRecruitmentType.PERIOD,
        responsibilities = "변경된 주요 업무",
    )
}
