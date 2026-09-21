package com.ogonggo.userapi.openapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:ogonggo-user-openapi;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true",
        "ogonggo.auth.jwt.secret=b2dvbmdnby1sb2NhbC10ZXN0LXNlY3JldC1rZXktcGxlYXNlLXJlcGxhY2UtaW4tcmVhbC1lbnZzISEwMDAwMDAwMA==",
        "ogonggo.letscareer.base-url=http://localhost:8090",
        "ogonggo.letscareer.internal-api-key=test-internal-api-key",
        // spring.mail.host가 있어야 Spring Boot가 JavaMailSender를 만든다. 실제로 발송하지는 않는다.
        "spring.mail.host=localhost",
        "ogonggo.advertisement.slack.inquiry-url=https://hooks.slack.com/services/T000/B000/test",
    ],
)
@AutoConfigureMockMvc
class UserOpenApiContractTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {

    /**
     * springdoc은 operationId를 메서드 이름에서 만들고, 겹치면 스캔 순서대로 `_1`을 붙인다.
     * 이 저장소는 행위자로 API를 나누어 `UserJobApi.getJobs`와 `CompanyJobApi.getJobs`처럼
     * 메서드 이름이 겹치는 것이 정상이므로 자동 생성에 맡기면 접미사가 붙는다.
     *
     * 접미사는 "두 번째로 스캔된 것"이라는 뜻일 뿐 어떤 경로도 식별하지 않는다.
     * 컨트롤러를 추가하거나 옮기면 순서가 바뀌어 이름과 경로의 짝이 조용히 뒤집히고,
     * 이 명세로 클라이언트 코드를 생성하는 쪽은 다른 API를 호출하게 된다.
     */
    @Test
    fun `모든 operationId는 유일하며 자동 생성 접미사가 붙지 않는다`() {
        val document = openApiDocument()

        val operationIds = document.at("/paths").fields().asSequence()
            .flatMap { (path, operations) ->
                operations.fields().asSequence().map { (method, operation) ->
                    "$method $path" to operation.at("/operationId").asText()
                }
            }
            .toList()

        val missing = operationIds.filter { (_, id) -> id.isEmpty() }
        assertTrue(missing.isEmpty(), "operationId가 없는 작업: ${missing.map { it.first }}")

        val generated = operationIds.filter { (_, id) -> id.matches(GENERATED_SUFFIX) }
        assertTrue(
            generated.isEmpty(),
            "이름이 겹쳐 접미사가 붙었습니다. @Operation(operationId = ...)를 지정하세요: $generated",
        )

        val duplicated = operationIds.groupBy { it.second }.filterValues { it.size > 1 }
        assertTrue(duplicated.isEmpty(), "operationId가 겹칩니다: ${duplicated.keys}")
    }

    @Test
    fun `사용자 OpenAPI는 인터페이스의 경로와 인증과 오류 명세를 노출한다`() {
        val document = openApiDocument()

        assertEquals("Ogonggo User API", document.at("/info/title").asText())
        // 절대 URL이면 HTTP ALB 주소가 새어 나가 HTTPS Swagger UI에서 Try it out이 막힌다.
        assertEquals("/", document.at("/servers/0/url").asText())
        assertTrue(document.at("/components/securitySchemes/BearerAuth").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1jobs/get").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1jobs~1calendar/get").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1job-bookmarks/get").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1job-bookmarks~1{jobId}/post").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1job-bookmarks~1{jobId}/delete").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1bootcamps/get").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1bootcamp-bookmarks/get").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1bootcamp-bookmarks~1{bootcampId}/post").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1bootcamp-bookmarks~1{bootcampId}/delete").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1users~1me/get").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1users~1me~1profile/put").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1recruitment-posts/post").isObject)
        val saveRecruitmentPostProperties = document.at(
            "/components/schemas/CreateRecruitmentPostRequest/properties",
        )
        assertTrue(saveRecruitmentPostProperties.has("saveMode"))
        assertTrue(saveRecruitmentPostProperties.has("agreedToPolicy"))
        val recruitmentPostList = document.at("/paths/~1api~1v1~1recruitment-posts/get")
        assertPageParameter(recruitmentPostList, "page", defaultValue = "1", minimum = 1, maximum = null)
        assertPageParameter(recruitmentPostList, "size", defaultValue = "10", minimum = 1, maximum = 100)
        val createRecruitmentPostDraft =
            document.at("/paths/~1api~1v1~1me~1recruitment-posts~1drafts/post")
        assertTrue(createRecruitmentPostDraft.at("/security/0/BearerAuth").isArray)
        assertTrue(createRecruitmentPostDraft.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(
            createRecruitmentPostDraft.at("/responses/400/description").asText()
                .startsWith("제목 또는 입력된 필드 검증 실패"),
        )
        val draftRequestProperties = document.at(
            "/components/schemas/CreateRecruitmentPostDraftRequest/properties",
        )
        assertTrue(draftRequestProperties.has("title"))
        listOf(
            "recruitmentType", "capacity", "progressMethod", "activityDurationMonths",
            "technologyStacks", "summary", "content", "eligibilityAndSelectionProcess",
            "recruitmentStartDate", "recruitmentEndDate", "positions", "contactMethod", "contactValue",
        ).forEach { field -> assertTrue(draftRequestProperties.has(field)) }
        val publishRecruitmentPost =
            document.at("/paths/~1api~1v1~1me~1recruitment-posts~1{postId}~1publish/post")
        assertTrue(publishRecruitmentPost.at("/security/0/BearerAuth").isArray)
        assertTrue(publishRecruitmentPost.at("/responses/200/content/application~1json/schema").isObject)
        assertTrue(
            publishRecruitmentPost.at("/responses/404/description").asText()
                .startsWith("RECRUITMENT_POST_NOT_FOUND"),
        )
        val publishRequestProperties = document.at(
            "/components/schemas/PublishRecruitmentPostRequest/properties",
        )
        assertTrue(publishRequestProperties.has("agreedToPolicy"))
        val recruitmentPostAuthorProperties = document.at(
            "/components/schemas/RecruitmentPostAuthorResponse/properties",
        )
        listOf("userId", "nickname", "profileImageUrl").forEach { field ->
            assertTrue(recruitmentPostAuthorProperties.has(field))
        }
        assertTrue(document.at("/components/schemas/RecruitmentPostSummaryResponse/properties/author").isObject)
        val recruitmentPostDelete = document.at("/paths/~1api~1v1~1recruitment-posts~1{postId}/delete")
        assertTrue(recruitmentPostDelete.isObject)
        assertTrue(recruitmentPostDelete.at("/security/0/BearerAuth").isArray)
        assertTrue(
            recruitmentPostDelete.at("/responses/404/description").asText()
                .startsWith("RECRUITMENT_POST_NOT_FOUND"),
        )
        listOf("close", "reopen").forEach { action ->
            val operation = document.at("/paths/~1api~1v1~1recruitment-posts~1{postId}~1$action/patch")
            assertTrue(operation.isObject)
            assertTrue(operation.at("/security/0/BearerAuth").isArray)
            assertTrue(operation.at("/responses/200/content/application~1json/schema").isObject)
        }
        val recruitmentPostBookmarks = document.at("/paths/~1api~1v1~1recruitment-post-bookmarks/get")
        assertTrue(recruitmentPostBookmarks.isObject)
        assertTrue(recruitmentPostBookmarks.at("/security/0/BearerAuth").isArray)
        assertPageParameter(recruitmentPostBookmarks, "page", defaultValue = "1", minimum = 1, maximum = null)
        assertPageParameter(recruitmentPostBookmarks, "size", defaultValue = "10", minimum = 1, maximum = 100)
        val addRecruitmentPostBookmark =
            document.at("/paths/~1api~1v1~1recruitment-posts~1{postId}~1bookmarks~1me/put")
        assertTrue(addRecruitmentPostBookmark.isObject)
        assertTrue(addRecruitmentPostBookmark.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(
            addRecruitmentPostBookmark.at("/responses/409/description").asText()
                .startsWith("RECRUITMENT_POST_BOOKMARK_ALREADY_EXISTS"),
        )
        val deleteRecruitmentPostBookmark =
            document.at("/paths/~1api~1v1~1recruitment-posts~1{postId}~1bookmarks~1me/delete")
        assertTrue(deleteRecruitmentPostBookmark.isObject)
        assertTrue(deleteRecruitmentPostBookmark.at("/responses/200/content/application~1json/schema").isObject)
        val recruitmentApplication =
            document.at("/paths/~1api~1v1~1recruitment-posts~1{postId}~1applications/post")
        assertTrue(recruitmentApplication.at("/security/0/BearerAuth").isArray)
        assertTrue(recruitmentApplication.at("/responses/200/content/application~1json/schema").isObject)
        assertTrue(
            recruitmentApplication.at("/responses/409/description").asText()
                .startsWith("RECRUITMENT_POST_CLOSED"),
        )
        val myRecruitmentApplications =
            document.at("/paths/~1api~1v1~1me~1recruitment-applications/get")
        assertTrue(myRecruitmentApplications.at("/security/0/BearerAuth").isArray)
        assertPageParameter(myRecruitmentApplications, "page", defaultValue = "1", minimum = 1, maximum = null)
        assertPageParameter(myRecruitmentApplications, "size", defaultValue = "10", minimum = 1, maximum = 100)
        listOf("recruitmentStatus", "recruitmentType", "applicationStatus", "keyword", "sort")
            .forEach { name -> assertTrue(myRecruitmentApplications.parameter(name).isObject) }
        val recruitmentApplicationItemProperties = document.at(
            "/components/schemas/RecruitmentApplicationItemResponse/properties",
        )
        listOf("progressMethod", "activityDurationMonths", "applicationStatus")
            .forEach { field -> assertTrue(recruitmentApplicationItemProperties.has(field)) }
        assertTrue(
            document.at("/components/schemas/RecruitmentApplicationPageResponse/properties/countsByRecruitmentType")
                .isObject,
        )
        val updateRecruitmentApplication = document.at(
            "/paths/~1api~1v1~1me~1recruitment-applications~1{postId}/patch",
        )
        assertTrue(updateRecruitmentApplication.at("/security/0/BearerAuth").isArray)
        assertTrue(updateRecruitmentApplication.at("/requestBody/content/application~1json/schema").isObject)
        val deleteRecruitmentApplication = document.at(
            "/paths/~1api~1v1~1me~1recruitment-applications~1{postId}/delete",
        )
        assertTrue(deleteRecruitmentApplication.at("/security/0/BearerAuth").isArray)
        assertTrue(deleteRecruitmentApplication.at("/responses/404/description").asText().startsWith("RECRUITMENT_POST_APPLICATION_NOT_FOUND"))
        val reportRecruitmentPostComment = document.at(
            "/paths/~1api~1v1~1recruitment-posts~1{postId}~1comments~1{commentId}~1reports/post",
        )
        assertTrue(reportRecruitmentPostComment.at("/security/0/BearerAuth").isArray)
        assertTrue(reportRecruitmentPostComment.at("/requestBody/content/application~1json/schema").isObject)
        assertTrue(
            document.at("/components/schemas/CreateRecruitmentPostCommentReportRequest/properties/reason")
                .isObject,
        )
        listOf("applicationCount", "bookmarkCount").forEach { field ->
            assertTrue(document.at("/components/schemas/RecruitmentPostSummaryResponse/properties/$field").isObject)
        }
        assertTrue(document.at("/components/schemas/RecruitmentPostDetailResponse/properties/bookmarkCount").isObject)
        val myRecruitmentPosts = document.at("/paths/~1api~1v1~1me~1recruitment-posts/get")
        assertTrue(myRecruitmentPosts.at("/security/0/BearerAuth").isArray)
        assertPageParameter(myRecruitmentPosts, "page", defaultValue = "1", minimum = 1, maximum = null)
        assertPageParameter(myRecruitmentPosts, "size", defaultValue = "10", minimum = 1, maximum = 100)
        listOf("status", "recruitmentStatus", "applicationStatus", "recruitmentType", "keyword", "sort")
            .forEach { name -> assertTrue(myRecruitmentPosts.parameter(name).isObject) }
        assertTrue(document.at("/components/schemas/RecruitmentPostManagementItemResponse/properties/applicationCount").isObject)
        val copyMyRecruitmentPost =
            document.at("/paths/~1api~1v1~1me~1recruitment-posts~1{postId}~1copies/post")
        assertTrue(copyMyRecruitmentPost.at("/security/0/BearerAuth").isArray)
        assertTrue(copyMyRecruitmentPost.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(
            copyMyRecruitmentPost.at("/responses/404/description").asText()
                .startsWith("RECRUITMENT_POST_NOT_FOUND"),
        )
        val myRecruitmentPostForm =
            document.at("/paths/~1api~1v1~1me~1recruitment-posts~1{postId}/get")
        assertTrue(myRecruitmentPostForm.at("/security/0/BearerAuth").isArray)
        assertTrue(myRecruitmentPostForm.at("/responses/200/content/application~1json/schema").isObject)
        assertTrue(
            myRecruitmentPostForm.at("/responses/404/description").asText()
                .startsWith("RECRUITMENT_POST_NOT_FOUND"),
        )
        assertEquals(
            copyMyRecruitmentPost.at("/responses/201/content/application~1json/schema"),
            myRecruitmentPostForm.at("/responses/200/content/application~1json/schema"),
        )
        val formResponseProperties = document.at("/components/schemas/RecruitmentPostFormResponse/properties")
        listOf("postId", "status", "recruitmentStatus", "title", "technologyStacks", "positions", "agreedToPolicy")
            .forEach { field -> assertTrue(formResponseProperties.has(field)) }
        assertTrue(document.at("/paths/~1api~1v1~1auth~1letscareer/post").isObject)
        assertFalse(document.at("/paths/~1health").isObject)

        // 목록·상세는 로그인 없이 열려 있지만, 토큰을 보내면 북마크 여부가 채워지므로 스키마는 그대로 노출한다.
        val jobList = document.at("/paths/~1api~1v1~1jobs/get")
        assertTrue(jobList.at("/security/0/BearerAuth").isArray)
        assertPageParameter(jobList, "page", defaultValue = "1", minimum = 1, maximum = null)
        assertPageParameter(jobList, "size", defaultValue = "10", minimum = 1, maximum = 100)

        // 인기 공고도 토큰을 보내면 북마크 여부가 채워지므로 목록과 같은 선택적 인증을 노출한다.
        val popularJobs = document.at("/paths/~1api~1v1~1jobs~1popular/get")
        assertTrue(popularJobs.isObject)
        assertTrue(popularJobs.at("/security/0/BearerAuth").isArray)
        assertEquals(listOf("employmentType"), popularJobs.at("/parameters").map { it.at("/name").asText() })

        // 비슷한 공고는 내 희망 직무·산업으로 고르는 사용자별 결과라 인증이 필수다.
        val similarJobs = document.at("/paths/~1api~1v1~1jobs~1similar/get")
        assertTrue(similarJobs.at("/security/0/BearerAuth").isArray)
        assertTrue(similarJobs.at("/responses/401/description").asText().startsWith("UNAUTHORIZED"))
        assertTrue(similarJobs.at("/responses/200/content/application~1json/schema").isObject)

        // 부트캠프 목록·상세도 토큰을 보내면 북마크 여부가 채워지므로 공고와 같은 선택적 인증을 노출한다.
        val bootcampList = document.at("/paths/~1api~1v1~1bootcamps/get")
        assertTrue(bootcampList.at("/security/0/BearerAuth").isArray)
        assertTrue(document.at("/paths/~1api~1v1~1bootcamps~1{bootcampId}/get/security/0/BearerAuth").isArray)
        assertPageParameter(bootcampList, "page", defaultValue = "1", minimum = 1, maximum = null)
        assertPageParameter(bootcampList, "size", defaultValue = "10", minimum = 1, maximum = 100)
        listOf("sort", "tuitionType", "status", "keyword")
            .forEach { name -> assertTrue(bootcampList.parameter(name).isObject) }
        assertTrue(
            bootcampList.at("/responses/400/description").asText().startsWith("BAD_REQUEST"),
        )

        val sourceUrlClick = document.at("/paths/~1api~1v1~1jobs~1{jobId}~1source-url-clicks/post")
        assertTrue(sourceUrlClick.at("/security/0/BearerAuth").isArray)

        val applicationUrlClick =
            document.at("/paths/~1api~1v1~1bootcamps~1{bootcampId}~1application-url-clicks/post")
        assertTrue(applicationUrlClick.at("/security/0/BearerAuth").isArray)
        assertTrue(applicationUrlClick.at("/responses/200/content/application~1json/schema").isObject)
        assertTrue(
            applicationUrlClick.at("/responses/404/description").asText().startsWith("BOOTCAMP_NOT_FOUND"),
        )

        val jobBookmarks = document.at("/paths/~1api~1v1~1job-bookmarks/get")
        assertTrue(jobBookmarks.at("/security/0/BearerAuth").isArray)
        assertPageParameter(jobBookmarks, "page", defaultValue = "1", minimum = 1, maximum = null)
        listOf("employmentType", "experienceType", "jobField", "jobRole", "keyword", "applicationStatus", "recruitmentStatus", "sort")
            .forEach { name -> assertTrue(jobBookmarks.parameter(name).isObject) }
        val addBookmark = document.at("/paths/~1api~1v1~1job-bookmarks~1{jobId}/post")
        assertTrue(addBookmark.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(addBookmark.at("/responses/409/description").asText().startsWith("JOB_BOOKMARK_ALREADY_EXISTS"))
        val deleteBookmark = document.at("/paths/~1api~1v1~1job-bookmarks~1{jobId}/delete")
        assertTrue(deleteBookmark.at("/responses/200/content/application~1json/schema").isObject)
        listOf("prepare", "cancel-preparation").forEach { command ->
            val move = document.at("/paths/~1api~1v1~1job-bookmarks~1{jobId}~1$command/post")
            assertTrue(move.at("/responses/200/content/application~1json/schema").isObject)
            assertTrue(move.at("/responses/404/description").asText().startsWith("JOB_BOOKMARK_NOT_FOUND"))
        }

        val bootcampBookmarks = document.at("/paths/~1api~1v1~1bootcamp-bookmarks/get")
        assertTrue(bootcampBookmarks.at("/security/0/BearerAuth").isArray)
        listOf("status", "keyword", "applicationStatus", "sort")
            .forEach { name -> assertTrue(bootcampBookmarks.parameter(name).isObject) }
        assertPageParameter(bootcampBookmarks, "page", defaultValue = "1", minimum = 1, maximum = null)
        listOf("tuitionType", "status", "keyword")
            .forEach { name -> assertTrue(bootcampBookmarks.parameter(name).isObject) }
        assertTrue(bootcampBookmarks.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))
        val addBootcampBookmark = document.at("/paths/~1api~1v1~1bootcamp-bookmarks~1{bootcampId}/post")
        assertTrue(addBootcampBookmark.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(
            addBootcampBookmark.at("/responses/409/description").asText()
                .startsWith("BOOTCAMP_BOOKMARK_ALREADY_EXISTS"),
        )
        val deleteBootcampBookmark = document.at("/paths/~1api~1v1~1bootcamp-bookmarks~1{bootcampId}/delete")
        assertTrue(deleteBootcampBookmark.at("/responses/200/content/application~1json/schema").isObject)
        listOf("prepare", "cancel-preparation").forEach { command ->
            val move = document.at("/paths/~1api~1v1~1bootcamp-bookmarks~1{bootcampId}~1$command/post")
            assertTrue(move.at("/responses/200/content/application~1json/schema").isObject)
            assertTrue(move.at("/responses/404/description").asText().startsWith("BOOTCAMP_BOOKMARK_NOT_FOUND"))
        }

        // 역할은 토큰에 없으므로 내 정보 조회는 인증이 필수다.
        val myAccount = document.at("/paths/~1api~1v1~1users~1me/get")
        assertTrue(myAccount.at("/security/0/BearerAuth").isArray)
        assertTrue(myAccount.at("/responses/401/description").asText().startsWith("UNAUTHORIZED"))
        assertTrue(myAccount.at("/responses/404/description").asText().startsWith("USER_NOT_FOUND"))
        val myAccountProperties = document.at("/components/schemas/MyAccountResponse/properties")
        listOf("userId", "role", "status", "email", "joinedAt", "profile", "companyProfile")
            .forEach { field -> assertTrue(myAccountProperties.has(field)) }

        // 학력과 희망 조건은 오공고가 소유하므로 조회는 내 정보에 함께 담고 수정만 따로 연다.
        val replaceProfile = document.at("/paths/~1api~1v1~1users~1me~1profile/put")
        assertTrue(replaceProfile.at("/security/0/BearerAuth").isArray)
        assertTrue(
            replaceProfile.at("/responses/409/description").asText().startsWith("USER_PROFILE_CONFLICT"),
        )
        val profileProperties = document.at("/components/schemas/MyProfileResponse/properties")
        listOf(
            "name", "nickname", "profileImageUrl",
            "university", "major", "grade",
            "wishField", "wishJob", "wishIndustry", "wishEmploymentType", "wishCompany",
        ).forEach { field -> assertTrue(profileProperties.has(field)) }

        val jobCalendar = document.at("/paths/~1api~1v1~1jobs~1calendar/get")
        assertFalse(jobCalendar.has("security"))
        assertEquals("date", jobCalendar.parameter("from").at("/schema/format").asText())
        assertEquals("date", jobCalendar.parameter("to").at("/schema/format").asText())

        val calendarBadRequest = document.at("/paths/~1api~1v1~1jobs~1calendar/get/responses/400")
        assertTrue(calendarBadRequest["description"].asText().startsWith("BAD_REQUEST"))
        assertTrue(calendarBadRequest.at("/content/application~1json/schema").isObject)

        val jobNotFound = document.at("/paths/~1api~1v1~1jobs~1{jobId}/get/responses/404")
        assertTrue(jobNotFound["description"].asText().startsWith("JOB_NOT_FOUND"))
        assertTrue(jobNotFound.at("/content/application~1json/schema").isObject)

        val jobDetailProperties = document.at("/components/schemas/UserJobDetailResponse/properties")
        listOf(
            "companyAndTeamIntroduction",
            "responsibilities",
            "qualifications",
            "preferredQualifications",
            "compensation",
            "benefits",
            "hiringProcess",
        ).forEach { field -> assertTrue(jobDetailProperties.has(field)) }
        assertFalse(jobDetailProperties.has("content"))
        assertTrue(jobDetailProperties.has("bookmarked"))
        assertTrue(document.at("/components/schemas/UserJobSummaryResponse/properties/bookmarked").isObject)
        assertTrue(document.at("/components/schemas/UserBootcampSummaryResponse/properties/bookmarked").isObject)
        assertTrue(document.at("/components/schemas/UserBootcampDetailResponse/properties/bookmarked").isObject)

        val companySignUp = document.at("/paths/~1api~1v1~1auth~1company~1signup/post")
        assertFalse(companySignUp.has("security"))
        assertTrue(companySignUp.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(companySignUp.at("/responses/409/description").asText().startsWith("EMAIL_ALREADY_EXISTS"))

        val companySignIn = document.at("/paths/~1api~1v1~1auth~1company~1signin/post")
        assertFalse(companySignIn.has("security"))
        assertTrue(
            companySignIn.at("/responses/401/description").asText().startsWith("INVALID_COMPANY_CREDENTIALS"),
        )

        // 광고 문의는 오공고 계정이 없는 기업 담당자가 호출하므로 Security Requirement를 붙이지 않는다.
        val advertisementInquiry = document.at("/paths/~1api~1v1~1advertisement-inquiries/post")
        assertTrue(advertisementInquiry.isObject)
        assertFalse(advertisementInquiry.has("security"))
        assertTrue(advertisementInquiry.at("/responses/200/content/application~1json/schema").isObject)
        assertTrue(
            advertisementInquiry.at("/responses/503/description").asText()
                .startsWith("ADVERTISEMENT_INQUIRY_NOTIFICATION_FAILED"),
        )

        val publicSignIn = document.at("/paths/~1api~1v1~1auth~1letscareer/post")
        assertFalse(publicSignIn.has("security"))
        val signOut = document.at("/paths/~1api~1v1~1auth~1signout/post")
        assertTrue(signOut.at("/security/0/BearerAuth").isArray)
    }

    @Test
    fun `EnumField enum은 값과 code와 설명을 문서에 노출한다`() {
        val document = openApiDocument()

        // 요청 본문 안의 enum
        val bodyEnum = document
            .at("/components/schemas/CreateAdvertisementInquiryRequest/properties/promotionChannel/description")
            .asText()
        assertTrue(bodyEnum.contains("| `OPEN_CHAT_MARKETING` | 2 | 오픈채팅방 · 마케팅 |"))

        // 쿼리 파라미터로 쓰는 enum
        val parameterEnum = document.at("/paths/~1api~1v1~1jobs/get/parameters")
            .first { it.at("/name").asText() == "employmentType" }
            .at("/schema/description").asText()
        assertTrue(parameterEnum.contains("| `FULL_TIME` | 1 | 정규직 |"))
    }

    private fun openApiDocument(): JsonNode {
        val response = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andReturn()
            .response
        // MockMvc 는 인코딩이 없으면 ISO-8859-1 로 읽어 한글 설명이 깨진다.
        return objectMapper.readTree(response.getContentAsString(Charsets.UTF_8))
    }

    private fun assertPageParameter(
        operation: JsonNode,
        name: String,
        defaultValue: String,
        minimum: Int,
        maximum: Int?,
    ) {
        val parameter = operation["parameters"].first { it["name"].asText() == name }
        val schema = parameter["schema"]
        assertEquals(defaultValue, schema["default"].asText())
        assertEquals(minimum, schema["minimum"].asInt())
        if (maximum != null) {
            assertEquals(maximum, schema["maximum"].asInt())
        }
    }

    private fun JsonNode.parameter(name: String): JsonNode =
        this["parameters"].first { it["name"].asText() == name }

    companion object {
        private val GENERATED_SUFFIX = Regex(""".*_\d+$""")
    }
}
