package com.ogonggo.adminapi.servicefeedback.business

import com.ogonggo.core.servicefeedback.implement.ServiceFeedbackReader
import org.springframework.stereotype.Service

@Service
class AdminServiceFeedbackService(
    private val serviceFeedbackReader: ServiceFeedbackReader,
) {

    fun getServiceFeedbacks(page: Int, size: Int): AdminServiceFeedbackPageResult =
        AdminServiceFeedbackPageResult.from(serviceFeedbackReader.readPage(page, size))
}
