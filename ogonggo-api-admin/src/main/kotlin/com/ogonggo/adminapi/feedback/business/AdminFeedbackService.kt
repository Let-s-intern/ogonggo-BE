package com.ogonggo.adminapi.feedback.business

import com.ogonggo.core.feedback.implement.FeedbackReader
import org.springframework.stereotype.Service

@Service
class AdminFeedbackService(
    private val feedbackReader: FeedbackReader,
) {

    fun getFeedbacks(page: Int, size: Int): AdminFeedbackPageResult =
        AdminFeedbackPageResult.from(feedbackReader.readPage(page, size))
}
