package com.ogonggo.userapi.community.presentation.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.RecruitmentStatus
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.userapi.community.business.RecruitmentPostApplicationCreateResult
import com.ogonggo.userapi.community.business.RecruitmentPostApplicationItemResult
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateRecruitmentPostApplicationResponse(
    val postId: Long,
    val contactMethod: ContactMethod,
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

data class RecruitmentApplicationItemResponse(
    val postId: Long,
    val title: String,
    val recruitmentType: RecruitmentType,
    val recruitmentStatus: RecruitmentStatus,
    val recruitmentEndDate: LocalDate,
    val lastClickedAt: LocalDateTime,
    val author: RecruitmentPostAuthorResponse,
) {
    companion object {
        fun from(result: RecruitmentPostApplicationItemResult): RecruitmentApplicationItemResponse =
            RecruitmentApplicationItemResponse(
                postId = result.postId,
                title = result.title,
                recruitmentType = result.recruitmentType,
                recruitmentStatus = result.recruitmentStatus,
                recruitmentEndDate = result.recruitmentEndDate,
                lastClickedAt = result.lastClickedAt,
                author = RecruitmentPostAuthorResponse.from(result.author),
            )
    }
}
