package com.vordain.guard.child

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import com.vordain.guard.core.policysync.PolicyVersion
import com.vordain.guard.core.policysync.SignedPolicySnapshotRestorer
import com.vordain.guard.core.statusreport.ChildSecurityOverallStatus
import com.vordain.guard.core.statusreport.ChildSecuritySignal
import com.vordain.guard.core.statusreport.ChildSecurityStatusEvaluator
import com.vordain.guard.core.statusreport.ChildSecurityStatusInput
import com.vordain.guard.core.statusreport.ChildSecurityStatusReport
import com.vordain.guard.core.statusreport.DebugChildSecurityReportCodec
import com.vordain.guard.data.review.ReviewRequestReason
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
import com.vordain.guard.vpn.service.LabCaptureDebugStatus
import com.vordain.guard.vpn.service.VordainVpnServiceIntents
import com.vordain.guard.vpn.service.VpnPermissionIntentFactory
import com.vordain.guard.vpn.service.VpnPrepareResult

class ChildMainActivity : Activity() {
    private val vpnPermissionIntentFactory = VpnPermissionIntentFactory()
    private val policySnapshotRestorer = SignedPolicySnapshotRestorer()
    private val pairingInviteCodec = DebugPairingInviteCodec()
    private val pairingAcceptanceCodec = DebugPairingAcceptanceCodec()
    private val pairingEvaluator = PairingEvaluator()
    private val policyDemo = ChildDebugPolicyDemo()
    private val compatibilityDemo = ChildDebugCompatibilityDemo { System.currentTimeMillis() }
    private val reviewDemo = ChildDebugReviewDemo()
    private val policyHandoff = ChildDebugPolicyHandoff { System.currentTimeMillis() }
    private val diagnosticsFormatter = ChildDebugDiagnosticsFormatter()
    private val hardeningSetupReducer = HardeningSetupReducer()
    private val hardeningSetupReportCodec = DebugHardeningSetupReportCodec()
    private val childSecurityStatusEvaluator = ChildSecurityStatusEvaluator()
    private val childSecurityReportCodec = DebugChildSecurityReportCodec()
    private lateinit var stateStore: ChildDebugStateStore
    private lateinit var statusText: TextView
    private lateinit var vpnPermissionText: TextView
    private lateinit var lastCommandText: TextView
    private lateinit var shellStatusText: TextView
    private lateinit var setupChecklistText: TextView
    private lateinit var hardeningSetupText: TextView
    private lateinit var childSecurityStatusText: TextView
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
    private var vpnPermissionStatus: String = ChildVpnSmokeLabels.PERMISSION_UNKNOWN
    private var lastCommand: String = ChildVpnSmokeLabels.COMMAND_NONE
    private var shellStatus: String = ChildVpnSmokeLabels.STATUS_NOT_RUNNING
    private var childDeviceId: String = ChildDebugStateSnapshot.DEFAULT_CHILD_DEVICE_ID
    private var latestPolicyPayload: String? = null
    private var latestPolicyAppliedAtMillis: Long = 0L
    private var latestAllowDomainsCsv: String? = null
    private var latestBlockDomainsCsv: String? = null
    private var currentPolicySource: String = "Default sample policy"
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
    private var lastDiagnosticsText: String? = null
    private var policyResult: ChildDebugPolicyResult? = null
    private var policyHandoffResult: ChildDebugPolicyHandoffResult? = null
    private var currentPolicyVersion: String = "debug-tablet-policy"
    private var compatibilityResult: ChildDebugCompatibilityResult? = null
    private var reviewResult: ChildDebugReviewResult? = null
    private val localDebugEvents = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateStore = ChildDebugStateStore(this)
        restoreState(stateStore.load())
        setContentView(createSmokeTestView())
        statusText.text = shellStatus
        refreshDiagnosticsViews()
    }

    override fun onPause() {
        saveCurrentState()
        super.onPause()
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
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_INTERNET_MAY_NOT_WORK, textSize = 14f))
        layout.addView(valueLabel(ChildVpnSmokeLabels.LAB_NOT_FULL_PROTECTION, textSize = 14f))
        layout.addView(button("Start lab capture") {
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
        layout.addView(button("Clear policy update result") {
            clearPolicyHandoffResult()
        })
        policyHandoffOutputText = valueLabel(
            buildString {
                append("Current debug policy source: $currentPolicySource\n")
                append(policyDemo.policySummary(currentPolicyVersion))
                latestAllowDomainsCsv?.let { append("\nLatest allow domains: $it") }
                latestBlockDomainsCsv?.let { append("\nLatest block domains: $it") }
            },
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
        layout.addView(localEventsText)
        layout.addView(diagnosticsText)
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
        setStatus(ChildVpnSmokeLabels.STATUS_STOPPED)
        setShellStatus(ChildVpnSmokeLabels.STATUS_STOPPED)
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
        saveCurrentState()
        refreshDiagnosticsViews()
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
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun endParentMaintenanceWindow() {
        hardeningSetupSnapshot = hardeningSetupReducer.closeMaintenanceWindow(
            snapshot = currentHardeningSetupSnapshot(),
            currentTimeMillis = System.currentTimeMillis(),
        )
        latestHardeningSetupReportPayload = hardeningSetupReportCodec.encode(currentHardeningSetupSnapshot())
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
            policyDemo.replacePolicy(result.policy)
            currentPolicyVersion = result.policyVersion.value
            latestPolicyPayload = policyHandoffPayloadInput.text.toString()
            latestPolicyAppliedAtMillis = System.currentTimeMillis()
            latestAllowDomainsCsv = result.policy.allowedDomains.toCsv()
            latestBlockDomainsCsv = result.policy.blockedDomains.toCsv()
            currentPolicySource = "Verified applied debug policy"
        }
        policyHandoffOutputText.text = result.asDisplayText()
        saveCurrentState()
        refreshDiagnosticsViews()
    }

    private fun clearPolicyHandoffResult() {
        policyHandoffResult = null
        policyHandoffOutputText.text = policyDemo.policySummary(currentPolicyVersion)
        refreshDiagnosticsViews()
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
        saveCurrentState()
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
        val lines = mutableListOf(
            "Lab mode status: $shellStatus",
            "Packet count: ${stats.packetCount}",
            "Byte count: ${stats.byteCount}",
            "DNS packet count: ${stats.dnsPacketCount}",
            "DNS query count: ${stats.dnsQueryCount}",
            "DNS blocked response count: ${stats.dnsBlockedResponseCount}",
            "DNS allowed-but-dropped count: ${stats.dnsAllowedDroppedCount}",
            "DNS alert-only dropped count: ${stats.dnsAlertDroppedCount}",
            "DNS response write failures: ${stats.dnsResponseWriteFailureCount}",
            "Allowed/block/alert counts: ${stats.allowedDomainCount}/${stats.blockedDomainCount}/${stats.alertOnlyDomainCount}",
            "Malformed packet/DNS counts: ${stats.malformedPacketCount}/${stats.malformedDnsCount}",
            "Last packet summary: ${stats.lastPacketSummary ?: "none"}",
            ChildVpnSmokeLabels.LAB_LOCAL_ONLY,
            ChildVpnSmokeLabels.LAB_DNS_LOCAL_ONLY,
            ChildVpnSmokeLabels.LAB_DNS_SINKHOLE,
            ChildVpnSmokeLabels.LAB_ALLOWED_DROPPED,
            ChildVpnSmokeLabels.LAB_INTERNET_MAY_NOT_WORK,
            ChildVpnSmokeLabels.LAB_NOT_FULL_PROTECTION,
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

    private fun refreshChildSecurityStatusReport() {
        generateChildSecurityStatusReport()
    }

    private fun currentChildSecurityStatusReport(): ChildSecurityStatusReport {
        val hardening = currentHardeningSetupSnapshot()
        val settingsLockConfirmed = hardening.isConfirmed(HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY) ||
            hardening.isConfirmed(HardeningSetupStep.SETTINGS_APP_LOCK)
        val adbDisabledConfirmed = hardening.isConfirmed(HardeningSetupStep.USB_DEBUGGING_DISABLED) &&
            hardening.isConfirmed(HardeningSetupStep.WIRELESS_DEBUGGING_DISABLED)
        val noUnrestrictedProfilesConfirmed = hardening.isConfirmed(HardeningSetupStep.NO_UNRESTRICTED_SECONDARY_USERS) &&
            hardening.isConfirmed(HardeningSetupStep.NO_UNRESTRICTED_WORK_PROFILE)
        val labCaptureActive = shellStatus == ChildVpnSmokeLabels.STATUS_LAB_CAPTURE_ACTIVE ||
            shellStatus == ChildVpnSmokeLabels.STATUS_LAB_CAPTURE_STARTING
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
                vpnSessionLabel = shellStatus,
                vpnSessionRunning = shellStatus == ChildVpnSmokeLabels.STATUS_SHELL_ACTIVE || labCaptureActive,
                vpnStopped = shellStatus == ChildVpnSmokeLabels.STATUS_STOPPED ||
                    shellStatus == ChildVpnSmokeLabels.STATUS_REVOKED_UNKNOWN,
                heartbeatLabel = "Unknown",
                heartbeatFresh = false,
                setupSummaryLabel = hardening.summaryStatus.toDisplayLabel(),
                bypassRiskLabel = hardening.latestPinCompromiseSignal.toDisplayLabel(),
                labCaptureActive = labCaptureActive,
                pinCompromiseSuspected = hardening.latestPinCompromiseSignal.suspected,
            ),
        )
    }

    private fun createChildSecurityStatusDisplay(report: ChildSecurityStatusReport): String {
        return buildString {
            append("Overall status: ${report.overallStatus.toDisplayLabel()}\n")
            append("Child device id: ${report.childDeviceId.value}\n")
            append("Generated at: ${report.generatedAtMillis}\n")
            append("Policy version: ${report.policyVersion ?: "none"}\n")
            append("VPN/session: ${report.vpnSessionLabel ?: "Unknown"}\n")
            append("Setup summary: ${report.setupSummaryLabel ?: "Unknown"}\n")
            append("Heartbeat: ${report.heartbeatLabel ?: "Unknown"}\n")
            append("Bypass risk: ${report.bypassRiskLabel ?: "Unknown"}\n")
            append("Signals: ${report.signals.toDisplayLabels()}\n")
            append(report.warningText)
        }
    }

    private fun restoreState(snapshot: ChildDebugStateSnapshot) {
        childDeviceId = snapshot.childDeviceId.ifBlank { ChildDebugStateSnapshot.DEFAULT_CHILD_DEVICE_ID }
        latestPolicyPayload = snapshot.latestPolicyPayload
        latestPolicyAppliedAtMillis = snapshot.latestPolicyAppliedAtMillis
        currentPolicyVersion = snapshot.latestPolicyVersion ?: currentPolicyVersion
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
        hardeningSetupSnapshot = restoreHardeningSetupSnapshot(snapshot.latestHardeningSetupReportPayload)
        latestChildSecurityStatusReport = restoreChildSecurityStatusReport(snapshot.latestChildSecurityStatusReportPayload)
        syncLegacySetupStateFromHardening()

        val payload = snapshot.latestPolicyPayload
        if (!payload.isNullOrBlank()) {
            val restored = policySnapshotRestorer.restore(
                snapshot = PersistedSignedPolicySnapshot(
                    encodedPayload = payload,
                    appliedAtMillis = snapshot.latestPolicyAppliedAtMillis,
                    expectedDeviceId = DeviceId(childDeviceId),
                    lastKnownPolicyVersion = snapshot.latestPolicyVersion?.let(::PolicyVersion),
                ),
                currentTimeMillis = System.currentTimeMillis(),
            )
            val restoredPolicy = restored.policy
            val restoredPolicyVersion = restored.policyVersion
            if (restored.accepted && restoredPolicy != null && restoredPolicyVersion != null) {
                policyDemo.replacePolicy(restoredPolicy)
                currentPolicyVersion = restoredPolicyVersion.value
                latestAllowDomainsCsv = restoredPolicy.allowedDomains.toCsv()
                latestBlockDomainsCsv = restoredPolicy.blockedDomains.toCsv()
                currentPolicySource = "Verified persisted debug policy"
            } else {
                currentPolicySource = "Persisted policy rejected: ${restored.reason}"
            }
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
            ),
        )
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
        val defaultPairingCapabilities = setOf(
            PairingCapability.POLICY_UPDATES,
            PairingCapability.HEARTBEAT_STATUS,
            PairingCapability.ENCRYPTED_ALERTS,
            PairingCapability.PARENT_REVIEW,
        )
    }
}
