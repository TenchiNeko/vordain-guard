package com.vordain.guard.core.alertcenter

import com.vordain.guard.core.model.DeviceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DebugChildAlertReportCodecTest {
    private val codec = DebugChildAlertReportCodec()

    @Test
    fun encodeDecodeRoundTrip() {
        val report = DebugChildAlertReport(
            childDeviceId = DeviceId("child-debug-device"),
            generatedAtMillis = 123L,
            alerts = listOf(alert()),
            summaryLabel = "1 critical alert",
        )

        val decoded = codec.decode(codec.encode(report))

        assertIs<DebugChildAlertReportCodecResult.Decoded>(decoded)
        assertEquals(report.childDeviceId, decoded.report.childDeviceId)
        assertEquals(report.generatedAtMillis, decoded.report.generatedAtMillis)
        assertEquals(report.summaryLabel, decoded.report.summaryLabel)
        assertEquals(report.alerts.first(), decoded.report.alerts.first())
    }

    @Test
    fun wrongHeaderRejected() {
        val decoded = codec.decode("NOPE\nchildDeviceId=child-debug-device")

        assertIs<DebugChildAlertReportCodecResult.Rejected>(decoded)
    }

    @Test
    fun unknownAlertTypeRejected() {
        val decoded = codec.decode(
            """
            VORDAIN_DEBUG_CHILD_ALERT_REPORT_V1
            childDeviceId=child-debug-device
            generatedAtMillis=123
            alert0.id=a
            alert0.type=NOPE
            alert0.severity=HIGH
            alert0.status=ACTIVE
            alert0.occurredAtMillis=123
            """.trimIndent(),
        )

        assertIs<DebugChildAlertReportCodecResult.Rejected>(decoded)
    }

    @Test
    fun missingChildDeviceIdRejected() {
        val decoded = codec.decode(
            """
            VORDAIN_DEBUG_CHILD_ALERT_REPORT_V1
            generatedAtMillis=123
            """.trimIndent(),
        )

        assertIs<DebugChildAlertReportCodecResult.Rejected>(decoded)
    }

    @Test
    fun forbiddenSensitiveFieldRejected() {
        val decoded = codec.decode(
            """
            VORDAIN_DEBUG_CHILD_ALERT_REPORT_V1
            childDeviceId=child-debug-device
            generatedAtMillis=123
            pinValue=123456
            """.trimIndent(),
        )

        assertIs<DebugChildAlertReportCodecResult.Rejected>(decoded)
    }

    @Test
    fun warningSaysDebugLocalOnly() {
        assertTrue(DebugChildAlertReport.WARNING_TEXT.contains("Debug/local alert report only"))
        assertTrue(DebugChildAlertReport.WARNING_TEXT.contains("encrypted relay later"))
    }

    @Test
    fun rejectsUnknownSeverityOrStatus() {
        val badSeverity = codec.decode(
            """
            VORDAIN_DEBUG_CHILD_ALERT_REPORT_V1
            childDeviceId=child-debug-device
            generatedAtMillis=123
            alert0.id=a
            alert0.type=VPN_REVOKED
            alert0.severity=NOPE
            alert0.status=ACTIVE
            alert0.occurredAtMillis=123
            """.trimIndent(),
        )
        val badStatus = codec.decode(
            """
            VORDAIN_DEBUG_CHILD_ALERT_REPORT_V1
            childDeviceId=child-debug-device
            generatedAtMillis=123
            alert0.id=a
            alert0.type=VPN_REVOKED
            alert0.severity=HIGH
            alert0.status=NOPE
            alert0.occurredAtMillis=123
            """.trimIndent(),
        )

        assertIs<DebugChildAlertReportCodecResult.Rejected>(badSeverity)
        assertIs<DebugChildAlertReportCodecResult.Rejected>(badStatus)
    }

    private fun alert(): ChildAlert {
        return ChildAlert(
            id = "a1",
            type = AlertType.VPN_REVOKED,
            severity = AlertSeverity.CRITICAL,
            status = AlertStatus.ACTIVE,
            childDeviceId = DeviceId("child-debug-device"),
            occurredAtMillis = 1000L,
            title = "VPN revoked",
            detail = "Basic DNS Guard needs review.",
            sourceLabel = "child",
            policyVersion = "debug-1",
        )
    }
}
