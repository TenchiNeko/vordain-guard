package com.vordain.guard.parent

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.LockdownMode
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.pairing.DebugPairingAcceptanceCodec
import com.vordain.guard.core.pairing.DebugPairingAcceptanceCodecResult
import com.vordain.guard.core.pairing.DebugPairingInviteCodec
import com.vordain.guard.core.pairing.DebugPairingInviteCodecResult
import com.vordain.guard.core.pairing.PairingCapability
import com.vordain.guard.core.pairing.PairingDeviceProfile
import com.vordain.guard.core.pairing.PairingEvaluator
import com.vordain.guard.core.pairing.PairingInvite
import com.vordain.guard.core.pairing.PairingPublicKeyFingerprint
import com.vordain.guard.core.pairing.PairingRole
import com.vordain.guard.core.pairing.PairingSessionId
import com.vordain.guard.core.pairing.PairingStatus
import com.vordain.guard.core.pairing.PairingVerificationCode
import com.vordain.guard.core.policy.Policy
import com.vordain.guard.core.policysync.DebugPolicyUpdateCodec
import com.vordain.guard.core.policysync.PolicyUpdateSignature
import com.vordain.guard.core.policysync.PolicyVersion
import com.vordain.guard.core.policysync.SignedPolicyUpdate
import com.vordain.guard.core.statusreport.ChildSecurityOverallStatus
import com.vordain.guard.core.statusreport.ChildSecuritySignal
import com.vordain.guard.core.statusreport.ChildSecurityStatusReport
import com.vordain.guard.core.statusreport.DebugChildSecurityReportCodec
import com.vordain.guard.core.statusreport.DebugChildSecurityReportCodecResult
import com.vordain.guard.features.setupchecklist.DebugHardeningSetupReportCodec
import com.vordain.guard.features.setupchecklist.DebugHardeningSetupReportCodecResult
import com.vordain.guard.features.setupchecklist.HardeningEvidenceType
import com.vordain.guard.features.setupchecklist.HardeningSetupSnapshot
import com.vordain.guard.features.setupchecklist.HardeningSetupStatus
import com.vordain.guard.features.setupchecklist.HardeningSetupStep
import com.vordain.guard.features.setupchecklist.HardeningSummaryStatus

class ParentMainActivity : Activity() {
    private val codec = DebugPolicyUpdateCodec()
    private val inviteCodec = DebugPairingInviteCodec()
    private val acceptanceCodec = DebugPairingAcceptanceCodec()
    private val pairingEvaluator = PairingEvaluator()
    private val hardeningSetupReportCodec = DebugHardeningSetupReportCodec()
    private val childSecurityReportCodec = DebugChildSecurityReportCodec()
    private lateinit var stateStore: ParentDebugStateStore
    private lateinit var targetDeviceInput: EditText
    private lateinit var policyVersionInput: EditText
    private lateinit var allowDomainsInput: EditText
    private lateinit var blockDomainsInput: EditText
    private lateinit var blockKnownProxyInput: CheckBox
    private lateinit var blockUnknownInput: CheckBox
    private lateinit var payloadOutput: TextView
    private lateinit var pairingSessionInput: EditText
    private lateinit var parentDeviceInput: EditText
    private lateinit var parentDisplayNameInput: EditText
    private lateinit var parentFingerprintInput: EditText
    private lateinit var verificationCodeInput: EditText
    private lateinit var childAcceptanceInput: EditText
    private lateinit var pairingOutput: TextView
    private lateinit var setupReportInput: EditText
    private lateinit var setupReportOutput: TextView
    private lateinit var childSecurityReportInput: EditText
    private lateinit var childSecurityReportOutput: TextView
    private var lastPayload: String = ""
    private var lastPairingInvitePayload: String = ""
    private var lastPairingAcceptancePayload: String = ""
    private var acceptedChildSummary: String? = null
    private var lastHardeningSetupReportPayload: String = ""
    private var decodedHardeningSetupReport: HardeningSetupSnapshot? = null
    private var lastChildSecurityStatusReportPayload: String = ""
    private var decodedChildSecurityStatusReport: ChildSecurityStatusReport? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateStore = ParentDebugStateStore(this)
        setContentView(createView())
    }

    override fun onPause() {
        stateStore.save(createSnapshot())
        super.onPause()
    }

    private fun createView(): ScrollView {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        layout.addView(centerLabel("Vordain Guard Parent", 28f))
        layout.addView(centerLabel("Debug policy handoff", 18f))
        layout.addView(centerLabel("Local debug only - no server delivery.", 16f))

        val snapshot = stateStore.load()
        lastPayload = snapshot.latestGeneratedPayload.orEmpty()
        lastPairingInvitePayload = snapshot.latestPairingInvitePayload.orEmpty()
        lastPairingAcceptancePayload = snapshot.latestPairingAcceptancePayload.orEmpty()
        acceptedChildSummary = snapshot.acceptedChildSummary
        lastHardeningSetupReportPayload = snapshot.latestHardeningSetupReportPayload.orEmpty()
        lastChildSecurityStatusReportPayload = snapshot.latestChildSecurityStatusReportPayload.orEmpty()
        pairingSessionInput = editText(snapshot.pairingSessionId)
        parentDeviceInput = editText(snapshot.parentDeviceId)
        parentDisplayNameInput = editText(snapshot.parentDisplayName)
        parentFingerprintInput = editText(snapshot.parentFingerprint)
        verificationCodeInput = editText(snapshot.verificationCode)
        childAcceptanceInput = editText(lastPairingAcceptancePayload)
        setupReportInput = editText(lastHardeningSetupReportPayload)
        childSecurityReportInput = editText(lastChildSecurityStatusReportPayload)
        targetDeviceInput = editText(snapshot.targetChildDeviceId)
        policyVersionInput = editText(snapshot.policyVersion)
        allowDomainsInput = editText(snapshot.allowDomainsText)
        blockDomainsInput = editText(snapshot.blockDomainsText)
        blockKnownProxyInput = checkBox("Block known proxy domains", checked = snapshot.blockKnownProxyDomains)
        blockUnknownInput = checkBox("Block unknown domains", checked = snapshot.blockUnknownDomains)

        layout.addView(sectionTitle("Debug pairing handoff"))
        layout.addView(valueLabel("Local debug only - no server transport.", 14f))
        layout.addView(labeledField("Parent device id", parentDeviceInput))
        layout.addView(labeledField("Parent display name", parentDisplayNameInput))
        layout.addView(labeledField("Parent fingerprint", parentFingerprintInput))
        layout.addView(labeledField("Pairing session id", pairingSessionInput))
        layout.addView(labeledField("Verification code", verificationCodeInput))
        layout.addView(button("Build pairing invite") {
            buildPairingInvite()
        })
        layout.addView(button("Copy pairing invite") {
            copyPairingInvite()
        })
        layout.addView(labeledField("Paste child acceptance", childAcceptanceInput))
        layout.addView(button("Verify child acceptance") {
            verifyChildAcceptance()
        })
        pairingOutput = valueLabel(createPairingOutput(), 14f)
        layout.addView(pairingOutput)
        layout.addView(valueLabel("Production pairing will use encrypted delivery and real key verification later.", 14f))

        layout.addView(sectionTitle("Debug policy fields"))
        layout.addView(labeledField("Target child device id", targetDeviceInput))
        layout.addView(labeledField("Policy version", policyVersionInput))
        layout.addView(labeledField("Allow domains", allowDomainsInput))
        layout.addView(labeledField("Block domains", blockDomainsInput))
        layout.addView(blockKnownProxyInput)
        layout.addView(blockUnknownInput)

        layout.addView(button("Build debug policy update") {
            buildDebugPolicyUpdate()
        })
        layout.addView(button("Copy debug policy update") {
            copyPayload()
        })

        layout.addView(sectionTitle("Payload preview"))
        payloadOutput = valueLabel(lastPayload.ifBlank { "No debug policy update built yet" }, 14f)
        layout.addView(payloadOutput)
        layout.addView(valueLabel("Production policy sync will use signed encrypted delivery later.", 14f))

        layout.addView(sectionTitle("Child hardening setup"))
        layout.addView(valueLabel("This debug report is parent/child copy-paste only. Production will use encrypted delivery later.", 14f))
        layout.addView(valueLabel("Vordain does not receive or record the PIN.", 14f))
        layout.addView(valueLabel("Compromise signals are based on setup state changes outside parent-authorized maintenance windows.", 14f))
        layout.addView(labeledField("Paste setup report", setupReportInput))
        layout.addView(button("Decode setup report") {
            decodeHardeningSetupReport()
        })
        setupReportOutput = valueLabel(createHardeningSetupOutput(), 14f)
        layout.addView(setupReportOutput)

        layout.addView(sectionTitle("Child security status"))
        layout.addView(valueLabel("This debug report is copy/paste only. Production will use encrypted relay later.", 14f))
        layout.addView(valueLabel("Vordain does not receive PINs or secrets.", 14f))
        layout.addView(valueLabel(ChildSecurityStatusReport.WARNING_TEXT, 14f))
        layout.addView(labeledField("Paste child status report", childSecurityReportInput))
        layout.addView(button("Decode status report") {
            decodeChildSecurityStatusReport()
        })
        layout.addView(button("Clear status report") {
            clearChildSecurityStatusReport()
        })
        childSecurityReportOutput = valueLabel(createChildSecurityStatusOutput(), 14f)
        layout.addView(childSecurityReportOutput)

        return ScrollView(this).apply { addView(layout) }
    }

    private fun buildPairingInvite() {
        val result = runCatching {
            val now = System.currentTimeMillis()
            val invite = PairingInvite(
                sessionId = PairingSessionId(pairingSessionInput.text.toString().trim()),
                parentDeviceProfile = PairingDeviceProfile(
                    deviceId = DeviceId(parentDeviceInput.text.toString().trim()),
                    role = PairingRole.PARENT,
                    displayName = parentDisplayNameInput.text.toString().trim(),
                    publicKeyFingerprint = PairingPublicKeyFingerprint(parentFingerprintInput.text.toString().trim()),
                    capabilities = defaultPairingCapabilities,
                ),
                createdAtMillis = now,
                expiresAtMillis = now + DEBUG_UPDATE_TTL_MILLIS,
                verificationCode = PairingVerificationCode(verificationCodeInput.text.toString().trim()),
            )
            inviteCodec.encode(invite)
        }

        lastPairingInvitePayload = result.getOrDefault("")
        pairingOutput.text = result.getOrElse { throwable ->
            "Could not build pairing invite: ${throwable.message}"
        }
        stateStore.save(createSnapshot())
    }

    private fun copyPairingInvite() {
        if (lastPairingInvitePayload.isBlank()) {
            buildPairingInvite()
        }
        if (lastPairingInvitePayload.isBlank()) {
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain debug pairing invite", lastPairingInvitePayload))
        pairingOutput.text = "$lastPairingInvitePayload\n\nCopied debug pairing invite."
        stateStore.save(createSnapshot())
    }

    private fun verifyChildAcceptance() {
        lastPairingAcceptancePayload = childAcceptanceInput.text.toString()
        val inviteResult = inviteCodec.decode(lastPairingInvitePayload)
        val acceptanceResult = acceptanceCodec.decode(lastPairingAcceptancePayload)
        pairingOutput.text = if (
            inviteResult is DebugPairingInviteCodecResult.Decoded &&
            acceptanceResult is DebugPairingAcceptanceCodecResult.Decoded
        ) {
            val result = pairingEvaluator.evaluateAcceptance(
                invite = inviteResult.invite,
                acceptance = acceptanceResult.acceptance,
                currentTimeMillis = System.currentTimeMillis(),
            )
            val pairedChild = result.pairedChild
            if (result.status == PairingStatus.PAIRED && pairedChild != null) {
                acceptedChildSummary = listOf(
                    "Child: ${pairedChild.deviceId.value}",
                    "Display name: ${pairedChild.displayName}",
                    "Fingerprint: ${pairedChild.publicKeyFingerprint.value}",
                ).joinToString(separator = "\n")
            }
            "Pairing result: ${result.status}\nReason: ${result.reason}\n${acceptedChildSummary.orEmpty()}"
        } else {
            acceptedChildSummary = null
            val reason = when {
                inviteResult !is DebugPairingInviteCodecResult.Decoded -> "Invite was not available or could not be decoded"
                acceptanceResult is DebugPairingAcceptanceCodecResult.Rejected -> acceptanceResult.reason
                else -> "Acceptance could not be decoded"
            }
            "Pairing result: ${PairingStatus.REJECTED}\nReason: $reason"
        }
        stateStore.save(createSnapshot())
    }

    private fun buildDebugPolicyUpdate() {
        val result = runCatching {
            val targetDeviceId = targetDeviceInput.text.toString().trim()
            require(targetDeviceId.isNotBlank()) { "Target child device id is required" }
            val policyVersion = policyVersionInput.text.toString().trim()
            require(policyVersion.isNotBlank()) { "Policy version is required" }
            val issuedAtMillis = System.currentTimeMillis()
            val update = SignedPolicyUpdate(
                updateId = "debug-$policyVersion",
                targetDeviceId = DeviceId(targetDeviceId),
                policyVersion = PolicyVersion(policyVersion),
                issuedAtMillis = issuedAtMillis,
                expiresAtMillis = issuedAtMillis + DEBUG_UPDATE_TTL_MILLIS,
                signature = PolicyUpdateSignature("debug-signature"),
                policy = Policy(
                    id = PolicyId("debug-$policyVersion"),
                    mode = LockdownMode.STANDARD,
                    allowedDomains = allowDomainsInput.text.toString().toDomainSet(),
                    blockedDomains = blockDomainsInput.text.toString().toDomainSet(),
                    allowedPackages = emptySet(),
                    blockedPackages = emptySet(),
                    blockUnknownDomains = blockUnknownInput.isChecked,
                    blockKnownProxyDomains = blockKnownProxyInput.isChecked,
                ),
            )
            codec.encode(update)
        }

        lastPayload = result.getOrDefault("")
        payloadOutput.text = result.getOrElse { throwable ->
            "Could not build debug policy update: ${throwable.message}"
        }
        stateStore.save(createSnapshot())
    }

    private fun copyPayload() {
        if (lastPayload.isBlank()) {
            buildDebugPolicyUpdate()
        }
        if (lastPayload.isBlank()) {
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain debug policy update", lastPayload))
        payloadOutput.text = "$lastPayload\n\nCopied debug policy update."
        stateStore.save(createSnapshot())
    }

    private fun createSnapshot(): ParentDebugStateSnapshot {
        return ParentDebugStateSnapshot(
            targetChildDeviceId = targetDeviceInput.text.toString(),
            policyVersion = policyVersionInput.text.toString(),
            allowDomainsText = allowDomainsInput.text.toString(),
            blockDomainsText = blockDomainsInput.text.toString(),
            blockKnownProxyDomains = blockKnownProxyInput.isChecked,
            blockUnknownDomains = blockUnknownInput.isChecked,
            latestGeneratedPayload = lastPayload.takeIf(String::isNotBlank),
            pairingSessionId = pairingSessionInput.text.toString(),
            parentDeviceId = parentDeviceInput.text.toString(),
            parentDisplayName = parentDisplayNameInput.text.toString(),
            parentFingerprint = parentFingerprintInput.text.toString(),
            verificationCode = verificationCodeInput.text.toString(),
            latestPairingInvitePayload = lastPairingInvitePayload.takeIf(String::isNotBlank),
            latestPairingAcceptancePayload = lastPairingAcceptancePayload.takeIf(String::isNotBlank),
            acceptedChildSummary = acceptedChildSummary,
            latestHardeningSetupReportPayload = lastHardeningSetupReportPayload.takeIf(String::isNotBlank),
            latestChildSecurityStatusReportPayload = lastChildSecurityStatusReportPayload.takeIf(String::isNotBlank),
        )
    }

    private fun decodeHardeningSetupReport() {
        lastHardeningSetupReportPayload = setupReportInput.text.toString()
        setupReportOutput.text = when (val result = hardeningSetupReportCodec.decode(lastHardeningSetupReportPayload)) {
            is DebugHardeningSetupReportCodecResult.Decoded -> {
                decodedHardeningSetupReport = result.snapshot
                createHardeningSetupOutput()
            }
            is DebugHardeningSetupReportCodecResult.Rejected -> {
                decodedHardeningSetupReport = null
                "Setup report rejected: ${result.reason}"
            }
        }
        stateStore.save(createSnapshot())
    }

    private fun createHardeningSetupOutput(): String {
        val snapshot = decodedHardeningSetupReport
            ?: hardeningSetupReportCodec.decode(lastHardeningSetupReportPayload)
                .let { result -> (result as? DebugHardeningSetupReportCodecResult.Decoded)?.snapshot }
            ?: return "No child setup report decoded yet"
        decodedHardeningSetupReport = snapshot
        val lines = mutableListOf(
            "Summary status: ${snapshot.summaryStatus.toDisplayLabel()}",
            "Child device id: ${snapshot.childDeviceId.value}",
            "Generated at: ${snapshot.generatedAtMillis}",
            "Maintenance window: ${snapshot.activeMaintenanceWindow?.windowId ?: "none"}",
            "PIN compromise signal: ${snapshot.latestPinCompromiseSignal.toDisplayLabel()}",
        )
        listOf(
            HardeningSetupStep.VPN_PERMISSION,
            HardeningSetupStep.VPN_ALWAYS_ON,
            HardeningSetupStep.BLOCK_WITHOUT_VPN,
            HardeningSetupStep.SETTINGS_APP_LOCK,
            HardeningSetupStep.SCREEN_PINNING_WITH_PIN,
            HardeningSetupStep.BATTERY_OPTIMIZATION,
            HardeningSetupStep.PRIVATE_DNS_REVIEW,
            HardeningSetupStep.UNKNOWN_SOURCES_REVIEWED,
            HardeningSetupStep.DEVELOPER_OPTIONS_DISABLED,
            HardeningSetupStep.USB_DEBUGGING_DISABLED,
            HardeningSetupStep.WIRELESS_DEBUGGING_DISABLED,
            HardeningSetupStep.NO_UNRESTRICTED_SECONDARY_USERS,
            HardeningSetupStep.NO_UNRESTRICTED_WORK_PROFILE,
            HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY,
            HardeningSetupStep.PARENT_PIN_NOT_SHARED,
            HardeningSetupStep.PIN_COMPROMISE_REVIEW,
        ).forEach { step ->
            val item = snapshot.itemFor(step)
            lines += "${step.toDisplayLabel()}: ${item.status.toDisplayLabel()} / ${item.evidenceType.toDisplayLabel()} / ${item.note ?: "No note"}"
        }
        return lines.joinToString(separator = "\n")
    }

    private fun decodeChildSecurityStatusReport() {
        lastChildSecurityStatusReportPayload = childSecurityReportInput.text.toString()
        childSecurityReportOutput.text = when (val result = childSecurityReportCodec.decode(lastChildSecurityStatusReportPayload)) {
            is DebugChildSecurityReportCodecResult.Decoded -> {
                decodedChildSecurityStatusReport = result.report
                createChildSecurityStatusOutput()
            }
            is DebugChildSecurityReportCodecResult.Rejected -> {
                decodedChildSecurityStatusReport = null
                "Child security status report rejected: ${result.reason}"
            }
        }
        stateStore.save(createSnapshot())
    }

    private fun clearChildSecurityStatusReport() {
        lastChildSecurityStatusReportPayload = ""
        decodedChildSecurityStatusReport = null
        childSecurityReportInput.setText("")
        childSecurityReportOutput.text = createChildSecurityStatusOutput()
        stateStore.save(createSnapshot())
    }

    private fun createChildSecurityStatusOutput(): String {
        val report = decodedChildSecurityStatusReport
            ?: childSecurityReportCodec.decode(lastChildSecurityStatusReportPayload)
                .let { result -> (result as? DebugChildSecurityReportCodecResult.Decoded)?.report }
            ?: return "No child security status report decoded yet"
        decodedChildSecurityStatusReport = report
        return listOf(
            "Overall status: ${report.overallStatus.toDisplayLabel()}",
            "Child device id: ${report.childDeviceId.value}",
            "Generated at: ${report.generatedAtMillis}",
            "Policy version: ${report.policyVersion ?: "none"}",
            "VPN/session: ${report.vpnSessionLabel ?: "Unknown"}",
            "Setup summary: ${report.setupSummaryLabel ?: "Unknown"}",
            "Heartbeat: ${report.heartbeatLabel ?: "Unknown"}",
            "Bypass risk: ${report.bypassRiskLabel ?: "Unknown"}",
            "Signals: ${report.signals.toDisplayLabels()}",
            report.warningText,
        ).joinToString(separator = "\n")
    }

    private fun createPairingOutput(): String {
        return when {
            acceptedChildSummary != null -> "Paired child summary:\n$acceptedChildSummary"
            lastPairingInvitePayload.isNotBlank() -> lastPairingInvitePayload
            else -> "No debug pairing invite built yet"
        }
    }

    private fun String.toDomainSet(): Set<DomainName> {
        return split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(DomainName::from)
            .toSet()
    }

    private fun centerLabel(text: String, textSize: Float): TextView {
        return TextView(this).apply {
            this.text = text
            this.textSize = textSize
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 10)
        }
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 20f
            setPadding(0, 28, 0, 8)
        }
    }

    private fun valueLabel(text: String, textSize: Float): TextView {
        return TextView(this).apply {
            this.text = text
            this.textSize = textSize
            setPadding(0, 8, 0, 8)
        }
    }

    private fun editText(initialText: String): EditText {
        return EditText(this).apply {
            setText(initialText)
            setSingleLine(false)
            setPadding(0, 8, 0, 8)
        }
    }

    private fun checkBox(text: String, checked: Boolean): CheckBox {
        return CheckBox(this).apply {
            this.text = text
            isChecked = checked
        }
    }

    private fun labeledField(label: String, field: EditText): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            addView(valueLabel(label, 14f))
            addView(field)
        }
    }

    private fun button(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
        }
    }

    private fun HardeningSummaryStatus.toDisplayLabel(): String {
        return when (this) {
            HardeningSummaryStatus.NOT_STARTED -> "Unknown"
            HardeningSummaryStatus.IN_PROGRESS -> "Needs attention"
            HardeningSummaryStatus.READY_FOR_LAB_TEST -> "Ready for lab test"
            HardeningSummaryStatus.NEEDS_ATTENTION -> "Needs attention"
            HardeningSummaryStatus.UNKNOWN -> "Unknown"
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
            "Parent PIN may be compromised / ${changedStep?.toDisplayLabel() ?: "unknown step"} / ${changedAtMillis ?: "unknown time"}"
        } else {
            "No signal"
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
            HardeningSetupStep.SETTINGS_LOCK_PARENT_PIN_ONLY -> "Settings/App Lock locked"
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

    private companion object {
        const val DEBUG_UPDATE_TTL_MILLIS = 7L * 24L * 60L * 60L * 1_000L
        val defaultPairingCapabilities = setOf(
            PairingCapability.POLICY_UPDATES,
            PairingCapability.HEARTBEAT_STATUS,
            PairingCapability.ENCRYPTED_ALERTS,
            PairingCapability.PARENT_REVIEW,
        )
    }
}
