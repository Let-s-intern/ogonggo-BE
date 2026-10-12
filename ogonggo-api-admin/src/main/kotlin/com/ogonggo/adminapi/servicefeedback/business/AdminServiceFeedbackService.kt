package com.ogonggo.adminapi.servicefeedback.business

import com.ogonggo.core.servicefeedback.implement.ServiceFeedbackReader
import com.ogonggo.core.user.implement.UserProfileReader
import org.springframework.stereotype.Service

@Service
class AdminServiceFeedbackService(
    private val serviceFeedbackReader: ServiceFeedbackReader,
    private val userProfileReader: UserProfileReader,
) {

    fun getServiceFeedbacks(page: Int, size: Int): AdminServiceFeedbackPageResult {
        val result = serviceFeedbackReader.readPage(page, size)
        val profiles = userProfileReader.readAll(result.serviceFeedbacks.mapNotNull { it.userId }.toSet())
        return AdminServiceFeedbackPageResult.from(result, profiles)
    }
}
