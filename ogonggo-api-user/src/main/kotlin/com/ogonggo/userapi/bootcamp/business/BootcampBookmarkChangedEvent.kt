package com.ogonggo.userapi.bootcamp.business

/** 부트캠프의 북마크가 등록되거나 해제됐다는 사실만 알린다. 등록·해제를 구분하지 않고 수신자가 다시 센다. */
data class BootcampBookmarkChangedEvent(val bootcampId: Long)
