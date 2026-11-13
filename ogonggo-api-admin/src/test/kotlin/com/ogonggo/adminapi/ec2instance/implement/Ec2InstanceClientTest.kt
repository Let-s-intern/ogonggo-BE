package com.ogonggo.adminapi.ec2instance.implement

import com.ogonggo.adminapi.ec2instance.error.Ec2InstanceErrorCode
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2Instance
import com.ogonggo.adminapi.ec2instance.implement.dto.Ec2InstanceState
import com.ogonggo.core.error.ConflictException
import com.ogonggo.core.error.InternalServerException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse
import software.amazon.awssdk.services.ec2.model.Ec2Exception
import software.amazon.awssdk.services.ec2.model.Instance
import software.amazon.awssdk.services.ec2.model.InstanceState
import software.amazon.awssdk.services.ec2.model.InstanceStateChange
import software.amazon.awssdk.services.ec2.model.InstanceStateName
import software.amazon.awssdk.services.ec2.model.Reservation
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest
import software.amazon.awssdk.services.ec2.model.StartInstancesResponse
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest
import software.amazon.awssdk.services.ec2.model.StopInstancesResponse

class Ec2InstanceClientTest {

    private val redash = Ec2Instance.REDASH
    private val crawler = Ec2Instance.CRAWLER
    private val properties = Ec2InstanceProperties(redashInstanceId = "i-redash", crawlerInstanceId = "i-crawler")

    @Test
    fun `AWS가 돌려준 인스턴스 ID를 인스턴스별 상태로 읽는다`() {
        // given
        val client = Ec2InstanceClient(
            FakeEc2Client(
                describe = {
                    DescribeInstancesResponse.builder()
                        .reservations(
                            Reservation.builder().instances(instance("i-redash", InstanceStateName.RUNNING)).build(),
                            Reservation.builder().instances(instance("i-crawler", InstanceStateName.STOPPED)).build(),
                        )
                        .build()
                },
            ),
            properties,
        )

        // when
        val states = client.readStates(listOf(redash, crawler))

        // then
        assertEquals(mapOf(redash to Ec2InstanceState.RUNNING, crawler to Ec2InstanceState.STOPPED), states)
    }

    @Test
    fun `켜기와 끄기는 요청 직후 상태를 돌려준다`() {
        // given
        val client = Ec2InstanceClient(
            FakeEc2Client(
                start = {
                    StartInstancesResponse.builder()
                        .startingInstances(stateChange(it.instanceIds().single(), InstanceStateName.PENDING))
                        .build()
                },
                stop = {
                    StopInstancesResponse.builder()
                        .stoppingInstances(stateChange(it.instanceIds().single(), InstanceStateName.STOPPING))
                        .build()
                },
            ),
            properties,
        )

        // when & then
        assertEquals(Ec2InstanceState.PENDING, client.start(redash))
        assertEquals(Ec2InstanceState.STOPPING, client.stop(crawler))
    }

    @Test
    fun `AWS가 지금 상태에서 받지 않는 요청은 409로 알린다`() {
        val client = Ec2InstanceClient(FakeEc2Client(start = { throw ec2Exception("IncorrectInstanceState") }), properties)

        val exception = assertThrows<ConflictException> { client.start(redash) }

        assertEquals(Ec2InstanceErrorCode.EC2_INSTANCE_STATE_CONFLICT, exception.errorCode)
    }

    @Test
    fun `권한 부족 같은 그 밖의 AWS 거절은 502로 알린다`() {
        val client = Ec2InstanceClient(FakeEc2Client(stop = { throw ec2Exception("UnauthorizedOperation") }), properties)

        val exception = assertThrows<InternalServerException> { client.stop(crawler) }

        assertEquals(Ec2InstanceErrorCode.EC2_UNAVAILABLE, exception.errorCode)
    }

    @Test
    fun `인스턴스 ID가 설정되지 않았으면 AWS를 부르지 않고 503으로 알린다`() {
        // given: ID가 비어 있고, AWS를 부르면 FakeEc2Client가 실패한다
        val client = Ec2InstanceClient(FakeEc2Client(), Ec2InstanceProperties(redashInstanceId = "i-redash"))

        // when & then
        val exception = assertThrows<InternalServerException> { client.start(crawler) }
        assertEquals(Ec2InstanceErrorCode.EC2_INSTANCE_NOT_CONFIGURED, exception.errorCode)
    }

    @Test
    fun `인스턴스 ID가 하나도 설정되지 않았으면 상태 조회도 AWS를 부르지 않는다`() {
        val client = Ec2InstanceClient(FakeEc2Client(), Ec2InstanceProperties())

        assertEquals(emptyMap<Ec2Instance, Ec2InstanceState>(), client.readStates(listOf(redash, crawler)))
    }

    private fun instance(instanceId: String, state: InstanceStateName): Instance =
        Instance.builder().instanceId(instanceId).state(InstanceState.builder().name(state).build()).build()

    private fun stateChange(instanceId: String, state: InstanceStateName): InstanceStateChange =
        InstanceStateChange.builder()
            .instanceId(instanceId)
            .currentState(InstanceState.builder().name(state).build())
            .build()

    private fun ec2Exception(errorCode: String): Ec2Exception =
        Ec2Exception.builder()
            .awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).build())
            .statusCode(400)
            .build() as Ec2Exception

    /** 기본 메서드는 요청 객체를 받는 메서드로 넘어오므로 그 세 개만 바꾼다. 나머지는 SDK 기본대로 지원하지 않는다고 던진다. */
    private class FakeEc2Client(
        private val describe: (DescribeInstancesRequest) -> DescribeInstancesResponse = { error("호출하지 않는다") },
        private val start: (StartInstancesRequest) -> StartInstancesResponse = { error("호출하지 않는다") },
        private val stop: (StopInstancesRequest) -> StopInstancesResponse = { error("호출하지 않는다") },
    ) : Ec2Client {
        override fun describeInstances(request: DescribeInstancesRequest): DescribeInstancesResponse = describe(request)
        override fun startInstances(request: StartInstancesRequest): StartInstancesResponse = start(request)
        override fun stopInstances(request: StopInstancesRequest): StopInstancesResponse = stop(request)
        override fun serviceName(): String = Ec2Client.SERVICE_NAME
        override fun close() = Unit
    }
}
