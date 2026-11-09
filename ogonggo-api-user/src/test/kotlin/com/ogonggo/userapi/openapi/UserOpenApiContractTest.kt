package com.ogonggo.userapi.openapi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
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
        "ogonggo.slack.advertisement-inquiry=https://hooks.slack.com/services/T000/B000/test",
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
    @DisplayName("모든 operationId는 유일하며 자동 생성 접미사가 붙지 않는다")
    fun `모든 operationId는 유일하며 자동 생성 접미사가 붙지 않는다`() {
        // given
        val document = openApiDocument()

        // when
        val operationIds = document.at("/paths").fields().asSequence()
            .flatMap { (path, operations) ->
                operations.fields().asSequence().map { (method, operation) ->
                    "$method $path" to operation.at("/operationId").asText()
                }
            }
            .toList()

        // then
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
    @DisplayName("광고 상품 신청 목록은 비회원 공개이며 신청 화면 필드만 반환한다")
    fun `광고 상품 신청 목록은 비회원 공개이며 신청 화면 필드만 반환한다`() {
        // given
        val document = openApiDocument()

        // when
        val operation = document.at("/paths/~1api~1v1~1advertisement-products/get")
        val response = document.at("/components/schemas/AdvertisementProductResponse")

        // then
        assertEquals("listPublicAdvertisementProducts", operation.at("/operationId").asText())
        assertFalse(operation.has("security"))
        assertTrue(operation.at("/responses/200/content/application~1json/schema").isObject)
        listOf("id", "category", "position", "name", "price").forEach { field ->
            assertTrue(response.at("/properties/$field").isObject, field)
        }
        assertTrue(response.at("/properties/position/nullable").asBoolean())
        assertTrue(response.at("/properties/price/nullable").asBoolean())
        assertFalse(response.at("/properties/registeredAt").isObject)
        assertFalse(response.at("/properties/updatedAt").isObject)
    }

    @Test
    @DisplayName("공개 광고 노출 명세는 필수 배너 자리와 최소 응답 및 오류를 명시한다")
    fun `공개 광고 노출 명세는 필수 배너 자리와 최소 응답 및 오류를 명시한다`() {
        // given
        val document = openApiDocument()

        // when
        val operation = document.at("/paths/~1api~1v1~1advertisement-exposures/get")
        val position = operation["parameters"].first { it.at("/name").asText() == "position" }
        val response = document.at("/components/schemas/AdvertisementExposureResponse")

        // then
        assertEquals("listPublicAdvertisementExposures", operation.at("/operationId").asText())
        assertFalse(operation.has("security"))
        assertTrue(position.at("/required").asBoolean())
        assertEquals("string", position.at("/schema/type").asText())
        assertEquals(
            setOf(
                "JOB_HOME_TOP_BANNER",
                "JOB_HOME_BOTTOM_BANNER",
                "JOB_DETAIL_BANNER",
                "BOOTCAMP_LIST_TOP_BANNER",
                "BOOTCAMP_LIST_BOTTOM_BANNER",
                "BOOTCAMP_DETAIL_BANNER",
            ),
            position.at("/schema/enum").map { it.asText() }.toSet(),
        )
        assertTrue(operation.at("/responses/200/content/application~1json/schema").isObject)
        assertTrue(operation.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))
        assertTrue(operation.at("/responses/400/content/application~1json/schema").isObject)
        assertEquals(setOf("id", "name", "imageUrl", "mobileImageUrl", "linkUrl", "adLabel"), response.at("/properties").fieldNames().asSequence().toSet())
        assertEquals(setOf("id", "name", "imageUrl", "linkUrl", "adLabel"), response.at("/required").map { it.asText() }.toSet())
        assertTrue(response.at("/properties/mobileImageUrl/nullable").asBoolean())
    }

    @Test
    @DisplayName("광고 신청은 선택 인증과 폼 검증 및 201 응답을 명시한다")
    fun `광고 신청은 선택 인증과 폼 검증 및 201 응답을 명시한다`() {
        // given
        val document = openApiDocument()

        // when
        val operation = document.at("/paths/~1api~1v1~1advertisement-contracts/post")
        val request = document.at("/components/schemas/CreateAdvertisementContractRequest")
        val response = document.at("/components/schemas/CreateAdvertisementContractResponse")

        // then
        assertEquals("createAdvertisementContract", operation.at("/operationId").asText())
        assertEquals(0, operation.path("parameters").size())
        val securityRequirements = operation.at("/security")
        assertEquals(2, securityRequirements.size())
        val anonymousRequirements = securityRequirements.filter { it.isObject && it.size() == 0 }
        assertEquals(1, anonymousRequirements.size)
        val bearerScopes = securityRequirements
            .filter { it.isObject && it.size() == 1 && it.has("BearerAuth") }
            .map { it.at("/BearerAuth") }
        assertEquals(1, bearerScopes.size)
        assertTrue(bearerScopes.single().isArray)
        assertEquals(0, bearerScopes.single().size())
        assertTrue(operation.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(operation.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))
        assertTrue(operation.at("/description").asText().contains("토큰이 없거나 유효하지 않거나"))
        assertTrue(operation.at("/description").asText().contains("Bearer 선택"))
        assertEquals("string", request.at("/properties/recruitmentUrl/type").asText())
        assertTrue(request.at("/properties/recruitmentUrl/description").asText().contains("HTTP 또는 HTTPS"))
        assertEquals(255, request.at("/properties/email/maxLength").asInt())
        assertEquals("array", request.at("/properties/categories/type").asText())
        assertEquals(7, request.at("/properties/categories/maxItems").asInt())
        assertTrue(request.at("/properties/categories/items/enum").any { it.asText() == "KAKAO_CRM" })
        assertEquals(
            setOf("BUSINESS_REGISTRATION", "CORPORATE_REGISTRATION", "OVERSEAS"),
            request.at("/properties/businessNumberType/enum").map { it.asText() }.toSet(),
        )
        assertEquals(13, request.at("/properties/businessNumber/maxLength").asInt())
        assertEquals("^[0-9]+$", request.at("/properties/businessNumber/pattern").asText())
        assertEquals(
            setOf(
                "promotionTarget", "companyName", "recruitmentTitle", "recruitmentUrl", "recruitmentDeadline",
                "purpose", "budgetRange", "categories", "scheduleRequest", "advertisementRequest", "managerName", "email",
                "phoneNumber", "businessNumberType", "businessNumber", "invoiceMethod", "creativeMethod",
            ),
            request.at("/properties").fieldNames().asSequence().toSet(),
        )
        assertEquals(setOf("promotionTarget", "companyName", "recruitmentTitle", "recruitmentUrl", "recruitmentDeadline", "purpose", "categories", "managerName", "email", "phoneNumber", "invoiceMethod", "creativeMethod"), request.at("/required").map { it.asText() }.toSet())
        assertEquals(
            setOf("UNDER_500K", "FROM_500K_TO_1M", "FROM_1M_TO_3M", "OVER_3M", "UNDECIDED"),
            request.at("/properties/budgetRange/enum").map { it.asText() }.toSet(),
        )
        assertEquals(setOf("id"), response.at("/properties").fieldNames().asSequence().toSet())
        assertEquals(listOf("id"), response.at("/required").map { it.asText() })
        assertEquals("integer", response.at("/properties/id/type").asText())
        assertEquals("int64", response.at("/properties/id/format").asText())
    }

    @Test
    @DisplayName("내 광고 신청 내역은 로그인 필수이며 페이지와 404와 신청자 필드만 명시한다")
    fun `내 광고 신청 내역은 로그인 필수이며 페이지와 404와 신청자 필드만 명시한다`() {
        // given
        val document = openApiDocument()
        val base = "/paths/~1api~1v1~1users~1me~1advertisement-contracts"

        // when
        val list = document.at("$base/get")
        val detail = document.at("$base~1{contractId}/get")
        val summary = document.at("/components/schemas/MyAdvertisementContractSummaryResponse")
        val detailResponse = document.at("/components/schemas/MyAdvertisementContractDetailResponse")
        val product = document.at("/components/schemas/MyAdvertisementContractProductResponse")

        // then
        assertEquals("listMyAdvertisementContracts", list.at("/operationId").asText())
        assertEquals("getMyAdvertisementContract", detail.at("/operationId").asText())
        listOf(list, detail).forEach { operation ->
            assertEquals(1, operation.at("/security").size())
            assertTrue(operation.at("/security/0/BearerAuth").isArray)
            assertTrue(operation.at("/responses/200/content/application~1json/schema").isObject)
            assertTrue(operation.at("/responses/401/content/application~1json/schema").isObject)
        }
        assertPageParameter(list, "page", defaultValue = "1", minimum = 1, maximum = null)
        assertPageParameter(list, "size", defaultValue = "10", minimum = 1, maximum = 100)
        assertEquals("array", list.parameter("status").at("/schema/type").asText())
        assertEquals("int64", detail.parameter("contractId").at("/schema/format").asText())
        assertTrue(detail.at("/responses/404/description").asText().startsWith("ADVERTISEMENT_CONTRACT_NOT_FOUND"))
        assertEquals(
            setOf(
                "id", "status", "promotionTarget", "companyName", "recruitmentTitle", "recruitmentDeadline",
                "categories", "paymentAmount", "registeredAt",
            ),
            summary.at("/properties").fieldNames().asSequence().toSet(),
        )
        assertEquals(
            setOf(
                "id", "status", "promotionTarget", "companyName", "recruitmentTitle", "recruitmentUrl",
                "recruitmentDeadline", "purpose", "categories", "scheduleRequest", "advertisementRequest",
                "managerName", "email", "phoneNumber", "invoiceMethod", "creativeMethod", "paymentAmount",
                "registeredAt", "products",
            ),
            detailResponse.at("/properties").fieldNames().asSequence().toSet(),
        )
        listOf("scheduleRequest", "advertisementRequest", "paymentAmount").forEach { field ->
            assertTrue(detailResponse.at("/properties/$field/nullable").asBoolean(), field)
        }
        assertEquals(
            setOf("contractProductId", "productName", "category", "position", "productDeleted"),
            product.at("/properties").fieldNames().asSequence().toSet(),
        )
        assertTrue(product.at("/properties/position/nullable").asBoolean())
    }

    @Test
    @DisplayName("알림함 OpenAPI는 커서 크기 읽음 인증과 nullable 필드를 명시한다")
    fun `알림함 OpenAPI는 커서 크기 읽음 인증과 nullable 필드를 명시한다`() {
        // given
        val document = openApiDocument()
        val base = "/paths/~1api~1v1~1users~1me~1notifications"
        // when
        listOf(
            "$base/get" to "listMyNotifications",
            "$base~1unread-count/get" to "getMyNotificationUnreadCount",
            "$base~1{notificationId}~1read/patch" to "readMyNotification",
            "$base~1read-all/patch" to "readAllMyNotifications",
            "$base~1broadcasts~1{batchId}~1read/patch" to "readMyBroadcastNotification",
        ).forEach { (path, operationId) ->
            val operation = document.at(path)
            // then
            assertEquals(operationId, operation.at("/operationId").asText())
            assertTrue(operation.at("/security/0/BearerAuth").isArray)
            assertTrue(operation.at("/responses/200/content/application~1json/schema").isObject)
            assertTrue(operation.at("/responses/403/description").asText().contains("USER_SUSPENDED"))
        }
        val list = document.at("$base/get")
        assertPageParameter(list, "size", defaultValue = "20", minimum = 1, maximum = 100)
        assertTrue(list.at("/parameters").any { it.at("/name").asText() == "cursor" && !it.at("/required").asBoolean() })
        assertTrue(document.at("$base~1{notificationId}~1read/patch/responses/404/description").asText().contains("USER_NOTIFICATION_NOT_FOUND"))
        val item = document.at("/components/schemas/UserNotificationResponse/properties")
        assertTrue(item.has("isRead"))
        assertTrue(item.has("source"))
        assertTrue(item.has("sourceId"))
        assertTrue(item.at("/actorProfileImageUrl/nullable").asBoolean())
        assertTrue(document.at("/components/schemas/UserNotificationPageResponse/properties/nextCursor/nullable").asBoolean())
        assertEquals(setOf("SIDE_STUDY_COMMENT_REPLY", "CONCERN_COMMENT", "ADMIN_NOTIFICATION"), item.at("/type/enum").map { it.asText() }.toSet())
        listOf("id", "postId", "commentId", "url", "batchId", "body").forEach { field ->
            assertTrue(item.at("/$field/nullable").asBoolean(), field)
        }
        assertTrue(item.at("/url/description").asText().contains("관리자 알림"))
    }

    @Test
    @DisplayName("FCM 토큰 계약은 설치 UUID와 해제 및 충돌 응답을 명시한다")
    fun `FCM 토큰 계약은 설치 UUID와 해제 및 충돌 응답을 명시한다`() {
        // given
        val document = openApiDocument()
        // when
        val operations = document.at("/paths/~1api~1v1~1users~1me~1fcm-token")
        val request = document.at("/components/schemas/ReplaceFcmTokenRequest")
        // then
        assertEquals("deleteMyFcmToken", operations.at("/delete/operationId").asText())
        assertTrue(operations.at("/delete/security/0/BearerAuth").isArray)
        assertTrue(operations.at("/delete/responses/200/content/application~1json/schema").isObject)
        assertTrue(operations.at("/put/responses/409/description").asText().contains("FCM_TOKEN_CONFLICT"))
        assertEquals(4096, request.at("/properties/token/maxLength").asInt())
        assertEquals("uuid", request.at("/properties/deviceId/format").asText())
        assertTrue(request.at("/required").any { it.asText() == "token" })
        assertFalse(request.at("/required").any { it.asText() == "deviceId" })
        assertEquals(4096, document.at("/components/schemas/DeleteFcmTokenRequest/properties/token/maxLength").asInt())
    }

    @Test
    @DisplayName("사용자 OpenAPI는 인터페이스의 경로와 인증과 오류 명세를 노출한다")
    fun `사용자 OpenAPI는 인터페이스의 경로와 인증과 오류 명세를 노출한다`() {
        // given
        // 클래스의 SpringBootTest 설정으로 사용자 API 문서를 조회한다.
        // when
        val document = openApiDocument()

        // then
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
        assertTrue(document.at("/paths/~1api~1v1~1users~1me~1fcm-token/get").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1users~1me~1fcm-token/put").isObject)
        assertTrue(document.at("/paths/~1api~1v1~1users~1me~1notifications~1fcm~1test/post").isObject)
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
            document.at("/paths/~1api~1v1~1users~1me~1recruitment-posts~1drafts/post")
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
            document.at("/paths/~1api~1v1~1users~1me~1recruitment-posts~1{postId}~1publish/post")
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
        listOf("recruitmentStatus", "recruitmentType", "keyword", "sort")
            .forEach { name -> assertTrue(recruitmentPostBookmarks.parameter(name).isObject) }
        listOf("prepare", "cancel-preparation").forEach { command ->
            val move = document.at("/paths/~1api~1v1~1recruitment-post-bookmarks~1{postId}~1$command/post")
            assertTrue(move.at("/responses/200/content/application~1json/schema").isObject)
            assertTrue(
                move.at("/responses/409/description").asText()
                    .startsWith("INVALID_RECRUITMENT_APPLICATION_STATUS_TRANSITION"),
            )
        }
        val addRecruitmentPostBookmark =
            document.at("/paths/~1api~1v1~1recruitment-post-bookmarks~1{postId}/post")
        assertTrue(addRecruitmentPostBookmark.isObject)
        assertTrue(addRecruitmentPostBookmark.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(
            addRecruitmentPostBookmark.at("/responses/409/description").asText()
                .startsWith("RECRUITMENT_POST_BOOKMARK_ALREADY_EXISTS"),
        )
        val deleteRecruitmentPostBookmark =
            document.at("/paths/~1api~1v1~1recruitment-post-bookmarks~1{postId}/delete")
        assertTrue(deleteRecruitmentPostBookmark.isObject)
        assertTrue(deleteRecruitmentPostBookmark.at("/responses/200/content/application~1json/schema").isObject)
        // 서버를 먼저 배포하려고 남겨 둔 예전 경로는 명세에 보이지 않는다.
        listOf(
            "~1api~1v1~1me~1recruitment-posts",
            "~1api~1v1~1me~1recruitment-applications",
            "~1api~1v1~1recruitment-posts~1{postId}~1bookmarks~1me",
        ).forEach { path -> assertTrue(document.at("/paths/$path").isMissingNode, path) }
        val recruitmentPostApplication =
            document.at("/paths/~1api~1v1~1recruitment-posts~1{postId}~1applications/post")
        assertTrue(recruitmentPostApplication.at("/security/0/BearerAuth").isArray)
        assertTrue(recruitmentPostApplication.at("/responses/200/content/application~1json/schema").isObject)
        assertTrue(
            recruitmentPostApplication.at("/responses/409/description").asText()
                .startsWith("RECRUITMENT_POST_CLOSED"),
        )
        val myRecruitmentPostApplications =
            document.at("/paths/~1api~1v1~1users~1me~1recruitment-post-applications/get")
        assertTrue(myRecruitmentPostApplications.at("/security/0/BearerAuth").isArray)
        assertPageParameter(myRecruitmentPostApplications, "page", defaultValue = "1", minimum = 1, maximum = null)
        assertPageParameter(myRecruitmentPostApplications, "size", defaultValue = "10", minimum = 1, maximum = 100)
        listOf("recruitmentStatus", "recruitmentType", "applicationStatus", "keyword", "sort")
            .forEach { name -> assertTrue(myRecruitmentPostApplications.parameter(name).isObject) }
        val recruitmentPostApplicationItemProperties = document.at(
            "/components/schemas/RecruitmentPostApplicationItemResponse/properties",
        )
        listOf("progressMethod", "activityDurationMonths", "applicationStatus")
            .forEach { field -> assertTrue(recruitmentPostApplicationItemProperties.has(field)) }
        assertTrue(
            document.at("/components/schemas/RecruitmentPostApplicationPageResponse/properties/countsByRecruitmentType")
                .isObject,
        )
        val updateRecruitmentPostApplication = document.at(
            "/paths/~1api~1v1~1users~1me~1recruitment-post-applications~1{postId}/patch",
        )
        assertTrue(updateRecruitmentPostApplication.at("/security/0/BearerAuth").isArray)
        assertTrue(updateRecruitmentPostApplication.at("/requestBody/content/application~1json/schema").isObject)
        val deleteRecruitmentPostApplication = document.at(
            "/paths/~1api~1v1~1users~1me~1recruitment-post-applications~1{postId}/delete",
        )
        assertTrue(deleteRecruitmentPostApplication.at("/security/0/BearerAuth").isArray)
        assertTrue(deleteRecruitmentPostApplication.at("/responses/404/description").asText().startsWith("RECRUITMENT_POST_APPLICATION_NOT_FOUND"))
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
        val myRecruitmentPosts = document.at("/paths/~1api~1v1~1users~1me~1recruitment-posts/get")
        assertTrue(myRecruitmentPosts.at("/security/0/BearerAuth").isArray)
        assertPageParameter(myRecruitmentPosts, "page", defaultValue = "1", minimum = 1, maximum = null)
        assertPageParameter(myRecruitmentPosts, "size", defaultValue = "10", minimum = 1, maximum = 100)
        listOf("status", "recruitmentStatus", "applicationStatus", "recruitmentType", "keyword", "sort")
            .forEach { name -> assertTrue(myRecruitmentPosts.parameter(name).isObject) }
        assertTrue(document.at("/components/schemas/RecruitmentPostManagementItemResponse/properties/applicationCount").isObject)
        val copyMyRecruitmentPost =
            document.at("/paths/~1api~1v1~1users~1me~1recruitment-posts~1{postId}~1copies/post")
        assertTrue(copyMyRecruitmentPost.at("/security/0/BearerAuth").isArray)
        assertTrue(copyMyRecruitmentPost.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(
            copyMyRecruitmentPost.at("/responses/404/description").asText()
                .startsWith("RECRUITMENT_POST_NOT_FOUND"),
        )
        val myRecruitmentPostForm =
            document.at("/paths/~1api~1v1~1users~1me~1recruitment-posts~1{postId}/get")
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

        // 오늘의 공고도 토큰을 보내면 북마크 여부가 채워지는 선택적 인증이며 받는 파라미터가 없다.
        val todayJobs = document.at("/paths/~1api~1v1~1jobs~1today/get")
        assertTrue(todayJobs.isObject)
        assertTrue(todayJobs.at("/security/0/BearerAuth").isArray)
        assertTrue(todayJobs.at("/parameters").isMissingNode)
        // 항목은 목록 항목의 필드를 같은 높이에 펼치고 추천 문구를 더한다. 중첩된 job 필드로 드러나면 계약과 다르다.
        val todayJobProperties = document.at("/components/schemas/UserTodayJobSummaryResponse/properties")
        listOf("id", "title", "bookmarked", "registeredAt", "recommendationTitle", "recommendationDescription").forEach { field ->
            assertTrue(todayJobProperties.has(field), field)
        }
        assertFalse(todayJobProperties.has("job"))

        // 비슷한 공고는 내 희망 직무·산업으로 고르는 사용자별 결과라 인증이 필수다.
        val similarJobs = document.at("/paths/~1api~1v1~1jobs~1similar/get")
        assertTrue(similarJobs.at("/security/0/BearerAuth").isArray)
        assertTrue(similarJobs.at("/responses/401/description").asText().startsWith("UNAUTHORIZED"))
        assertTrue(similarJobs.at("/responses/200/content/application~1json/schema").isObject)

        // 홈 히어로 공고 수는 토큰이 선택이라 인증 요구를 노출하고 401 응답은 없다. 클라이언트는 count와 matchedWishJob으로 문구를 고른다.
        val headlineCount = document.at("/paths/~1api~1v1~1jobs~1headline-count/get")
        assertEquals("getPublicJobHeadlineCount", headlineCount.at("/operationId").asText())
        assertTrue(headlineCount.at("/security/0/BearerAuth").isArray)
        assertTrue(headlineCount.at("/responses/401").isMissingNode)
        val headlineCountProperties = document.at("/components/schemas/UserJobHeadlineCountResponse/properties")
        listOf("count", "matchedWishJob").forEach { field -> assertTrue(headlineCountProperties.has(field), field) }

        // 공지는 토큰으로 달라지는 값이 없어 인증 요구를 노출하지 않는다.
        val announcementList = document.at("/paths/~1api~1v1~1announcements/get")
        assertTrue(announcementList.isObject)
        assertTrue(announcementList.at("/security").isMissingNode)
        assertTrue(document.at("/paths/~1api~1v1~1announcements~1{announcementId}/get/security").isMissingNode)
        assertPageParameter(announcementList, "page", defaultValue = "1", minimum = 1, maximum = null)
        assertPageParameter(announcementList, "size", defaultValue = "10", minimum = 1, maximum = 100)

        // enum 선택지는 누구에게나 같은 고정 값이라 인증 요구를 노출하지 않는다.
        val enums = document.at("/paths/~1api~1v1~1enums/get")
        assertTrue(enums.isObject)
        assertTrue(enums.at("/security").isMissingNode)

        // 추천 챌린지는 토큰을 보내면 그 사용자로 추천하므로 채용공고 목록과 같은 선택적 인증을 노출한다.
        val recommendedChallenges = document.at("/paths/~1api~1v1~1recommended-challenges/get")
        assertTrue(recommendedChallenges.isObject)
        assertTrue(recommendedChallenges.at("/security/0/BearerAuth").isArray)

        // 개선 의견도 로그인 없이 남기고, 토큰을 보내면 작성자를 기록하는 선택적 인증이다.
        val createServiceFeedback = document.at("/paths/~1api~1v1~1service-feedbacks/post")
        assertTrue(createServiceFeedback.at("/security/0/BearerAuth").isArray)
        assertTrue(createServiceFeedback.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(createServiceFeedback.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))

        // 기업 공고·부트캠프 등록·수정은 정리 작업이 지운 로고·대표 이미지 URL을 400으로 막는다(LC-3564). FE가 이 코드로 다시 올리게 한다.
        listOf(
            "/paths/~1api~1v1~1users~1me~1jobs/post" to "201",
            "/paths/~1api~1v1~1users~1me~1jobs~1{jobId}/put" to "200",
            "/paths/~1api~1v1~1users~1me~1bootcamps/post" to "201",
            "/paths/~1api~1v1~1users~1me~1bootcamps~1{bootcampId}/put" to "200",
        ).forEach { (path, successCode) ->
            val operation = document.at(path)
            assertTrue(operation.at("/responses/$successCode/content/application~1json/schema").isObject, path)
            assertTrue(operation.at("/responses/400/description").asText().startsWith("IMAGE_ASSET_NOT_AVAILABLE"), path)
        }

        // 부트캠프 목록·상세도 토큰을 보내면 북마크 여부가 채워지므로 공고와 같은 선택적 인증을 노출한다.
        val bootcampList = document.at("/paths/~1api~1v1~1bootcamps/get")
        assertTrue(bootcampList.at("/security/0/BearerAuth").isArray)
        assertTrue(document.at("/paths/~1api~1v1~1bootcamps~1{bootcampId}/get/security/0/BearerAuth").isArray)
        assertPageParameter(bootcampList, "page", defaultValue = "1", minimum = 1, maximum = null)
        assertPageParameter(bootcampList, "size", defaultValue = "10", minimum = 1, maximum = 100)
        listOf("sort", "category", "keyword")
            .forEach { name -> assertTrue(bootcampList.parameter(name).isObject) }

        val sourceUrlClick = document.at("/paths/~1api~1v1~1jobs~1{jobId}~1source-url-clicks/post")
        assertTrue(sourceUrlClick.at("/security/0/BearerAuth").isArray)

        val applicationUrlClick =
            document.at("/paths/~1api~1v1~1bootcamps~1{bootcampId}~1source-url-clicks/post")
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
        val movejobId = document.at("/paths/~1api~1v1~1job-bookmarks~1{jobId}~1application-status/put")
        assertTrue(movejobId.at("/responses/200/content/application~1json/schema").isObject)
        assertTrue(movejobId.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))
        assertTrue(movejobId.at("/responses/404/description").asText().startsWith("JOB_BOOKMARK_NOT_FOUND"))
        assertFalse(document.at("/paths/~1api~1v1~1job-bookmarks~1{jobId}~1prepare").isObject)

        val bootcampBookmarks = document.at("/paths/~1api~1v1~1bootcamp-bookmarks/get")
        assertTrue(bootcampBookmarks.at("/security/0/BearerAuth").isArray)
        listOf("category", "keyword", "applicationStatus", "sort")
            .forEach { name -> assertTrue(bootcampBookmarks.parameter(name).isObject) }
        assertPageParameter(bootcampBookmarks, "page", defaultValue = "1", minimum = 1, maximum = null)
        val addBootcampBookmark = document.at("/paths/~1api~1v1~1bootcamp-bookmarks~1{bootcampId}/post")
        assertTrue(addBootcampBookmark.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(
            addBootcampBookmark.at("/responses/409/description").asText()
                .startsWith("BOOTCAMP_BOOKMARK_ALREADY_EXISTS"),
        )
        val deleteBootcampBookmark = document.at("/paths/~1api~1v1~1bootcamp-bookmarks~1{bootcampId}/delete")
        assertTrue(deleteBootcampBookmark.at("/responses/200/content/application~1json/schema").isObject)
        val movebootcampId = document.at("/paths/~1api~1v1~1bootcamp-bookmarks~1{bootcampId}~1application-status/put")
        assertTrue(movebootcampId.at("/responses/200/content/application~1json/schema").isObject)
        assertTrue(movebootcampId.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))
        assertTrue(movebootcampId.at("/responses/404/description").asText().startsWith("BOOTCAMP_BOOKMARK_NOT_FOUND"))
        assertFalse(document.at("/paths/~1api~1v1~1bootcamp-bookmarks~1{bootcampId}~1prepare").isObject)

        // 역할은 토큰에 없으므로 내 정보 조회는 인증이 필수다.
        val myAccount = document.at("/paths/~1api~1v1~1users~1me/get")
        assertTrue(myAccount.at("/security/0/BearerAuth").isArray)
        assertTrue(myAccount.at("/responses/401/description").asText().startsWith("UNAUTHORIZED"))
        assertTrue(myAccount.at("/responses/404/description").asText().startsWith("USER_NOT_FOUND"))
        val myAccountProperties = document.at("/components/schemas/MyAccountResponse/properties")
        listOf("userId", "role", "status", "email", "joinedAt", "profile", "companyProfile")
            .forEach { field -> assertTrue(myAccountProperties.has(field)) }
        val myCompanyProfileProperties = document.at("/components/schemas/MyCompanyProfileResponse/properties")
        listOf(
            "managerPhone", "businessNumberType", "businessNumber", "companyType", "employeeCountRange", "industry",
            "address", "homepageUrl", "managerTitle", "managerDuty", "recommender",
        ).forEach { field -> assertTrue(myCompanyProfileProperties.has(field), field) }

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

        // 기업 정보도 조회는 내 정보에 담고 수정만 따로 연다.
        // 마이페이지가 기본 정보와 담당자 정보를 따로 저장하므로 수정도 나눠 연다.
        listOf("basic-info", "manager-info").forEach { path ->
            val replaceCompanyProfile = document.at("/paths/~1api~1v1~1users~1me~1company-profile~1$path/put")
            assertTrue(replaceCompanyProfile.at("/security/0/BearerAuth").isArray)
            assertTrue(
                replaceCompanyProfile.at("/responses/403/description").asText().startsWith("COMPANY_ROLE_REQUIRED"),
            )
        }
        val basicInfoRequest = document.at("/components/schemas/ReplaceMyCompanyBasicInfoRequest")
        assertEquals(150, basicInfoRequest.at("/properties/organizationName/maxLength").asInt())
        assertEquals(36, basicInfoRequest.at("/properties/logoImageId/maxLength").asInt())
        val managerInfoRequest = document.at("/components/schemas/ReplaceMyCompanyManagerInfoRequest")
        assertEquals(100, managerInfoRequest.at("/properties/managerName/maxLength").asInt())
        // 가입 때 받은 기업·담당자 정보도 마이페이지에서 고치며, 형식 제약은 가입과 같다.
        assertEquals(13, basicInfoRequest.at("/properties/businessNumber/maxLength").asInt())
        assertTrue(basicInfoRequest.at("/properties/businessNumberType/enum").any { it.asText() == "OVERSEAS" })
        assertTrue(basicInfoRequest.at("/properties/companyType").isObject)
        assertTrue(basicInfoRequest.at("/properties/employeeCountRange").isObject)
        assertEquals(2048, basicInfoRequest.at("/properties/homepageUrl/maxLength").asInt())
        assertEquals(50, managerInfoRequest.at("/properties/managerTitle/maxLength").asInt())
        assertTrue(managerInfoRequest.at("/properties/managerDuty/enum").any { it.asText() == "HR" })
        assertEquals(100, managerInfoRequest.at("/properties/recommender/maxLength").asInt())

        val jobCalendar = document.at("/paths/~1api~1v1~1jobs~1calendar/get")
        // 스크랩 공고만 거를 때 토큰이 필요하므로 Swagger UI에서 토큰을 보낼 수 있어야 한다.
        assertTrue(jobCalendar.at("/security/0/BearerAuth").isArray)
        assertEquals("date", jobCalendar.parameter("from").at("/schema/format").asText())
        assertEquals("date", jobCalendar.parameter("to").at("/schema/format").asText())
        assertEquals(2, jobCalendar.parameter("keyword").at("/schema/minLength").asInt())
        assertEquals("array", jobCalendar.parameter("jobRole").at("/schema/type").asText())
        assertTrue(jobCalendar.parameter("jobRole").at("/schema/items/enum").any { it.asText() == "IT_BACKEND" })
        assertFalse(jobCalendar.parameter("employmentType")["required"]?.asBoolean() ?: false)

        val calendarBadRequest = document.at("/paths/~1api~1v1~1jobs~1calendar/get/responses/400")
        assertTrue(calendarBadRequest["description"].asText().startsWith("BAD_REQUEST"))
        assertTrue(calendarBadRequest.at("/content/application~1json/schema").isObject)
        assertTrue(
            document.at("/paths/~1api~1v1~1jobs~1calendar/get/responses/401/description").asText()
                .startsWith("UNAUTHORIZED"),
        )

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
        // 웹의 NEW 배지가 쓰는 등록 일시는 목록·상세 모두에 있다(LC-3567).
        assertTrue(document.at("/components/schemas/UserJobSummaryResponse/properties/registeredAt").isObject)
        assertTrue(document.at("/components/schemas/UserJobDetailResponse/properties/registeredAt").isObject)
        assertTrue(document.at("/components/schemas/UserBootcampSummaryResponse/properties/bookmarked").isObject)
        assertTrue(document.at("/components/schemas/UserBootcampDetailResponse/properties/bookmarked").isObject)

        val companySignUp = document.at("/paths/~1api~1v1~1auth~1company~1signup/post")
        assertFalse(companySignUp.has("security"))
        assertTrue(companySignUp.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(companySignUp.at("/responses/409/description").asText().startsWith("EMAIL_ALREADY_EXISTS"))
        assertTrue(companySignUp.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))
        // 기업·담당자 정보는 이 값을 받기 전의 가입 화면이 보내지 않으므로 모두 선택이다.
        val companySignUpRequest = document.at("/components/schemas/CompanySignUpRequest")
        val companySignUpRequired = companySignUpRequest.at("/required").map { it.asText() }.toSet()
        assertEquals(setOf("email", "password", "organizationName", "managerName"), companySignUpRequired)
        listOf(
            "businessNumberType", "businessNumber", "companyType", "employeeCountRange", "industry", "address",
            "homepageUrl", "managerTitle", "managerDuty", "managerPhone", "recommender",
        ).forEach { field -> assertTrue(companySignUpRequest.at("/properties/$field").isObject, field) }
        assertEquals(13, companySignUpRequest.at("/properties/businessNumber/maxLength").asInt())

        val companySignIn = document.at("/paths/~1api~1v1~1auth~1company~1signin/post")
        assertFalse(companySignIn.has("security"))
        assertTrue(
            companySignIn.at("/responses/401/description").asText().startsWith("INVALID_COMPANY_CREDENTIALS"),
        )

        // 광고 문의는 비회원도 남기고, 토큰을 보내면 회원 구분을 서버가 정하는 선택적 인증이다. 저장하므로 201이다.
        val advertisementInquiry = document.at("/paths/~1api~1v1~1advertisement-inquiries/post")
        assertTrue(advertisementInquiry.at("/security/0/BearerAuth").isArray)
        assertTrue(advertisementInquiry.at("/responses/201/content/application~1json/schema").isObject)
        assertTrue(advertisementInquiry.at("/responses/400/description").asText().startsWith("BAD_REQUEST"))
        assertTrue(advertisementInquiry.at("/responses/503").isMissingNode)
        // 회사명 입력 방식은 이 필드 없이 부르는 이전 폼이 있어 선택이다.
        val inquiryRequest = document.at("/components/schemas/CreateAdvertisementInquiryRequest")
        assertTrue(inquiryRequest.at("/properties/companyNameSource").isObject)
        assertFalse(inquiryRequest.at("/required").any { it.asText() == "companyNameSource" })

        // 회사명 검색은 광고 문의 폼의 비회원이 쓰는 공개 조회다.
        val companyNames = document.at("/paths/~1api~1v1~1company-names/get")
        assertEquals("listPublicCompanyNames", companyNames.at("/operationId").asText())
        assertFalse(companyNames.has("security"))
        val companyNameKeyword = companyNames.at("/parameters").first { it.at("/name").asText() == "keyword" }
        assertFalse(companyNameKeyword.at("/required").asBoolean())
        assertEquals(50, companyNameKeyword.at("/schema/maxLength").asInt())

        val publicSignIn = document.at("/paths/~1api~1v1~1auth~1letscareer/post")
        assertFalse(publicSignIn.has("security"))
        val signOut = document.at("/paths/~1api~1v1~1auth~1signout/post")
        assertTrue(signOut.at("/security/0/BearerAuth").isArray)
    }

    @Test
    @DisplayName("EnumField enum은 값과 code와 설명을 문서에 노출한다")
    fun `EnumField enum은 값과 code와 설명을 문서에 노출한다`() {
        // given
        // 클래스의 SpringBootTest 설정으로 사용자 API 문서를 조회한다.
        // when
        val document = openApiDocument()

        // then
        // 요청 본문 안의 enum
        val bodyEnum = document
            .at("/components/schemas/CreateAdvertisementInquiryRequest/properties/promotionChannel/description")
            .asText()
        assertTrue(bodyEnum.contains("| `OPEN_CHAT_MARKETING` | 2 | 오픈채팅방 · 마케팅 |"))

        // 쿼리 파라미터로 쓰는 enum
        val parameterEnum = document.at("/paths/~1api~1v1~1jobs~1popular/get/parameters")
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
