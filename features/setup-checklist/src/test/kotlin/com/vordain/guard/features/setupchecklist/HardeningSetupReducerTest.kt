package com.vordain.guard.features.setupchecklist

import com.vordain.guard.core.model.DeviceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HardeningSetupReducerTest {
    private val reducer = HardeningSetupReducer()
    private val childDeviceId = DeviceId("child-debug-device")

    @Test
    fun emptySnapshotSummarizesNotStarted() {
        val snapshot = reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L)

        assertEquals(HardeningSummaryStatus.NOT_STARTED, snapshot.summaryStatus)
    }

    @Test
    fun vpnPermissionAloneIsNotReady() {
        val snapshot = reducer.updateStep(
            snapshot = reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L),
            step = HardeningSetupStep.VPN_PERMISSION,
            status = HardeningSetupStatus.USER_CONFIRMED,
            evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
            note = "VPN permission granted",
            currentTimeMillis = 2_000L,
        )

        assertEquals(HardeningSummaryStatus.NEEDS_ATTENTION, snapshot.summaryStatus)
    }

    @Test
    fun criticalVpnStepsConfirmedAreReadyForLabTest() {
        val snapshot = listOf(
            HardeningSetupStep.VPN_PERMISSION,
            HardeningSetupStep.VPN_ALWAYS_ON,
            HardeningSetupStep.BLOCK_WITHOUT_VPN,
        ).fold(reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L)) { current, step ->
            reducer.updateStep(
                snapshot = current,
                step = step,
                status = HardeningSetupStatus.USER_CONFIRMED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = null,
                currentTimeMillis = 2_000L,
            )
        }

        assertEquals(HardeningSummaryStatus.READY_FOR_LAB_TEST, snapshot.summaryStatus)
    }

    @Test
    fun missingBlockWithoutVpnNeedsAttention() {
        val snapshot = listOf(
            HardeningSetupStep.VPN_PERMISSION,
            HardeningSetupStep.VPN_ALWAYS_ON,
        ).fold(reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L)) { current, step ->
            reducer.updateStep(
                snapshot = current,
                step = step,
                status = HardeningSetupStatus.USER_CONFIRMED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = null,
                currentTimeMillis = 2_000L,
            )
        }

        assertEquals(HardeningSummaryStatus.NEEDS_ATTENTION, snapshot.summaryStatus)
    }

    @Test
    fun settingsAppLockCanBeParentConfirmed() {
        val snapshot = reducer.updateStep(
            snapshot = reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L),
            step = HardeningSetupStep.SETTINGS_APP_LOCK,
            status = HardeningSetupStatus.USER_CONFIRMED,
            evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
            note = "Parent confirmed OEM App Lock",
            currentTimeMillis = 2_000L,
        )

        val item = snapshot.itemFor(HardeningSetupStep.SETTINGS_APP_LOCK)
        assertEquals(HardeningSetupStatus.USER_CONFIRMED, item.status)
        assertEquals(HardeningEvidenceType.PARENT_CONFIRMATION, item.evidenceType)
    }

    @Test
    fun unknownOemLockRemainsUnknown() {
        val snapshot = reducer.updateStep(
            snapshot = reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L),
            step = HardeningSetupStep.SETTINGS_APP_LOCK,
            status = HardeningSetupStatus.UNKNOWN,
            evidenceType = HardeningEvidenceType.UNKNOWN,
            note = "OEM support unknown",
            currentTimeMillis = 2_000L,
        )

        assertEquals(HardeningSetupStatus.UNKNOWN, snapshot.itemFor(HardeningSetupStep.SETTINGS_APP_LOCK).status)
    }

    @Test
    fun codecRoundTripsSetupReport() {
        val snapshot = reducer.updateStep(
            snapshot = reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L),
            step = HardeningSetupStep.VPN_PERMISSION,
            status = HardeningSetupStatus.USER_CONFIRMED,
            evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
            note = "Confirmed",
            currentTimeMillis = 2_000L,
        )
        val codec = DebugHardeningSetupReportCodec()

        val decoded = codec.decode(codec.encode(snapshot))

        check(decoded is DebugHardeningSetupReportCodecResult.Decoded)
        assertEquals(childDeviceId, decoded.snapshot.childDeviceId)
        assertEquals(HardeningSetupStatus.USER_CONFIRMED, decoded.snapshot.itemFor(HardeningSetupStep.VPN_PERMISSION).status)
    }

    @Test
    fun codecRejectsWrongHeader() {
        val result = DebugHardeningSetupReportCodec().decode("WRONG\nchildDeviceId=child-debug-device")

        check(result is DebugHardeningSetupReportCodecResult.Rejected)
    }

    @Test
    fun codecRejectsMissingChildDeviceId() {
        val result = DebugHardeningSetupReportCodec().decode(
            "${DebugHardeningSetupReportCodec.HEADER}\ngeneratedAtMillis=1",
        )

        check(result is DebugHardeningSetupReportCodecResult.Rejected)
    }

    @Test
    fun codecRejectsUnknownEnumValues() {
        val result = DebugHardeningSetupReportCodec().decode(
            """
            ${DebugHardeningSetupReportCodec.HEADER}
            childDeviceId=child-debug-device
            generatedAtMillis=1
            VPN_PERMISSION=YES|PARENT_CONFIRMATION|
            """.trimIndent(),
        )

        check(result is DebugHardeningSetupReportCodecResult.Rejected)
    }

    @Test
    fun codecSourceTermsStayBoundarySafe() {
        val encoded = DebugHardeningSetupReportCodec().encode(
            reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L),
        )

        forbiddenTerms.forEach { term ->
            assertFalse(encoded.contains(term, ignoreCase = true), "$term should not appear in setup report")
        }
    }

    private companion object {
        val forbiddenTerms = listOf(
            "browsing history",
            "traffic log",
            "screenshot",
            "message content",
            "child activity",
        )
    }
}
