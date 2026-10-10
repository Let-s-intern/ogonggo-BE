package com.ogonggo.userapi.recruitmentpost.presentation.response

import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostContactMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostProgressMethod
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostApplicationProgressStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostRecruitmentStatus
import com.ogonggo.core.recruitmentpost.domain.RecruitmentPostType
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostApplicationCreateResult
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostApplicationItemResult
import com.ogonggo.userapi.response.PageInfo
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateRecruitmentPostApplicationResponse(
    val postId: Long,
    val contactMethod: RecruitmentPostContactMethod,
    val contactValue: String,
    val clickedAt: LocalDateTime,
) {
    companion object {
        fun from(result: RecruitmentPostApplicationCreateResult): CreateRecruitmentPostApplicationResponse =
            CreateRecruitmentPostApplicationResponse(
                postId = result.postId,
                contactMethod = result.contactMethod,
                contactValue = result.contactValue,
                clickedAt = result.clickedAt,
            )
    }
}

data class RecruitmentPostApplicationItemResponse(
    val postId: Long,
    val title: String,
    val recruitmentType: RecruitmentPostType,
    val recruitmentStatus: RecruitmentPostRecruitmentStatus,
    val recruitmentEndDate: LocalDate,
    val progressMethod: RecruitmentPostProgressMethod,
    val activityDurationMonths: Int,
    val applicationStatus: RecruitmentPostApplicationProgressStatus,
    val lastClickedAt: LocalDateTime,
    val author: RecruitmentPostAuthorResponse,
) {
    companion object {
        fun from(result: RecruitmentPostApplicationItemResult): RecruitmentPostApplicationItemResponse =
            RecruitmentPostApplicationItemResponse(
                postId = result.postId,
                title = result.title,
                recruitmentType = result.recruitmentType,
                recruitmentStatus = result.recruitmentStatus,
                recruitmentEndDate = result.recruitmentEndDate,
                progressMethod = result.progressMethod,
                activityDurationMonths = result.activityDurationMonths,
                applicationStatus = result.applicationStatus,
                lastClickedAt = result.lastClickedAt,
                author = RecruitmentPostAuthorResponse.from(result.author),
            )
    }
}

data class RecruitmentPostApplicationPageResponse(
    val items: List<RecruitmentPostApplicationItemResponse>,
    val pageInfo: PageInfo,
    val countsByRecruitmentType: Map<RecruitmentPostType, Long>,
)
