package com.ogonggo.userapi.job.business

/** 사용자가 채용공고 상세를 조회했다는 사실만 알린다. 이 사실로 무엇을 할지는 수신자가 정한다. */
data class JobViewedEvent(val jobId: Long)
