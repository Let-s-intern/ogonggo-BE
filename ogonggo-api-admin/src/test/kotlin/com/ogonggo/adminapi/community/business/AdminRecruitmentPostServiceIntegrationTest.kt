package com.ogonggo.adminapi.community.business

import com.ogonggo.core.community.domain.ContactMethod
import com.ogonggo.core.community.domain.ProgressMethod
import com.ogonggo.core.community.domain.RecruitmentPosition
import com.ogonggo.core.community.domain.RecruitmentPostConsoleSearchCondition
import com.ogonggo.core.community.domain.RecruitmentPostSortType
import com.ogonggo.core.community.domain.RecruitmentType
import com.ogonggo.core.community.implement.RecruitmentPostAppender
import com.ogonggo.core.community.implement.dto.RecruitmentPostAppendDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

/** 관리자 API는 OSIV를 끄므로 지연 로딩 컬렉션을 결과로 옮기는 동안 영속성 컨텍스트가 열려 있어야 한다. */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:admin-recruitment-post;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
    ],
)
class AdminRecruitmentPostServiceIntegrationTest @Autowired constructor(
    private val service: AdminRecruitmentPostService,
    private val postAppender: RecruitmentPostAppender,
) {

    @Test
    fun `목록은 모집 포지션과 기술 스택을 실어 반환한다`() {
        // given
        postAppender.append(
            RecruitmentPostAppendDto(
                authorUserId = 1L,
                title = "사이드 프로젝트 팀원 모집",
                recruitmentType = RecruitmentType.SIDE_PROJECT,
                capacity = 4,
                progressMethod = ProgressMethod.ONLINE,
                activityDurationMonths = 3,
                technologyStacks = listOf("Kotlin", "Spring"),
                summary = "함께 서비스를 만들어 볼 팀원을 모집합니다.",
                content = "{\"root\":{\"children\":[]}}",
                eligibilityAndSelectionProcess = null,
                recruitmentStartDate = LocalDate.of(2026, 9, 1),
                recruitmentEndDate = LocalDate.of(2026, 9, 30),
                positions = listOf(RecruitmentPosition.BACKEND, RecruitmentPosition.DESIGN),
                contactMethod = ContactMethod.EMAIL,
                contactValue = "team@example.com",
            ),
        )

        // when
        val result = service.getRecruitmentPosts(
            RecruitmentPostConsoleSearchCondition(),
            RecruitmentPostSortType.LATEST,
            page = 0,
            size = 20,
        )

        // then
        val item = result.items.single()
        assertEquals(listOf(RecruitmentPosition.BACKEND, RecruitmentPosition.DESIGN), item.positions)
        assertEquals(listOf("Kotlin", "Spring"), item.technologyStacks)
    }
}
