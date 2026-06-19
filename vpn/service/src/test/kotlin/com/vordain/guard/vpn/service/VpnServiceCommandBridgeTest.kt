package com.vordain.guard.vpn.service

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VpnServiceCommandBridgeTest {
    @Test
    fun startActionMapsToStartCommand() {
        val sink = RecordingVpnSessionSink()

        val result = VpnServiceCommandBridge(sink)
            .handleAction(VordainVpnServiceActions.ACTION_START_PROTECTION)

        assertEquals(VpnServiceCommandResult.HandledStart, result)
        assertEquals(listOf("start-requested"), sink.calls)
    }

    @Test
    fun stopActionMapsToStopCommand() {
        val sink = RecordingVpnSessionSink()

        val result = VpnServiceCommandBridge(sink)
            .handleAction(VordainVpnServiceActions.ACTION_STOP_PROTECTION)

        assertEquals(VpnServiceCommandResult.HandledStop, result)
        assertEquals(listOf("stop-requested"), sink.calls)
    }

    @Test
    fun labCaptureActionMapsToDistinctLabStartCommand() {
        val sink = RecordingVpnSessionSink()

        val result = VpnServiceCommandBridge(sink)
            .handleAction(VordainVpnServiceActions.ACTION_START_LAB_CAPTURE)

        assertEquals(VpnServiceCommandResult.HandledLabStart, result)
        assertEquals(listOf("start-requested"), sink.calls)
    }

    @Test
    fun basicDnsGuardActionMapsToDistinctStartCommand() {
        val sink = RecordingVpnSessionSink()

        val result = VpnServiceCommandBridge(sink)
            .handleAction(VordainVpnServiceActions.ACTION_START_BASIC_DNS_GUARD)

        assertEquals(VpnServiceCommandResult.HandledBasicDnsGuardStart, result)
        assertEquals(listOf("start-requested"), sink.calls)
    }

    @Test
    fun stopBasicDnsGuardActionMapsToStopCommand() {
        val sink = RecordingVpnSessionSink()

        val result = VpnServiceCommandBridge(sink)
            .handleAction(VordainVpnServiceActions.ACTION_STOP_BASIC_DNS_GUARD)

        assertEquals(VpnServiceCommandResult.HandledStop, result)
        assertEquals(listOf("stop-requested"), sink.calls)
    }

    @Test
    fun stopLabCaptureActionMapsToStopCommand() {
        val sink = RecordingVpnSessionSink()

        val result = VpnServiceCommandBridge(sink)
            .handleAction(VordainVpnServiceActions.ACTION_STOP_LAB_CAPTURE)

        assertEquals(VpnServiceCommandResult.HandledStop, result)
        assertEquals(listOf("stop-requested"), sink.calls)
    }

    @Test
    fun unknownActionIsSafeNoOp() {
        val sink = RecordingVpnSessionSink()

        val result = VpnServiceCommandBridge(sink).handleAction("unknown")

        assertEquals(VpnServiceCommandResult.Ignored, result)
        assertTrue(sink.calls.isEmpty())
    }

    @Test
    fun nullActionIsSafeNoOp() {
        val sink = RecordingVpnSessionSink()

        val result = VpnServiceCommandBridge(sink).handleAction(null)

        assertEquals(VpnServiceCommandResult.Ignored, result)
        assertTrue(sink.calls.isEmpty())
    }

    @Test
    fun revokedCallbackMapsToSessionSink() {
        val sink = RecordingVpnSessionSink()

        sink.onVpnRevoked()

        assertEquals(listOf("revoked"), sink.calls)
    }

    @Test
    fun noOpDefaultBridgeDoesNotThrow() {
        VpnServiceCommandBridge(VpnSessionSink.NoOp)
            .handleAction(VordainVpnServiceActions.ACTION_START_PROTECTION)
        VpnServiceCommandBridge(VpnSessionSink.NoOp)
            .handleAction(VordainVpnServiceActions.ACTION_STOP_PROTECTION)
        VpnServiceCommandBridge(VpnSessionSink.NoOp).handleAction("unknown")
    }

    @Test
    fun notificationTextContainsNoSensitiveActivityData() {
        val notificationText = listOf(
            VpnForegroundNotification.TITLE,
            VpnForegroundNotification.BODY,
            VpnForegroundNotification.CHANNEL_NAME,
        ).joinToString(" ")

        assertFalse(notificationText.contains("domain", ignoreCase = true))
        assertFalse(notificationText.contains("browser", ignoreCase = true))
        assertFalse(notificationText.contains("website", ignoreCase = true))
        assertFalse(notificationText.contains("app package", ignoreCase = true))
        assertFalse(notificationText.contains("message", ignoreCase = true))
        assertTrue(notificationText.contains("shell", ignoreCase = true))
    }

    @Test
    fun intentFactorySourceUsesExpectedActionStrings() {
        val source = repositoryRoot()
            .resolve("vpn/service/src/main/kotlin/com/vordain/guard/vpn/service/VordainVpnServiceIntents.kt")
            .readText()

        assertTrue(source.contains("ACTION_START_PROTECTION"))
        assertTrue(source.contains("ACTION_STOP_PROTECTION"))
        assertTrue(source.contains("ACTION_START_LAB_CAPTURE"))
        assertTrue(source.contains("ACTION_STOP_LAB_CAPTURE"))
        assertTrue(source.contains("ACTION_START_BASIC_DNS_GUARD"))
        assertTrue(source.contains("ACTION_STOP_BASIC_DNS_GUARD"))
        assertTrue(source.contains("VordainVpnService::class.java"))
    }

    @Test
    fun vordainVpnServiceSourceHandlesExplicitStartAndStopActions() {
        val source = serviceSource()

        assertTrue(source.contains("override fun onStartCommand"))
        assertTrue(source.contains("HandledStart"))
        assertTrue(source.contains("HandledLabStart"))
        assertTrue(source.contains("HandledBasicDnsGuardStart"))
        assertTrue(source.contains("HandledStop"))
        assertTrue(source.contains("startForeground("))
        assertTrue(source.contains("stopForeground("))
    }

    @Test
    fun notificationTextForBasicDnsGuardIsConservative() {
        val text = listOf(
            VpnForegroundNotificationMode.BASIC_DNS_GUARD.title,
            VpnForegroundNotificationMode.BASIC_DNS_GUARD.body,
            VpnForegroundNotificationMode.DNS_ONLY_LAB.title,
            VpnForegroundNotificationMode.DNS_ONLY_LAB.body,
            VpnForegroundNotificationMode.FULL_TUNNEL_LAB.title,
            VpnForegroundNotificationMode.FULL_TUNNEL_LAB.body,
        ).joinToString(" ")

        assertTrue(text.contains("Not full protection"))
        assertFalse(text.contains("domain", ignoreCase = true))
        assertFalse(text.contains("browser", ignoreCase = true))
        assertFalse(text.contains("activity", ignoreCase = true))
        assertFalse(text.contains("Filtering active"))
    }

    @Test
    fun tunnelOpenerSourceEstablishesVpnInterfaceWithoutDnsOrSockets() {
        val source = repositoryRoot()
            .resolve("vpn/service/src/main/kotlin/com/vordain/guard/vpn/service/AndroidVpnTunnelOpener.kt")
            .readText()

        assertTrue(source.contains("establish()"))
        assertTrue(source.contains("addAddress"))
        assertTrue(source.contains("addRoute"))
        assertTrue(source.contains("addDnsServer"))
        assertDoesNotContain(source, "DatagramSocket")
        assertDoesNotContain(source, "Socket(")
        assertDoesNotContain(source, ".read(")
        assertDoesNotContain(source, "write(")
    }

    @Test
    fun vordainVpnServiceSourceDoesNotContainPolicyDnsRelayOrPacketLogic() {
        val source = serviceSource()

        assertDoesNotContain(source, "PolicyEngine")
        assertDoesNotContain(source, "DomainTrafficEvaluator")
        assertDoesNotContain(source, "DnsMessageParser")
        assertDoesNotContain(source, "RelayClient")
        assertDoesNotContain(source, "data.outbox")
        assertDoesNotContain(source, "data.relay")
        assertDoesNotContain(source, "backend")
        assertDoesNotContain(source, ".read(")
        assertDoesNotContain(source, "write(")
        assertDoesNotContain(source, "FileDescriptor")
        assertDoesNotContain(source, "DatagramSocket")
        assertDoesNotContain(source, "Socket(")
    }

    @Test
    fun childDashboardWarnsThatLabCaptureDropsTrafficLocally() {
        val source = repositoryRoot()
            .resolve("apps/child-app/src/main/java/com/vordain/guard/child/ChildVpnSmokeLabels.kt")
            .readText()

        assertTrue(source.contains("Internet may stop"))
        assertTrue(source.contains("No forwarding yet"))
        assertTrue(source.contains("Not full protection"))
    }

    private class RecordingVpnSessionSink : VpnSessionSink {
        val calls = mutableListOf<String>()

        override fun onVpnStartRequested() {
            calls += "start-requested"
        }

        override fun onVpnStarted() {
            calls += "started"
        }

        override fun onVpnStopRequested() {
            calls += "stop-requested"
        }

        override fun onVpnStopped() {
            calls += "stopped"
        }

        override fun onVpnRevoked() {
            calls += "revoked"
        }

        override fun onVpnError(message: String?) {
            calls += "error:$message"
        }
    }

    private fun serviceSource(): String {
        return repositoryRoot()
            .resolve("vpn/service/src/main/kotlin/com/vordain/guard/vpn/service/VordainVpnService.kt")
            .readText()
    }

    private fun assertDoesNotContain(source: String, forbiddenText: String) {
        assertTrue(
            actual = !source.contains(forbiddenText),
            message = "Forbidden text $forbiddenText found in source",
        )
    }

    private fun repositoryRoot(): File {
        val userDir = System.getProperty("user.dir") ?: "."
        var current = File(userDir).absoluteFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) {
                return current
            }
            val parent = current.parentFile
                ?: error("Could not find repository root from $userDir")
            current = parent
        }
    }
}
