package com.vordain.guard.child

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import com.vordain.guard.data.review.ReviewRequestReason
import com.vordain.guard.vpn.service.LabPacketCaptureDebugStatus
import com.vordain.guard.vpn.service.TunPacketCaptureStats
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
    private lateinit var stateStore: ChildDebugStateStore
    private lateinit var statusText: TextView
    private lateinit var vpnPermissionText: TextView
    private lateinit var lastCommandText: TextView
    private lateinit var shellStatusText: TextView
    private lateinit var setupChecklistText: TextView
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
            setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_GRANTED)
        } else {
            setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_REQUIRED)
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
        labCaptureText = valueLabel(createLabCaptureDisplay(LabPacketCaptureDebugStatus.snapshot()), textSize = 14f)
        layout.addView(labCaptureText)

        layout.addView(sectionTitle("Setup checklist"))
        setupChecklistText = valueLabel("", textSize = 15f)
        layout.addView(setupChecklistText)

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
                setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_GRANTED)
            }
            is VpnPrepareResult.ConsentRequired -> {
                setVpnPermissionStatus(ChildVpnSmokeLabels.PERMISSION_REQUIRED)
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
            labCaptureText.text = createLabCaptureDisplay(LabPacketCaptureDebugStatus.snapshot())
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
            labCaptureStats = LabPacketCaptureDebugStatus.snapshot(),
        )
    }

    private fun createLabCaptureDisplay(stats: TunPacketCaptureStats): String {
        return listOf(
            "Lab mode status: $shellStatus",
            "Packet count: ${stats.packetCount}",
            "Byte count: ${stats.byteCount}",
            "IPv4/IPv6: ${stats.ipv4Count}/${stats.ipv6Count}",
            "TCP/UDP/ICMP: ${stats.tcpCount}/${stats.udpCount}/${stats.icmpCount}",
            "Malformed: ${stats.malformedCount}",
            "Last packet: ${stats.lastPacketSummary ?: "none"}",
            ChildVpnSmokeLabels.LAB_LOCAL_ONLY,
            ChildVpnSmokeLabels.LAB_NOT_FULL_PROTECTION,
        ).joinToString(separator = "\n")
    }

    private fun createSetupChecklist(): VpnSetupChecklist {
        return VpnSetupChecklist(
            vpnPermission = if (vpnPermissionStatus == ChildVpnSmokeLabels.PERMISSION_GRANTED) {
                SetupCheckState.CONFIGURED
            } else {
                SetupCheckState.UNKNOWN
            },
            alwaysOnVpn = setupAlwaysOnVpnStatus,
            blockConnectionsWithoutVpn = setupBlockWithoutVpnStatus,
            batteryOptimizationWarning = setupBatteryOptimizationStatus,
            appProtection = if (shellStatus == ChildVpnSmokeLabels.STATUS_SHELL_ACTIVE) {
                SetupCheckState.USER_CONFIRMED
            } else {
                SetupCheckState.UNKNOWN
            },
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
            ),
        )
    }

    private fun String.toSetupCheckState(): SetupCheckState {
        return runCatching { SetupCheckState.valueOf(this) }.getOrDefault(SetupCheckState.UNKNOWN)
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
        val defaultPairingCapabilities = setOf(
            PairingCapability.POLICY_UPDATES,
            PairingCapability.HEARTBEAT_STATUS,
            PairingCapability.ENCRYPTED_ALERTS,
            PairingCapability.PARENT_REVIEW,
        )
    }
}
