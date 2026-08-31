package com.ogonggo.core.domain

import com.ogonggo.core.bootcamp.domain.BootcampBookmark
import com.ogonggo.core.bootcamp.domain.BootcampPartner
import com.ogonggo.core.job.domain.JobBookmark
import com.ogonggo.core.job.domain.JobTag
import com.ogonggo.core.job.domain.Tag
import com.ogonggo.core.user.domain.CompanyProfile
import com.ogonggo.core.user.domain.UserProfile
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ConnectionEntityValidationTest {

    @Test
    fun `연결 엔티티의 대상 식별자는 양수여야 한다`() {
        assertThrows(IllegalArgumentException::class.java) { JobBookmark(jobId = 0L, userId = 1L) }
        assertThrows(IllegalArgumentException::class.java) { JobBookmark(jobId = 1L, userId = 0L) }
        assertThrows(IllegalArgumentException::class.java) { BootcampBookmark(bootcampId = 0L, userId = 1L) }
        assertThrows(IllegalArgumentException::class.java) { BootcampBookmark(bootcampId = 1L, userId = 0L) }
        assertThrows(IllegalArgumentException::class.java) { JobTag(jobId = 0L, tagId = 1L) }
        assertThrows(IllegalArgumentException::class.java) { JobTag(jobId = 1L, tagId = 0L) }
        assertThrows(IllegalArgumentException::class.java) {
            UserProfile(userId = 0L, lastSyncedAt = LocalDateTime.now())
        }
        assertThrows(IllegalArgumentException::class.java) {
            CompanyProfile(userId = 0L, organizationName = "렛츠커리어", managerName = "김담당")
        }
    }

    @Test
    fun `기업 프로필의 기관명과 담당자 이름은 비어 있을 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            CompanyProfile(userId = 1L, organizationName = " ", managerName = "김담당")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CompanyProfile(userId = 1L, organizationName = "렛츠커리어", managerName = " ")
        }
    }

    @Test
    fun `태그명과 파트너사 정보는 유효해야 한다`() {
        assertThrows(IllegalArgumentException::class.java) { Tag(name = " ") }
        assertThrows(IllegalArgumentException::class.java) {
            BootcampPartner(bootcampId = 0L, partnerName = "파트너", displayOrder = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BootcampPartner(bootcampId = 1L, partnerName = " ", displayOrder = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BootcampPartner(bootcampId = 1L, partnerName = "파트너", displayOrder = -1)
        }
    }
}
