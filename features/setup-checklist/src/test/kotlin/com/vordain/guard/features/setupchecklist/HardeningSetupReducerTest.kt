package com.vordain.guard.features.setupchecklist

import com.vordain.guard.core.model.DeviceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
            HardeningSetupStep.VPN_PERMISSION to HardeningSetupStatus.USER_CONFIRMED,
            HardeningSetupStep.VPN_ALWAYS_ON to HardeningSetupStatus.USER_CONFIRMED,
            HardeningSetupStep.BLOCK_WITHOUT_VPN to HardeningSetupStatus.USER_CONFIRMED,
            HardeningSetupStep.DEVELOPER_OPTIONS_DISABLED to HardeningSetupStatus.CONFIRMED_DISABLED,
            HardeningSetupStep.USB_DEBUGGING_DISABLED to HardeningSetupStatus.CONFIRMED_DISABLED,
            HardeningSetupStep.WIRELESS_DEBUGGING_DISABLED to HardeningSetupStatus.CONFIRMED_DISABLED,
            HardeningSetupStep.NO_UNRESTRICTED_SECONDARY_USERS to HardeningSetupStatus.CONFIRMED_ABSENT,
            HardeningSetupStep.NO_UNRESTRICTED_WORK_PROFILE to HardeningSetupStatus.CONFIRMED_ABSENT,
            HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY to HardeningSetupStatus.PARENT_CONFIRMED,
            HardeningSetupStep.PARENT_PIN_NOT_SHARED to HardeningSetupStatus.PARENT_CONFIRMED,
            HardeningSetupStep.UNKNOWN_SOURCES_REVIEWED to HardeningSetupStatus.PARENT_CONFIRMED,
        ).fold(reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L)) { current, pair ->
            reducer.updateStep(
                snapshot = current,
                step = pair.first,
                status = pair.second,
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

    @Test
    fun bypassRiskConfirmationsAreRepresented() {
        val steps = listOf(
            HardeningSetupStep.DEVELOPER_OPTIONS_DISABLED to HardeningSetupStatus.CONFIRMED_DISABLED,
            HardeningSetupStep.USB_DEBUGGING_DISABLED to HardeningSetupStatus.CONFIRMED_DISABLED,
            HardeningSetupStep.WIRELESS_DEBUGGING_DISABLED to HardeningSetupStatus.CONFIRMED_DISABLED,
            HardeningSetupStep.NO_UNRESTRICTED_SECONDARY_USERS to HardeningSetupStatus.CONFIRMED_ABSENT,
            HardeningSetupStep.NO_UNRESTRICTED_WORK_PROFILE to HardeningSetupStatus.CONFIRMED_ABSENT,
            HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY to HardeningSetupStatus.PARENT_CONFIRMED,
            HardeningSetupStep.PARENT_PIN_NOT_SHARED to HardeningSetupStatus.PARENT_CONFIRMED,
        )

        val snapshot = steps.fold(reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L)) { current, pair ->
            reducer.updateStep(
                snapshot = current,
                step = pair.first,
                status = pair.second,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent confirmed",
                currentTimeMillis = 2_000L,
            )
        }

        steps.forEach { (step, status) ->
            assertEquals(status, snapshot.itemFor(step).status)
        }
    }

    @Test
    fun criticalChangeInsideMaintenanceWindowDoesNotCreateCompromiseSignal() {
        val window = ParentMaintenanceWindow(
            windowId = "window-1",
            openedAtMillis = 1_000L,
            expiresAtMillis = 5_000L,
            reason = MaintenanceWindowReason.APP_LOCK_REVIEW,
            openedByParentDeviceId = DeviceId("parent-debug-device"),
        )
        val snapshot = reducer.updateStep(
            snapshot = reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L)
                .copy(activeMaintenanceWindow = window),
            step = HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY,
            status = HardeningSetupStatus.NEEDS_ATTENTION,
            evidenceType = HardeningEvidenceType.MANUAL_SETTINGS_REVIEW,
            note = "Parent is reviewing lock settings",
            currentTimeMillis = 2_000L,
        )

        assertFalse(snapshot.latestPinCompromiseSignal.suspected)
    }

    @Test
    fun criticalChangeOutsideMaintenanceWindowCreatesCompromiseSignal() {
        val snapshot = reducer.updateStep(
            snapshot = reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L),
            step = HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY,
            status = HardeningSetupStatus.NEEDS_ATTENTION,
            evidenceType = HardeningEvidenceType.STATE_CHANGE_OUTSIDE_AUTHORIZED_WINDOW,
            note = "Lock setting changed",
            currentTimeMillis = 2_000L,
        )

        assertTrue(snapshot.latestPinCompromiseSignal.suspected)
        assertEquals(HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY, snapshot.latestPinCompromiseSignal.changedStep)
    }

    @Test
    fun vpnLifecycleChangeOutsideMaintenanceWindowCreatesCompromiseSignal() {
        val signal = PinCompromiseSentinel().evaluate(
            change = HardeningStateChange(
                step = HardeningSetupStep.VPN_LIFECYCLE_HEALTH,
                previousStatus = HardeningSetupStatus.USER_CONFIRMED,
                newStatus = HardeningSetupStatus.NEEDS_ATTENTION,
                changedAtMillis = 2_000L,
                evidenceType = HardeningEvidenceType.STATE_CHANGE_OUTSIDE_AUTHORIZED_WINDOW,
                note = "VPN lifecycle changed",
            ),
            activeMaintenanceWindow = null,
            currentTimeMillis = 2_000L,
        )

        assertTrue(signal.suspected)
    }

    @Test
    fun nonCriticalNoteChangeDoesNotCreateCompromiseSignal() {
        val signal = PinCompromiseSentinel().evaluate(
            change = HardeningStateChange(
                step = HardeningSetupStep.PRIVATE_DNS_REVIEW,
                previousStatus = HardeningSetupStatus.UNKNOWN,
                newStatus = HardeningSetupStatus.NEEDS_ATTENTION,
                changedAtMillis = 2_000L,
                evidenceType = HardeningEvidenceType.MANUAL_SETTINGS_REVIEW,
                note = "Needs parent review",
            ),
            activeMaintenanceWindow = null,
            currentTimeMillis = 2_000L,
        )

        assertFalse(signal.suspected)
    }

    @Test
    fun expiredMaintenanceWindowIsNotActive() {
        val window = ParentMaintenanceWindow(
            windowId = "window-1",
            openedAtMillis = 1_000L,
            expiresAtMillis = 2_000L,
            reason = MaintenanceWindowReason.DEVELOPER_OPTIONS_REVIEW,
        )

        assertFalse(window.isActive(currentTimeMillis = 2_000L))
    }

    @Test
    fun codecRoundTripsNewChecklistFieldsAndCompromiseSignal() {
        val base = reducer.initialSnapshot(childDeviceId, currentTimeMillis = 1_000L)
        val snapshot = reducer.updateStep(
            snapshot = base,
            step = HardeningSetupStep.DEVELOPER_OPTIONS_DISABLED,
            status = HardeningSetupStatus.CONFIRMED_DISABLED,
            evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
            note = "Developer Options disabled",
            currentTimeMillis = 2_000L,
        ).copy(
            latestPinCompromiseSignal = PinCompromiseSignal(
                suspected = true,
                reason = "Parent PIN may be compromised",
                changedStep = HardeningSetupStep.USB_DEBUGGING_DISABLED,
                changedAtMillis = 3_000L,
            ),
        )
        val codec = DebugHardeningSetupReportCodec()

        val decoded = codec.decode(codec.encode(snapshot))

        check(decoded is DebugHardeningSetupReportCodecResult.Decoded)
        assertEquals(
            HardeningSetupStatus.CONFIRMED_DISABLED,
            decoded.snapshot.itemFor(HardeningSetupStep.DEVELOPER_OPTIONS_DISABLED).status,
        )
        assertTrue(decoded.snapshot.latestPinCompromiseSignal.suspected)
    }

    @Test
    fun codecRejectsSensitiveFieldNames() {
        val result = DebugHardeningSetupReportCodec().decode(
            """
            ${DebugHardeningSetupReportCodec.HEADER}
            childDeviceId=child-debug-device
            generatedAtMillis=1
            ${"pin" + "Value"}=1234
            """.trimIndent(),
        )

        check(result is DebugHardeningSetupReportCodecResult.Rejected)
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
