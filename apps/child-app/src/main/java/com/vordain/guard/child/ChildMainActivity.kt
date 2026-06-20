package com.vordain.guard.child

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.vordain.guard.core.alertcenter.AlertSeverity
import com.vordain.guard.core.alertcenter.AlertStatus
import com.vordain.guard.core.alertcenter.AlertType
import com.vordain.guard.core.alertcenter.ChildAlert
import com.vordain.guard.core.alertcenter.ChildAlertReducer
import com.vordain.guard.core.alertcenter.ChildAlertTimeline
import com.vordain.guard.core.alertcenter.DebugChildAlertReport
import com.vordain.guard.core.alertcenter.DebugChildAlertReportCodec
import com.vordain.guard.core.auditlog.AuditEntry
import com.vordain.guard.core.auditlog.AuditEntryType
import com.vordain.guard.core.auditlog.AuditSeverity
import com.vordain.guard.core.auditlog.AuditTimeline
import com.vordain.guard.core.auditlog.AuditTimelineReducer
import com.vordain.guard.core.auditlog.VordainDebugPayloadEnvelope
import com.vordain.guard.core.auditlog.VordainDebugPayloadEnvelopeCodec
import com.vordain.guard.core.auditlog.VordainDebugPayloadKind
import com.vordain.guard.core.devrelay.DevRelayDirection
import com.vordain.guard.core.devrelay.DevRelayMessage
import com.vordain.guard.core.devrelay.DevRelayMessageStatus
import com.vordain.guard.core.model.AppTrafficMode
import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.pairing.DebugPairingAcceptanceCodec
import com.vordain.guard.core.pairing.DebugPairingInviteCodec
import com.vordain.guard.core.pairing.DebugPairingInviteCodecResult
import com.vordain.guard.core.pairing.PairingAcceptance
import com.vordain.guard.core.pairing.PairingCapability
import com.vordain.guard.core.pairing.PairingDeviceProfile
import com.vordain.guard.core.pairing.PairingEvaluator
import com.vordain.guard.core.pairing.PairingPublicKeyFingerprint
import com.vordain.guard.core.pairing.PairingRole
import com.vordain.guard.core.pairing.PairingStatus
import com.vordain.guard.core.pairing.PairingVerificationCode
import com.vordain.guard.core.policysync.PersistedSignedPolicySnapshot
import com.vordain.guard.core.policysync.DebugPolicyUpdateCodec
import com.vordain.guard.core.policysync.DebugPolicyUpdateCodecResult
import com.vordain.guard.core.policysync.PolicyVersion
import com.vordain.guard.core.policysync.SignedPolicySnapshotRestorer
import com.vordain.guard.core.statusreport.ChildSecurityActiveMode
import com.vordain.guard.core.statusreport.ChildSecurityOverallStatus
import com.vordain.guard.core.statusreport.ChildSecuritySignal
import com.vordain.guard.core.statusreport.ChildSecurityStatusEvaluator
import com.vordain.guard.core.statusreport.ChildSecurityStatusInput
import com.vordain.guard.core.statusreport.ChildSecurityStatusReport
import com.vordain.guard.core.statusreport.BasicDnsGuardDiagnosticsReport
import com.vordain.guard.core.statusreport.DebugBasicDnsGuardDiagnosticsCodec
import com.vordain.guard.core.statusreport.DebugChildSecurityReportCodec
import com.vordain.guard.core.syncbundle.SyncBundle
import com.vordain.guard.core.syncbundle.SyncBundleCodec
import com.vordain.guard.core.syncbundle.SyncBundleDirection
import com.vordain.guard.core.syncbundle.SyncBundleImportEvaluator
import com.vordain.guard.core.syncbundle.SyncBundleImportResult
import com.vordain.guard.core.syncbundle.SyncBundleImportStatus
import com.vordain.guard.core.syncbundle.SyncBundleKind
import com.vordain.guard.core.syncbundle.MvpAcceptanceChecklist
import com.vordain.guard.core.syncbundle.MvpAcceptanceItem
import com.vordain.guard.core.syncbundle.MvpAcceptanceStatus
import com.vordain.guard.core.syncbundle.MvpAcceptanceStep
import com.vordain.guard.core.syncbundle.SyncBundlePayload
import com.vordain.guard.core.syncbundle.SyncBundlePayloadKind
import com.vordain.guard.data.review.ReviewRequestReason
import com.vordain.guard.features.bypassrisk.BypassRiskCategory
import com.vordain.guard.features.bypassrisk.BypassRiskEvaluator
import com.vordain.guard.features.bypassrisk.BypassRiskItem
import com.vordain.guard.features.bypassrisk.BypassRiskOverallStatus
import com.vordain.guard.features.bypassrisk.BypassRiskSeverity
import com.vordain.guard.features.bypassrisk.BypassRiskStatus
import com.vordain.guard.features.bypassrisk.DebugBypassRiskReport
import com.vordain.guard.features.bypassrisk.DebugBypassRiskReportCodec
import com.vordain.guard.features.bypassrisk.DebugBypassRiskReportCodecResult
import com.vordain.guard.features.bypassrisk.DnsOnlyReadinessEvaluator
import com.vordain.guard.features.bypassrisk.DnsOnlyReadinessInput
import com.vordain.guard.features.setupchecklist.DebugHardeningSetupReportCodec
import com.vordain.guard.features.setupchecklist.DebugHardeningSetupReportCodecResult
import com.vordain.guard.features.setupchecklist.HardeningEvidenceType
import com.vordain.guard.features.setupchecklist.HardeningSetupReducer
import com.vordain.guard.features.setupchecklist.HardeningSetupSnapshot
import com.vordain.guard.features.setupchecklist.HardeningSetupStatus
import com.vordain.guard.features.setupchecklist.HardeningSetupStep
import com.vordain.guard.features.setupchecklist.HardeningSummaryStatus
import com.vordain.guard.features.setupchecklist.MaintenanceWindowReason
import com.vordain.guard.features.setupchecklist.ParentMaintenanceWindow
import com.vordain.guard.vpn.lab.LabTrafficObservationStats
import com.vordain.guard.vpn.service.BasicDnsGuardHeartbeatDebugStatus
import com.vordain.guard.vpn.service.LabCaptureDebugStatus
import com.vordain.guard.vpn.service.VordainVpnServiceIntents
import com.vordain.guard.vpn.service.VpnRuntimeDebugStatus
import com.vordain.guard.vpn.service.VpnPermissionIntentFactory
import com.vordain.guard.vpn.service.VpnPrepareResult
import com.vordain.guard.vpn.session.BasicDnsGuardHeartbeatStatus
import com.vordain.guard.vpn.session.VordainOperatingMode
import com.vordain.guard.vpn.session.VpnRuntimeSessionState

class ChildMainActivity : Activity() {
    private val vpnPermissionIntentFactory = VpnPermissionIntentFactory()
    private val policySnapshotRestorer = SignedPolicySnapshotRestorer()
    private val pairingInviteCodec = DebugPairingInviteCodec()
    private val pairingAcceptanceCodec = DebugPairingAcceptanceCodec()
    private val pairingEvaluator = PairingEvaluator()
    private val debugPolicyUpdateCodec = DebugPolicyUpdateCodec()
    private val policyDemo = ChildDebugPolicyDemo()
    private val compatibilityDemo = ChildDebugCompatibilityDemo { System.currentTimeMillis() }
    private val reviewDemo = ChildDebugReviewDemo()
    private val policyHandoff = ChildDebugPolicyHandoff { System.currentTimeMillis() }
    private val diagnosticsFormatter = ChildDebugDiagnosticsFormatter()
    private val auditTimelineReducer = AuditTimelineReducer()
    private val childAlertReducer = ChildAlertReducer()
    private val childAlertReportCodec = DebugChildAlertReportCodec()
    private val debugPayloadEnvelopeCodec = VordainDebugPayloadEnvelopeCodec()
    private val hardeningSetupReducer = HardeningSetupReducer()
    private val hardeningSetupReportCodec = DebugHardeningSetupReportCodec()
    private val childSecurityStatusEvaluator = ChildSecurityStatusEvaluator()
    private val childSecurityReportCodec = DebugChildSecurityReportCodec()
    private val basicDnsGuardDiagnosticsCodec = DebugBasicDnsGuardDiagnosticsCodec()
    private val syncBundleCodec = SyncBundleCodec()
    private val syncBundleImportEvaluator = SyncBundleImportEvaluator()
    private val mvpAcceptanceChecklist = MvpAcceptanceChecklist()
    private val bypassRiskEvaluator = BypassRiskEvaluator()
    private val bypassRiskReportCodec = DebugBypassRiskReportCodec()
    private val dnsOnlyReadinessEvaluator = DnsOnlyReadinessEvaluator()
    private val localDevRelayClient = ChildLocalDevRelayClient()
    private lateinit var remoteTestController: ChildRemoteTestController
    private lateinit var stateStore: ChildDebugStateStore
    private lateinit var auditStore: ChildAuditStateStore
    private lateinit var alertStore: ChildAlertStateStore
    private lateinit var bundleInboxStore: ChildBundleInboxStore
    private lateinit var statusText: TextView
    private lateinit var vpnPermissionText: TextView
    private lateinit var lastCommandText: TextView
    private lateinit var shellStatusText: TextView
    private lateinit var setupChecklistText: TextView
    private lateinit var hardeningSetupText: TextView
    private lateinit var childSecurityStatusText: TextView
    private lateinit var basicDnsGuardText: TextView
    private lateinit var bypassRiskText: TextView
    private lateinit var labCaptureText: TextView
    private lateinit var policyDomainInput: EditText
    private lateinit var policyOutputText: TextView
    private lateinit var policyHandoffTargetInput: EditText
    private lateinit var policyHandoffPayloadInput: EditText
    private lateinit var policyHandoffOutputText: TextView
    private lateinit var childDisplayNameInput: EditText
    private lateinit var childFingerprintInput: EditText
    private lateinit var pairingInviteInput: EditText
    private lateinit var pairingOutputText: TextView
    private lateinit var compatibilityPackageInput: EditText
    private lateinit var compatibilityDomainInput: EditText
    private lateinit var compatibilityOutputText: TextView
    private lateinit var reviewSubjectInput: EditText
    private lateinit var reviewOutputText: TextView
    private lateinit var localEventsText: TextView
    private lateinit var diagnosticsText: TextView
    private lateinit var auditTimelineText: TextView
    private lateinit var localAlertsText: TextView
    private lateinit var childSyncBundleOutputText: TextView
    private lateinit var parentSyncBundleInput: EditText
    private lateinit var parentSyncBundleOutputText: TextView
    private lateinit var relayBaseUrlInput: EditText
    private lateinit var parentRelayDeviceInput: EditText
    private lateinit var relayOutputText: TextView
    private lateinit var remoteTestOutputText: TextView
    private lateinit var bundleInboxText: TextView
    private var vpnPermissionStatus: String = ChildVpnSmokeLabels.PERMISSION_UNKNOWN
    private var lastCommand: String = ChildVpnSmokeLabels.COMMAND_NONE
    private var shellStatus: String = ChildVpnSmokeLabels.STATUS_NOT_RUNNING
    private var childDeviceId: String = ChildDebugStateSnapshot.DEFAULT_CHILD_DEVICE_ID
    private var latestPolicyPayload: String? = null
    private var latestPolicyAppliedAtMillis: Long = 0L
    private var latestAllowDomainsCsv: String? = null
    private var latestBlockDomainsCsv: String? = null
    private var currentPolicySource: String = "Default sample policy"
    private var currentPolicyPresetName: String? = null
    private var currentPolicyDisplayLabel: String? = null
    private var currentPolicyBlockEncryptedDnsResolvers: Boolean = true
    private var lastPolicyVerificationResult: String = "No debug policy verified yet"
    private var childDisplayName: String = ChildDebugStateSnapshot.DEFAULT_CHILD_DISPLAY_NAME
    private var childFingerprint: String = ChildDebugStateSnapshot.DEFAULT_CHILD_FINGERPRINT
    private var latestPairingInvitePayload: String? = null
    private var latestPairingAcceptancePayload: String? = null
    private var acceptedParentSummary: String? = null
    private var setupForegroundNotificationStatus: SetupCheckState = SetupCheckState.UNKNOWN
    private var setupAlwaysOnVpnStatus: SetupCheckState = SetupCheckState.UNKNOWN
    private var setupBlockWithoutVpnStatus: SetupCheckState = SetupCheckState.UNKNOWN
    private var setupBatteryOptimizationStatus: SetupCheckState = SetupCheckState.UNKNOWN
    private var setupSettingsAppLockStatus: SetupCheckState = SetupCheckState.UNKNOWN
    private var setupScreenPinningStatus: SetupCheckState = SetupCheckState.UNKNOWN
    private var setupPrivateDnsStatus: SetupCheckState = SetupCheckState.UNKNOWN
    private var setupUnknownSourcesStatus: SetupCheckState = SetupCheckState.UNKNOWN
    private var hardeningSetupSnapshot: HardeningSetupSnapshot? = null
    private var latestHardeningSetupReportPayload: String? = null
    private var latestChildSecurityStatusReportPayload: String? = null
    private var latestChildSecurityStatusReport: ChildSecurityStatusReport? = null
    private var latestBypassRiskReportPayload: String? = null
    private var latestChildSyncBundlePayload: String? = null
    private var latestParentSyncBundlePayload: String? = null
    private var relayBaseUrl: String = ChildDebugStateSnapshot.DEFAULT_RELAY_BASE_URL
    private var parentRelayDeviceId: String = ChildDebugStateSnapshot.DEFAULT_PARENT_RELAY_DEVICE_ID
    private var latestRelayMessageId: String = ""
    private var latestRelayDiagnostics: String = "No local dev relay action yet."
    private var expectedBasicDnsGuardRunning: Boolean = false
    private var latestVpnRuntimeStatus: String = ChildDebugStateSnapshot.DEFAULT_RUNTIME_STATUS
    private var latestHeartbeatStatus: String = ChildDebugStateSnapshot.DEFAULT_HEARTBEAT_STATUS
    private var bypassRiskItems: List<BypassRiskItem> = emptyList()
    private var lastDiagnosticsText: String? = null
    private var policyResult: ChildDebugPolicyResult? = null
    private var policyHandoffResult: ChildDebugPolicyHandoffResult? = null
    private var currentPolicyVersion: String = "debug-tablet-policy"
    private var compatibilityResult: ChildDebugCompatibilityResult? = null
    private var reviewResult: ChildDebugReviewResult? = null
    private val localDebugEvents = mutableListOf<String>()
    private var auditTimeline: AuditTimeline = AuditTimeline()
    private var childAlertTimeline: ChildAlertTimeline = ChildAlertTimeline()
    private var bundleInbox: List<ChildBundleInboxEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateStore = ChildDebugStateStore(this)
        auditStore = ChildAuditStateStore(this)
        alertStore = ChildAlertStateStore(this)
        bundleInboxStore = ChildBundleInboxStore(this)
        remoteTestController = ChildRemoteTestController(this, localDevRelayClient)
        auditTimeline = auditStore.load()
        childAlertTimeline = alertStore.load()
        bundleInbox = bundleInboxStore.load()
        restoreState(stateStore.load())
        setContentView(createSmokeTestView())
        statusText.text = shellStatus
        refreshDiagnosticsViews()
        handleSharedTextIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedTextIntent(intent)
    }

    override fun onPause() {
        saveCurrentState()
        super.onPause()
    }

    override fun onDestroy() {
        remoteTestController.stop()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_VPN_PERMISSION) {
            return
        }

        if (resultCode == RESULT_OK) {
            setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_GRANTED)
            updateHardeningStep(
                step = HardeningSetupStep.VPN_PERMISSION,
                status = HardeningSetupStatus.AUTO_CONFIRMED,
                evidenceType = HardeningEvidenceType.AUTOMATIC_CHECK,
                note = "VPN permission prepared by Android.",
            )
            setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_GRANTED)
        } else {
            setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_REQUIRED)
            updateHardeningStep(
                step = HardeningSetupStep.VPN_PERMISSION,
                status = HardeningSetupStatus.NEEDS_ATTENTION,
                evidenceType = HardeningEvidenceType.AUTOMATIC_CHECK,
                note = "VPN permission still needs parent action.",
            )
            setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_REQUIRED)
        }
    }

    private fun createSmokeTestView(): ScrollView {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }

        layout.addView(centerLabel(ChildVpnSmokeLabels.TITLE, textSize = 28f))
        layout.addView(centerLabel("Debug tablet build", textSize = 16f))
        layout.addView(centerLabel(ChildVpnSmokeLabels.WARNING, textSize = 18f))
        layout.addView(sectionTitle("Local MVP flow"))
        layout.addView(valueLabel(createMvpAcceptanceSummary(), textSize = 14f))

        layout.addView(sectionTitle(ChildVpnSmokeLabels.BASIC_DNS_TITLE))
        layout.addView(valueLabel(ChildVpnSmokeLabels.BASIC_DNS_NOT_FULL, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.BASIC_DNS_NON_DNS, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.BASIC_DNS_PRODUCTION, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.DNS_ONLY_HARDENING, textSize = 14f))
        layout.addView(button("Start Basic DNS Guard") {
            startBasicDnsGuardWhenAllowed()
        })
        layout.addView(button("Stop Basic DNS Guard") {
            stopBasicDnsGuard()
        })
        layout.addView(button("Refresh Basic DNS status") {
            refreshDiagnosticsViews()
        })
        layout.addView(button("Copy Basic DNS diagnostics") {
            copyBasicDnsGuardDiagnostics()
        })
        layout.addView(button("Continue setup / review hardening") {
            hardeningSetupText.text = createHardeningSetupDisplay(currentHardeningSetupSnapshot())
            bypassRiskText.text = createBypassRiskDisplay()
        })
        basicDnsGuardText = valueLabel(createBasicDnsGuardDisplay(), textSize = 14f)
        layout.addView(basicDnsGuardText)

        layout.addView(sectionTitle("VPN shell controls"))
        statusText = valueLabel(ChildVpnSmokeLabels.STATUS_NOT_RUNNING, textSize = 18f)
        layout.addView(statusText)

        vpnPermissionText = valueLabel("", textSize = 16f)
        lastCommandText = valueLabel("", textSize = 16f)
        shellStatusText = valueLabel("", textSize = 16f)
        layout.addView(vpnPermissionText)
        layout.addView(lastCommandText)
        layout.addView(shellStatusText)

        layout.addView(button(ChildVpnSmokeLabels.REQUEST_PERMISSION_BUTTON) {
            requestVpnPermission()
        })
        layout.addView(button(ChildVpnSmokeLabels.START_BUTTON) {
            startVpnShellWhenAllowed()
        })
        layout.addView(button(ChildVpnSmokeLabels.STOP_BUTTON) {
            stopVpnShell()
        })

        layout.addView(sectionTitle("Lab full-tunnel capture"))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_WARNING, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_LOCAL_ONLY, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_DNS_LOCAL_ONLY, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_DNS_SINKHOLE, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_ALLOWED_DROPPED, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_ALLOWED_NON_DNS_DROPPED, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_INTERNET_MAY_NOT_WORK, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_NOT_FULL_PROTECTION, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_AUTO_STOP, textSize = 14f))
        layout.addView(button("Start lab capture / DNS enforcement") {
            startLabCaptureWhenAllowed()
        })
        layout.addView(button("Stop lab capture") {
            stopLabCapture()
        })
        layout.addView(button("Refresh lab stats") {
            refreshDiagnosticsViews()
        })
        labCaptureText = valueLabel(createLabCaptureDisplay(LabCaptureDebugStatus.snapshot()), textSize = 14f)
        layout.addView(labCaptureText)

        layout.addView(sectionTitle(ChildVpnSmokeLabels.DNS_ONLY_TITLE))
        layout.addView(valueLabel(ChildVpnSmokeLabels.DNS_ONLY_DESCRIPTION, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_DNS_SINKHOLE, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_ALLOWED_DROPPED, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.DNS_ONLY_NON_DNS, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.DNS_ONLY_DOH_WARNING, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.WARNING, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_NOT_FULL_PROTECTION, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.DNS_ONLY_HARDENING, textSize = 14f))
        layout.addView(button("Start DNS-only lab") {
            startDnsOnlyLabWhenAllowed()
        })
        layout.addView(button("Stop DNS-only lab") {
            stopDnsOnlyLab()
        })
        layout.addView(button("Refresh DNS-only stats") {
            refreshDiagnosticsViews()
        })
        layout.addView(button("Copy DNS-only diagnostics") {
            copyDiagnostics()
        })
        layout.addView(button("Share DNS-only diagnostics") {
            shareDiagnostics()
        })

        layout.addView(sectionTitle("DNS bypass hardening"))
        layout.addView(valueLabel("Turn off Android Private DNS or set it to a parent-approved provider.", textSize = 14f))
        layout.addView(valueLabel("Remove or block alternate VPN apps.", textSize = 14f))
        layout.addView(valueLabel("Remove or block proxy apps and private browsers.", textSize = 14f))
        layout.addView(valueLabel("Confirm Developer Options and USB debugging are off.", textSize = 14f))
        layout.addView(valueLabel("Confirm no unrestricted secondary users/profiles.", textSize = 14f))
        layout.addView(valueLabel("DNS-only mode does not inspect non-DNS traffic.", textSize = 14f))
        layout.addView(valueLabel("Vordain does not read HTTPS content.", textSize = 14f))
        layout.addView(valueLabel("DNS-only mode cannot block every direct-IP or app-level encrypted DNS path without additional hardening.", textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_NOT_FULL_PROTECTION, textSize = 14f))
        layout.addView(button("Open Private DNS / Network settings") {
            openSettings(Settings.ACTION_WIRELESS_SETTINGS)
        })
        layout.addView(button("Mark Private DNS reviewed") {
            markBypassRisk(
                category = BypassRiskCategory.PRIVATE_DNS,
                status = BypassRiskStatus.CONFIRMED_SAFE,
                severity = BypassRiskSeverity.HIGH,
                note = "Parent reviewed Android Private DNS settings.",
            )
        })
        layout.addView(button("Mark no alternate VPN apps") {
            markBypassRisk(
                category = BypassRiskCategory.ALTERNATE_VPN_APP,
                status = BypassRiskStatus.CONFIRMED_SAFE,
                severity = BypassRiskSeverity.HIGH,
                note = "Parent confirmed no alternate VPN apps are available.",
            )
        })
        layout.addView(button("Mark no proxy/private browser apps") {
            markBypassRisk(
                category = BypassRiskCategory.PROXY_APP,
                status = BypassRiskStatus.CONFIRMED_SAFE,
                severity = BypassRiskSeverity.HIGH,
                note = "Parent confirmed proxy apps are removed or blocked.",
            )
            markBypassRisk(
                category = BypassRiskCategory.PRIVATE_BROWSER,
                status = BypassRiskStatus.CONFIRMED_SAFE,
                severity = BypassRiskSeverity.HIGH,
                note = "Parent confirmed private browsers are removed or blocked.",
            )
        })
        layout.addView(button("Mark DoH risk reviewed") {
            markBypassRisk(
                category = BypassRiskCategory.DNS_OVER_HTTPS,
                status = BypassRiskStatus.CONFIRMED_SAFE,
                severity = BypassRiskSeverity.HIGH,
                note = "Parent reviewed DoH resolver bypass risk.",
            )
        })
        layout.addView(button("Mark direct-IP limitation acknowledged") {
            markBypassRisk(
                category = BypassRiskCategory.DIRECT_IP_ACCESS,
                status = BypassRiskStatus.CONFIRMED_SAFE,
                severity = BypassRiskSeverity.MEDIUM,
                note = "Parent acknowledged DNS-only mode does not cover direct-IP paths.",
            )
        })
        layout.addView(button("Generate bypass-risk report") {
            generateBypassRiskReport()
        })
        layout.addView(button("Copy bypass-risk report") {
            copyBypassRiskReport()
        })
        layout.addView(button("Share bypass-risk report") {
            shareBypassRiskReport()
        })
        bypassRiskText = valueLabel(createBypassRiskDisplay(), textSize = 14f)
        layout.addView(bypassRiskText)

        layout.addView(sectionTitle("Setup checklist"))
        layout.addView(valueLabel("These steps help prevent silent bypass. Some settings must be turned on manually by the parent.", textSize = 14f))
        setupChecklistText = valueLabel("", textSize = 15f)
        layout.addView(setupChecklistText)
        hardeningSetupText = valueLabel("", textSize = 15f)
        layout.addView(hardeningSetupText)
        layout.addView(button("Request VPN permission") {
            requestVpnPermission()
        })
        layout.addView(button("Open VPN settings") {
            openSettings(Settings.ACTION_VPN_SETTINGS)
            updateHardeningStep(
                step = HardeningSetupStep.VPN_ALWAYS_ON,
                status = HardeningSetupStatus.OPENED_SETTINGS,
                evidenceType = HardeningEvidenceType.MANUAL_DEVICE_SETTING,
                note = "Open VPN settings, tap Vordain Guard, enable Always-on VPN.",
            )
        })
        layout.addView(button("Mark Always-on VPN enabled") {
            updateHardeningStep(
                step = HardeningSetupStep.VPN_ALWAYS_ON,
                status = HardeningSetupStatus.USER_CONFIRMED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent confirmed Always-on VPN is enabled.",
            )
        })
        layout.addView(button("Mark Block without VPN enabled") {
            updateHardeningStep(
                step = HardeningSetupStep.BLOCK_WITHOUT_VPN,
                status = HardeningSetupStatus.USER_CONFIRMED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "In the Vordain VPN settings, Block connections without VPN is enabled.",
            )
        })
        layout.addView(button("Open Security settings") {
            openSettings(Settings.ACTION_SECURITY_SETTINGS)
            updateHardeningStep(
                step = HardeningSetupStep.SETTINGS_APP_LOCK,
                status = HardeningSetupStatus.OPENED_SETTINGS,
                evidenceType = HardeningEvidenceType.MANUAL_DEVICE_SETTING,
                note = "If available, lock Settings and VPN settings behind the parent PIN.",
            )
        })
        layout.addView(button("Mark Settings/App Lock enabled") {
            updateHardeningStep(
                step = HardeningSetupStep.SETTINGS_APP_LOCK,
                status = HardeningSetupStatus.USER_CONFIRMED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent confirmed Settings or VPN settings are locked.",
            )
        })
        layout.addView(button("Mark Screen pinning with PIN enabled") {
            updateHardeningStep(
                step = HardeningSetupStep.SCREEN_PINNING_WITH_PIN,
                status = HardeningSetupStatus.USER_CONFIRMED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent confirmed PIN is required to unpin.",
            )
        })
        layout.addView(button("Open app/battery settings") {
            openSettings(Settings.ACTION_APPLICATION_SETTINGS)
            updateHardeningStep(
                step = HardeningSetupStep.BATTERY_OPTIMIZATION,
                status = HardeningSetupStatus.OPENED_SETTINGS,
                evidenceType = HardeningEvidenceType.MANUAL_DEVICE_SETTING,
                note = "Parent opened app or battery settings for review.",
            )
        })
        layout.addView(button("Mark battery optimization reviewed") {
            updateHardeningStep(
                step = HardeningSetupStep.BATTERY_OPTIMIZATION,
                status = HardeningSetupStatus.USER_CONFIRMED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent reviewed battery optimization behavior.",
            )
        })
        layout.addView(button("Mark Private DNS reviewed") {
            updateHardeningStep(
                step = HardeningSetupStep.PRIVATE_DNS_REVIEW,
                status = HardeningSetupStatus.USER_CONFIRMED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent reviewed Private DNS settings.",
            )
        })
        layout.addView(button("Mark Unknown sources reviewed") {
            updateHardeningStep(
                step = HardeningSetupStep.UNKNOWN_SOURCES_REVIEWED,
                status = HardeningSetupStatus.USER_CONFIRMED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent reviewed unknown app install sources.",
            )
        })
        layout.addView(sectionTitle("Bypass-risk checklist"))
        layout.addView(valueLabel("Developer Options, ADB, wireless debugging, and user/profile checks are manual or best-effort checks on non-managed Android.", textSize = 14f))
        layout.addView(valueLabel("Vordain does not record PINs.", textSize = 14f))
        layout.addView(valueLabel("Compromise warnings are based on hardening changes outside parent-authorized setup windows.", textSize = 14f))
        layout.addView(button("Open Developer Options") {
            openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            updateHardeningStep(
                step = HardeningSetupStep.DEVELOPER_OPTIONS_DISABLED,
                status = HardeningSetupStatus.OPENED_SETTINGS,
                evidenceType = HardeningEvidenceType.MANUAL_SETTINGS_REVIEW,
                note = "Parent opened Developer Options for manual review.",
            )
        })
        layout.addView(button("Mark Developer Options disabled") {
            updateHardeningStep(
                step = HardeningSetupStep.DEVELOPER_OPTIONS_DISABLED,
                status = HardeningSetupStatus.CONFIRMED_DISABLED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent confirmed Developer Options are disabled.",
            )
        })
        layout.addView(button("Mark USB debugging disabled") {
            updateHardeningStep(
                step = HardeningSetupStep.USB_DEBUGGING_DISABLED,
                status = HardeningSetupStatus.CONFIRMED_DISABLED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent confirmed USB debugging is disabled.",
            )
        })
        layout.addView(button("Mark Wireless debugging disabled") {
            updateHardeningStep(
                step = HardeningSetupStep.WIRELESS_DEBUGGING_DISABLED,
                status = HardeningSetupStatus.CONFIRMED_DISABLED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent confirmed Wireless debugging is disabled if present.",
            )
        })
        layout.addView(button("Open user/profile settings") {
            openSettings(ACTION_USER_SETTINGS)
            updateHardeningStep(
                step = HardeningSetupStep.NO_UNRESTRICTED_SECONDARY_USERS,
                status = HardeningSetupStatus.OPENED_SETTINGS,
                evidenceType = HardeningEvidenceType.MANUAL_SETTINGS_REVIEW,
                note = "Parent opened user/profile settings for manual review.",
            )
        })
        layout.addView(button("Mark no unrestricted secondary users") {
            updateHardeningStep(
                step = HardeningSetupStep.NO_UNRESTRICTED_SECONDARY_USERS,
                status = HardeningSetupStatus.CONFIRMED_ABSENT,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent confirmed no unrestricted secondary users.",
            )
        })
        layout.addView(button("Mark no unrestricted work profile") {
            updateHardeningStep(
                step = HardeningSetupStep.NO_UNRESTRICTED_WORK_PROFILE,
                status = HardeningSetupStatus.CONFIRMED_ABSENT,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent confirmed no unrestricted work profile.",
            )
        })
        layout.addView(button("Mark Settings/App Lock uses parent PIN only") {
            updateHardeningStep(
                step = HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY,
                status = HardeningSetupStatus.PARENT_CONFIRMED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent confirmed Settings/App Lock is controlled by parent PIN only.",
            )
        })
        layout.addView(button("Mark parent PIN not shared") {
            updateHardeningStep(
                step = HardeningSetupStep.PARENT_PIN_NOT_SHARED,
                status = HardeningSetupStatus.PARENT_CONFIRMED,
                evidenceType = HardeningEvidenceType.PARENT_CONFIRMATION,
                note = "Parent confirmed the PIN has not been shared.",
            )
        })
        layout.addView(button("Start parent maintenance window") {
            startParentMaintenanceWindow()
        })
        layout.addView(button("End parent maintenance window") {
            endParentMaintenanceWindow()
        })
        layout.addView(button("Simulate hardening change outside parent window") {
            simulateHardeningChangeOutsideParentWindow()
        })
        layout.addView(button("Copy setup report") {
            copyHardeningSetupReport()
        })
        layout.addView(button("Share setup report") {
            shareHardeningSetupReport()
        })
        layout.addView(button("Clear setup confirmations") {
            clearHardeningSetup()
        })

        layout.addView(sectionTitle("Child security status report"))
        layout.addView(valueLabel(ChildSecurityStatusReport.WARNING_TEXT, textSize = 14f))
        layout.addView(button("Generate status report") {
            generateChildSecurityStatusReport()
        })
        layout.addView(button("Copy status report") {
            copyChildSecurityStatusReport()
        })
        layout.addView(button("Share status report") {
            shareChildSecurityStatusReport()
        })
        layout.addView(button("Refresh status report") {
            refreshChildSecurityStatusReport()
        })
        childSecurityStatusText = valueLabel(
            createChildSecurityStatusDisplay(currentChildSecurityStatusReport()),
            textSize = 14f,
        )
        layout.addView(childSecurityStatusText)

        layout.addView(sectionTitle("Local policy/domain tester"))
        policyDomainInput = editText("blocked.example")
        layout.addView(policyDomainInput)
        layout.addView(button("Evaluate domain") {
            evaluatePolicyDomain()
        })
        policyOutputText = valueLabel("No policy demo result yet", textSize = 14f)
        layout.addView(policyOutputText)

        layout.addView(sectionTitle("Debug policy handoff"))
        policyHandoffTargetInput = editText(childDeviceId)
        policyHandoffPayloadInput = multiLineEditText(latestPolicyPayload.orEmpty())
        layout.addView(labeledField("Target child device id", policyHandoffTargetInput))
        layout.addView(labeledField("Paste debug policy update payload", policyHandoffPayloadInput))
        layout.addView(button("Apply debug policy update") {
            applyDebugPolicyUpdate()
        })
        layout.addView(button("Clear applied debug policy") {
            clearPolicyHandoffResult()
        })
        layout.addView(button("Re-verify stored policy") {
            reverifyStoredPolicy()
        })
        layout.addView(button("Copy active policy diagnostics") {
            copyActivePolicyDiagnostics()
        })
        layout.addView(button("Share active policy diagnostics") {
            shareActivePolicyDiagnostics()
        })
        policyHandoffOutputText = valueLabel(
            createActivePolicyDisplay(),
            textSize = 14f,
        )
        layout.addView(policyHandoffOutputText)

        layout.addView(sectionTitle("Debug pairing handoff"))
        layout.addView(valueLabel("Debug pairing only. No server delivery.", textSize = 14f))
        childDisplayNameInput = editText(childDisplayName)
        childFingerprintInput = editText(childFingerprint)
        pairingInviteInput = multiLineEditText(latestPairingInvitePayload.orEmpty())
        layout.addView(labeledField("Child display name", childDisplayNameInput))
        layout.addView(labeledField("Child fingerprint", childFingerprintInput))
        layout.addView(labeledField("Paste parent pairing invite", pairingInviteInput))
        layout.addView(button("Accept pairing invite") {
            acceptPairingInvite()
        })
        layout.addView(button("Copy child acceptance") {
            copyChildAcceptance()
        })
        layout.addView(button("Share child acceptance") {
            shareChildAcceptance()
        })
        layout.addView(button("Clear pairing result") {
            clearPairingResult()
        })
        pairingOutputText = valueLabel(createPairingOutput(), textSize = 14f)
        layout.addView(pairingOutputText)

        layout.addView(sectionTitle("Compatibility Mode tester"))
        compatibilityPackageInput = editText("com.netflix.mediaclient")
        compatibilityDomainInput = editText("video.example")
        layout.addView(compatibilityPackageInput)
        layout.addView(compatibilityDomainInput)
        layout.addView(horizontalButtons(
            "STRICT" to { evaluateCompatibility(AppTrafficMode.STRICT) },
            "COMPAT" to { evaluateCompatibility(AppTrafficMode.COMPATIBILITY) },
            "BLOCKED" to { evaluateCompatibility(AppTrafficMode.BLOCKED) },
            "MONITOR" to { evaluateCompatibility(AppTrafficMode.MONITOR) },
        ))
        compatibilityOutputText = valueLabel("No compatibility demo result yet", textSize = 14f)
        layout.addView(compatibilityOutputText)

        layout.addView(sectionTitle("Parent Review demo"))
        reviewSubjectInput = editText("https://school.example.edu/login?student=123")
        layout.addView(reviewSubjectInput)
        layout.addView(horizontalButtons(
            "School" to { buildReviewRequest(ReviewRequestReason.SCHOOL_ACCESS) },
            "App" to { buildReviewRequest(ReviewRequestReason.APP_NOT_WORKING) },
            "Unsure" to { buildReviewRequest(ReviewRequestReason.PARENT_UNSURE) },
        ))
        reviewOutputText = valueLabel("No review demo result yet", textSize = 14f)
        layout.addView(reviewOutputText)

        layout.addView(sectionTitle("Local events and diagnostics"))
        localEventsText = valueLabel("No local debug events", textSize = 14f)
        diagnosticsText = valueLabel("", textSize = 13f)
        layout.addView(button("Simulate VPN stopped event") {
            simulateVpnStoppedEvent()
        })
        layout.addView(button(ChildVpnSmokeLabels.COPY_DIAGNOSTICS_BUTTON) {
            copyDiagnostics()
        })
        layout.addView(button("Share diagnostics") {
            shareDiagnostics()
        })
        layout.addView(localEventsText)
        layout.addView(diagnosticsText)

        layout.addView(sectionTitle("Local alerts"))
        layout.addView(valueLabel("Debug/local alert report only.", textSize = 14f))
        layout.addView(valueLabel("Production alerts will use encrypted relay later.", textSize = 14f))
        layout.addView(valueLabel("Local security/status alerts only.", textSize = 14f))
        layout.addView(valueLabel("No web history or packet logs.", textSize = 14f))
        layout.addView(button("Copy alert report") {
            copyChildAlertReport()
        })
        layout.addView(button("Share alert report") {
            shareChildAlertReport()
        })
        layout.addView(button("Acknowledge all alerts") {
            acknowledgeAllAlerts()
        })
        layout.addView(button("Clear local alerts") {
            clearLocalAlerts()
        })
        layout.addView(button("Simulate stale heartbeat alert") {
            simulateStaleHeartbeatAlert()
        })
        localAlertsText = valueLabel(createLocalAlertsDisplay(), textSize = 13f)
        layout.addView(localAlertsText)

        layout.addView(sectionTitle("Export child sync bundle"))
        layout.addView(valueLabel("Local debug bundle only.", textSize = 14f))
        layout.addView(valueLabel("Production sync will use encrypted relay later.", textSize = 14f))
        layout.addView(valueLabel("No PINs, account secrets, web history, or packet logs are included.", textSize = 14f))
        layout.addView(button("Build child sync bundle") {
            buildChildSyncBundle()
        })
        layout.addView(button("Copy child sync bundle") {
            copyChildSyncBundle()
        })
        layout.addView(button("Share child sync bundle") {
            shareChildSyncBundle()
        })
        childSyncBundleOutputText = valueLabel(createChildSyncBundleOutput(), textSize = 13f)
        layout.addView(childSyncBundleOutputText)

        layout.addView(sectionTitle("Local dev relay"))
        layout.addView(valueLabel("Manual send/fetch only. Use on a trusted local network.", textSize = 14f))
        layout.addView(valueLabel("Local dev relay only. Production sync will use encrypted relay later.", textSize = 14f))
        relayBaseUrlInput = editText(relayBaseUrl)
        parentRelayDeviceInput = editText(parentRelayDeviceId)
        layout.addView(labeledField("Relay base URL", relayBaseUrlInput))
        layout.addView(labeledField("Parent device id", parentRelayDeviceInput))
        layout.addView(button("Test relay connection") {
            testRelayConnection()
        })
        layout.addView(button("Fetch parent bundles from relay") {
            fetchParentBundlesFromRelay()
        })
        layout.addView(button("Send child sync bundle to relay") {
            sendChildSyncBundleToRelay()
        })
        layout.addView(button("Ack latest fetched bundle") {
            ackLatestRelayMessage()
        })
        layout.addView(sectionTitle("Debug remote test harness"))
        layout.addView(valueLabel("Debug/local only. Remote test mode must be visibly enabled in this app.", textSize = 14f))
        layout.addView(valueLabel("Only allowlisted child app actions can run. Release builds use a no-op controller.", textSize = 14f))
        layout.addView(button("Enable remote test mode") {
            startRemoteTestMode()
        })
        layout.addView(button("Disable remote test mode") {
            stopRemoteTestMode()
        })
        remoteTestOutputText = valueLabel(createRemoteTestOutput(), textSize = 13f)
        layout.addView(remoteTestOutputText)
        layout.addView(button("Copy relay diagnostics") {
            copyRelayDiagnostics()
        })
        relayOutputText = valueLabel(createRelayOutput(), textSize = 13f)
        layout.addView(relayOutputText)

        layout.addView(sectionTitle("Import parent sync bundle"))
        layout.addView(valueLabel("Local debug bundle only.", textSize = 14f))
        layout.addView(valueLabel("Production sync will use encrypted relay later.", textSize = 14f))
        layout.addView(valueLabel("Policy payloads are verified before use.", textSize = 14f))
        layout.addView(valueLabel("Android share-sheet imports and copy/paste imports use the same local validation.", textSize = 14f))
        parentSyncBundleInput = multiLineEditText(latestParentSyncBundlePayload.orEmpty())
        layout.addView(labeledField("Paste parent sync bundle", parentSyncBundleInput))
        layout.addView(button("Import parent sync bundle") {
            importParentSyncBundle()
        })
        layout.addView(button("Clear parent sync bundle") {
            clearParentSyncBundleImport()
        })
        parentSyncBundleOutputText = valueLabel("No parent sync bundle imported yet", textSize = 13f)
        layout.addView(parentSyncBundleOutputText)

        layout.addView(sectionTitle("Bundle inbox"))
        layout.addView(valueLabel("Local/debug bundle import history only.", textSize = 14f))
        layout.addView(valueLabel("Policy payloads are verified before use.", textSize = 14f))
        layout.addView(button("Copy latest bundle summary") {
            copyLatestBundleInboxSummary()
        })
        layout.addView(button("Clear bundle inbox") {
            clearBundleInbox()
        })
        bundleInboxText = valueLabel(createBundleInboxDisplay(), textSize = 13f)
        layout.addView(bundleInboxText)

        layout.addView(sectionTitle("Local audit timeline"))
        layout.addView(valueLabel("Local explicit app-action timeline only.", textSize = 14f))
        layout.addView(valueLabel("No web history or packet logs.", textSize = 14f))
        layout.addView(button("Copy audit summary") {
            copyAuditSummary()
        })
        layout.addView(button("Share audit summary") {
            shareAuditSummary()
        })
        layout.addView(button("Clear local audit timeline") {
            clearAuditTimeline()
        })
        auditTimelineText = valueLabel(createAuditTimelineDisplay(), textSize = 13f)
        layout.addView(auditTimelineText)
        refreshDiagnosticsViews()
        return ScrollView(this).apply {
            addView(layout)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }

    private fun centerLabel(
        text: String,
        textSize: Float,
    ): TextView {
        return TextView(this).apply {
            this.text = text
            this.textSize = textSize
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 12)
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 20f
            gravity = Gravity.START
            setPadding(0, 28, 0, 8)
        }
    }

    private fun valueLabel(
        text: String,
        textSize: Float,
    ): TextView {
        return TextView(this).apply {
            this.text = text
            this.textSize = textSize
            gravity = Gravity.START
            setPadding(0, 8, 0, 8)
        }
    }

    private fun editText(initialText: String): EditText {
        return EditText(this).apply {
            setText(initialText)
            setSingleLine(true)
            setPadding(0, 8, 0, 8)
        }
    }

    private fun multiLineEditText(initialText: String): EditText {
        return EditText(this).apply {
            setText(initialText)
            setSingleLine(false)
            minLines = 5
            setPadding(0, 8, 0, 8)
        }
    }

    private fun labeledField(label: String, field: EditText): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(valueLabel(label, 14f))
            addView(field)
        }
    }

    private fun button(
        text: String,
        onClick: () -> Unit,
    ): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
        }
    }

    private fun horizontalButtons(vararg buttons: Pair<String, () -> Unit>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            buttons.forEach { (text, onClick) ->
                addView(button(text, onClick).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f,
                    )
                })
            }
        }
    }

    private fun requestVpnPermission() {
        recordAudit(
            type = AuditEntryType.VPN_PERMISSION_REQUESTED,
            severity = AuditSeverity.INFO,
            title = "VPN permission requested",
            detail = "Parent opened or reviewed the Android VPN permission flow.",
        )
        setLastCommand(ChildVpnSmokeLabels.COMMAND_PERMISSION_REQUESTED)
        when (val result = vpnPermissionIntentFactory.createPrepareResult(this)) {
            VpnPrepareResult.AlreadyGranted -> {
                setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_GRANTED)
                updateHardeningStep(
                    step = HardeningSetupStep.VPN_PERMISSION,
                    status = HardeningSetupStatus.AUTO_CONFIRMED,
                    evidenceType = HardeningEvidenceType.AUTOMATIC_CHECK,
                    note = "VPN permission is already prepared.",
                )
                setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_GRANTED)
            }
            is VpnPrepareResult.ConsentRequired -> {
                setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_REQUIRED)
                updateHardeningStep(
                    step = HardeningSetupStep.VPN_PERMISSION,
                    status = HardeningSetupStatus.OPENED_SETTINGS,
                    evidenceType = HardeningEvidenceType.MANUAL_DEVICE_SETTING,
                    note = "VPN permission prompt opened for parent confirmation.",
                )
                setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_REQUIRED)
                startActivityForResult(result.intent, REQUEST_VPN_PERMISSION)
            }
        }
    }

    private fun startVpnShellWhenAllowed() {
        when (val result = vpnPermissionIntentFactory.createPrepareResult(this)) {
            VpnPrepareResult.AlreadyGranted -> {
                setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_GRANTED)
                setLastCommand(ChildVpnSmokeLabels.COMMAND_START_SENT)
                setShellStatus(ChildVpnSmokeLabels.STATUS_STARTING_SHELL)
                val intent = VordainVpnServiceIntents.startProtection(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                recordAudit(
                    type = AuditEntryType.VPN_SHELL_STARTED,
                    severity = AuditSeverity.INFO,
                    title = "VPN shell started",
                    detail = "Establish-only VPN shell command was sent.",
                )
                setShellStatus(ChildVpnSmokeLabels.STATUS_SHELL_COMMAND_SENT)
                setStatus(ChildVpnSmokeLabels.STATUS_SHELL_ACTIVE)
            }
            is VpnPrepareResult.ConsentRequired -> {
                setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_REQUIRED)
                setLastCommand(ChildVpnSmokeLabels.COMMAND_PERMISSION_REQUESTED)
                setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_REQUIRED)
                startActivityForResult(result.intent, REQUEST_VPN_PERMISSION)
            }
        }
    }

    private fun stopVpnShell() {
        setLastCommand(ChildVpnSmokeLabels.COMMAND_STOP_SENT)
        setShellStatus(ChildVpnSmokeLabels.STATUS_STOP_COMMAND_SENT)
        startService(VordainVpnServiceIntents.stopProtection(this))
        setStatus(ChildVpnSmokeLabels.STATUS_STOPPED)
        setShellStatus(ChildVpnSmokeLabels.STATUS_STOPPED)
    }

    private fun startLabCaptureWhenAllowed() {
        when (val result = vpnPermissionIntentFactory.createPrepareResult(this)) {
            VpnPrepareResult.AlreadyGranted -> {
                setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_GRANTED)
                setLastCommand(ChildVpnSmokeLabels.COMMAND_LAB_START_SENT)
                setShellStatus(ChildVpnSmokeLabels.STATUS_LAB_CAPTURE_STARTING)
                val intent = VordainVpnServiceIntents.startLabCapture(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                recordAudit(
                    type = AuditEntryType.FULL_TUNNEL_LAB_STARTED,
                    severity = AuditSeverity.WARNING,
                    title = "Full-tunnel lab started",
                    detail = "Full-tunnel lab command was sent; packets are local lab handled.",
                )
                setStatus(ChildVpnSmokeLabels.STATUS_LAB_CAPTURE_ACTIVE)
            }
            is VpnPrepareResult.ConsentRequired -> {
                setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_REQUIRED)
                setLastCommand(ChildVpnSmokeLabels.COMMAND_PERMISSION_REQUESTED)
                setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_REQUIRED)
                startActivityForResult(result.intent, REQUEST_VPN_PERMISSION)
            }
        }
    }

    private fun stopLabCapture() {
        setLastCommand(ChildVpnSmokeLabels.COMMAND_LAB_STOP_SENT)
        setShellStatus(ChildVpnSmokeLabels.STATUS_STOP_COMMAND_SENT)
        startService(VordainVpnServiceIntents.stopLabCapture(this))
        recordAudit(
            type = AuditEntryType.FULL_TUNNEL_LAB_STOPPED,
            severity = AuditSeverity.INFO,
            title = "Full-tunnel lab stopped",
            detail = "Full-tunnel lab stop command was sent.",
        )
        setStatus(ChildVpnSmokeLabels.STATUS_STOPPED)
        setShellStatus(ChildVpnSmokeLabels.STATUS_STOPPED)
    }

    private fun startDnsOnlyLabWhenAllowed() {
        when (val result = vpnPermissionIntentFactory.createPrepareResult(this)) {
            VpnPrepareResult.AlreadyGranted -> {
                setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_GRANTED)
                setLastCommand(ChildVpnSmokeLabels.COMMAND_DNS_ONLY_START_SENT)
                setShellStatus(ChildVpnSmokeLabels.STATUS_DNS_ONLY_LAB_STARTING)
                val intent = VordainVpnServiceIntents.startDnsOnlyLab(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                recordAudit(
                    type = AuditEntryType.DNS_ONLY_LAB_STARTED,
                    severity = AuditSeverity.INFO,
                    title = "DNS-only lab started",
                    detail = "DNS-only lab filtering command was sent.",
                )
                setStatus(ChildVpnSmokeLabels.STATUS_DNS_ONLY_LAB_ACTIVE)
            }
            is VpnPrepareResult.ConsentRequired -> {
                setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_REQUIRED)
                setLastCommand(ChildVpnSmokeLabels.COMMAND_PERMISSION_REQUESTED)
                setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_REQUIRED)
                startActivityForResult(result.intent, REQUEST_VPN_PERMISSION)
            }
        }
    }

    private fun stopDnsOnlyLab() {
        setLastCommand(ChildVpnSmokeLabels.COMMAND_DNS_ONLY_STOP_SENT)
        setShellStatus(ChildVpnSmokeLabels.STATUS_STOP_COMMAND_SENT)
        startService(VordainVpnServiceIntents.stopDnsOnlyLab(this))
        recordAudit(
            type = AuditEntryType.DNS_ONLY_LAB_STOPPED,
            severity = AuditSeverity.INFO,
            title = "DNS-only lab stopped",
            detail = "DNS-only lab stop command was sent.",
        )
        setStatus(ChildVpnSmokeLabels.STATUS_STOPPED)
        setShellStatus(ChildVpnSmokeLabels.STATUS_STOPPED)
    }

    private fun startBasicDnsGuardWhenAllowed() {
        when (val result = vpnPermissionIntentFactory.createPrepareResult(this)) {
            VpnPrepareResult.AlreadyGranted -> {
                setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_GRANTED)
                setLastCommand(ChildVpnSmokeLabels.COMMAND_BASIC_DNS_START_SENT)
                setShellStatus(ChildVpnSmokeLabels.STATUS_BASIC_DNS_GUARD_STARTING)
                expectedBasicDnsGuardRunning = true
                val intent = VordainVpnServiceIntents.startBasicDnsGuard(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                recordAudit(
                    type = AuditEntryType.BASIC_DNS_GUARD_STARTED,
                    severity = AuditSeverity.INFO,
                    title = "Basic DNS Guard started",
                    detail = "Basic DNS Guard start command was sent.",
                )
                recordAlert(
                    type = AlertType.DNS_GUARD_STARTED,
                    severity = AlertSeverity.INFO,
                    title = "Basic DNS Guard started",
                    detail = "Basic DNS Guard start command was sent.",
                    sourceLabel = "Child app",
                )
                setStatus(ChildVpnSmokeLabels.STATUS_BASIC_DNS_GUARD_ACTIVE)
                saveCurrentState()
                refreshDiagnosticsViews()
            }
            is VpnPrepareResult.ConsentRequired -> {
                setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_REQUIRED)
                setLastCommand(ChildVpnSmokeLabels.COMMAND_PERMISSION_REQUESTED)
                recordAudit(
                    type = AuditEntryType.BASIC_DNS_GUARD_START_NEEDS_ATTENTION,
                    severity = AuditSeverity.WARNING,
                    title = "Basic DNS Guard needs attention",
                    detail = "VPN permission is required before Basic DNS Guard can start.",
                )
                setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_REQUIRED)
                expectedBasicDnsGuardRunning = false
                saveCurrentState()
                startActivityForResult(result.intent, REQUEST_VPN_PERMISSION)
            }
        }
    }

    private fun stopBasicDnsGuard() {
        setLastCommand(ChildVpnSmokeLabels.COMMAND_BASIC_DNS_STOP_SENT)
        setShellStatus(ChildVpnSmokeLabels.STATUS_STOP_COMMAND_SENT)
        expectedBasicDnsGuardRunning = false
        startService(VordainVpnServiceIntents.stopBasicDnsGuard(this))
        recordAudit(
            type = AuditEntryType.BASIC_DNS_GUARD_STOPPED,
            severity = AuditSeverity.INFO,
            title = "Basic DNS Guard stopped",
            detail = "Basic DNS Guard stop command was sent.",
        )
        recordAlert(
            type = AlertType.DNS_GUARD_STOPPED,
            severity = AlertSeverity.WARNING,
            title = "Basic DNS Guard stopped",
            detail = "Basic DNS Guard stop command was sent.",
            sourceLabel = "Child app",
        )
        recordAlert(
            type = AlertType.BASIC_DNS_GUARD_STOPPED,
            severity = AlertSeverity.WARNING,
            title = "Basic DNS Guard stopped",
            detail = "Basic DNS Guard is stopped.",
            sourceLabel = "Child app",
        )
        setStatus(ChildVpnSmokeLabels.STATUS_STOPPED)
        setShellStatus(ChildVpnSmokeLabels.STATUS_STOPPED)
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun openSettings(action: String) {
        val intent = Intent(action)
        runCatching {
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
    }

    private fun updateHardeningStep(
        step: HardeningSetupStep,
        status: HardeningSetupStatus,
        evidenceType: HardeningEvidenceType,
        note: String,
    ) {
        hardeningSetupSnapshot = hardeningSetupReducer.updateStep(
            snapshot = currentHardeningSetupSnapshot(),
            step = step,
            status = status,
            evidenceType = evidenceType,
            note = note,
            currentTimeMillis = System.currentTimeMillis(),
        )
        syncLegacySetupStateFromHardening()
        latestHardeningSetupReportPayload = hardeningSetupReportCodec.encode(currentHardeningSetupSnapshot())
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun copyHardeningSetupReport() {
        val snapshot = currentHardeningSetupSnapshot().copy(generatedAtMillis = System.currentTimeMillis())
        hardeningSetupSnapshot = snapshot.copy(summaryStatus = hardeningSetupReducer.summarize(snapshot))
        latestHardeningSetupReportPayload = hardeningSetupReportCodec.encode(currentHardeningSetupSnapshot())
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Vordain debug hardening setup report", latestHardeningSetupReportPayload.orEmpty()),
        )
        recordAudit(
            type = AuditEntryType.SETUP_REPORT_GENERATED,
            severity = AuditSeverity.INFO,
            title = "Setup report copied",
            detail = "Parent-confirmed setup report was copied locally.",
        )
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun shareHardeningSetupReport() {
        if (latestHardeningSetupReportPayload.isNullOrBlank()) {
            copyHardeningSetupReport()
        }
        shareEnvelope(
            kind = VordainDebugPayloadKind.SETUP_REPORT,
            title = "Share Vordain setup report",
            payload = latestHardeningSetupReportPayload.orEmpty(),
        )
    }

    private fun clearHardeningSetup() {
        hardeningSetupSnapshot = hardeningSetupReducer.initialSnapshot(
            childDeviceId = DeviceId(childDeviceId),
            currentTimeMillis = System.currentTimeMillis(),
        )
        latestHardeningSetupReportPayload = hardeningSetupReportCodec.encode(currentHardeningSetupSnapshot())
        setupForegroundNotificationStatus = SetupCheckState.UNKNOWN
        setupAlwaysOnVpnStatus = SetupCheckState.UNKNOWN
        setupBlockWithoutVpnStatus = SetupCheckState.UNKNOWN
        setupBatteryOptimizationStatus = SetupCheckState.UNKNOWN
        setupSettingsAppLockStatus = SetupCheckState.UNKNOWN
        setupScreenPinningStatus = SetupCheckState.UNKNOWN
        setupPrivateDnsStatus = SetupCheckState.UNKNOWN
        setupUnknownSourcesStatus = SetupCheckState.UNKNOWN
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun markBypassRisk(
        category: BypassRiskCategory,
        status: BypassRiskStatus,
        severity: BypassRiskSeverity,
        note: String,
    ) {
        val nextItem = BypassRiskItem(
            category = category,
            status = status,
            severity = severity,
            evidenceLabel = "Parent manual review",
            note = note,
        )
        bypassRiskItems = (bypassRiskItems.filterNot { it.category == category } + nextItem)
            .sortedBy { it.category.ordinal }
        latestBypassRiskReportPayload = bypassRiskReportCodec.encode(currentBypassRiskReport())
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun generateBypassRiskReport() {
        latestBypassRiskReportPayload = bypassRiskReportCodec.encode(currentBypassRiskReport())
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun copyBypassRiskReport() {
        if (latestBypassRiskReportPayload.isNullOrBlank()) {
            generateBypassRiskReport()
        }
        val payload = latestBypassRiskReportPayload.orEmpty()
        if (payload.isBlank()) {
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain debug bypass-risk report", payload))
        if (::bypassRiskText.isInitialized) {
            bypassRiskText.text = "${createBypassRiskDisplay()}\n\nCopied bypass-risk report."
        }
        recordAudit(
            type = AuditEntryType.BYPASS_REPORT_GENERATED,
            severity = AuditSeverity.INFO,
            title = "Bypass-risk report copied",
            detail = "DNS-only bypass-risk report was copied locally.",
        )
        saveCurrentState()
    }

    private fun shareBypassRiskReport() {
        if (latestBypassRiskReportPayload.isNullOrBlank()) {
            generateBypassRiskReport()
        }
        shareEnvelope(
            kind = VordainDebugPayloadKind.BYPASS_REPORT,
            title = "Share Vordain bypass-risk report",
            payload = latestBypassRiskReportPayload.orEmpty(),
        )
    }

    private fun startParentMaintenanceWindow() {
        val now = System.currentTimeMillis()
        hardeningSetupSnapshot = hardeningSetupReducer.openMaintenanceWindow(
            snapshot = currentHardeningSetupSnapshot(),
            window = ParentMaintenanceWindow(
                windowId = "debug-maintenance-$now",
                openedAtMillis = now,
                expiresAtMillis = now + MAINTENANCE_WINDOW_MILLIS,
                reason = MaintenanceWindowReason.APP_LOCK_REVIEW,
            ),
            currentTimeMillis = now,
        )
        latestHardeningSetupReportPayload = hardeningSetupReportCodec.encode(currentHardeningSetupSnapshot())
        recordAudit(
            type = AuditEntryType.MAINTENANCE_WINDOW_STARTED,
            severity = AuditSeverity.INFO,
            title = "Parent maintenance window started",
            detail = "Parent-authorized setup window was opened.",
        )
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun endParentMaintenanceWindow() {
        hardeningSetupSnapshot = hardeningSetupReducer.closeMaintenanceWindow(
            snapshot = currentHardeningSetupSnapshot(),
            currentTimeMillis = System.currentTimeMillis(),
        )
        latestHardeningSetupReportPayload = hardeningSetupReportCodec.encode(currentHardeningSetupSnapshot())
        recordAudit(
            type = AuditEntryType.MAINTENANCE_WINDOW_ENDED,
            severity = AuditSeverity.INFO,
            title = "Parent maintenance window ended",
            detail = "Parent-authorized setup window was closed.",
        )
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun simulateHardeningChangeOutsideParentWindow() {
        val withoutWindow = currentHardeningSetupSnapshot().copy(activeMaintenanceWindow = null)
        hardeningSetupSnapshot = hardeningSetupReducer.updateStep(
            snapshot = withoutWindow,
            step = HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY,
            status = HardeningSetupStatus.NEEDS_ATTENTION,
            evidenceType = HardeningEvidenceType.STATE_CHANGE_OUTSIDE_AUTHORIZED_WINDOW,
            note = "Settings/App Lock hardening changed outside parent maintenance window.",
            currentTimeMillis = System.currentTimeMillis(),
        )
        latestHardeningSetupReportPayload = hardeningSetupReportCodec.encode(currentHardeningSetupSnapshot())
        recordAudit(
            type = AuditEntryType.PIN_COMPROMISE_SIGNAL,
            severity = AuditSeverity.HIGH,
            title = "PIN compromise signal",
            detail = "Hardening changed outside a parent maintenance window.",
        )
        recordAlert(
            type = AlertType.PIN_COMPROMISE_SUSPECTED,
            severity = AlertSeverity.CRITICAL,
            title = "Parent PIN may be compromised",
            detail = "Hardening changed outside a parent maintenance window.",
            sourceLabel = "Hardening setup",
        )
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun evaluatePolicyDomain() {
        policyResult = policyDemo.evaluate(policyDomainInput.text.toString())
        policyOutputText.text = policyResult?.asDisplayText().orEmpty()
        refreshDiagnosticsViews()
    }

    private fun applyDebugPolicyUpdate() {
        childDeviceId = policyHandoffTargetInput.text.toString().trim().ifBlank {
            ChildDebugStateSnapshot.DEFAULT_CHILD_DEVICE_ID
        }
        val result = policyHandoff.apply(
            expectedDeviceId = DeviceId(childDeviceId),
            payload = policyHandoffPayloadInput.text.toString(),
        )
        policyHandoffResult = result
        if (result.accepted && result.policy != null && result.policyVersion != null) {
            policyDemo.replacePolicy(
                policy = result.policy,
                blockEncryptedDnsResolvers = result.blockEncryptedDnsResolvers,
            )
            currentPolicyVersion = result.policyVersion.value
            latestPolicyPayload = policyHandoffPayloadInput.text.toString()
            latestPolicyAppliedAtMillis = System.currentTimeMillis()
            latestAllowDomainsCsv = result.policy.allowedDomains.toCsv()
            latestBlockDomainsCsv = result.policy.blockedDomains.toCsv()
            currentPolicySource = "Verified applied debug policy"
            currentPolicyPresetName = result.presetName
            currentPolicyDisplayLabel = result.policyDisplayLabel
            currentPolicyBlockEncryptedDnsResolvers = result.blockEncryptedDnsResolvers
            lastPolicyVerificationResult = result.reason
            LabCaptureDebugStatus.useVerifiedPolicy(result.policy)
            recordAudit(
                type = AuditEntryType.POLICY_PAYLOAD_APPLIED,
                severity = AuditSeverity.INFO,
                title = "Policy payload applied",
                detail = "Verified debug DNS policy ${result.policyVersion.value} was applied.",
            )
        } else {
            lastPolicyVerificationResult = result.reason
            recordAudit(
                type = AuditEntryType.POLICY_PAYLOAD_REJECTED,
                severity = AuditSeverity.WARNING,
                title = "Policy payload rejected",
                detail = "Debug DNS policy payload was rejected: ${result.reason}.",
            )
            recordAlert(
                type = AlertType.POLICY_REJECTED,
                severity = AlertSeverity.HIGH,
                title = "Policy rejected",
                detail = "Debug DNS policy payload was rejected: ${result.reason}.",
                sourceLabel = "Policy handoff",
            )
        }
        policyHandoffOutputText.text = "${result.asDisplayText()}\n\n${createActivePolicyDisplay()}"
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun clearPolicyHandoffResult() {
        policyHandoffResult = null
        latestPolicyPayload = null
        latestPolicyAppliedAtMillis = 0L
        latestAllowDomainsCsv = null
        latestBlockDomainsCsv = null
        currentPolicySource = "Default sample policy"
        currentPolicyVersion = "debug-tablet-policy"
        currentPolicyPresetName = null
        currentPolicyDisplayLabel = null
        currentPolicyBlockEncryptedDnsResolvers = true
        lastPolicyVerificationResult = "Applied debug policy cleared"
        policyDemo.resetToDefault()
        LabCaptureDebugStatus.useDefaultPolicy()
        policyHandoffPayloadInput.setText("")
        policyHandoffOutputText.text = createActivePolicyDisplay()
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun reverifyStoredPolicy() {
        val payload = latestPolicyPayload ?: policyHandoffPayloadInput.text.toString().takeIf(String::isNotBlank)
        if (payload.isNullOrBlank()) {
            lastPolicyVerificationResult = "No stored debug policy payload to re-verify"
            policyHandoffOutputText.text = createActivePolicyDisplay()
            refreshDiagnosticsViews()
            return
        }
        restorePersistedPolicyPayload(
            payload = payload,
            appliedAtMillis = latestPolicyAppliedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis(),
            lastKnownPolicyVersion = currentPolicyVersion,
        )
        policyHandoffOutputText.text = createActivePolicyDisplay()
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun copyActivePolicyDiagnostics() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Vordain active DNS-only lab policy diagnostics",
                createActivePolicyDisplay(),
            ),
        )
        policyHandoffOutputText.text = "${createActivePolicyDisplay()}\n\nCopied active policy diagnostics."
        recordAudit(
            type = AuditEntryType.DIAGNOSTICS_COPIED,
            severity = AuditSeverity.INFO,
            title = "Active policy diagnostics copied",
            detail = "Active DNS-only lab policy diagnostics were copied locally.",
        )
        saveCurrentState()
    }

    private fun shareActivePolicyDiagnostics() {
        shareEnvelope(
            kind = VordainDebugPayloadKind.DIAGNOSTICS,
            title = "Share Vordain active policy diagnostics",
            payload = createActivePolicyDisplay(),
        )
    }

    private fun acceptPairingInvite() {
        childDeviceId = policyHandoffTargetInput.text.toString().trim().ifBlank {
            ChildDebugStateSnapshot.DEFAULT_CHILD_DEVICE_ID
        }
        childDisplayName = childDisplayNameInput.text.toString().trim().ifBlank {
            ChildDebugStateSnapshot.DEFAULT_CHILD_DISPLAY_NAME
        }
        childFingerprint = childFingerprintInput.text.toString().trim().ifBlank {
            ChildDebugStateSnapshot.DEFAULT_CHILD_FINGERPRINT
        }
        latestPairingInvitePayload = pairingInviteInput.text.toString()

        val output = when (val inviteResult = pairingInviteCodec.decode(pairingInviteInput.text.toString())) {
            is DebugPairingInviteCodecResult.Rejected -> {
                latestPairingAcceptancePayload = null
                acceptedParentSummary = null
                "Pairing result: ${PairingStatus.REJECTED}\nReason: ${inviteResult.reason}"
            }
            is DebugPairingInviteCodecResult.Decoded -> {
                val acceptance = PairingAcceptance(
                    sessionId = inviteResult.invite.sessionId,
                    childDeviceProfile = PairingDeviceProfile(
                        deviceId = DeviceId(childDeviceId),
                        role = PairingRole.CHILD,
                        displayName = childDisplayName,
                        publicKeyFingerprint = PairingPublicKeyFingerprint(childFingerprint),
                        capabilities = defaultPairingCapabilities,
                    ),
                    acceptedAtMillis = System.currentTimeMillis(),
                    verificationCode = inviteResult.invite.verificationCode,
                )
                val evaluation = pairingEvaluator.evaluateAcceptance(
                    invite = inviteResult.invite,
                    acceptance = acceptance,
                    currentTimeMillis = System.currentTimeMillis(),
                )
                val pairedParent = evaluation.pairedParent
                if (evaluation.status == PairingStatus.PAIRED && pairedParent != null) {
                    latestPairingAcceptancePayload = pairingAcceptanceCodec.encode(acceptance)
                    acceptedParentSummary = listOf(
                        "Parent: ${pairedParent.deviceId.value}",
                        "Display name: ${pairedParent.displayName}",
                        "Fingerprint: ${pairedParent.publicKeyFingerprint.value}",
                    ).joinToString(separator = "\n")
                } else {
                    latestPairingAcceptancePayload = null
                    acceptedParentSummary = null
                }
                "Pairing result: ${evaluation.status}\nReason: ${evaluation.reason}\n${acceptedParentSummary.orEmpty()}"
            }
        }

        pairingOutputText.text = output
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun copyChildAcceptance() {
        val acceptancePayload = latestPairingAcceptancePayload
        if (acceptancePayload.isNullOrBlank()) {
            pairingOutputText.text = "No child acceptance payload is available yet"
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain debug pairing acceptance", acceptancePayload))
        pairingOutputText.text = "$acceptancePayload\n\nCopied child acceptance."
        saveCurrentState()
    }

    private fun shareChildAcceptance() {
        val acceptancePayload = latestPairingAcceptancePayload
        if (acceptancePayload.isNullOrBlank()) {
            pairingOutputText.text = "No child acceptance payload is available yet"
            return
        }
        shareEnvelope(
            kind = VordainDebugPayloadKind.PAIRING_ACCEPTANCE,
            title = "Share Vordain pairing acceptance",
            payload = acceptancePayload,
        )
    }

    private fun clearPairingResult() {
        latestPairingAcceptancePayload = null
        acceptedParentSummary = null
        pairingOutputText.text = createPairingOutput()
        saveCurrentState()
    }

    private fun evaluateCompatibility(mode: AppTrafficMode) {
        compatibilityResult = compatibilityDemo.evaluate(
            rawPackageName = compatibilityPackageInput.text.toString(),
            rawDomain = compatibilityDomainInput.text.toString(),
            mode = mode,
        )
        compatibilityOutputText.text = compatibilityResult?.asDisplayText().orEmpty()
        refreshDiagnosticsViews()
    }

    private fun buildReviewRequest(reason: ReviewRequestReason) {
        reviewResult = reviewDemo.buildReview(
            parentInput = reviewSubjectInput.text.toString(),
            reason = reason,
        )
        reviewOutputText.text = reviewResult?.asDisplayText().orEmpty()
        refreshDiagnosticsViews()
    }

    private fun simulateVpnStoppedEvent() {
        localDebugEvents += "VPN stopped debug event queued locally in memory"
        recordAlert(
            type = AlertType.VPN_STOPPED,
            severity = AlertSeverity.CRITICAL,
            title = "VPN stopped",
            detail = "Debug simulation: VPN stopped or needs review.",
            sourceLabel = "Debug simulation",
        )
        refreshDiagnosticsViews()
    }

    private fun setStatus(status: String) {
        statusText.text = status
        shellStatus = status
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun setVpnPermissionStatus(status: String) {
        vpnPermissionStatus = status
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun setLastCommand(command: String) {
        lastCommand = command
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun setShellStatus(status: String) {
        shellStatus = status
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun copyDiagnostics() {
        val diagnostics = diagnosticsFormatter.format(
            diagnostics = createDiagnostics(),
            state = createDashboardState(),
        )
        lastDiagnosticsText = diagnostics
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain Guard diagnostics", diagnostics))
        diagnosticsText.text = ChildVpnSmokeLabels.DIAGNOSTICS_COPIED
        recordAudit(
            type = AuditEntryType.DIAGNOSTICS_COPIED,
            severity = AuditSeverity.INFO,
            title = "Diagnostics copied",
            detail = "Local MVP diagnostics were copied.",
        )
        recordAlert(
            type = AlertType.DIAGNOSTICS_GENERATED,
            severity = AlertSeverity.INFO,
            title = "Diagnostics copied",
            detail = "Local diagnostics were copied by user action.",
            sourceLabel = "Child app",
        )
        saveCurrentState()
    }

    private fun shareDiagnostics() {
        val diagnostics = diagnosticsFormatter.format(
            diagnostics = createDiagnostics(),
            state = createDashboardState(),
        )
        lastDiagnosticsText = diagnostics
        shareEnvelope(
            kind = VordainDebugPayloadKind.DIAGNOSTICS,
            title = "Share Vordain diagnostics",
            payload = diagnostics,
        )
    }

    private fun copyAuditSummary() {
        val summary = createAuditTimelineDisplay()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain local debug audit summary", summary))
        recordAudit(
            type = AuditEntryType.DIAGNOSTICS_COPIED,
            severity = AuditSeverity.INFO,
            title = "Audit summary copied",
            detail = "Local MVP audit summary was copied.",
        )
    }

    private fun shareAuditSummary() {
        shareEnvelope(
            kind = VordainDebugPayloadKind.DIAGNOSTICS,
            title = "Share Vordain audit summary",
            payload = createAuditTimelineDisplay(),
        )
    }

    private fun clearAuditTimeline() {
        auditTimeline = auditTimelineReducer.clear(auditTimeline)
        auditStore.save(auditTimeline)
        if (::auditTimelineText.isInitialized) {
            auditTimelineText.text = createAuditTimelineDisplay()
        }
        if (::localAlertsText.isInitialized) {
            localAlertsText.text = createLocalAlertsDisplay()
        }
    }

    private fun copyChildAlertReport() {
        val payload = childAlertReportCodec.encode(currentChildAlertReport())
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain child alert report", payload))
        recordAlert(
            type = AlertType.DIAGNOSTICS_GENERATED,
            severity = AlertSeverity.INFO,
            title = "Alert report copied",
            detail = "Local alert report was copied by user action.",
            sourceLabel = "Child app",
        )
        recordAudit(
            type = AuditEntryType.DIAGNOSTICS_COPIED,
            severity = AuditSeverity.INFO,
            title = "Alert report copied",
            detail = "Local alert report was copied.",
        )
        refreshDiagnosticsViews()
    }

    private fun shareChildAlertReport() {
        shareEnvelope(
            kind = VordainDebugPayloadKind.DIAGNOSTICS,
            title = "Share Vordain child alert report",
            payload = childAlertReportCodec.encode(currentChildAlertReport()),
        )
    }

    private fun acknowledgeAllAlerts() {
        val now = System.currentTimeMillis()
        childAlertTimeline.alerts
            .filter { it.status == AlertStatus.ACTIVE }
            .forEach { alert ->
                childAlertTimeline = childAlertReducer.acknowledge(childAlertTimeline, alert.id, now)
            }
        alertStore.save(childAlertTimeline)
        if (::localAlertsText.isInitialized) {
            localAlertsText.text = createLocalAlertsDisplay()
        }
    }

    private fun clearLocalAlerts() {
        childAlertTimeline = ChildAlertTimeline()
        alertStore.save(childAlertTimeline)
        if (::localAlertsText.isInitialized) {
            localAlertsText.text = createLocalAlertsDisplay()
        }
        refreshDiagnosticsViews()
    }

    private fun simulateStaleHeartbeatAlert() {
        recordAlert(
            type = AlertType.BASIC_DNS_GUARD_HEARTBEAT_STALE,
            severity = AlertSeverity.CRITICAL,
            title = "Heartbeat stale",
            detail = "Debug simulation: Basic DNS Guard heartbeat needs review.",
            sourceLabel = "Debug simulation",
            policyVersion = currentVerifiedPolicyVersion(),
        )
        recordAudit(
            type = AuditEntryType.PIN_COMPROMISE_SIGNAL,
            severity = AuditSeverity.WARNING,
            title = "Heartbeat alert simulated",
            detail = "Debug heartbeat stale alert was simulated locally.",
        )
        refreshDiagnosticsViews()
    }

    private fun currentChildAlertReport(): DebugChildAlertReport {
        return DebugChildAlertReport(
            childDeviceId = DeviceId(childDeviceId),
            generatedAtMillis = System.currentTimeMillis(),
            alerts = childAlertReducer.latest(childAlertTimeline, ALERT_REPORT_COUNT),
            summaryLabel = createAlertSummaryLabel(),
        )
    }

    private fun recordAlert(
        type: AlertType,
        severity: AlertSeverity,
        title: String,
        detail: String,
        sourceLabel: String,
        policyVersion: String? = currentVerifiedPolicyVersion(),
    ) {
        if (!::alertStore.isInitialized) {
            return
        }
        val now = System.currentTimeMillis()
        val alert = ChildAlert(
            id = "$now-${type.name}",
            type = type,
            severity = severity,
            status = AlertStatus.ACTIVE,
            childDeviceId = DeviceId(childDeviceId),
            occurredAtMillis = now,
            title = title,
            detail = detail,
            sourceLabel = sourceLabel,
            policyVersion = policyVersion,
        )
        childAlertTimeline = childAlertReducer.append(
            timeline = childAlertTimeline,
            alert = alert,
            maxAlerts = ALERT_MAX_ENTRIES,
        )
        alertStore.save(childAlertTimeline)
        if (::localAlertsText.isInitialized) {
            localAlertsText.text = createLocalAlertsDisplay()
        }
        if (severity == AlertSeverity.CRITICAL || severity == AlertSeverity.HIGH) {
            showLocalAlertNotification()
        }
    }

    private fun createAlertSummaryLabel(): String {
        val activeCritical = childAlertReducer.activeCriticalCount(childAlertTimeline)
        val activeTotal = childAlertTimeline.alerts.count { it.status == AlertStatus.ACTIVE }
        return if (activeCritical > 0) {
            "$activeCritical active critical alert(s)"
        } else {
            "$activeTotal active alert(s)"
        }
    }

    private fun createLocalAlertsDisplay(): String {
        val latest = childAlertReducer.latest(childAlertTimeline, ALERT_DISPLAY_COUNT)
        if (latest.isEmpty()) {
            return "No local alerts yet\nDebug/local alert report only.\nNo web history or packet logs."
        }
        return buildString {
            append("Active critical count: ${childAlertReducer.activeCriticalCount(childAlertTimeline)}\n")
            append("Summary: ${createAlertSummaryLabel()}\n")
            append("Debug/local alert report only.\n")
            append("No web history or packet logs.\n")
            latest.forEach { alert ->
                append("${alert.occurredAtMillis} / ${alert.severity} / ${alert.status} / ${alert.type}\n")
                append("${alert.title}: ${alert.detail}\n")
                append("Source: ${alert.sourceLabel}\n")
                append("Policy version: ${alert.policyVersion ?: "none"}\n")
            }
        }.trimEnd()
    }

    private fun showLocalAlertNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureLocalAlertNotificationChannel()
        val notification = android.app.Notification.Builder(this, LOCAL_ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Vordain Guard needs attention")
            .setContentText("DNS Guard stopped or needs review")
            .setShowWhen(true)
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(LOCAL_ALERT_NOTIFICATION_ID, notification)
    }

    private fun ensureLocalAlertNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(
            LOCAL_ALERT_CHANNEL_ID,
            "Vordain Guard local alerts",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildChildSyncBundle() {
        latestChildSecurityStatusReportPayload = childSecurityReportCodec.encode(currentChildSecurityStatusReport())
        latestHardeningSetupReportPayload = hardeningSetupReportCodec.encode(currentHardeningSetupSnapshot())
        latestBypassRiskReportPayload = bypassRiskReportCodec.encode(currentBypassRiskReport())
        val alertReportPayload = childAlertReportCodec.encode(currentChildAlertReport())
        val diagnosticsPayload = basicDnsGuardDiagnosticsCodec.encode(currentBasicDnsGuardDiagnosticsReport())
        val auditSummary = createAuditTimelineDisplay()
        val bundle = SyncBundle(
            bundleId = "child-sync-${System.currentTimeMillis()}",
            direction = SyncBundleDirection.CHILD_TO_PARENT,
            kind = SyncBundleKind.CHILD_STATUS_EXPORT,
            createdAtMillis = System.currentTimeMillis(),
            sourceDeviceId = DeviceId(childDeviceId),
            targetDeviceId = acceptedParentSummary?.lineSequence()
                ?.firstOrNull { it.startsWith("Parent:") }
                ?.substringAfter("Parent:")
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?.let(::DeviceId),
            payloads = listOf(
                SyncBundlePayload(
                    kind = SyncBundlePayloadKind.CHILD_SECURITY_STATUS_REPORT,
                    label = "Child security status report",
                    payloadText = latestChildSecurityStatusReportPayload.orEmpty(),
                ),
                SyncBundlePayload(
                    kind = SyncBundlePayloadKind.CHILD_ALERT_REPORT,
                    label = "Child alert report",
                    payloadText = alertReportPayload,
                ),
                SyncBundlePayload(
                    kind = SyncBundlePayloadKind.HARDENING_SETUP_REPORT,
                    label = "Hardening setup report",
                    payloadText = latestHardeningSetupReportPayload.orEmpty(),
                ),
                SyncBundlePayload(
                    kind = SyncBundlePayloadKind.BYPASS_RISK_REPORT,
                    label = "Bypass-risk report",
                    payloadText = latestBypassRiskReportPayload.orEmpty(),
                ),
                SyncBundlePayload(
                    kind = SyncBundlePayloadKind.ACTIVE_POLICY_SUMMARY,
                    label = "Active policy summary",
                    payloadText = createActivePolicyDisplay(),
                ),
                SyncBundlePayload(
                    kind = SyncBundlePayloadKind.AUDIT_SUMMARY,
                    label = "Audit summary",
                    payloadText = auditSummary,
                ),
                SyncBundlePayload(
                    kind = SyncBundlePayloadKind.DIAGNOSTICS_TEXT,
                    label = "Basic DNS Guard diagnostics",
                    payloadText = diagnosticsPayload,
                ),
            ),
        )
        latestChildSyncBundlePayload = syncBundleCodec.encode(bundle)
        recordAudit(
            type = AuditEntryType.SYNC_BUNDLE_CREATED,
            severity = AuditSeverity.INFO,
            title = "Child sync bundle created",
            detail = "Child-to-parent sync bundle created with ${bundle.payloads.size} payload labels.",
        )
        saveCurrentState()
        if (::childSyncBundleOutputText.isInitialized) {
            childSyncBundleOutputText.text = createChildSyncBundleOutput()
        }
    }

    private fun copyChildSyncBundle() {
        if (latestChildSyncBundlePayload.isNullOrBlank()) {
            buildChildSyncBundle()
        }
        val payload = latestChildSyncBundlePayload.orEmpty()
        if (payload.isBlank()) {
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain child sync bundle", payload))
        recordAudit(
            type = AuditEntryType.CHILD_SYNC_BUNDLE_SHARED,
            severity = AuditSeverity.INFO,
            title = "Child sync bundle copied",
            detail = "Child-to-parent sync bundle copied by user action.",
        )
        if (::childSyncBundleOutputText.isInitialized) {
            childSyncBundleOutputText.text = "${createChildSyncBundleOutput()}\n\nCopied child sync bundle."
        }
        saveCurrentState()
    }

    private fun shareChildSyncBundle() {
        if (latestChildSyncBundlePayload.isNullOrBlank()) {
            buildChildSyncBundle()
        }
        val payload = latestChildSyncBundlePayload.orEmpty()
        if (payload.isBlank()) {
            return
        }
        shareText(
            title = "Share Vordain child sync bundle",
            text = payload,
        )
        recordAudit(
            type = AuditEntryType.CHILD_SYNC_BUNDLE_SHARED,
            severity = AuditSeverity.INFO,
            title = "Child sync bundle shared",
            detail = "Child-to-parent sync bundle shared by user action.",
        )
    }

    private fun fetchParentBundlesFromRelay() {
        updateRelayDiagnostics("Fetching parent bundles from local dev relay...")
        val targetDeviceId = currentChildDeviceId()
        runRelayAction {
            val result = localDevRelayClient.fetchMessages(
                baseUrl = relayBaseUrlInput.text.toString(),
                targetDeviceId = targetDeviceId,
            )
            runOnUiThread {
                var acceptedCount = 0
                val parentMessages = result.messages.filter { message ->
                    message.direction == DevRelayDirection.PARENT_TO_CHILD
                }
                parentMessages.forEach { message ->
                    latestRelayMessageId = message.messageId
                    importParentSyncBundle(message.bundleText, sourceLabel = "Local dev relay")
                    acceptedCount += 1
                }
                val skippedCount = result.messages.size - parentMessages.size
                updateRelayDiagnostics(
                    listOf(
                        "Local dev relay fetch result",
                        result.summary,
                        "Target child: $targetDeviceId",
                        "Fetched messages: ${result.messages.size}",
                        "Imported parent bundles: $acceptedCount",
                        "Skipped wrong direction: $skippedCount",
                        "Latest message id: ${latestRelayMessageId.ifBlank { "none" }}",
                        "Policy payloads are verified before use.",
                    ).joinToString(separator = "\n"),
                )
            }
        }
    }

    private fun testRelayConnection() {
        updateRelayDiagnostics("Testing local dev relay /health...")
        runRelayAction {
            val result = localDevRelayClient.health(relayBaseUrlInput.text.toString())
            runOnUiThread {
                updateRelayDiagnostics(
                    listOf(
                        "Local dev relay health check",
                        result.summary,
                        if (result.success) "Relay reachable" else "Relay needs attention",
                        "Use trusted local network only.",
                    ).joinToString(separator = "\n"),
                )
            }
        }
    }

    private fun sendChildSyncBundleToRelay() {
        if (latestChildSyncBundlePayload.isNullOrBlank()) {
            buildChildSyncBundle()
        }
        val payload = latestChildSyncBundlePayload.orEmpty()
        if (payload.isBlank()) {
            updateRelayDiagnostics("Child sync bundle is not ready for relay send.")
            return
        }
        val now = System.currentTimeMillis()
        val message = DevRelayMessage(
            messageId = "child-relay-$now",
            direction = DevRelayDirection.CHILD_TO_PARENT,
            sourceDeviceId = DeviceId(currentChildDeviceId()),
            targetDeviceId = DeviceId(parentRelayDeviceInput.text.toString().trim().ifBlank {
                ChildDebugStateSnapshot.DEFAULT_PARENT_RELAY_DEVICE_ID
            }),
            createdAtMillis = now,
            bundleText = payload,
            status = DevRelayMessageStatus.PENDING,
        )
        updateRelayDiagnostics("Sending child sync bundle to local dev relay...")
        runRelayAction {
            val result = localDevRelayClient.sendMessage(
                baseUrl = relayBaseUrlInput.text.toString(),
                message = message,
            )
            runOnUiThread {
                if (result.success) {
                    latestRelayMessageId = message.messageId
                    recordAudit(
                        type = AuditEntryType.SYNC_BUNDLE_CREATED,
                        severity = AuditSeverity.INFO,
                        title = "Child relay send",
                        detail = "Child-to-parent sync bundle sent to local dev relay by user action.",
                    )
                }
                updateRelayDiagnostics(
                    listOf(
                        "Local dev relay send result",
                        result.summary,
                        "Message id: ${message.messageId}",
                        "Target parent: ${message.targetDeviceId.value}",
                        "Manual send/fetch only.",
                    ).joinToString(separator = "\n"),
                )
            }
        }
    }

    private fun ackLatestRelayMessage() {
        val messageId = latestRelayMessageId.takeIf(String::isNotBlank)
        if (messageId == null) {
            updateRelayDiagnostics("No fetched/sent relay message id to acknowledge.")
            return
        }
        updateRelayDiagnostics("Acknowledging local dev relay message $messageId...")
        runRelayAction {
            val result = localDevRelayClient.ackMessage(
                baseUrl = relayBaseUrlInput.text.toString(),
                messageId = messageId,
            )
            runOnUiThread {
                updateRelayDiagnostics(
                    listOf(
                        "Local dev relay ack result",
                        result.summary,
                        "Message id: $messageId",
                    ).joinToString(separator = "\n"),
                )
            }
        }
    }

    private fun startRemoteTestMode() {
        val baseUrl = relayBaseUrlInput.text.toString()
        val childId = currentChildDeviceId()
        updateRemoteTestStatus("Remote test mode starting for $childId. Debug/local only.")
        remoteTestController.start(
            config = ChildRemoteTestConfig(
                baseUrl = baseUrl,
                childDeviceId = childId,
            ),
            handlers = ChildRemoteTestHandlers(
                relayHealthCheck = {
                    val result = localDevRelayClient.health(baseUrl)
                    ChildRemoteTestActionResult(result.success, result.summary)
                },
                fetchPolicyBundle = {
                    fetchParentBundlesFromRelay()
                    ChildRemoteTestActionResult(true, "Requested parent policy bundle fetch/import from local dev relay.")
                },
                importLatestPolicyBundle = {
                    val payload = latestParentSyncBundlePayload.orEmpty()
                    if (payload.isBlank()) {
                        ChildRemoteTestActionResult(false, "No latest parent sync bundle is available to import.")
                    } else {
                        importParentSyncBundle(payload, sourceLabel = "Remote test harness")
                        ChildRemoteTestActionResult(true, "Requested latest parent sync bundle import through verified policy path.")
                    }
                },
                sendChildStatusBundle = {
                    buildChildSyncBundle()
                    sendChildSyncBundleToRelay()
                    ChildRemoteTestActionResult(true, "Requested child status bundle send through local dev relay.")
                },
                sendHeartbeatStatusReport = {
                    buildChildSyncBundle()
                    sendChildSyncBundleToRelay()
                    ChildRemoteTestActionResult(true, "Requested child heartbeat/status report send through local dev relay.")
                },
                startBasicDnsGuard = {
                    startBasicDnsGuardWhenAllowed()
                    ChildRemoteTestActionResult(true, "Requested Basic DNS Guard start through normal VPN permission-safe path.")
                },
                stopBasicDnsGuard = {
                    stopBasicDnsGuard()
                    ChildRemoteTestActionResult(true, "Requested Basic DNS Guard stop.")
                },
            ),
            onStatus = ::updateRemoteTestStatus,
        )
    }

    private fun stopRemoteTestMode() {
        remoteTestController.stop()
        updateRemoteTestStatus("Remote test mode stopped.")
    }

    private fun updateRemoteTestStatus(text: String) {
        if (::remoteTestOutputText.isInitialized) {
            remoteTestOutputText.text = createRemoteTestOutput(text)
        }
    }

    private fun createRemoteTestOutput(
        status: String = if (::remoteTestController.isInitialized) {
            remoteTestController.status()
        } else {
            "Remote test mode stopped."
        },
    ): String {
        return buildString {
            append("Remote test mode: ")
            append(if (::remoteTestController.isInitialized && remoteTestController.isActive()) "ACTIVE" else "STOPPED")
            append('\n')
            append(status)
            append('\n')
            append("Local dev relay only. No arbitrary UI control, shell commands, hidden control, or VPN permission bypass.")
        }
    }

    private fun copyRelayDiagnostics() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain local dev relay diagnostics", createRelayOutput()))
        updateRelayDiagnostics("${createRelayOutput()}\n\nCopied relay diagnostics.")
    }

    private fun runRelayAction(action: () -> Unit) {
        Thread {
            try {
                action()
            } catch (error: Exception) {
                runOnUiThread {
                    updateRelayDiagnostics(
                        listOf(
                            "Local dev relay error",
                            error::class.java.simpleName,
                            error.message.orEmpty(),
                            "Use trusted local network only. Production sync will use encrypted relay later.",
                        ).joinToString(separator = "\n"),
                    )
                }
            }
        }.start()
    }

    private fun updateRelayDiagnostics(text: String) {
        latestRelayDiagnostics = text
        relayBaseUrl = if (::relayBaseUrlInput.isInitialized) {
            relayBaseUrlInput.text.toString()
        } else {
            relayBaseUrl
        }
        parentRelayDeviceId = if (::parentRelayDeviceInput.isInitialized) {
            parentRelayDeviceInput.text.toString()
        } else {
            parentRelayDeviceId
        }
        if (::relayOutputText.isInitialized) {
            relayOutputText.text = createRelayOutput()
        }
        saveCurrentState()
    }

    private fun createRelayOutput(): String {
        val baseUrl = if (::relayBaseUrlInput.isInitialized) relayBaseUrlInput.text.toString() else relayBaseUrl
        val parentId = if (::parentRelayDeviceInput.isInitialized) {
            parentRelayDeviceInput.text.toString()
        } else {
            parentRelayDeviceId
        }
        return buildString {
            append("Local dev relay only. Manual send/fetch; not production secure.\n")
            append("Base URL: $baseUrl\n")
            append("Child device id: ${currentChildDeviceId()}\n")
            append("Parent device id: $parentId\n")
            append("Latest relay message id: ${latestRelayMessageId.ifBlank { "none" }}\n")
            append(latestRelayDiagnostics)
        }.trimEnd()
    }

    private fun currentChildDeviceId(): String {
        return if (::policyHandoffTargetInput.isInitialized) {
            policyHandoffTargetInput.text.toString().trim().ifBlank { childDeviceId }
        } else {
            childDeviceId
        }
    }

    private fun importParentSyncBundle(
        payload: String = parentSyncBundleInput.text.toString(),
        sourceLabel: String = "Copy/paste import",
    ) {
        latestParentSyncBundlePayload = payload
        if (::parentSyncBundleInput.isInitialized) {
            parentSyncBundleInput.setText(payload)
        }
        val result = syncBundleCodec.decode(payload)
        val bundle = result.bundle
        if (!result.accepted || bundle == null) {
            recordSyncBundleRejected("Parent sync bundle rejected: ${result.reason}")
            recordBundleInbox(syncBundleImportEvaluator.malformedForChild(result.reason), sourceLabel)
            if (::parentSyncBundleOutputText.isInitialized) {
                parentSyncBundleOutputText.text = "Parent sync bundle rejected: ${result.reason}"
            }
            saveCurrentState()
            return
        }
        val importResult = syncBundleImportEvaluator.evaluateForChild(bundle)
        if (importResult.status != SyncBundleImportStatus.ACCEPTED) {
            recordSyncBundleRejected("Parent sync bundle rejected: ${importResult.summary}")
            recordBundleInbox(importResult, sourceLabel)
            if (::parentSyncBundleOutputText.isInitialized) {
                parentSyncBundleOutputText.text = "Parent sync bundle rejected: ${importResult.summary}"
            }
            saveCurrentState()
            return
        }
        val policyPayload = bundle.payloads.firstOrNull { it.kind == SyncBundlePayloadKind.POLICY_UPDATE }?.payloadText
        if (policyPayload.isNullOrBlank()) {
            recordSyncBundleRejected("Parent sync bundle rejected: no policy update payload")
            recordBundleInbox(importResult.copy(status = SyncBundleImportStatus.MISSING_REQUIRED_PAYLOAD), sourceLabel)
            if (::parentSyncBundleOutputText.isInitialized) {
                parentSyncBundleOutputText.text = "Parent sync bundle rejected: no policy update payload"
            }
            saveCurrentState()
            return
        }
        policyHandoffPayloadInput.setText(policyPayload)
        applyDebugPolicyUpdate()
        val accepted = policyHandoffResult?.accepted == true
        if (accepted) {
            recordAudit(
                type = AuditEntryType.POLICY_APPLIED_FROM_BUNDLE,
                severity = AuditSeverity.INFO,
                title = "Parent sync bundle imported",
                detail = "Parent-to-child sync bundle imported and verified policy update accepted.",
            )
            recordAlert(
                type = AlertType.DIAGNOSTICS_GENERATED,
                severity = AlertSeverity.INFO,
                title = "Parent sync bundle imported",
                detail = "Verified policy update was accepted from local sync bundle.",
                sourceLabel = "Sync bundle",
            )
            recordBundleInbox(importResult, sourceLabel, appliedPolicyVersion = currentPolicyVersion)
            if (::parentSyncBundleOutputText.isInitialized) {
                parentSyncBundleOutputText.text = createParentSyncBundleImportOutput(bundle, "Policy update accepted")
            }
        } else {
            recordSyncBundleRejected("Parent sync bundle policy update rejected")
            recordAudit(
                type = AuditEntryType.POLICY_REJECTED_FROM_BUNDLE,
                severity = AuditSeverity.WARNING,
                title = "Parent sync bundle policy rejected",
                detail = "Parent-to-child sync bundle imported but policy verification rejected it.",
            )
            recordBundleInbox(importResult.copy(status = SyncBundleImportStatus.REJECTED), sourceLabel)
            if (::parentSyncBundleOutputText.isInitialized) {
                parentSyncBundleOutputText.text = createParentSyncBundleImportOutput(bundle, "Policy update rejected")
            }
        }
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun clearParentSyncBundleImport() {
        latestParentSyncBundlePayload = null
        parentSyncBundleInput.setText("")
        parentSyncBundleOutputText.text = "No parent sync bundle imported yet"
        saveCurrentState()
    }

    private fun handleSharedTextIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") {
            return
        }
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (sharedText.isBlank()) {
            return
        }
        importParentSyncBundle(sharedText, sourceLabel = "Android share import")
    }

    private fun recordBundleInbox(
        result: SyncBundleImportResult,
        sourceLabel: String,
        appliedPolicyVersion: String = "none",
    ) {
        val now = System.currentTimeMillis()
        val entry = ChildBundleInboxEntry(
            id = "${now}-${result.status}",
            importedAtMillis = now,
            status = result.status.name,
            sourceDeviceId = result.sourceDeviceId?.value ?: sourceLabel,
            targetDeviceId = result.targetDeviceId?.value ?: "unspecified",
            summary = result.summary,
            payloadLabels = result.payloadLabels.take(12),
            appliedPolicyVersion = appliedPolicyVersion,
        )
        bundleInbox = (listOf(entry) + bundleInbox).take(ChildBundleInboxStore.MAX_ENTRIES)
        bundleInboxStore.save(bundleInbox)
        if (::bundleInboxText.isInitialized) {
            bundleInboxText.text = createBundleInboxDisplay()
        }
    }

    private fun createBundleInboxDisplay(): String {
        if (bundleInbox.isEmpty()) {
            return "No bundle inbox entries yet\nLocal/debug bundle import history only."
        }
        return buildString {
            append("Local/debug bundle import history only.\n")
            bundleInbox.take(8).forEach { entry ->
                append("${entry.importedAtMillis} / ${entry.status}\n")
                append("Source: ${entry.sourceDeviceId}\n")
                append("Target: ${entry.targetDeviceId}\n")
                append("${entry.summary}\n")
                append("Applied policy version: ${entry.appliedPolicyVersion}\n")
                if (entry.payloadLabels.isNotEmpty()) {
                    append("Payloads: ${entry.payloadLabels.joinToString(separator = ", ")}\n")
                }
            }
        }.trimEnd()
    }

    private fun copyLatestBundleInboxSummary() {
        val latest = bundleInbox.firstOrNull() ?: return
        val summary = listOf(
            "Vordain Guard local debug bundle inbox summary",
            "${latest.importedAtMillis} / ${latest.status}",
            "Source: ${latest.sourceDeviceId}",
            "Target: ${latest.targetDeviceId}",
            latest.summary,
            "Applied policy version: ${latest.appliedPolicyVersion}",
            latest.payloadLabels.joinToString(separator = ", "),
        ).joinToString(separator = "\n")
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain bundle inbox summary", summary))
        bundleInboxText.text = "${createBundleInboxDisplay()}\n\nCopied latest bundle summary."
    }

    private fun clearBundleInbox() {
        bundleInbox = emptyList()
        bundleInboxStore.save(bundleInbox)
        bundleInboxText.text = createBundleInboxDisplay()
    }

    private fun recordSyncBundleRejected(reason: String) {
        recordAudit(
            type = AuditEntryType.SYNC_BUNDLE_REJECTED,
            severity = AuditSeverity.WARNING,
            title = "Sync bundle rejected",
            detail = reason,
        )
        recordAlert(
            type = AlertType.POLICY_REJECTED,
            severity = AlertSeverity.HIGH,
            title = "Sync bundle rejected",
            detail = reason,
            sourceLabel = "Sync bundle",
        )
    }

    private fun createChildSyncBundleOutput(): String {
        val payload = latestChildSyncBundlePayload
        val decoded = payload?.let(syncBundleCodec::decode)
        val bundle = decoded?.bundle
        if (bundle == null) {
            return "No child sync bundle built yet\nLocal debug bundle only.\nProduction sync will use encrypted relay later."
        }
        return buildString {
            append("Bundle id: ${bundle.bundleId}\n")
            append("Direction: ${bundle.direction}\n")
            append("Created at: ${bundle.createdAtMillis}\n")
            append("Payloads: ${bundle.payloads.size}\n")
            bundle.payloads.forEach { syncPayload ->
                append("- ${syncPayload.kind}: ${syncPayload.label}\n")
            }
            append(bundle.warningText)
        }
    }

    private fun createParentSyncBundleImportOutput(
        bundle: SyncBundle,
        resultLabel: String,
    ): String {
        return buildString {
            append("Import result: $resultLabel\n")
            append("Bundle id: ${bundle.bundleId}\n")
            append("Source: ${bundle.sourceDeviceId.value}\n")
            append("Target: ${bundle.targetDeviceId?.value ?: "unspecified"}\n")
            append("Payload labels:\n")
            bundle.payloads.forEach { syncPayload ->
                append("- ${syncPayload.kind}: ${syncPayload.label}\n")
            }
            append("Policy payloads are verified before use.\n")
            append(bundle.warningText)
        }
    }

    private fun createMvpAcceptanceSummary(): String {
        val items = mutableListOf<MvpAcceptanceItem>()
        if (!latestParentSyncBundlePayload.isNullOrBlank()) {
            items += MvpAcceptanceItem(MvpAcceptanceStep.CHILD_IMPORTS_SYNC_BUNDLE, MvpAcceptanceStatus.DONE)
        }
        if (currentPolicySource == "Verified debug policy") {
            items += MvpAcceptanceItem(MvpAcceptanceStep.CHILD_APPLIES_VERIFIED_POLICY, MvpAcceptanceStatus.DONE)
        }
        if (hardeningSetupSnapshot != null) {
            items += MvpAcceptanceItem(MvpAcceptanceStep.CHILD_COMPLETES_HARDENING_REVIEW, MvpAcceptanceStatus.DONE)
        }
        if (
            shellStatus == ChildVpnSmokeLabels.STATUS_BASIC_DNS_GUARD_ACTIVE ||
            shellStatus == ChildVpnSmokeLabels.STATUS_BASIC_DNS_GUARD_STARTING
        ) {
            items += MvpAcceptanceItem(MvpAcceptanceStep.CHILD_STARTS_BASIC_DNS_GUARD, MvpAcceptanceStatus.DONE)
        }
        if (!latestChildSyncBundlePayload.isNullOrBlank()) {
            items += MvpAcceptanceItem(MvpAcceptanceStep.CHILD_EXPORTS_STATUS_BUNDLE, MvpAcceptanceStatus.DONE)
        }
        if (relayBaseUrl.isNotBlank()) {
            items += MvpAcceptanceItem(MvpAcceptanceStep.DEV_RELAY_RUNNING, MvpAcceptanceStatus.DONE)
        }
        if (latestRelayDiagnostics.contains("Imported parent bundles:", ignoreCase = true)) {
            items += MvpAcceptanceItem(MvpAcceptanceStep.CHILD_FETCHED_POLICY_FROM_RELAY, MvpAcceptanceStatus.DONE)
        }
        if (latestRelayDiagnostics.contains("send", ignoreCase = true)) {
            items += MvpAcceptanceItem(MvpAcceptanceStep.CHILD_SENT_STATUS_VIA_RELAY, MvpAcceptanceStatus.DONE)
        }
        val summary = mvpAcceptanceChecklist.summarize(items)
        return listOf(
            "Local MVP acceptance checklist: ${summary.completedCount}/${summary.totalCount} done",
            "Next recommended step: ${summary.nextRecommendedStep?.toDisplayLabel() ?: "Complete"}",
            "Local debug only. Production sync will use encrypted relay later.",
        ).joinToString(separator = "\n")
    }

    private fun recordAudit(
        type: AuditEntryType,
        severity: AuditSeverity,
        title: String,
        detail: String,
    ) {
        if (!::auditStore.isInitialized) {
            return
        }
        val now = System.currentTimeMillis()
        auditTimeline = auditTimelineReducer.append(
            timeline = auditTimeline,
            entry = AuditEntry(
                id = "$now-${type.name}",
                type = type,
                severity = severity,
                occurredAtMillis = now,
                title = title,
                detail = detail,
                deviceId = DeviceId(childDeviceId),
            ),
            maxEntries = AUDIT_MAX_ENTRIES,
        )
        auditStore.save(auditTimeline)
        if (::auditTimelineText.isInitialized) {
            auditTimelineText.text = createAuditTimelineDisplay()
        }
    }

    private fun createAuditTimelineDisplay(): String {
        val entries = auditTimelineReducer.latest(auditTimeline, AUDIT_DISPLAY_COUNT)
        if (entries.isEmpty()) {
            return "No local audit entries yet\nLocal explicit app-action timeline only.\nNo web history or packet logs."
        }
        return buildString {
            append("Local explicit app-action timeline only.\n")
            append("No web history or packet logs.\n")
            entries.forEach { entry ->
                append("${entry.occurredAtMillis} / ${entry.severity} / ${entry.type}\n")
                append("${entry.title}: ${entry.detail}\n")
            }
        }.trimEnd()
    }

    private fun shareEnvelope(
        kind: VordainDebugPayloadKind,
        title: String,
        payload: String,
    ) {
        if (payload.isBlank()) {
            return
        }
        val envelope = debugPayloadEnvelopeCodec.encode(
            VordainDebugPayloadEnvelope(
                kind = kind,
                version = 1,
                createdAtMillis = System.currentTimeMillis(),
                payloadText = "Local debug export\n$payload",
            ),
        )
        shareText(title = title, text = envelope)
    }

    private fun shareText(
        title: String,
        text: String,
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Vordain Guard local debug bundle")
        }
        startActivity(Intent.createChooser(intent, title))
    }

    private fun refreshDiagnosticsViews() {
        if (!::vpnPermissionText.isInitialized) {
            return
        }
        vpnPermissionText.text = "${ChildVpnSmokeLabels.VPN_PERMISSION_PREFIX}: $vpnPermissionStatus"
        lastCommandText.text = "${ChildVpnSmokeLabels.LAST_COMMAND_PREFIX}: $lastCommand"
        shellStatusText.text = "${ChildVpnSmokeLabels.SHELL_STATUS_PREFIX}: $shellStatus"
        setupChecklistText.text = createSetupChecklistDisplay(createSetupChecklist())
        if (::hardeningSetupText.isInitialized) {
            hardeningSetupText.text = createHardeningSetupDisplay(currentHardeningSetupSnapshot())
        }
        if (::childSecurityStatusText.isInitialized) {
            childSecurityStatusText.text = createChildSecurityStatusDisplay(
                latestChildSecurityStatusReport ?: currentChildSecurityStatusReport(),
            )
        }
        if (::bypassRiskText.isInitialized) {
            bypassRiskText.text = createBypassRiskDisplay()
        }
        localEventsText.text = if (localDebugEvents.isEmpty()) {
            "No local debug events"
        } else {
            localDebugEvents.joinToString(separator = "\n")
        }
        if (::diagnosticsText.isInitialized && diagnosticsText.text != ChildVpnSmokeLabels.DIAGNOSTICS_COPIED) {
            diagnosticsText.text = diagnosticsFormatter.format(
                diagnostics = createDiagnostics(),
                state = createDashboardState(),
            )
        }
        if (::labCaptureText.isInitialized) {
            labCaptureText.text = createLabCaptureDisplay(LabCaptureDebugStatus.snapshot())
        }
        if (::basicDnsGuardText.isInitialized) {
            basicDnsGuardText.text = createBasicDnsGuardDisplay()
        }
        if (::auditTimelineText.isInitialized) {
            auditTimelineText.text = createAuditTimelineDisplay()
        }
    }

    private fun createBasicDnsGuardDisplay(): String {
        val report = currentChildSecurityStatusReport()
        val stats = LabCaptureDebugStatus.snapshot()
        val readiness = currentDnsOnlyReadinessResult()
        val hardening = currentHardeningSetupSnapshot()
        val bypassSummary = currentBypassRiskSummary()
        val heartbeatSnapshot = BasicDnsGuardHeartbeatDebugStatus.snapshot()
        val runtimeSnapshot = VpnRuntimeDebugStatus.snapshot()
        return listOf(
            "Overall local MVP status: ${report.overallStatus.toDisplayLabel()}",
            "Basic DNS Guard mode: ${basicDnsGuardModeLabel()}",
            "Runtime state: ${runtimeSnapshot.statusLabel}",
            "Runtime updated: ${runtimeSnapshot.updatedAtMillis}",
            "Descriptor established: ${runtimeSnapshot.descriptorEstablished}",
            "DNS loop running: ${runtimeSnapshot.dnsLoopRunning}",
            "Restart/state check: ${runtimeMismatchLabel(runtimeSnapshot.sessionState)}",
            "Heartbeat: ${heartbeatSnapshot.statusLabel}",
            "Active critical alerts: ${childAlertReducer.activeCriticalCount(childAlertTimeline)}",
            "Readiness: ${readiness.status.toBasicDnsDisplayLabel()} - ${readiness.reason}",
            "Active policy source: $currentPolicySource",
            "Active policy version: $currentPolicyVersion",
            "Active policy preset: ${currentPolicyDisplayLabel ?: currentPolicyPresetName ?: "unspecified"}",
            "Always-on VPN: ${hardening.itemFor(HardeningSetupStep.VPN_ALWAYS_ON).status.toDisplayLabel()}",
            "Block without VPN: ${hardening.itemFor(HardeningSetupStep.BLOCK_WITHOUT_VPN).status.toDisplayLabel()}",
            "Settings/App Lock: ${hardening.itemFor(HardeningSetupStep.SETTINGS_APP_LOCK).status.toDisplayLabel()}",
            "Private DNS reviewed: ${currentBypassRiskItems().firstOrNull { it.category == BypassRiskCategory.PRIVATE_DNS }?.status?.toDisplayLabel() ?: "Unknown"}",
            "Alternate VPN/proxy reviewed: ${currentBypassRiskItems().firstOrNull { it.category == BypassRiskCategory.ALTERNATE_VPN_APP }?.status?.toDisplayLabel() ?: "Unknown"} / ${currentBypassRiskItems().firstOrNull { it.category == BypassRiskCategory.PROXY_APP }?.status?.toDisplayLabel() ?: "Unknown"}",
            "Bypass risk: ${bypassSummary.overallStatus.toDisplayLabel()}",
            "Blocked DNS: ${stats.dnsBlockedResponseCount}",
            "Allowed DNS forwarded: ${stats.dnsAllowedForwardedCount}",
            "Encrypted DNS blocked: ${stats.encryptedDnsBlockedCount}",
            "DNS failures: ${stats.dnsAllowedForwardFailureCount + stats.dnsResponseWriteFailureCount}",
            ChildVpnSmokeLabels.BASIC_DNS_NON_DNS,
            ChildVpnSmokeLabels.BASIC_DNS_NOT_FULL,
            ChildVpnSmokeLabels.DNS_ONLY_HARDENING,
        ).joinToString(separator = "\n")
    }

    private fun basicDnsGuardModeLabel(): String {
        val runtime = VpnRuntimeDebugStatus.snapshot()
        if (runtime.operatingMode == VordainOperatingMode.BASIC_DNS_GUARD) {
            return when (runtime.sessionState) {
                VpnRuntimeSessionState.STARTING -> "Starting"
                VpnRuntimeSessionState.ESTABLISHED,
                VpnRuntimeSessionState.RUNNING -> "Running"
                VpnRuntimeSessionState.STOPPED -> "Stopped"
                VpnRuntimeSessionState.REVOKED,
                VpnRuntimeSessionState.ERROR -> "Needs attention"
                VpnRuntimeSessionState.STALE,
                VpnRuntimeSessionState.UNKNOWN -> "Unknown"
                VpnRuntimeSessionState.IDLE -> "Idle"
            }
        }
        return when (shellStatus) {
            ChildVpnSmokeLabels.STATUS_BASIC_DNS_GUARD_ACTIVE -> "Running"
            ChildVpnSmokeLabels.STATUS_BASIC_DNS_GUARD_STARTING -> "Starting"
            ChildVpnSmokeLabels.STATUS_STOPPED -> "Stopped"
            ChildVpnSmokeLabels.STATUS_PERMISSION_REQUIRED -> "Needs attention"
            else -> "Idle"
        }
    }

    private fun runtimeMismatchLabel(state: VpnRuntimeSessionState): String {
        return if (expectedBasicDnsGuardRunning && state != VpnRuntimeSessionState.RUNNING) {
            "Needs attention - app expected Basic DNS Guard but runtime is ${state.name}"
        } else {
            "Confirmed from local runtime snapshot"
        }
    }

    private fun copyBasicDnsGuardDiagnostics() {
        val payload = basicDnsGuardDiagnosticsCodec.encode(currentBasicDnsGuardDiagnosticsReport())
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain Basic DNS Guard diagnostics", payload))
        recordAudit(
            type = AuditEntryType.BASIC_DNS_DIAGNOSTICS_COPIED,
            severity = AuditSeverity.INFO,
            title = "Basic DNS diagnostics copied",
            detail = "Local Basic DNS Guard diagnostics were copied by user action.",
        )
        basicDnsGuardText.text = "${createBasicDnsGuardDisplay()}\n\nCopied Basic DNS diagnostics."
        saveCurrentState()
    }

    private fun currentBasicDnsGuardDiagnosticsReport(): BasicDnsGuardDiagnosticsReport {
        val stats = LabCaptureDebugStatus.snapshot()
        val readiness = currentDnsOnlyReadinessResult()
        val heartbeatSnapshot = BasicDnsGuardHeartbeatDebugStatus.snapshot()
        val runtimeSnapshot = VpnRuntimeDebugStatus.snapshot()
        return BasicDnsGuardDiagnosticsReport(
            childDeviceId = DeviceId(childDeviceId),
            generatedAtMillis = System.currentTimeMillis(),
            mode = ChildSecurityActiveMode.BASIC_DNS_GUARD,
            activePolicySource = currentPolicySource,
            activePolicyVersion = currentPolicyVersion.takeIf { currentPolicySource.startsWith("Verified") },
            activePreset = currentPolicyDisplayLabel ?: currentPolicyPresetName,
            readinessStatus = readiness.status.toBasicDnsDisplayLabel(),
            hardeningSummary = currentHardeningSetupSnapshot().summaryStatus.toDisplayLabel(),
            bypassRiskSummary = currentBypassRiskSummary().overallStatus.toDisplayLabel(),
            dnsBlockedCount = stats.dnsBlockedResponseCount,
            dnsAllowedForwardedCount = stats.dnsAllowedForwardedCount,
            encryptedDnsBlockedCount = stats.encryptedDnsBlockedCount,
            dnsFailureCount = stats.dnsAllowedForwardFailureCount + stats.dnsResponseWriteFailureCount,
            activeCriticalAlertCount = childAlertReducer.activeCriticalCount(childAlertTimeline),
            latestAlertSeverity = childAlertTimeline.alerts.firstOrNull()?.severity?.name,
            heartbeatStatusLabel = heartbeatSnapshot.statusLabel,
            lastHeartbeatAtMillis = heartbeatSnapshot.lastTickAtMillis,
            alertSummaryLabel = createAlertSummaryLabel(),
        )
    }

    private fun createDiagnostics(): ChildVpnSmokeDiagnostics {
        return ChildVpnSmokeDiagnostics(
            buildLabel = ChildVpnSmokeLabels.BUILD_LABEL,
            androidSdkVersion = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            vpnPermissionStatus = vpnPermissionStatus,
            lastCommand = lastCommand,
            shellStatus = shellStatus,
            warning = ChildVpnSmokeLabels.WARNING,
            createdAtMillis = System.currentTimeMillis(),
        )
    }

    private fun createDashboardState(): ChildDebugDashboardState {
        return ChildDebugDashboardState(
            vpnPermissionStatus = vpnPermissionStatus,
            lastCommand = lastCommand,
            shellStatus = shellStatus,
            setupChecklist = createSetupChecklist(),
            policyResult = policyResult,
            policyHandoffResult = policyHandoffResult,
            currentPolicyVersion = currentPolicyVersion,
            compatibilityResult = compatibilityResult,
            reviewResult = reviewResult,
            localEvents = localDebugEvents.toList(),
            labCaptureStats = LabCaptureDebugStatus.snapshot(),
            hardeningSetupSnapshot = currentHardeningSetupSnapshot(),
        )
    }

    private fun createLabCaptureDisplay(stats: LabTrafficObservationStats): String {
        val watchdogLabel = LabCaptureDebugStatus.watchdogLabel()
        val lines = mutableListOf(
            "Lab mode status: $shellStatus",
            "Lab capture mode: ${stats.activeModeLabel}",
            "Lab auto-stop watchdog: $watchdogLabel",
            "Packet count: ${stats.packetCount}",
            "Byte count: ${stats.byteCount}",
            "Active lab policy source: $currentPolicySource",
            "Active policy version: $currentPolicyVersion",
            "Active policy preset: ${currentPolicyDisplayLabel ?: currentPolicyPresetName ?: "unspecified"}",
            "Encrypted DNS resolver blocking: ${if (currentPolicyBlockEncryptedDnsResolvers) "enabled" else "not requested"}",
            "Latest allow/block counts: ${latestAllowDomainsCsv.countCsvEntries()}/${latestBlockDomainsCsv.countCsvEntries()}",
            "Upstream DNS: ${stats.dnsUpstreamHost}:${stats.dnsUpstreamPort}",
            "DNS packet count: ${stats.dnsPacketCount}",
            "DNS query count: ${stats.dnsQueryCount}",
            "DNS blocked response count: ${stats.dnsBlockedResponseCount}",
            "DNS allowed-but-dropped count: ${stats.dnsAllowedDroppedCount}",
            "DNS allowed forwarded count: ${stats.dnsAllowedForwardedCount}",
            "DNS allowed forward failures: ${stats.dnsAllowedForwardFailureCount}",
            "DNS allowed forward timeouts: ${stats.dnsAllowedForwardTimeoutCount}",
            "DNS alert-only dropped count: ${stats.dnsAlertDroppedCount}",
            "DNS response write successes: ${stats.dnsResponseWriteSuccessCount}",
            "DNS response write failures: ${stats.dnsResponseWriteFailureCount}",
            "Encrypted-DNS resolver blocks: ${stats.encryptedDnsBlockedCount}",
            "DNS-only packet count: ${stats.dnsOnlyLabPacketCount}",
            "DNS-only unexpected non-DNS packets: ${stats.dnsOnlyUnexpectedNonDnsCount}",
            "DNS-only blocked responses: ${stats.dnsOnlyBlockedResponseCount}",
            "DNS-only allowed forwarded: ${stats.dnsOnlyAllowedForwardedCount}",
            "DNS-only allowed failures: ${stats.dnsOnlyAllowedForwardFailureCount}",
            "Allowed/block/alert counts: ${stats.allowedDomainCount}/${stats.blockedDomainCount}/${stats.alertOnlyDomainCount}",
            "Malformed packet/DNS counts: ${stats.malformedPacketCount}/${stats.malformedDnsCount}",
            "Last packet summary: ${stats.lastPacketSummary ?: "none"}",
            ChildVpnSmokeLabels.LAB_LOCAL_ONLY,
            ChildVpnSmokeLabels.LAB_DNS_LOCAL_ONLY,
            ChildVpnSmokeLabels.LAB_DNS_SINKHOLE,
            ChildVpnSmokeLabels.LAB_ALLOWED_DROPPED,
            ChildVpnSmokeLabels.LAB_ALLOWED_NON_DNS_DROPPED,
            ChildVpnSmokeLabels.DNS_ONLY_NON_DNS,
            ChildVpnSmokeLabels.DNS_ONLY_DOH_WARNING,
            ChildVpnSmokeLabels.LAB_INTERNET_MAY_NOT_WORK,
            ChildVpnSmokeLabels.LAB_NOT_FULL_PROTECTION,
            ChildVpnSmokeLabels.LAB_AUTO_STOP,
        )
        if (stats.recentDnsObservations.isNotEmpty()) {
            lines += "Recent DNS observations:"
            stats.recentDnsObservations.forEach { observation ->
                lines += observation.summary()
            }
        }
        return lines.joinToString(separator = "\n")
    }

    private fun createSetupChecklist(): VpnSetupChecklist {
        val hardening = currentHardeningSetupSnapshot()
        return VpnSetupChecklist(
            vpnPermission = hardening.itemFor(HardeningSetupStep.VPN_PERMISSION).status.toSetupCheckState(),
            alwaysOnVpn = hardening.itemFor(HardeningSetupStep.VPN_ALWAYS_ON).status.toSetupCheckState(),
            blockConnectionsWithoutVpn = hardening.itemFor(HardeningSetupStep.BLOCK_WITHOUT_VPN).status.toSetupCheckState(),
            batteryOptimizationWarning = hardening.itemFor(HardeningSetupStep.BATTERY_OPTIMIZATION).status.toSetupCheckState(),
            appProtection = hardening.itemFor(HardeningSetupStep.SETTINGS_APP_LOCK).status.toSetupCheckState(),
        )
    }

    private fun createSetupChecklistDisplay(checklist: VpnSetupChecklist): String {
        return listOf(
            "VPN permission: ${checklist.vpnPermission}",
            "Start shell: $shellStatus",
            "Foreground notification: $setupForegroundNotificationStatus - manual check",
            "Always-on VPN: ${checklist.alwaysOnVpn} - manual check",
            "Block connections without VPN: ${checklist.blockConnectionsWithoutVpn} - manual check",
            "Battery optimization: ${checklist.batteryOptimizationWarning} - manual check",
            "App protection: ${checklist.appProtection}",
        ).joinToString(separator = "\n")
    }

    private fun createHardeningSetupDisplay(snapshot: HardeningSetupSnapshot): String {
        val lines = mutableListOf(
            "Hardening summary: ${snapshot.summaryStatus.toDisplayLabel()}",
            snapshot.warningText,
            "Maintenance window: ${snapshot.activeMaintenanceWindow?.windowId ?: "none"}",
            "Compromise signal: ${snapshot.latestPinCompromiseSignal.toDisplayLabel()}",
            "Always-on VPN: Open VPN settings, tap Vordain Guard, enable Always-on VPN.",
            "Block without VPN: In the Vordain VPN settings, enable Block connections without VPN.",
            "Settings/App Lock: If this device has App Lock, lock Settings and VPN settings behind the parent PIN.",
            "Screen pinning: If App Lock is unavailable, enable PIN required to unpin.",
        )
        HardeningSetupStep.entries.forEach { step ->
            val item = snapshot.itemFor(step)
            lines += "${step.toDisplayLabel()}: ${item.status.toDisplayLabel()} / ${item.evidenceType.toDisplayLabel()} / ${item.note ?: "No note"}"
        }
        return lines.joinToString(separator = "\n")
    }

    private fun generateChildSecurityStatusReport() {
        val report = currentChildSecurityStatusReport()
        latestChildSecurityStatusReport = report
        latestChildSecurityStatusReportPayload = childSecurityReportCodec.encode(report)
        childSecurityStatusText.text = createChildSecurityStatusDisplay(report)
        recordAudit(
            type = AuditEntryType.STATUS_REPORT_GENERATED,
            severity = AuditSeverity.INFO,
            title = "Child status report generated",
            detail = "Parent-visible child security status report was generated.",
        )
        saveCurrentState()
    }

    private fun copyChildSecurityStatusReport() {
        if (latestChildSecurityStatusReportPayload.isNullOrBlank()) {
            generateChildSecurityStatusReport()
        }
        val payload = latestChildSecurityStatusReportPayload.orEmpty()
        if (payload.isBlank()) {
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain child security status report", payload))
        childSecurityStatusText.text = "${createChildSecurityStatusDisplay(currentChildSecurityStatusReport())}\n\nCopied status report."
        saveCurrentState()
    }

    private fun shareChildSecurityStatusReport() {
        if (latestChildSecurityStatusReportPayload.isNullOrBlank()) {
            generateChildSecurityStatusReport()
        }
        shareEnvelope(
            kind = VordainDebugPayloadKind.CHILD_STATUS_REPORT,
            title = "Share Vordain child status report",
            payload = latestChildSecurityStatusReportPayload.orEmpty(),
        )
    }

    private fun refreshChildSecurityStatusReport() {
        generateChildSecurityStatusReport()
    }

    private fun currentChildSecurityStatusReport(): ChildSecurityStatusReport {
        val hardening = currentHardeningSetupSnapshot()
        val bypassSummary = currentBypassRiskSummary()
        val readiness = currentDnsOnlyReadinessResult()
        val settingsLockConfirmed = hardening.isConfirmed(HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY) ||
            hardening.isConfirmed(HardeningSetupStep.SETTINGS_APP_LOCK)
        val adbDisabledConfirmed = hardening.isConfirmed(HardeningSetupStep.USB_DEBUGGING_DISABLED) &&
            hardening.isConfirmed(HardeningSetupStep.WIRELESS_DEBUGGING_DISABLED)
        val noUnrestrictedProfilesConfirmed = hardening.isConfirmed(HardeningSetupStep.NO_UNRESTRICTED_SECONDARY_USERS) &&
            hardening.isConfirmed(HardeningSetupStep.NO_UNRESTRICTED_WORK_PROFILE)
        val labCaptureActive = shellStatus == ChildVpnSmokeLabels.STATUS_LAB_CAPTURE_ACTIVE ||
            shellStatus == ChildVpnSmokeLabels.STATUS_LAB_CAPTURE_STARTING ||
            shellStatus == ChildVpnSmokeLabels.STATUS_DNS_ONLY_LAB_ACTIVE ||
            shellStatus == ChildVpnSmokeLabels.STATUS_DNS_ONLY_LAB_STARTING ||
            shellStatus == ChildVpnSmokeLabels.STATUS_BASIC_DNS_GUARD_ACTIVE ||
            shellStatus == ChildVpnSmokeLabels.STATUS_BASIC_DNS_GUARD_STARTING
        val labStats = LabCaptureDebugStatus.snapshot()
        val heartbeatSnapshot = BasicDnsGuardHeartbeatDebugStatus.snapshot()
        val heartbeatFresh = heartbeatSnapshot.status == BasicDnsGuardHeartbeatStatus.FRESH
        val runtimeSnapshot = VpnRuntimeDebugStatus.snapshot()
        val activeMode = if (
            runtimeSnapshot.operatingMode == VordainOperatingMode.BASIC_DNS_GUARD ||
            shellStatus == ChildVpnSmokeLabels.STATUS_BASIC_DNS_GUARD_ACTIVE ||
            shellStatus == ChildVpnSmokeLabels.STATUS_BASIC_DNS_GUARD_STARTING
        ) {
            ChildSecurityActiveMode.BASIC_DNS_GUARD
        } else if (
            runtimeSnapshot.operatingMode == VordainOperatingMode.DNS_ONLY_LAB ||
            shellStatus == ChildVpnSmokeLabels.STATUS_DNS_ONLY_LAB_ACTIVE ||
            shellStatus == ChildVpnSmokeLabels.STATUS_DNS_ONLY_LAB_STARTING ||
            labStats.activeModeLabel == "DNS-only lab"
        ) {
            ChildSecurityActiveMode.DNS_ONLY_LAB
        } else if (runtimeSnapshot.operatingMode == VordainOperatingMode.FULL_TUNNEL_LAB || labCaptureActive) {
            ChildSecurityActiveMode.FULL_TUNNEL_LAB
        } else {
            ChildSecurityActiveMode.NONE
        }
        return childSecurityStatusEvaluator.evaluate(
            ChildSecurityStatusInput(
                childDeviceId = DeviceId(childDeviceId),
                generatedAtMillis = System.currentTimeMillis(),
                vpnPermissionConfirmed = hardening.isConfirmed(HardeningSetupStep.VPN_PERMISSION) ||
                    vpnPermissionStatus == ChildVpnSmokeLabels.PERMISSION_GRANTED,
                alwaysOnVpnConfirmed = hardening.isConfirmed(HardeningSetupStep.VPN_ALWAYS_ON),
                blockWithoutVpnConfirmed = hardening.isConfirmed(HardeningSetupStep.BLOCK_WITHOUT_VPN),
                settingsLockConfirmed = settingsLockConfirmed,
                developerOptionsDisabledConfirmed = hardening.isConfirmed(HardeningSetupStep.DEVELOPER_OPTIONS_DISABLED),
                adbDisabledConfirmed = adbDisabledConfirmed,
                noUnrestrictedProfilesConfirmed = noUnrestrictedProfilesConfirmed,
                policyVersion = currentPolicyVersion.takeIf { currentPolicySource.startsWith("Verified") },
                policyApplied = currentPolicySource.startsWith("Verified"),
                activePolicySource = currentPolicySource,
                activePolicyPreset = currentPolicyDisplayLabel ?: currentPolicyPresetName,
                encryptedDnsBlockingEnabled = currentPolicyBlockEncryptedDnsResolvers,
                proxyBlockingEnabled = policyDemo.blockKnownProxyDomains(),
                policyAllowDomainCount = policyDemo.allowDomainCount(),
                policyBlockDomainCount = policyDemo.blockDomainCount(),
                vpnSessionLabel = "${runtimeSnapshot.statusLabel} / $shellStatus",
                vpnSessionRunning = runtimeSnapshot.sessionState == VpnRuntimeSessionState.RUNNING ||
                    shellStatus == ChildVpnSmokeLabels.STATUS_SHELL_ACTIVE || labCaptureActive,
                vpnStopped = runtimeSnapshot.sessionState == VpnRuntimeSessionState.STOPPED ||
                    runtimeSnapshot.sessionState == VpnRuntimeSessionState.REVOKED ||
                    shellStatus == ChildVpnSmokeLabels.STATUS_STOPPED ||
                    shellStatus == ChildVpnSmokeLabels.STATUS_REVOKED_UNKNOWN,
                heartbeatLabel = heartbeatSnapshot.statusLabel,
                heartbeatFresh = heartbeatFresh,
                setupSummaryLabel = hardening.summaryStatus.toDisplayLabel(),
                bypassRiskLabel = "${bypassSummary.overallStatus.toDisplayLabel()} / ${readiness.status}: ${readiness.reason}",
                labCaptureActive = labCaptureActive,
                pinCompromiseSuspected = hardening.latestPinCompromiseSignal.suspected,
                activeMode = activeMode,
                dnsBlockedResponseCount = labStats.dnsBlockedResponseCount,
                dnsAllowedForwardedCount = labStats.dnsAllowedForwardedCount,
                dnsAllowedForwardFailureCount = labStats.dnsAllowedForwardFailureCount,
                activeCriticalAlertCount = childAlertReducer.activeCriticalCount(childAlertTimeline),
                latestAlertSeverity = childAlertTimeline.alerts.firstOrNull()?.severity?.name,
                heartbeatStatusLabel = heartbeatSnapshot.statusLabel,
                lastHeartbeatAtMillis = heartbeatSnapshot.lastTickAtMillis,
                alertSummaryLabel = createAlertSummaryLabel(),
            ),
        )
    }

    private fun createChildSecurityStatusDisplay(report: ChildSecurityStatusReport): String {
        return buildString {
            append("Overall status: ${report.overallStatus.toDisplayLabel()}\n")
            append("Child device id: ${report.childDeviceId.value}\n")
            append("Generated at: ${report.generatedAtMillis}\n")
            append("Policy version: ${report.policyVersion ?: "none"}\n")
            append("Active policy source: ${report.activePolicySource ?: "Unknown"}\n")
            append("Active policy preset: ${report.activePolicyPreset ?: "unspecified"}\n")
            append("Encrypted DNS blocking: ${if (report.encryptedDnsBlockingEnabled) "enabled" else "not requested"}\n")
            append("Proxy blocking: ${if (report.proxyBlockingEnabled) "enabled" else "not requested"}\n")
            append("Policy allow/block counts: ${report.policyAllowDomainCount}/${report.policyBlockDomainCount}\n")
            append("VPN/session: ${report.vpnSessionLabel ?: "Unknown"}\n")
            append("Setup summary: ${report.setupSummaryLabel ?: "Unknown"}\n")
            append("Heartbeat: ${report.heartbeatLabel ?: "Unknown"}\n")
            append("Heartbeat status: ${report.heartbeatStatusLabel ?: "Unknown"}\n")
            append("Last heartbeat at: ${report.lastHeartbeatAtMillis ?: "none"}\n")
            append("Active critical alerts: ${report.activeCriticalAlertCount}\n")
            append("Latest alert severity: ${report.latestAlertSeverity ?: "none"}\n")
            append("Alert summary: ${report.alertSummaryLabel ?: "none"}\n")
            append("Bypass risk: ${report.bypassRiskLabel ?: "Unknown"}\n")
            append("Active mode: ${report.activeMode.toDisplayLabel()}\n")
            append("DNS blocked responses: ${report.dnsBlockedResponseCount}\n")
            append("DNS allowed forwarded: ${report.dnsAllowedForwardedCount}\n")
            append("DNS allowed forward failures: ${report.dnsAllowedForwardFailureCount}\n")
            if (report.activeMode == ChildSecurityActiveMode.BASIC_DNS_GUARD ||
                report.activeMode == ChildSecurityActiveMode.DNS_ONLY_LAB
            ) {
                append("Non-DNS traffic is not inspected in DNS-only mode.\n")
            }
            append("Signals: ${report.signals.toDisplayLabels()}\n")
            append(report.warningText)
        }
    }

    private fun currentBypassRiskReport(): DebugBypassRiskReport {
        return DebugBypassRiskReport(
            childDeviceId = DeviceId(childDeviceId),
            generatedAtMillis = System.currentTimeMillis(),
            summary = currentBypassRiskSummary(),
        )
    }

    private fun createActivePolicyDisplay(): String {
        return buildString {
            append("Active policy source: $currentPolicySource\n")
            append("Active policy version: $currentPolicyVersion\n")
            append("Preset: ${currentPolicyDisplayLabel ?: currentPolicyPresetName ?: "unspecified"}\n")
            append("Allow domain count: ${policyDemo.allowDomainCount()}\n")
            append("Block domain count: ${policyDemo.blockDomainCount()}\n")
            append("Proxy blocking: ${if (policyDemo.blockKnownProxyDomains()) "enabled" else "not requested"}\n")
            append("Encrypted DNS resolver blocking: ${if (currentPolicyBlockEncryptedDnsResolvers) "enabled" else "not requested"}\n")
            append("Unknown-domain behavior: ${if (policyDemo.blockUnknownDomains()) "blocked" else "allowed unless listed"}\n")
            append("Last verification result: $lastPolicyVerificationResult\n")
            append(policyDemo.policySummary(currentPolicyVersion))
            latestAllowDomainsCsv?.let { append("\nLatest allow domains: $it") }
            latestBlockDomainsCsv?.let { append("\nLatest block domains: $it") }
            append("\nFiltering is not production-enabled yet. Not full protection.")
        }
    }

    private fun restorePersistedPolicyPayload(
        payload: String,
        appliedAtMillis: Long,
        lastKnownPolicyVersion: String?,
    ) {
        val restored = policySnapshotRestorer.restore(
            snapshot = PersistedSignedPolicySnapshot(
                encodedPayload = payload,
                appliedAtMillis = appliedAtMillis,
                expectedDeviceId = DeviceId(childDeviceId),
                lastKnownPolicyVersion = lastKnownPolicyVersion?.let(::PolicyVersion),
            ),
            currentTimeMillis = System.currentTimeMillis(),
        )
        val restoredPolicy = restored.policy
        val restoredPolicyVersion = restored.policyVersion
        if (restored.accepted && restoredPolicy != null && restoredPolicyVersion != null) {
            val decoded = debugPolicyUpdateCodec.decode(payload)
            val decodedUpdate = (decoded as? DebugPolicyUpdateCodecResult.Decoded)?.update
            policyDemo.replacePolicy(
                policy = restoredPolicy,
                blockEncryptedDnsResolvers = decodedUpdate?.blockEncryptedDnsResolvers ?: true,
            )
            currentPolicyVersion = restoredPolicyVersion.value
            latestPolicyPayload = payload
            latestPolicyAppliedAtMillis = appliedAtMillis
            latestAllowDomainsCsv = restoredPolicy.allowedDomains.toCsv()
            latestBlockDomainsCsv = restoredPolicy.blockedDomains.toCsv()
            currentPolicySource = "Verified persisted debug policy"
            currentPolicyPresetName = decodedUpdate?.presetName
            currentPolicyDisplayLabel = decodedUpdate?.policyDisplayLabel
            currentPolicyBlockEncryptedDnsResolvers = decodedUpdate?.blockEncryptedDnsResolvers ?: true
            lastPolicyVerificationResult = restored.reason.toString()
            LabCaptureDebugStatus.useVerifiedPolicy(restoredPolicy)
            recordAudit(
                type = AuditEntryType.POLICY_RESTORED,
                severity = AuditSeverity.INFO,
                title = "Policy restored",
                detail = "Verified debug DNS policy ${restoredPolicyVersion.value} was restored.",
            )
        } else {
            currentPolicySource = "Persisted policy rejected: ${restored.reason}"
            currentPolicyPresetName = null
            currentPolicyDisplayLabel = null
            currentPolicyBlockEncryptedDnsResolvers = true
            lastPolicyVerificationResult = restored.reason.toString()
            policyDemo.resetToDefault()
            LabCaptureDebugStatus.useDefaultPolicy()
            recordAudit(
                type = AuditEntryType.POLICY_PAYLOAD_REJECTED,
                severity = AuditSeverity.WARNING,
                title = "Stored policy rejected",
                detail = "Stored debug DNS policy was rejected: ${restored.reason}.",
            )
            recordAlert(
                type = AlertType.POLICY_RESTORED_FAILED,
                severity = AlertSeverity.HIGH,
                title = "Stored policy rejected",
                detail = "Stored debug DNS policy was rejected: ${restored.reason}.",
                sourceLabel = "Policy restore",
            )
        }
    }

    private fun currentBypassRiskSummary() = bypassRiskEvaluator.summarize(currentBypassRiskItems())

    private fun currentDnsOnlyReadinessResult() = dnsOnlyReadinessEvaluator.evaluate(
        DnsOnlyReadinessInput(
            vpnPermissionConfirmed = vpnPermissionStatus == ChildVpnSmokeLabels.PERMISSION_GRANTED ||
                currentHardeningSetupSnapshot().isConfirmed(HardeningSetupStep.VPN_PERMISSION),
            alwaysOnVpnConfirmed = currentHardeningSetupSnapshot().isConfirmed(HardeningSetupStep.VPN_ALWAYS_ON),
            blockWithoutVpnConfirmed = currentHardeningSetupSnapshot().isConfirmed(HardeningSetupStep.BLOCK_WITHOUT_VPN),
            settingsLockConfirmed = currentHardeningSetupSnapshot().isConfirmed(HardeningSetupStep.SETTINGS_APP_LOCK) ||
                currentHardeningSetupSnapshot().isConfirmed(HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY),
            pinCompromiseSuspected = currentHardeningSetupSnapshot().latestPinCompromiseSignal.suspected,
            activePolicyVersion = currentPolicyVersion.takeIf { currentPolicySource.startsWith("Verified") },
            dnsOnlyLabAvailable = true,
            localLabOnlyMode = true,
            bypassRiskSummary = currentBypassRiskSummary(),
        ),
    )

    private fun currentBypassRiskItems(): List<BypassRiskItem> {
        val manualItems = bypassRiskItems.associateBy { it.category }
        val hardening = currentHardeningSetupSnapshot()
        val merged = defaultBypassRiskItems().associateBy { it.category }.toMutableMap()
        merged += manualItems
        merged[BypassRiskCategory.DEVELOPER_OPTIONS] = hardeningRiskItem(
            category = BypassRiskCategory.DEVELOPER_OPTIONS,
            confirmed = hardening.isConfirmed(HardeningSetupStep.DEVELOPER_OPTIONS_DISABLED),
            severity = BypassRiskSeverity.HIGH,
            note = "Developer Options disabled check.",
        )
        merged[BypassRiskCategory.USB_DEBUGGING] = hardeningRiskItem(
            category = BypassRiskCategory.USB_DEBUGGING,
            confirmed = hardening.isConfirmed(HardeningSetupStep.USB_DEBUGGING_DISABLED),
            severity = BypassRiskSeverity.HIGH,
            note = "USB debugging disabled check.",
        )
        merged[BypassRiskCategory.WIRELESS_DEBUGGING] = hardeningRiskItem(
            category = BypassRiskCategory.WIRELESS_DEBUGGING,
            confirmed = hardening.isConfirmed(HardeningSetupStep.WIRELESS_DEBUGGING_DISABLED),
            severity = BypassRiskSeverity.HIGH,
            note = "Wireless debugging disabled check.",
        )
        merged[BypassRiskCategory.UNRESTRICTED_PROFILE] = hardeningRiskItem(
            category = BypassRiskCategory.UNRESTRICTED_PROFILE,
            confirmed = hardening.isConfirmed(HardeningSetupStep.NO_UNRESTRICTED_SECONDARY_USERS) &&
                hardening.isConfirmed(HardeningSetupStep.NO_UNRESTRICTED_WORK_PROFILE),
            severity = BypassRiskSeverity.HIGH,
            note = "Secondary user/profile review.",
        )
        return merged.values.sortedBy { it.category.ordinal }
    }

    private fun hardeningRiskItem(
        category: BypassRiskCategory,
        confirmed: Boolean,
        severity: BypassRiskSeverity,
        note: String,
    ): BypassRiskItem {
        return BypassRiskItem(
            category = category,
            status = if (confirmed) BypassRiskStatus.CONFIRMED_SAFE else BypassRiskStatus.NOT_CHECKED,
            severity = severity,
            evidenceLabel = if (confirmed) "Parent confirmation" else "Manual review needed",
            note = note,
        )
    }

    private fun defaultBypassRiskItems(): List<BypassRiskItem> {
        return listOf(
            BypassRiskItem(BypassRiskCategory.DNS_OVER_HTTPS, BypassRiskStatus.NOT_CHECKED, BypassRiskSeverity.HIGH, "Manual review needed", "Review DoH resolver app/browser settings."),
            BypassRiskItem(BypassRiskCategory.PRIVATE_DNS, BypassRiskStatus.NOT_CHECKED, BypassRiskSeverity.HIGH, "Manual review needed", "Review Android Private DNS settings."),
            BypassRiskItem(BypassRiskCategory.ALTERNATE_VPN_APP, BypassRiskStatus.NOT_CHECKED, BypassRiskSeverity.HIGH, "Manual review needed", "Review alternate VPN apps."),
            BypassRiskItem(BypassRiskCategory.PROXY_APP, BypassRiskStatus.NOT_CHECKED, BypassRiskSeverity.HIGH, "Manual review needed", "Review proxy apps."),
            BypassRiskItem(BypassRiskCategory.PRIVATE_BROWSER, BypassRiskStatus.NOT_CHECKED, BypassRiskSeverity.HIGH, "Manual review needed", "Review private browsers."),
            BypassRiskItem(BypassRiskCategory.DEVELOPER_OPTIONS, BypassRiskStatus.NOT_CHECKED, BypassRiskSeverity.HIGH, "Manual review needed", "Confirm Developer Options are off."),
            BypassRiskItem(BypassRiskCategory.USB_DEBUGGING, BypassRiskStatus.NOT_CHECKED, BypassRiskSeverity.HIGH, "Manual review needed", "Confirm USB debugging is off."),
            BypassRiskItem(BypassRiskCategory.WIRELESS_DEBUGGING, BypassRiskStatus.NOT_CHECKED, BypassRiskSeverity.HIGH, "Manual review needed", "Confirm wireless debugging is off."),
            BypassRiskItem(BypassRiskCategory.UNRESTRICTED_PROFILE, BypassRiskStatus.NOT_CHECKED, BypassRiskSeverity.HIGH, "Manual review needed", "Review secondary users and profiles."),
            BypassRiskItem(BypassRiskCategory.DIRECT_IP_ACCESS, BypassRiskStatus.NEEDS_REVIEW, BypassRiskSeverity.MEDIUM, "Limitation notice", "DNS-only mode cannot cover every direct-IP path."),
        )
    }

    private fun createBypassRiskDisplay(): String {
        val summary = currentBypassRiskSummary()
        val readiness = currentDnsOnlyReadinessResult()
        return buildString {
            append("Bypass risk status: ${summary.overallStatus.toDisplayLabel()}\n")
            append("Highest severity: ${summary.highestSeverity}\n")
            append("DNS-only readiness: ${readiness.status} - ${readiness.reason}\n")
            append(summary.warningText)
            append('\n')
            currentBypassRiskItems().forEach { item ->
                append("${item.category.toDisplayLabel()}: ${item.status.toDisplayLabel()} / ${item.severity} / ${item.evidenceLabel} / ${item.note}\n")
            }
        }.trimEnd()
    }

    private fun restoreState(snapshot: ChildDebugStateSnapshot) {
        childDeviceId = snapshot.childDeviceId.ifBlank { ChildDebugStateSnapshot.DEFAULT_CHILD_DEVICE_ID }
        latestPolicyPayload = snapshot.latestPolicyPayload
        latestPolicyAppliedAtMillis = snapshot.latestPolicyAppliedAtMillis
        currentPolicyVersion = snapshot.latestPolicyVersion ?: currentPolicyVersion
        currentPolicyPresetName = snapshot.latestPolicyPresetName
        currentPolicyDisplayLabel = snapshot.latestPolicyDisplayLabel
        currentPolicyBlockEncryptedDnsResolvers = snapshot.latestPolicyBlockEncryptedDnsResolvers
        latestAllowDomainsCsv = snapshot.latestAllowDomainsCsv
        latestBlockDomainsCsv = snapshot.latestBlockDomainsCsv
        childDisplayName = snapshot.childDisplayName
        childFingerprint = snapshot.childFingerprint
        vpnPermissionStatus = snapshot.vpnPermissionStatusLabel
        lastCommand = snapshot.lastVpnCommandLabel
        shellStatus = snapshot.shellStatusLabel
        setupForegroundNotificationStatus = snapshot.setupForegroundNotificationStatus.toSetupCheckState()
        setupAlwaysOnVpnStatus = snapshot.setupAlwaysOnVpnStatus.toSetupCheckState()
        setupBlockWithoutVpnStatus = snapshot.setupBlockWithoutVpnStatus.toSetupCheckState()
        setupBatteryOptimizationStatus = snapshot.setupBatteryOptimizationStatus.toSetupCheckState()
        lastDiagnosticsText = snapshot.lastDiagnosticsText
        latestPairingInvitePayload = snapshot.latestPairingInvitePayload
        latestPairingAcceptancePayload = snapshot.latestPairingAcceptancePayload
        acceptedParentSummary = snapshot.acceptedParentSummary
        latestHardeningSetupReportPayload = snapshot.latestHardeningSetupReportPayload
        latestChildSecurityStatusReportPayload = snapshot.latestChildSecurityStatusReportPayload
        latestBypassRiskReportPayload = snapshot.latestBypassRiskReportPayload
        latestChildSyncBundlePayload = snapshot.latestChildSyncBundlePayload
        latestParentSyncBundlePayload = snapshot.latestParentSyncBundlePayload
        relayBaseUrl = snapshot.relayBaseUrl
        parentRelayDeviceId = snapshot.parentRelayDeviceId
        latestRelayMessageId = snapshot.latestRelayMessageId.orEmpty()
        latestRelayDiagnostics = snapshot.latestRelayDiagnostics ?: latestRelayDiagnostics
        expectedBasicDnsGuardRunning = snapshot.expectedBasicDnsGuardRunning
        latestVpnRuntimeStatus = snapshot.latestVpnRuntimeStatus
        latestHeartbeatStatus = snapshot.latestHeartbeatStatus
        bypassRiskItems = restoreBypassRiskItems(snapshot.latestBypassRiskReportPayload)
        hardeningSetupSnapshot = restoreHardeningSetupSnapshot(snapshot.latestHardeningSetupReportPayload)
        latestChildSecurityStatusReport = restoreChildSecurityStatusReport(snapshot.latestChildSecurityStatusReportPayload)
        syncLegacySetupStateFromHardening()

        val payload = snapshot.latestPolicyPayload
        if (!payload.isNullOrBlank()) {
            restorePersistedPolicyPayload(
                payload = payload,
                appliedAtMillis = snapshot.latestPolicyAppliedAtMillis,
                lastKnownPolicyVersion = snapshot.latestPolicyVersion,
            )
        } else {
            lastPolicyVerificationResult = "No debug policy verified yet"
            LabCaptureDebugStatus.useDefaultPolicy()
        }
    }

    private fun saveCurrentState() {
        if (!::stateStore.isInitialized) {
            return
        }
        stateStore.save(
            ChildDebugStateSnapshot(
                childDeviceId = if (::policyHandoffTargetInput.isInitialized) {
                    policyHandoffTargetInput.text.toString().ifBlank { childDeviceId }
                } else {
                    childDeviceId
                },
                latestPolicyPayload = latestPolicyPayload,
                latestPolicyAppliedAtMillis = latestPolicyAppliedAtMillis,
                latestPolicyVersion = currentPolicyVersion,
                latestPolicyPresetName = currentPolicyPresetName,
                latestPolicyDisplayLabel = currentPolicyDisplayLabel,
                latestPolicyBlockEncryptedDnsResolvers = currentPolicyBlockEncryptedDnsResolvers,
                latestAllowDomainsCsv = latestAllowDomainsCsv,
                latestBlockDomainsCsv = latestBlockDomainsCsv,
                childDisplayName = if (::childDisplayNameInput.isInitialized) {
                    childDisplayNameInput.text.toString().ifBlank { childDisplayName }
                } else {
                    childDisplayName
                },
                childFingerprint = if (::childFingerprintInput.isInitialized) {
                    childFingerprintInput.text.toString().ifBlank { childFingerprint }
                } else {
                    childFingerprint
                },
                vpnPermissionStatusLabel = vpnPermissionStatus,
                lastVpnCommandLabel = lastCommand,
                shellStatusLabel = shellStatus,
                setupVpnPermissionStatus = createSetupChecklist().vpnPermission.name,
                setupStartShellStatus = if (shellStatus == ChildVpnSmokeLabels.STATUS_NOT_RUNNING) {
                    SetupCheckState.UNKNOWN.name
                } else {
                    SetupCheckState.USER_CONFIRMED.name
                },
                setupForegroundNotificationStatus = setupForegroundNotificationStatus.name,
                setupAlwaysOnVpnStatus = setupAlwaysOnVpnStatus.name,
                setupBlockWithoutVpnStatus = setupBlockWithoutVpnStatus.name,
                setupBatteryOptimizationStatus = setupBatteryOptimizationStatus.name,
                lastDiagnosticsText = lastDiagnosticsText,
                latestPairingInvitePayload = if (::pairingInviteInput.isInitialized) {
                    pairingInviteInput.text.toString().takeIf(String::isNotBlank) ?: latestPairingInvitePayload
                } else {
                    latestPairingInvitePayload
                },
                latestPairingAcceptancePayload = latestPairingAcceptancePayload,
                acceptedParentSummary = acceptedParentSummary,
                latestHardeningSetupReportPayload = latestHardeningSetupReportPayload,
                latestChildSecurityStatusReportPayload = latestChildSecurityStatusReportPayload,
                latestBypassRiskReportPayload = latestBypassRiskReportPayload,
                latestChildSyncBundlePayload = latestChildSyncBundlePayload,
                latestParentSyncBundlePayload = latestParentSyncBundlePayload,
                relayBaseUrl = if (::relayBaseUrlInput.isInitialized) {
                    relayBaseUrlInput.text.toString().ifBlank { relayBaseUrl }
                } else {
                    relayBaseUrl
                },
                parentRelayDeviceId = if (::parentRelayDeviceInput.isInitialized) {
                    parentRelayDeviceInput.text.toString().ifBlank { parentRelayDeviceId }
                } else {
                    parentRelayDeviceId
                },
                latestRelayMessageId = latestRelayMessageId.takeIf(String::isNotBlank),
                latestRelayDiagnostics = latestRelayDiagnostics.takeIf(String::isNotBlank),
                expectedBasicDnsGuardRunning = expectedBasicDnsGuardRunning,
                latestVpnRuntimeStatus = VpnRuntimeDebugStatus.snapshot().statusLabel,
                latestHeartbeatStatus = BasicDnsGuardHeartbeatDebugStatus.snapshot().statusLabel,
                currentOnboardingStep = currentChildOnboardingStep(),
                schemaVersion = ChildDebugStateSnapshot.SCHEMA_VERSION,
            ),
        )
    }

    private fun currentChildOnboardingStep(): String {
        return when {
            childDeviceId.isBlank() -> "CONFIRM_CHILD_DEVICE"
            currentPolicySource != "Verified debug policy" -> "IMPORT_PARENT_POLICY"
            vpnPermissionStatus != ChildVpnSmokeLabels.PERMISSION_GRANTED -> "REQUEST_VPN_PERMISSION"
            hardeningSetupSnapshot == null -> "COMPLETE_HARDENING"
            currentDnsOnlyReadinessResult().status.name.contains("READY").not() -> "REVIEW_DNS_LIMITS"
            VpnRuntimeDebugStatus.snapshot().operatingMode != VordainOperatingMode.BASIC_DNS_GUARD -> "START_BASIC_DNS_GUARD"
            latestChildSyncBundlePayload.isNullOrBlank() -> "EXPORT_CHILD_STATUS"
            else -> "READY_FOR_TESTING"
        }
    }

    private fun currentHardeningSetupSnapshot(): HardeningSetupSnapshot {
        val current = hardeningSetupSnapshot
        if (current != null) {
            return current
        }
        return hardeningSetupReducer.initialSnapshot(
            childDeviceId = DeviceId(childDeviceId),
            currentTimeMillis = System.currentTimeMillis(),
        ).also { snapshot ->
            hardeningSetupSnapshot = snapshot
            latestHardeningSetupReportPayload = hardeningSetupReportCodec.encode(snapshot)
        }
    }

    private fun restoreHardeningSetupSnapshot(payload: String?): HardeningSetupSnapshot {
        if (!payload.isNullOrBlank()) {
            when (val result = hardeningSetupReportCodec.decode(payload)) {
                is DebugHardeningSetupReportCodecResult.Decoded -> return result.snapshot
                is DebugHardeningSetupReportCodecResult.Rejected -> {
                    latestHardeningSetupReportPayload = null
                }
            }
        }
        return hardeningSetupReducer.initialSnapshot(
            childDeviceId = DeviceId(childDeviceId),
            currentTimeMillis = System.currentTimeMillis(),
        )
    }

    private fun restoreChildSecurityStatusReport(payload: String?): ChildSecurityStatusReport? {
        if (payload.isNullOrBlank()) {
            return null
        }
        return when (val result = childSecurityReportCodec.decode(payload)) {
            is com.vordain.guard.core.statusreport.DebugChildSecurityReportCodecResult.Decoded -> result.report
            is com.vordain.guard.core.statusreport.DebugChildSecurityReportCodecResult.Rejected -> {
                latestChildSecurityStatusReportPayload = null
                null
            }
        }
    }

    private fun restoreBypassRiskItems(payload: String?): List<BypassRiskItem> {
        if (payload.isNullOrBlank()) {
            return emptyList()
        }
        return when (val result = bypassRiskReportCodec.decode(payload)) {
            is DebugBypassRiskReportCodecResult.Decoded -> result.report.summary.items
            is DebugBypassRiskReportCodecResult.Rejected -> {
                latestBypassRiskReportPayload = null
                emptyList()
            }
        }
    }

    private fun syncLegacySetupStateFromHardening() {
        val snapshot = currentHardeningSetupSnapshot()
        setupAlwaysOnVpnStatus = snapshot.itemFor(HardeningSetupStep.VPN_ALWAYS_ON).status.toSetupCheckState()
        setupBlockWithoutVpnStatus = snapshot.itemFor(HardeningSetupStep.BLOCK_WITHOUT_VPN).status.toSetupCheckState()
        setupBatteryOptimizationStatus = snapshot.itemFor(HardeningSetupStep.BATTERY_OPTIMIZATION).status.toSetupCheckState()
        setupSettingsAppLockStatus = snapshot.itemFor(HardeningSetupStep.SETTINGS_APP_LOCK).status.toSetupCheckState()
        setupScreenPinningStatus = snapshot.itemFor(HardeningSetupStep.SCREEN_PINNING_WITH_PIN).status.toSetupCheckState()
        setupPrivateDnsStatus = snapshot.itemFor(HardeningSetupStep.PRIVATE_DNS_REVIEW).status.toSetupCheckState()
        setupUnknownSourcesStatus = snapshot.itemFor(HardeningSetupStep.UNKNOWN_SOURCES_REVIEWED).status.toSetupCheckState()
    }

    private fun String.toSetupCheckState(): SetupCheckState {
        return runCatching { SetupCheckState.valueOf(this) }.getOrDefault(SetupCheckState.UNKNOWN)
    }

    private fun HardeningSetupStatus.toSetupCheckState(): SetupCheckState {
        return when (this) {
            HardeningSetupStatus.AUTO_CONFIRMED -> SetupCheckState.CONFIGURED
            HardeningSetupStatus.USER_CONFIRMED -> SetupCheckState.USER_CONFIRMED
            HardeningSetupStatus.CONFIRMED_DISABLED,
            HardeningSetupStatus.CONFIRMED_ABSENT,
            HardeningSetupStatus.BEST_EFFORT_AUTO_CHECK,
            HardeningSetupStatus.PARENT_CONFIRMED,
            -> SetupCheckState.USER_CONFIRMED
            HardeningSetupStatus.NEEDS_ATTENTION -> SetupCheckState.NOT_CONFIGURED
            HardeningSetupStatus.NOT_STARTED,
            HardeningSetupStatus.OPENED_SETTINGS,
            HardeningSetupStatus.NOT_SUPPORTED,
            HardeningSetupStatus.COMPROMISE_SUSPECTED,
            HardeningSetupStatus.UNKNOWN,
            -> SetupCheckState.UNKNOWN
        }
    }

    private fun HardeningSummaryStatus.toDisplayLabel(): String {
        return when (this) {
            com.vordain.guard.features.setupchecklist.HardeningSummaryStatus.NOT_STARTED -> "Unknown"
            com.vordain.guard.features.setupchecklist.HardeningSummaryStatus.IN_PROGRESS -> "Needs attention"
            com.vordain.guard.features.setupchecklist.HardeningSummaryStatus.READY_FOR_LAB_TEST -> "Ready for lab test"
            com.vordain.guard.features.setupchecklist.HardeningSummaryStatus.NEEDS_ATTENTION -> "Needs attention"
            com.vordain.guard.features.setupchecklist.HardeningSummaryStatus.UNKNOWN -> "Unknown"
        }
    }

    private fun HardeningSetupStatus.toDisplayLabel(): String {
        return when (this) {
            HardeningSetupStatus.AUTO_CONFIRMED -> "Confirmed"
            HardeningSetupStatus.USER_CONFIRMED -> "Parent confirmed"
            HardeningSetupStatus.CONFIRMED_DISABLED -> "Confirmed"
            HardeningSetupStatus.CONFIRMED_ABSENT -> "Confirmed"
            HardeningSetupStatus.BEST_EFFORT_AUTO_CHECK -> "Confirmed"
            HardeningSetupStatus.PARENT_CONFIRMED -> "Parent confirmed"
            HardeningSetupStatus.COMPROMISE_SUSPECTED -> "Parent PIN may be compromised"
            HardeningSetupStatus.NEEDS_ATTENTION -> "Needs attention"
            HardeningSetupStatus.NOT_SUPPORTED -> "Not supported"
            HardeningSetupStatus.OPENED_SETTINGS -> "Needs attention"
            HardeningSetupStatus.NOT_STARTED,
            HardeningSetupStatus.UNKNOWN,
            -> "Unknown"
        }
    }

    private fun HardeningEvidenceType.toDisplayLabel(): String {
        return when (this) {
            HardeningEvidenceType.AUTOMATIC_CHECK -> "automatic"
            HardeningEvidenceType.PARENT_CONFIRMATION -> "parent confirmed"
            HardeningEvidenceType.MANUAL_DEVICE_SETTING -> "manual instruction"
            HardeningEvidenceType.MANUAL_SETTINGS_REVIEW -> "manual instruction"
            HardeningEvidenceType.BEST_EFFORT_DEVICE_CHECK -> "automatic"
            HardeningEvidenceType.STATE_CHANGE_OUTSIDE_AUTHORIZED_WINDOW -> "state change outside parent window"
            HardeningEvidenceType.BEHAVIOR_TEST -> "behavior test"
            HardeningEvidenceType.UNKNOWN -> "unknown"
        }
    }

    private fun com.vordain.guard.features.setupchecklist.PinCompromiseSignal.toDisplayLabel(): String {
        return if (suspected) {
            "Parent PIN may be compromised: $reason / ${changedStep?.toDisplayLabel() ?: "unknown step"}"
        } else {
            "No compromise signal"
        }
    }

    private fun HardeningSetupStep.toDisplayLabel(): String {
        return when (this) {
            HardeningSetupStep.VPN_PERMISSION -> "VPN permission"
            HardeningSetupStep.VPN_ALWAYS_ON -> "Always-on VPN"
            HardeningSetupStep.BLOCK_WITHOUT_VPN -> "Block connections without VPN"
            HardeningSetupStep.SETTINGS_APP_LOCK -> "Settings/App Lock"
            HardeningSetupStep.SCREEN_PINNING_WITH_PIN -> "Screen pinning with PIN"
            HardeningSetupStep.BATTERY_OPTIMIZATION -> "Battery optimization"
            HardeningSetupStep.PRIVATE_DNS_REVIEW -> "Private DNS"
            HardeningSetupStep.UNKNOWN_SOURCES_REVIEWED -> "Unknown sources"
            HardeningSetupStep.DEVELOPER_OPTIONS_DISABLED -> "Developer Options disabled"
            HardeningSetupStep.USB_DEBUGGING_DISABLED -> "USB debugging disabled"
            HardeningSetupStep.WIRELESS_DEBUGGING_DISABLED -> "Wireless debugging disabled"
            HardeningSetupStep.NO_UNRESTRICTED_SECONDARY_USERS -> "No unrestricted secondary users"
            HardeningSetupStep.NO_UNRESTRICTED_WORK_PROFILE -> "No unrestricted work profile"
            HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY -> "Settings/App Lock parent PIN only"
            HardeningSetupStep.PARENT_PIN_NOT_SHARED -> "Parent PIN not shared"
            HardeningSetupStep.PARENT_MAINTENANCE_WINDOW -> "Parent maintenance window"
            HardeningSetupStep.PIN_COMPROMISE_REVIEW -> "PIN compromise review"
            HardeningSetupStep.VPN_LIFECYCLE_HEALTH -> "VPN lifecycle health"
            HardeningSetupStep.FINAL_PARENT_REVIEW -> "Final parent review"
        }
    }

    private fun ChildSecurityOverallStatus.toDisplayLabel(): String {
        return when (this) {
            ChildSecurityOverallStatus.NOT_STARTED -> "Unknown"
            ChildSecurityOverallStatus.SETUP_IN_PROGRESS -> "Needs attention"
            ChildSecurityOverallStatus.READY_FOR_LAB_TEST -> "Ready for lab test"
            ChildSecurityOverallStatus.NEEDS_ATTENTION -> "Needs attention"
            ChildSecurityOverallStatus.VPN_STOPPED -> "VPN stopped"
            ChildSecurityOverallStatus.PIN_COMPROMISE_SUSPECTED -> "PIN may be compromised"
            ChildSecurityOverallStatus.UNKNOWN -> "Unknown"
        }
    }

    private fun ChildSecurityActiveMode.toDisplayLabel(): String {
        return when (this) {
            ChildSecurityActiveMode.NONE -> "None"
            ChildSecurityActiveMode.BASIC_DNS_GUARD -> "Basic DNS Guard active"
            ChildSecurityActiveMode.DNS_ONLY_LAB -> "DNS-only lab active"
            ChildSecurityActiveMode.FULL_TUNNEL_LAB -> "Full-tunnel lab active"
        }
    }

    private fun MvpAcceptanceStep.toDisplayLabel(): String {
        return when (this) {
            MvpAcceptanceStep.PARENT_BUILDS_POLICY -> "Parent builds DNS policy"
            MvpAcceptanceStep.PARENT_EXPORTS_SYNC_BUNDLE -> "Parent exports sync bundle"
            MvpAcceptanceStep.CHILD_IMPORTS_SYNC_BUNDLE -> "Import parent sync bundle"
            MvpAcceptanceStep.CHILD_APPLIES_VERIFIED_POLICY -> "Apply verified policy"
            MvpAcceptanceStep.CHILD_COMPLETES_HARDENING_REVIEW -> "Complete hardening review"
            MvpAcceptanceStep.CHILD_STARTS_BASIC_DNS_GUARD -> "Start Basic DNS Guard"
            MvpAcceptanceStep.CHILD_EXPORTS_STATUS_BUNDLE -> "Export child sync bundle"
            MvpAcceptanceStep.PARENT_IMPORTS_STATUS_BUNDLE -> "Parent imports child sync bundle"
            MvpAcceptanceStep.PARENT_REVIEWS_ALERTS -> "Parent reviews alerts"
            MvpAcceptanceStep.DEV_RELAY_RUNNING -> "Start local dev relay"
            MvpAcceptanceStep.PARENT_SENT_POLICY_VIA_RELAY -> "Parent sends policy via relay"
            MvpAcceptanceStep.CHILD_FETCHED_POLICY_FROM_RELAY -> "Fetch policy from relay"
            MvpAcceptanceStep.CHILD_SENT_STATUS_VIA_RELAY -> "Send status via relay"
            MvpAcceptanceStep.PARENT_FETCHED_STATUS_FROM_RELAY -> "Parent fetches status via relay"
        }
    }

    private fun com.vordain.guard.features.bypassrisk.DnsOnlyReadinessStatus.toBasicDnsDisplayLabel(): String {
        return when (this) {
            com.vordain.guard.features.bypassrisk.DnsOnlyReadinessStatus.NOT_READY -> "Needs attention"
            com.vordain.guard.features.bypassrisk.DnsOnlyReadinessStatus.READY_FOR_DNS_LAB -> "Ready for DNS Guard"
            com.vordain.guard.features.bypassrisk.DnsOnlyReadinessStatus.NEEDS_REVIEW -> "Needs attention"
            com.vordain.guard.features.bypassrisk.DnsOnlyReadinessStatus.HIGH_RISK -> "High risk"
            com.vordain.guard.features.bypassrisk.DnsOnlyReadinessStatus.UNKNOWN -> "Unknown"
        }
    }

    private fun BypassRiskOverallStatus.toDisplayLabel(): String {
        return when (this) {
            BypassRiskOverallStatus.NOT_REVIEWED -> "Unknown"
            BypassRiskOverallStatus.READY_FOR_DNS_LAB -> "Ready for DNS lab"
            BypassRiskOverallStatus.NEEDS_ATTENTION -> "Needs attention"
            BypassRiskOverallStatus.HIGH_RISK -> "Needs attention - high risk"
            BypassRiskOverallStatus.UNKNOWN -> "Unknown"
        }
    }

    private fun BypassRiskStatus.toDisplayLabel(): String {
        return when (this) {
            BypassRiskStatus.NOT_CHECKED -> "Unknown"
            BypassRiskStatus.NEEDS_REVIEW -> "Needs attention"
            BypassRiskStatus.CONFIRMED_SAFE -> "Confirmed"
            BypassRiskStatus.RISK_FOUND -> "Needs attention"
            BypassRiskStatus.NOT_SUPPORTED -> "Not supported"
            BypassRiskStatus.UNKNOWN -> "Unknown"
        }
    }

    private fun BypassRiskCategory.toDisplayLabel(): String {
        return when (this) {
            BypassRiskCategory.DNS_OVER_HTTPS -> "DoH resolver risk"
            BypassRiskCategory.PRIVATE_DNS -> "Private DNS"
            BypassRiskCategory.ALTERNATE_VPN_APP -> "Alternate VPN app"
            BypassRiskCategory.PROXY_APP -> "Proxy app"
            BypassRiskCategory.PRIVATE_BROWSER -> "Private browser"
            BypassRiskCategory.DEVELOPER_OPTIONS -> "Developer Options"
            BypassRiskCategory.USB_DEBUGGING -> "USB debugging"
            BypassRiskCategory.WIRELESS_DEBUGGING -> "Wireless debugging"
            BypassRiskCategory.UNRESTRICTED_PROFILE -> "Unrestricted profile"
            BypassRiskCategory.DIRECT_IP_ACCESS -> "Direct-IP limitation"
            BypassRiskCategory.UNKNOWN -> "Unknown"
        }
    }

    private fun Set<ChildSecuritySignal>.toDisplayLabels(): String {
        if (isEmpty()) {
            return "none"
        }
        return sortedBy(ChildSecuritySignal::name)
            .joinToString(separator = ", ") { signal -> signal.toDisplayLabel() }
    }

    private fun ChildSecuritySignal.toDisplayLabel(): String {
        return when (this) {
            ChildSecuritySignal.VPN_PERMISSION_CONFIRMED -> "VPN permission confirmed"
            ChildSecuritySignal.VPN_ALWAYS_ON_CONFIRMED -> "Always-on VPN confirmed"
            ChildSecuritySignal.BLOCK_WITHOUT_VPN_CONFIRMED -> "Block without VPN confirmed"
            ChildSecuritySignal.SETTINGS_LOCK_CONFIRMED -> "Settings lock confirmed"
            ChildSecuritySignal.DEVELOPER_OPTIONS_DISABLED_CONFIRMED -> "Developer Options disabled"
            ChildSecuritySignal.ADB_DISABLED_CONFIRMED -> "ADB disabled"
            ChildSecuritySignal.NO_UNRESTRICTED_PROFILES_CONFIRMED -> "No unrestricted profiles confirmed"
            ChildSecuritySignal.POLICY_APPLIED -> "Policy applied"
            ChildSecuritySignal.HEARTBEAT_FRESH -> "Heartbeat fresh"
            ChildSecuritySignal.VPN_SESSION_RUNNING -> "VPN session running"
            ChildSecuritySignal.LAB_CAPTURE_ACTIVE -> "Lab capture active"
            ChildSecuritySignal.PIN_COMPROMISE_SUSPECTED -> "PIN may be compromised"
            ChildSecuritySignal.VPN_STOPPED -> "VPN stopped"
        }
    }

    private fun HardeningSetupSnapshot.isConfirmed(step: HardeningSetupStep): Boolean {
        return itemFor(step).status.isConfirmedStatus()
    }

    private fun HardeningSetupStatus.isConfirmedStatus(): Boolean {
        return when (this) {
            HardeningSetupStatus.AUTO_CONFIRMED,
            HardeningSetupStatus.USER_CONFIRMED,
            HardeningSetupStatus.CONFIRMED_DISABLED,
            HardeningSetupStatus.CONFIRMED_ABSENT,
            HardeningSetupStatus.BEST_EFFORT_AUTO_CHECK,
            HardeningSetupStatus.PARENT_CONFIRMED,
            -> true
            HardeningSetupStatus.NOT_STARTED,
            HardeningSetupStatus.OPENED_SETTINGS,
            HardeningSetupStatus.NEEDS_ATTENTION,
            HardeningSetupStatus.NOT_SUPPORTED,
            HardeningSetupStatus.UNKNOWN,
            HardeningSetupStatus.COMPROMISE_SUSPECTED,
            -> false
        }
    }

    private fun Set<DomainName>.toCsv(): String {
        return map(DomainName::value).sorted().joinToString(separator = ",")
    }

    private fun String?.countCsvEntries(): Int {
        return this
            ?.split(',')
            ?.map(String::trim)
            ?.count(String::isNotBlank)
            ?: 0
    }

    private fun currentVerifiedPolicyVersion(): String? {
        return currentPolicyVersion.takeIf { currentPolicySource.startsWith("Verified") }
    }

    private fun createPairingOutput(): String {
        return when {
            acceptedParentSummary != null -> "Paired parent summary:\n$acceptedParentSummary"
            !latestPairingAcceptancePayload.isNullOrBlank() -> latestPairingAcceptancePayload.orEmpty()
            else -> "No debug pairing accepted yet"
        }
    }

    private companion object {
        const val REQUEST_VPN_PERMISSION = 1001
        const val MAINTENANCE_WINDOW_MILLIS = 15L * 60L * 1_000L
        const val ACTION_USER_SETTINGS = "android.settings.USER_SETTINGS"
        const val AUDIT_MAX_ENTRIES = 40
        const val AUDIT_DISPLAY_COUNT = 20
        const val ALERT_MAX_ENTRIES = 50
        const val ALERT_DISPLAY_COUNT = 20
        const val ALERT_REPORT_COUNT = 20
        const val LOCAL_ALERT_CHANNEL_ID = "vordain_guard_local_alerts"
        const val LOCAL_ALERT_NOTIFICATION_ID = 42_201
        val defaultPairingCapabilities = setOf(
            PairingCapability.POLICY_UPDATES,
            PairingCapability.HEARTBEAT_STATUS,
            PairingCapability.ENCRYPTED_ALERTS,
            PairingCapability.PARENT_REVIEW,
        )
    }
}
