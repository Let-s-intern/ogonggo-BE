package com.ogonggo.core.companycontentpriority.implement

import com.ogonggo.core.companycontentpriority.domain.CompanyContentPriority
import com.ogonggo.core.companycontentpriority.persistence.CompanyContentPriorityJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component

@Component
class CompanyContentPriorityReader internal constructor(
    private val priorityRepository: CompanyContentPriorityJpaRepository,
) {

    /** 저장한 값이다. 운영자가 한 번도 바꾸지 않았으면 켜짐이다. 읽지 못하면 예외를 그대로 던진다. */
    fun isEnabled(): Boolean =
        priorityRepository.findById(CompanyContentPriority.SINGLETON_ID).orElse(null)?.enabled
            ?: CompanyContentPriority.DEFAULT_ENABLED

    /**
     * 공개 목록이 쓰는 값이다. 설정을 읽지 못해도(예: 운영 DB에 테이블이 없을 때) 목록이 깨지지 않도록
     * 켜짐으로 보고 오류 로그를 남긴다. 켜짐은 이 설정이 생기기 전의 동작이다.
     */
    fun isEnabledOrDefault(): Boolean =
        try {
            isEnabled()
        } catch (exception: DataAccessException) {
            log.error("기업회원 공고 우선 노출 설정을 읽지 못해 켜짐으로 봅니다.", exception)
            CompanyContentPriority.DEFAULT_ENABLED
        }

    private companion object {
        private val log = LoggerFactory.getLogger(CompanyContentPriorityReader::class.java)
    }
}
