package com.vordain.guard.parent

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.vordain.guard.core.alertcenter.AlertStatus
import com.vordain.guard.core.alertcenter.AlertSeverity
import com.vordain.guard.core.alertcenter.DebugChildAlertReport
import com.vordain.guard.core.alertcenter.DebugChildAlertReportCodec
import com.vordain.guard.core.alertcenter.DebugChildAlertReportCodecResult
import com.vordain.guard.core.auditlog.VordainDebugPayloadEnvelope
import com.vordain.guard.core.auditlog.VordainDebugPayloadEnvelopeCodec
import com.vordain.guard.core.auditlog.VordainDebugPayloadKind
import com.vordain.guard.core.model.DeviceId
import com.vordain.guard.core.model.DomainName
import com.vordain.guard.core.model.PolicyId
import com.vordain.guard.core.intelligence.EncryptedDnsResolverSeedList
import com.vordain.guard.core.policy.DefaultPolicyEngine
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
import com.vordain.guard.core.policypresets.PolicyPreset
import com.vordain.guard.core.policypresets.PolicyPresetFactory
import com.vordain.guard.core.policysync.DebugPolicyUpdateCodec
import com.vordain.guard.core.policysync.PolicyUpdateSignature
import com.vordain.guard.core.policysync.PolicyVersion
import com.vordain.guard.core.policysync.SignedPolicyUpdate
import com.vordain.guard.core.statusreport.ChildSecurityActiveMode
import com.vordain.guard.core.statusreport.ChildSecurityOverallStatus
import com.vordain.guard.core.statusreport.ChildSecuritySignal
import com.vordain.guard.core.statusreport.ChildSecurityStatusReport
import com.vordain.guard.core.statusreport.DebugChildSecurityReportCodec
import com.vordain.guard.core.statusreport.DebugChildSecurityReportCodecResult
import com.vordain.guard.core.syncbundle.SyncBundle
import com.vordain.guard.core.syncbundle.SyncBundleCodec
import com.vordain.guard.core.syncbundle.SyncBundleDirection
import com.vordain.guard.core.syncbundle.SyncBundleKind
import com.vordain.guard.core.syncbundle.SyncBundlePayload
import com.vordain.guard.core.syncbundle.SyncBundlePayloadKind
import com.vordain.guard.features.bypassrisk.BypassRiskCategory
import com.vordain.guard.features.bypassrisk.BypassRiskOverallStatus
import com.vordain.guard.features.bypassrisk.BypassRiskStatus
import com.vordain.guard.features.bypassrisk.BypassRiskSummary
import com.vordain.guard.features.bypassrisk.DebugBypassRiskReport
import com.vordain.guard.features.bypassrisk.DebugBypassRiskReportCodec
import com.vordain.guard.features.bypassrisk.DebugBypassRiskReportCodecResult
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
    private val childAlertReportCodec = DebugChildAlertReportCodec()
    private val bypassRiskReportCodec = DebugBypassRiskReportCodec()
    private val debugPayloadEnvelopeCodec = VordainDebugPayloadEnvelopeCodec()
    private val syncBundleCodec = SyncBundleCodec()
    private val policyPresetFactory = PolicyPresetFactory()
    private val policyPreviewEngine = DefaultPolicyEngine()
    private val encryptedDnsResolverSeedList = EncryptedDnsResolverSeedList()
    private lateinit var stateStore: ParentDebugStateStore
    private lateinit var reportHistoryStore: ParentReportHistoryStore
    private lateinit var targetDeviceInput: EditText
    private lateinit var policyVersionInput: EditText
    private lateinit var allowDomainsInput: EditText
    private lateinit var blockDomainsInput: EditText
    private lateinit var blockKnownProxyInput: CheckBox
    private lateinit var blockEncryptedDnsInput: CheckBox
    private lateinit var blockUnknownInput: CheckBox
    private lateinit var presetDescriptionOutput: TextView
    private lateinit var policySummaryOutput: TextView
    private lateinit var policyPreviewDomainInput: EditText
    private lateinit var policyPreviewOutput: TextView
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
    private lateinit var childDnsGuardStatusOutput: TextView
    private lateinit var childAlertReportInput: EditText
    private lateinit var childAlertReportOutput: TextView
    private lateinit var bypassRiskReportInput: EditText
    private lateinit var bypassRiskReportOutput: TextView
    private lateinit var childSyncBundleInput: EditText
    private lateinit var childSyncBundleOutput: TextView
    private lateinit var parentSyncBundleOutput: TextView
    private lateinit var reportHistoryOutput: TextView
    private var lastPayload: String = ""
    private var lastPairingInvitePayload: String = ""
    private var lastPairingAcceptancePayload: String = ""
    private var acceptedChildSummary: String? = null
    private var lastHardeningSetupReportPayload: String = ""
    private var decodedHardeningSetupReport: HardeningSetupSnapshot? = null
    private var lastChildSecurityStatusReportPayload: String = ""
    private var decodedChildSecurityStatusReport: ChildSecurityStatusReport? = null
    private var lastChildAlertReportPayload: String = ""
    private var decodedChildAlertReport: DebugChildAlertReport? = null
    private var lastBypassRiskReportPayload: String = ""
    private var decodedBypassRiskReport: DebugBypassRiskReport? = null
    private var lastChildSyncBundlePayload: String = ""
    private var lastParentSyncBundlePayload: String = ""
    private var decodedChildSyncBundle: SyncBundle? = null
    private var selectedPolicyPreset: PolicyPreset = PolicyPreset.BASIC_DNS_GUARD
    private var reportHistory: List<ParentReportHistoryEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateStore = ParentDebugStateStore(this)
        reportHistoryStore = ParentReportHistoryStore(this)
        reportHistory = reportHistoryStore.load()
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
        layout.addView(sectionTitle("Recommended local MVP order"))
        layout.addView(valueLabel(
            listOf(
                "1. Build/edit DNS policy.",
                "2. Export parent sync bundle.",
                "3. Import child sync bundle.",
                "4. Review child status, alerts, and hardening.",
                "5. Adjust policy.",
            ).joinToString(separator = "\n"),
            14f,
        ))

        val snapshot = stateStore.load()
        lastPayload = snapshot.latestGeneratedPayload.orEmpty()
        lastPairingInvitePayload = snapshot.latestPairingInvitePayload.orEmpty()
        lastPairingAcceptancePayload = snapshot.latestPairingAcceptancePayload.orEmpty()
        acceptedChildSummary = snapshot.acceptedChildSummary
        lastHardeningSetupReportPayload = snapshot.latestHardeningSetupReportPayload.orEmpty()
        lastChildSecurityStatusReportPayload = snapshot.latestChildSecurityStatusReportPayload.orEmpty()
        lastChildAlertReportPayload = snapshot.latestChildAlertReportPayload.orEmpty()
        lastBypassRiskReportPayload = snapshot.latestBypassRiskReportPayload.orEmpty()
        lastChildSyncBundlePayload = snapshot.latestChildSyncBundlePayload.orEmpty()
        lastParentSyncBundlePayload = snapshot.latestParentSyncBundlePayload.orEmpty()
        selectedPolicyPreset = snapshot.selectedPolicyPreset.toPolicyPreset()
        pairingSessionInput = editText(snapshot.pairingSessionId)
        parentDeviceInput = editText(snapshot.parentDeviceId)
        parentDisplayNameInput = editText(snapshot.parentDisplayName)
        parentFingerprintInput = editText(snapshot.parentFingerprint)
        verificationCodeInput = editText(snapshot.verificationCode)
        childAcceptanceInput = editText(lastPairingAcceptancePayload)
        setupReportInput = editText(lastHardeningSetupReportPayload)
        childSecurityReportInput = editText(lastChildSecurityStatusReportPayload)
        childAlertReportInput = editText(lastChildAlertReportPayload)
        bypassRiskReportInput = editText(lastBypassRiskReportPayload)
        childSyncBundleInput = editText(lastChildSyncBundlePayload)
        targetDeviceInput = editText(snapshot.targetChildDeviceId)
        policyVersionInput = editText(snapshot.policyVersion)
        allowDomainsInput = editText(snapshot.allowDomainsText)
        blockDomainsInput = editText(snapshot.blockDomainsText)
        blockKnownProxyInput = checkBox("Block known proxy domains", checked = snapshot.blockKnownProxyDomains)
        blockEncryptedDnsInput = checkBox("Block encrypted DNS resolver domains", checked = snapshot.blockEncryptedDnsResolvers)
        blockUnknownInput = checkBox("Block unknown domains", checked = snapshot.blockUnknownDomains)

        layout.addView(sectionTitle("Child DNS Guard status"))
        layout.addView(valueLabel("DNS-only mode is not full protection.", 14f))
        layout.addView(valueLabel("Production sync will use encrypted relay later.", 14f))
        layout.addView(button("Build/edit DNS policy") {
            buildDebugPolicyUpdate()
            childDnsGuardStatusOutput.text = createChildDnsGuardStatusOutput()
        })
        layout.addView(button("Import child status report") {
            decodeChildSecurityStatusReport()
            childDnsGuardStatusOutput.text = createChildDnsGuardStatusOutput()
        })
        layout.addView(button("Import bypass report") {
            decodeBypassRiskReport()
            childDnsGuardStatusOutput.text = createChildDnsGuardStatusOutput()
        })
        layout.addView(button("View report history") {
            reportHistoryOutput.text = createReportHistoryDisplay()
        })
        layout.addView(button("Share latest policy payload") {
            sharePolicyPayload()
        })
        childDnsGuardStatusOutput = valueLabel(createChildDnsGuardStatusOutput(), 14f)
        layout.addView(childDnsGuardStatusOutput)

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
        layout.addView(button("Share pairing invite") {
            sharePairingInvite()
        })
        layout.addView(labeledField("Paste child acceptance", childAcceptanceInput))
        layout.addView(button("Verify child acceptance") {
            verifyChildAcceptance()
        })
        pairingOutput = valueLabel(createPairingOutput(), 14f)
        layout.addView(pairingOutput)
        layout.addView(valueLabel("Production pairing will use encrypted delivery and real key verification later.", 14f))

        layout.addView(sectionTitle("DNS policy editor"))
        layout.addView(valueLabel("DNS-only filtering is not full protection.", 14f))
        layout.addView(valueLabel("Production sync will use encrypted relay later.", 14f))
        layout.addView(labeledField("Target child device id", targetDeviceInput))
        layout.addView(labeledField("Policy version", policyVersionInput))
        layout.addView(valueLabel("Policy preset", 14f))
        layout.addView(horizontalButtons(
            "Basic DNS Guard" to { selectPolicyPreset(PolicyPreset.BASIC_DNS_GUARD) },
            "Strict Browser" to { selectPolicyPreset(PolicyPreset.STRICT_BROWSER) },
        ))
        layout.addView(horizontalButtons(
            "School Friendly" to { selectPolicyPreset(PolicyPreset.SCHOOL_FRIENDLY) },
            "High Risk Lockdown" to { selectPolicyPreset(PolicyPreset.HIGH_RISK_LOCKDOWN) },
        ))
        layout.addView(button("Custom") {
            selectPolicyPreset(PolicyPreset.CUSTOM)
        })
        presetDescriptionOutput = valueLabel(createPresetDescription(), 14f)
        layout.addView(presetDescriptionOutput)
        layout.addView(labeledField("Allow domains", allowDomainsInput))
        layout.addView(labeledField("Block domains", blockDomainsInput))
        layout.addView(blockKnownProxyInput)
        layout.addView(blockEncryptedDnsInput)
        layout.addView(blockUnknownInput)
        policySummaryOutput = valueLabel(createPolicySummary(), 14f)
        layout.addView(policySummaryOutput)

        layout.addView(button("Build debug policy update") {
            buildDebugPolicyUpdate()
        })
        layout.addView(button("Copy debug policy update") {
            copyPayload()
        })
        layout.addView(button("Share debug policy update") {
            sharePolicyPayload()
        })
        layout.addView(sectionTitle("Policy preview"))
        policyPreviewDomainInput = editText("blocked.example")
        layout.addView(labeledField("Test domain", policyPreviewDomainInput))
        layout.addView(button("Preview selected policy") {
            previewSelectedPolicy()
        })
        policyPreviewOutput = valueLabel("No policy preview yet", 14f)
        layout.addView(policyPreviewOutput)

        layout.addView(sectionTitle("Payload preview"))
        payloadOutput = valueLabel(lastPayload.ifBlank { "No debug policy update built yet" }, 14f)
        layout.addView(payloadOutput)
        layout.addView(valueLabel("Production policy sync will use signed encrypted delivery later.", 14f))

        layout.addView(sectionTitle("Export parent sync bundle"))
        layout.addView(valueLabel("Local debug bundle only.", 14f))
        layout.addView(valueLabel("Production sync will use encrypted relay later.", 14f))
        layout.addView(valueLabel("Child app must verify policy payload before using it.", 14f))
        layout.addView(button("Build parent sync bundle") {
            buildParentSyncBundle()
        })
        layout.addView(button("Copy parent sync bundle") {
            copyParentSyncBundle()
        })
        layout.addView(button("Share parent sync bundle") {
            shareParentSyncBundle()
        })
        parentSyncBundleOutput = valueLabel(createParentSyncBundleOutput(), 13f)
        layout.addView(parentSyncBundleOutput)

        layout.addView(sectionTitle("Child hardening setup"))
        layout.addView(valueLabel("This debug report is parent/child copy-paste only. Production will use encrypted delivery later.", 14f))
        layout.addView(valueLabel("Vordain does not receive or record the PIN.", 14f))
        layout.addView(valueLabel("Compromise signals are based on setup state changes outside parent-authorized maintenance windows.", 14f))
        layout.addView(labeledField("Paste setup report", setupReportInput))
        layout.addView(button("Decode setup report") {
            decodeHardeningSetupReport()
        })
        layout.addView(button("Share latest setup report") {
            shareLatestSetupReport()
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
        layout.addView(button("Share latest status report") {
            shareLatestChildSecurityStatusReport()
        })
        childSecurityReportOutput = valueLabel(createChildSecurityStatusOutput(), 14f)
        layout.addView(childSecurityReportOutput)

        layout.addView(sectionTitle("Child alerts"))
        layout.addView(valueLabel("Debug/local report import only.", 14f))
        layout.addView(valueLabel("Production alerts will use encrypted relay later.", 14f))
        layout.addView(valueLabel("Vordain does not receive PINs or account secrets.", 14f))
        layout.addView(labeledField("Paste child alert report", childAlertReportInput))
        layout.addView(button("Import child alert report") {
            decodeChildAlertReport()
        })
        layout.addView(button("Copy latest imported alert report") {
            copyLatestChildAlertReport()
        })
        layout.addView(button("Clear local alert report history") {
            clearChildAlertReportHistory()
        })
        layout.addView(button("Share latest imported alert summary") {
            shareLatestChildAlertSummary()
        })
        childAlertReportOutput = valueLabel(createChildAlertReportOutput(), 14f)
        layout.addView(childAlertReportOutput)

        layout.addView(sectionTitle("Import child sync bundle"))
        layout.addView(valueLabel("Local debug bundle only.", 14f))
        layout.addView(valueLabel("Production sync will use encrypted relay later.", 14f))
        layout.addView(labeledField("Paste child sync bundle", childSyncBundleInput))
        layout.addView(button("Import child sync bundle") {
            importChildSyncBundle()
        })
        layout.addView(button("Clear child sync bundle import") {
            clearChildSyncBundleImport()
        })
        childSyncBundleOutput = valueLabel(createChildSyncBundleImportOutput(), 13f)
        layout.addView(childSyncBundleOutput)

        layout.addView(sectionTitle("DNS-only bypass risk"))
        layout.addView(valueLabel("DNS-only filtering is not full protection.", 14f))
        layout.addView(valueLabel("Production sync will use encrypted relay later.", 14f))
        layout.addView(labeledField("Paste bypass-risk report", bypassRiskReportInput))
        layout.addView(button("Decode bypass-risk report") {
            decodeBypassRiskReport()
        })
        layout.addView(button("Clear bypass-risk report") {
            clearBypassRiskReport()
        })
        layout.addView(button("Share latest bypass-risk report") {
            shareLatestBypassRiskReport()
        })
        bypassRiskReportOutput = valueLabel(createBypassRiskOutput(), 14f)
        layout.addView(bypassRiskReportOutput)

        layout.addView(sectionTitle("Local report history"))
        layout.addView(valueLabel("Local debug history only.", 14f))
        layout.addView(valueLabel("Production sync will use encrypted relay later.", 14f))
        layout.addView(valueLabel("No PINs or account secrets.", 14f))
        layout.addView(button("Copy latest report") {
            copyLatestHistoryEntry()
        })
        layout.addView(button("Share latest report") {
            shareLatestHistoryEntry()
        })
        layout.addView(button("Clear report history") {
            clearReportHistory()
        })
        reportHistoryOutput = valueLabel(createReportHistoryDisplay(), 14f)
        layout.addView(reportHistoryOutput)

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
        if (result.isSuccess) {
            appendReportHistory(
                type = "PAIRING_INVITE",
                title = "Pairing invite",
                payload = lastPairingInvitePayload,
            )
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

    private fun sharePairingInvite() {
        if (lastPairingInvitePayload.isBlank()) {
            buildPairingInvite()
        }
        shareEnvelope(
            kind = VordainDebugPayloadKind.PAIRING_INVITE,
            title = "Share Vordain pairing invite",
            payload = lastPairingInvitePayload,
        )
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
                appendReportHistory(
                    type = "PAIRING_ACCEPTANCE",
                    title = "Pairing acceptance",
                    payload = lastPairingAcceptancePayload,
                )
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
            val selectedPolicy = createSelectedPolicy(policyVersion)
            val presetDefinition = policyPresetFactory.describe(selectedPolicyPreset)
            val update = SignedPolicyUpdate(
                updateId = "debug-$policyVersion",
                targetDeviceId = DeviceId(targetDeviceId),
                policyVersion = PolicyVersion(policyVersion),
                issuedAtMillis = issuedAtMillis,
                expiresAtMillis = issuedAtMillis + DEBUG_UPDATE_TTL_MILLIS,
                signature = PolicyUpdateSignature("debug-signature"),
                presetName = selectedPolicyPreset.name,
                blockEncryptedDnsResolvers = blockEncryptedDnsInput.isChecked,
                policyDisplayLabel = presetDefinition.displayName,
                policy = selectedPolicy,
            )
            codec.encode(update)
        }

        lastPayload = result.getOrDefault("")
        payloadOutput.text = result.getOrElse { throwable ->
            "Could not build debug policy update: ${throwable.message}"
        }
        if (result.isSuccess) {
            payloadOutput.text = "${result.getOrDefault("")}\n\n${createPolicySummary()}"
            appendReportHistory(
                type = "PARENT_POLICY_EDITED",
                title = "Parent DNS policy edited",
                payload = lastPayload,
            )
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

    private fun sharePolicyPayload() {
        if (lastPayload.isBlank()) {
            buildDebugPolicyUpdate()
        }
        shareEnvelope(
            kind = VordainDebugPayloadKind.POLICY_UPDATE,
            title = "Share Vordain DNS policy update",
            payload = lastPayload,
        )
        appendReportHistory(
            type = "PARENT_POLICY_SHARED",
            title = "Parent DNS policy shared",
            payload = lastPayload,
        )
    }

    private fun buildParentSyncBundle() {
        if (lastPayload.isBlank()) {
            buildDebugPolicyUpdate()
        }
        val payloads = mutableListOf<SyncBundlePayload>()
        if (lastPayload.isNotBlank()) {
            payloads += SyncBundlePayload(
                kind = SyncBundlePayloadKind.POLICY_UPDATE,
                label = "Verified-on-child policy update payload",
                payloadText = lastPayload,
            )
        }
        if (lastPairingInvitePayload.isNotBlank()) {
            payloads += SyncBundlePayload(
                kind = SyncBundlePayloadKind.PAIRING_INVITE,
                label = "Debug pairing invite",
                payloadText = lastPairingInvitePayload,
            )
        }
        if (lastPairingAcceptancePayload.isNotBlank()) {
            payloads += SyncBundlePayload(
                kind = SyncBundlePayloadKind.PAIRING_ACCEPTANCE,
                label = "Debug pairing acceptance",
                payloadText = lastPairingAcceptancePayload,
            )
        }
        payloads += SyncBundlePayload(
            kind = SyncBundlePayloadKind.DIAGNOSTICS_TEXT,
            label = "Parent setup note",
            payloadText = listOf(
                "Local debug parent instructions.",
                "Child app must verify policy payload before using it.",
                "Production sync will use encrypted relay later.",
                "Not full protection.",
            ).joinToString(separator = "\n"),
        )
        val now = System.currentTimeMillis()
        val bundle = SyncBundle(
            bundleId = "parent-sync-$now",
            direction = SyncBundleDirection.PARENT_TO_CHILD,
            kind = SyncBundleKind.PARENT_POLICY_UPDATE,
            createdAtMillis = now,
            sourceDeviceId = DeviceId(parentDeviceInput.text.toString().trim()),
            targetDeviceId = targetDeviceInput.text.toString().trim()
                .takeIf(String::isNotBlank)
                ?.let(::DeviceId),
            payloads = payloads,
        )
        lastParentSyncBundlePayload = syncBundleCodec.encode(bundle)
        appendReportHistory(
            type = "PARENT_SYNC_BUNDLE",
            title = "Parent sync bundle",
            payload = lastParentSyncBundlePayload,
        )
        parentSyncBundleOutput.text = createParentSyncBundleOutput()
        stateStore.save(createSnapshot())
    }

    private fun copyParentSyncBundle() {
        if (lastParentSyncBundlePayload.isBlank()) {
            buildParentSyncBundle()
        }
        if (lastParentSyncBundlePayload.isBlank()) {
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain parent sync bundle", lastParentSyncBundlePayload))
        appendReportHistory(
            type = "PARENT_SYNC_BUNDLE_SHARED",
            title = "Parent sync bundle copied",
            payload = lastParentSyncBundlePayload,
        )
        parentSyncBundleOutput.text = "${createParentSyncBundleOutput()}\n\nCopied parent sync bundle."
        stateStore.save(createSnapshot())
    }

    private fun shareParentSyncBundle() {
        if (lastParentSyncBundlePayload.isBlank()) {
            buildParentSyncBundle()
        }
        if (lastParentSyncBundlePayload.isBlank()) {
            return
        }
        shareText(
            title = "Share Vordain parent sync bundle",
            text = "Local debug export\n$lastParentSyncBundlePayload",
        )
        appendReportHistory(
            type = "PARENT_SYNC_BUNDLE_SHARED",
            title = "Parent sync bundle shared",
            payload = lastParentSyncBundlePayload,
        )
        parentSyncBundleOutput.text = "${createParentSyncBundleOutput()}\n\nShared parent sync bundle."
        stateStore.save(createSnapshot())
    }

    private fun importChildSyncBundle() {
        lastChildSyncBundlePayload = childSyncBundleInput.text.toString()
        val result = syncBundleCodec.decode(lastChildSyncBundlePayload)
        if (!result.accepted || result.bundle == null) {
            decodedChildSyncBundle = null
            childSyncBundleOutput.text = "Child sync bundle rejected: ${result.reason}"
            stateStore.save(createSnapshot())
            return
        }
        val bundle = result.bundle ?: run {
            decodedChildSyncBundle = null
            childSyncBundleOutput.text = "Child sync bundle rejected: ${result.reason}"
            stateStore.save(createSnapshot())
            return
        }
        if (bundle.direction != SyncBundleDirection.CHILD_TO_PARENT) {
            decodedChildSyncBundle = null
            childSyncBundleOutput.text = "Child sync bundle rejected: wrong direction ${bundle.direction}"
            stateStore.save(createSnapshot())
            return
        }
        decodedChildSyncBundle = bundle
        bundle.payloads.forEach { payload ->
            when (payload.kind) {
                SyncBundlePayloadKind.CHILD_SECURITY_STATUS_REPORT -> {
                    lastChildSecurityStatusReportPayload = payload.payloadText
                    childSecurityReportInput.setText(payload.payloadText)
                    decodeChildSecurityStatusReport()
                }
                SyncBundlePayloadKind.CHILD_ALERT_REPORT -> {
                    lastChildAlertReportPayload = payload.payloadText
                    childAlertReportInput.setText(payload.payloadText)
                    decodeChildAlertReport()
                }
                SyncBundlePayloadKind.BYPASS_RISK_REPORT -> {
                    lastBypassRiskReportPayload = payload.payloadText
                    bypassRiskReportInput.setText(payload.payloadText)
                    decodeBypassRiskReport()
                }
                SyncBundlePayloadKind.HARDENING_SETUP_REPORT -> {
                    lastHardeningSetupReportPayload = payload.payloadText
                    setupReportInput.setText(payload.payloadText)
                    decodeHardeningSetupReport()
                }
                else -> Unit
            }
        }
        appendReportHistory(
            type = "CHILD_SYNC_BUNDLE",
            title = "Child sync bundle",
            payload = lastChildSyncBundlePayload,
        )
        childSyncBundleOutput.text = createChildSyncBundleImportOutput()
        if (::childDnsGuardStatusOutput.isInitialized) {
            childDnsGuardStatusOutput.text = createChildDnsGuardStatusOutput()
        }
        stateStore.save(createSnapshot())
    }

    private fun clearChildSyncBundleImport() {
        lastChildSyncBundlePayload = ""
        decodedChildSyncBundle = null
        childSyncBundleInput.setText("")
        childSyncBundleOutput.text = createChildSyncBundleImportOutput()
        stateStore.save(createSnapshot())
    }

    private fun createParentSyncBundleOutput(): String {
        if (lastParentSyncBundlePayload.isBlank()) {
            return "No parent sync bundle built yet\nLocal debug bundle only.\nProduction sync will use encrypted relay later."
        }
        val bundle = syncBundleCodec.decode(lastParentSyncBundlePayload).bundle
            ?: return "Parent sync bundle could not be decoded locally"
        return buildString {
            append("Parent sync bundle ready\n")
            append("Bundle id: ${bundle.bundleId}\n")
            append("Direction: ${bundle.direction}\n")
            append("Target child: ${bundle.targetDeviceId?.value ?: "unspecified"}\n")
            append("Payload count: ${bundle.payloads.size}\n")
            bundle.payloads.forEach { payload ->
                append("${payload.kind}: ${payload.label}\n")
            }
            append("Child app must verify policy payload before using it.\n")
            append(bundle.warningText)
        }.trimEnd()
    }

    private fun createChildSyncBundleImportOutput(): String {
        val bundle = decodedChildSyncBundle
            ?: syncBundleCodec.decode(lastChildSyncBundlePayload).bundle
            ?: return "No child sync bundle imported yet\nLocal debug bundle only.\nProduction sync will use encrypted relay later."
        decodedChildSyncBundle = bundle
        return buildString {
            append("Child sync bundle imported\n")
            append("Bundle id: ${bundle.bundleId}\n")
            append("Source child: ${bundle.sourceDeviceId.value}\n")
            append("Created at: ${bundle.createdAtMillis}\n")
            append("Included payloads:\n")
            bundle.payloads.forEach { payload ->
                append("- ${payload.kind}: ${payload.label}\n")
                if (
                    payload.kind == SyncBundlePayloadKind.ACTIVE_POLICY_SUMMARY ||
                    payload.kind == SyncBundlePayloadKind.AUDIT_SUMMARY ||
                    payload.kind == SyncBundlePayloadKind.DIAGNOSTICS_TEXT
                ) {
                    append(payload.payloadText.lineSequence().take(4).joinToString(separator = "\n"))
                    append('\n')
                }
            }
            append("Child status summary: ${decodedChildSecurityStatusReport?.overallStatus?.toDisplayLabel() ?: "Unknown"}\n")
            append("Alert summary: ${decodedChildAlertReport?.summaryLabel ?: "Unknown"}\n")
            append("Bypass risk summary: ${decodedBypassRiskReport?.summary?.overallStatus?.toDisplayLabel() ?: "Unknown"}\n")
            append(bundle.warningText)
        }.trimEnd()
    }

    private fun createSnapshot(): ParentDebugStateSnapshot {
        return ParentDebugStateSnapshot(
            targetChildDeviceId = targetDeviceInput.text.toString(),
            policyVersion = policyVersionInput.text.toString(),
            allowDomainsText = allowDomainsInput.text.toString(),
            blockDomainsText = blockDomainsInput.text.toString(),
            blockKnownProxyDomains = blockKnownProxyInput.isChecked,
            blockEncryptedDnsResolvers = blockEncryptedDnsInput.isChecked,
            blockUnknownDomains = blockUnknownInput.isChecked,
            selectedPolicyPreset = selectedPolicyPreset.name,
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
            latestBypassRiskReportPayload = lastBypassRiskReportPayload.takeIf(String::isNotBlank),
            latestChildAlertReportPayload = lastChildAlertReportPayload.takeIf(String::isNotBlank),
            latestChildSyncBundlePayload = lastChildSyncBundlePayload.takeIf(String::isNotBlank),
            latestParentSyncBundlePayload = lastParentSyncBundlePayload.takeIf(String::isNotBlank),
        )
    }

    private fun decodeHardeningSetupReport() {
        lastHardeningSetupReportPayload = setupReportInput.text.toString()
        setupReportOutput.text = when (val result = hardeningSetupReportCodec.decode(lastHardeningSetupReportPayload)) {
            is DebugHardeningSetupReportCodecResult.Decoded -> {
                decodedHardeningSetupReport = result.snapshot
                appendReportHistory(
                    type = "SETUP_REPORT",
                    title = "Child hardening setup report",
                    payload = lastHardeningSetupReportPayload,
                )
                createHardeningSetupOutput()
            }
            is DebugHardeningSetupReportCodecResult.Rejected -> {
                decodedHardeningSetupReport = null
                "Setup report rejected: ${result.reason}"
            }
        }
        stateStore.save(createSnapshot())
    }

    private fun shareLatestSetupReport() {
        shareEnvelope(
            kind = VordainDebugPayloadKind.SETUP_REPORT,
            title = "Share Vordain setup report",
            payload = lastHardeningSetupReportPayload,
        )
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
                appendReportHistory(
                    type = "CHILD_DNS_GUARD_STATUS_IMPORTED",
                    title = "Child DNS Guard status imported",
                    payload = lastChildSecurityStatusReportPayload,
                )
                if (::childDnsGuardStatusOutput.isInitialized) {
                    childDnsGuardStatusOutput.text = createChildDnsGuardStatusOutput()
                }
                createChildSecurityStatusOutput()
            }
            is DebugChildSecurityReportCodecResult.Rejected -> {
                decodedChildSecurityStatusReport = null
                "Child security status report rejected: ${result.reason}"
            }
        }
        stateStore.save(createSnapshot())
    }

    private fun shareLatestChildSecurityStatusReport() {
        shareEnvelope(
            kind = VordainDebugPayloadKind.CHILD_STATUS_REPORT,
            title = "Share Vordain child status report",
            payload = lastChildSecurityStatusReportPayload,
        )
    }

    private fun clearChildSecurityStatusReport() {
        lastChildSecurityStatusReportPayload = ""
        decodedChildSecurityStatusReport = null
        childSecurityReportInput.setText("")
        childSecurityReportOutput.text = createChildSecurityStatusOutput()
        if (::childDnsGuardStatusOutput.isInitialized) {
            childDnsGuardStatusOutput.text = createChildDnsGuardStatusOutput()
        }
        stateStore.save(createSnapshot())
    }

    private fun decodeChildAlertReport() {
        lastChildAlertReportPayload = childAlertReportInput.text.toString()
        childAlertReportOutput.text = when (val result = childAlertReportCodec.decode(lastChildAlertReportPayload)) {
            is DebugChildAlertReportCodecResult.Decoded -> {
                decodedChildAlertReport = result.report
                appendReportHistory(
                    type = "CHILD_ALERT_REPORT",
                    title = "Child alert report",
                    payload = lastChildAlertReportPayload,
                )
                createChildAlertReportOutput()
            }
            is DebugChildAlertReportCodecResult.Rejected -> {
                decodedChildAlertReport = null
                "Child alert report rejected: ${result.reason}"
            }
        }
        stateStore.save(createSnapshot())
    }

    private fun copyLatestChildAlertReport() {
        if (lastChildAlertReportPayload.isBlank()) {
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain child alert report", lastChildAlertReportPayload))
        childAlertReportOutput.text = "${createChildAlertReportOutput()}\n\nCopied latest imported alert report."
    }

    private fun clearChildAlertReportHistory() {
        lastChildAlertReportPayload = ""
        decodedChildAlertReport = null
        childAlertReportInput.setText("")
        reportHistory = reportHistory.filterNot { it.type == "CHILD_ALERT_REPORT" }
        reportHistoryStore.save(reportHistory)
        childAlertReportOutput.text = createChildAlertReportOutput()
        reportHistoryOutput.text = createReportHistoryDisplay()
        stateStore.save(createSnapshot())
    }

    private fun shareLatestChildAlertSummary() {
        val summary = createChildAlertReportOutput()
        shareEnvelope(
            kind = VordainDebugPayloadKind.DIAGNOSTICS,
            title = "Share Vordain child alert summary",
            payload = summary,
        )
    }

    private fun createChildAlertReportOutput(): String {
        val report = decodedChildAlertReport
            ?: childAlertReportCodec.decode(lastChildAlertReportPayload)
                .let { result -> (result as? DebugChildAlertReportCodecResult.Decoded)?.report }
            ?: return "No child alert report decoded yet"
        decodedChildAlertReport = report
        val activeCritical = report.alerts.count {
            it.status == AlertStatus.ACTIVE && it.severity == AlertSeverity.CRITICAL
        }
        return buildString {
            append("Active critical alerts: $activeCritical\n")
            append("Summary: ${report.summaryLabel}\n")
            append("Child device id: ${report.childDeviceId.value}\n")
            append("Generated at: ${report.generatedAtMillis}\n")
            append("Debug/local report import only.\n")
            append("Production alerts will use encrypted relay later.\n")
            append("Vordain does not receive PINs or account secrets.\n")
            report.alerts.forEach { alert ->
                append("${alert.occurredAtMillis} / ${alert.severity} / ${alert.status} / ${alert.type}\n")
                append("${alert.title}: ${alert.detail}\n")
                append("Policy version: ${alert.policyVersion ?: "none"}\n")
            }
            append(report.warningText)
        }
    }

    private fun createChildDnsGuardStatusOutput(): String {
        val report = decodedChildSecurityStatusReport
            ?: childSecurityReportCodec.decode(lastChildSecurityStatusReportPayload)
                .let { result -> (result as? DebugChildSecurityReportCodecResult.Decoded)?.report }
        val bypass = decodedBypassRiskReport
            ?: bypassRiskReportCodec.decode(lastBypassRiskReportPayload)
                .let { result -> (result as? DebugBypassRiskReportCodecResult.Decoded)?.report }
        if (report == null) {
            return listOf(
                "Basic DNS Guard: Unknown",
                "Readiness: Unknown",
                "Import a child status report to review mode, policy, hardening, bypass risk, and DNS counters.",
                "DNS-only mode is not full protection.",
                "Production sync will use encrypted relay later.",
            ).joinToString(separator = "\n")
        }
        decodedChildSecurityStatusReport = report
        val modeLabel = when (report.activeMode) {
            ChildSecurityActiveMode.BASIC_DNS_GUARD -> "Running"
            ChildSecurityActiveMode.DNS_ONLY_LAB -> "DNS-only lab active"
            ChildSecurityActiveMode.FULL_TUNNEL_LAB -> "Full-tunnel lab active"
            ChildSecurityActiveMode.NONE -> "Idle or stopped"
        }
        val readiness = when (report.overallStatus) {
            ChildSecurityOverallStatus.READY_FOR_LAB_TEST -> "Ready for DNS Guard"
            ChildSecurityOverallStatus.PIN_COMPROMISE_SUSPECTED -> "High risk"
            ChildSecurityOverallStatus.NEEDS_ATTENTION,
            ChildSecurityOverallStatus.SETUP_IN_PROGRESS,
            ChildSecurityOverallStatus.VPN_STOPPED -> "Needs attention"
            ChildSecurityOverallStatus.NOT_STARTED,
            ChildSecurityOverallStatus.UNKNOWN -> "Unknown"
        }
        return listOf(
            "Basic DNS Guard: $modeLabel",
            "Readiness: $readiness",
            "Active policy version: ${report.policyVersion ?: "none"}",
            "Active policy preset: ${report.activePolicyPreset ?: "unspecified"}",
            "Hardening highlights: ${report.setupSummaryLabel ?: "Unknown"}",
            "Bypass risk summary: ${bypass?.summary?.overallStatus?.toDisplayLabel() ?: report.bypassRiskLabel ?: "Unknown"}",
            "DNS blocked responses: ${report.dnsBlockedResponseCount}",
            "DNS allowed forwarded: ${report.dnsAllowedForwardedCount}",
            "DNS allowed failures: ${report.dnsAllowedForwardFailureCount}",
            "Active critical alerts: ${report.activeCriticalAlertCount}",
            "Heartbeat status: ${report.heartbeatStatusLabel ?: report.heartbeatLabel ?: "Unknown"}",
            "Latest alert severity: ${report.latestAlertSeverity ?: "none"}",
            "Non-DNS traffic is not inspected in DNS-only mode.",
            "DNS-only mode is not full protection.",
            "Production sync will use encrypted relay later.",
        ).joinToString(separator = "\n")
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
            "Active policy source: ${report.activePolicySource ?: "Unknown"}",
            "Active policy preset: ${report.activePolicyPreset ?: "unspecified"}",
            "Encrypted DNS blocking: ${if (report.encryptedDnsBlockingEnabled) "enabled" else "not requested"}",
            "Proxy blocking: ${if (report.proxyBlockingEnabled) "enabled" else "not requested"}",
            "Policy allow/block counts: ${report.policyAllowDomainCount}/${report.policyBlockDomainCount}",
            "VPN/session: ${report.vpnSessionLabel ?: "Unknown"}",
            "Setup summary: ${report.setupSummaryLabel ?: "Unknown"}",
            "Heartbeat: ${report.heartbeatLabel ?: "Unknown"}",
            "Heartbeat status: ${report.heartbeatStatusLabel ?: "Unknown"}",
            "Last heartbeat at: ${report.lastHeartbeatAtMillis ?: "none"}",
            "Active critical alerts: ${report.activeCriticalAlertCount}",
            "Latest alert severity: ${report.latestAlertSeverity ?: "none"}",
            "Alert summary: ${report.alertSummaryLabel ?: "none"}",
            "Bypass risk: ${report.bypassRiskLabel ?: "Unknown"}",
            "Active mode: ${report.activeMode.toDisplayLabel()}",
            "DNS blocked responses: ${report.dnsBlockedResponseCount}",
            "DNS allowed forwarded: ${report.dnsAllowedForwardedCount}",
            "DNS allowed forward failures: ${report.dnsAllowedForwardFailureCount}",
            if (report.activeMode == ChildSecurityActiveMode.BASIC_DNS_GUARD ||
                report.activeMode == ChildSecurityActiveMode.DNS_ONLY_LAB
            ) {
                "Non-DNS traffic is not inspected in DNS-only mode."
            } else {
                "DNS-only lab is not active."
            },
            "Not full protection.",
            "Signals: ${report.signals.toDisplayLabels()}",
            report.warningText,
        ).joinToString(separator = "\n")
    }

    private fun decodeBypassRiskReport() {
        lastBypassRiskReportPayload = bypassRiskReportInput.text.toString()
        bypassRiskReportOutput.text = when (val result = bypassRiskReportCodec.decode(lastBypassRiskReportPayload)) {
            is DebugBypassRiskReportCodecResult.Decoded -> {
                decodedBypassRiskReport = result.report
                appendReportHistory(
                    type = "BYPASS_REPORT",
                    title = "DNS-only bypass-risk report",
                    payload = lastBypassRiskReportPayload,
                )
                createBypassRiskOutput()
            }
            is DebugBypassRiskReportCodecResult.Rejected -> {
                decodedBypassRiskReport = null
                "Bypass-risk report rejected: ${result.reason}"
            }
        }
        stateStore.save(createSnapshot())
    }

    private fun shareLatestBypassRiskReport() {
        shareEnvelope(
            kind = VordainDebugPayloadKind.BYPASS_REPORT,
            title = "Share Vordain bypass-risk report",
            payload = lastBypassRiskReportPayload,
        )
    }

    private fun clearBypassRiskReport() {
        lastBypassRiskReportPayload = ""
        decodedBypassRiskReport = null
        bypassRiskReportInput.setText("")
        bypassRiskReportOutput.text = createBypassRiskOutput()
        stateStore.save(createSnapshot())
    }

    private fun createBypassRiskOutput(): String {
        val report = decodedBypassRiskReport
            ?: bypassRiskReportCodec.decode(lastBypassRiskReportPayload)
                .let { result -> (result as? DebugBypassRiskReportCodecResult.Decoded)?.report }
            ?: return "No bypass-risk report decoded yet"
        decodedBypassRiskReport = report
        val summary = report.summary
        return buildString {
            append("Child device id: ${report.childDeviceId.value}\n")
            append("Generated at: ${report.generatedAtMillis}\n")
            append("Overall bypass risk: ${summary.overallStatus.toDisplayLabel()}\n")
            append("Highest severity: ${summary.highestSeverity}\n")
            append(summary.warningText)
            append('\n')
            append("DoH resolver risk: ${summary.itemLabel(BypassRiskCategory.DNS_OVER_HTTPS)}\n")
            append("Private DNS: ${summary.itemLabel(BypassRiskCategory.PRIVATE_DNS)}\n")
            append("Alternate VPN app: ${summary.itemLabel(BypassRiskCategory.ALTERNATE_VPN_APP)}\n")
            append("Proxy app: ${summary.itemLabel(BypassRiskCategory.PROXY_APP)}\n")
            append("Private browser: ${summary.itemLabel(BypassRiskCategory.PRIVATE_BROWSER)}\n")
            append("Developer Options/ADB: ${summary.itemLabel(BypassRiskCategory.DEVELOPER_OPTIONS)} / ${summary.itemLabel(BypassRiskCategory.USB_DEBUGGING)}\n")
            append("Unrestricted profiles: ${summary.itemLabel(BypassRiskCategory.UNRESTRICTED_PROFILE)}\n")
            append("Direct IP limitation: ${summary.itemLabel(BypassRiskCategory.DIRECT_IP_ACCESS)}")
        }
    }

    private fun createPairingOutput(): String {
        return when {
            acceptedChildSummary != null -> "Paired child summary:\n$acceptedChildSummary"
            lastPairingInvitePayload.isNotBlank() -> lastPairingInvitePayload
            else -> "No debug pairing invite built yet"
        }
    }

    private fun appendReportHistory(
        type: String,
        title: String,
        payload: String,
    ) {
        if (payload.isBlank() || !::reportHistoryStore.isInitialized) {
            return
        }
        val now = System.currentTimeMillis()
        val entry = ParentReportHistoryEntry(
            id = "$now-$type",
            type = type,
            createdAtMillis = now,
            title = title,
            payload = payload,
        )
        reportHistory = (listOf(entry) + reportHistory).take(ParentReportHistoryStore.MAX_ENTRIES)
        reportHistoryStore.save(reportHistory)
        if (::reportHistoryOutput.isInitialized) {
            reportHistoryOutput.text = createReportHistoryDisplay()
        }
    }

    private fun createReportHistoryDisplay(): String {
        if (reportHistory.isEmpty()) {
            return "No local report history yet\nLocal debug history only.\nProduction sync will use encrypted relay later."
        }
        return buildString {
            append("Local debug history only.\n")
            append("Production sync will use encrypted relay later.\n")
            reportHistory.take(REPORT_HISTORY_DISPLAY_COUNT).forEach { entry ->
                append("${entry.createdAtMillis} / ${entry.type}\n")
                append("${entry.title}\n")
            }
        }.trimEnd()
    }

    private fun copyLatestHistoryEntry() {
        val entry = reportHistory.firstOrNull() ?: return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain local debug report", entry.payload))
        reportHistoryOutput.text = "${createReportHistoryDisplay()}\n\nCopied latest report."
    }

    private fun shareLatestHistoryEntry() {
        val entry = reportHistory.firstOrNull() ?: return
        shareEnvelope(
            kind = entry.type.toPayloadKind(),
            title = "Share Vordain local report",
            payload = entry.payload,
        )
    }

    private fun clearReportHistory() {
        reportHistory = emptyList()
        reportHistoryStore.save(reportHistory)
        reportHistoryOutput.text = createReportHistoryDisplay()
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
        }
        startActivity(Intent.createChooser(intent, title))
    }

    private fun String.toPayloadKind(): VordainDebugPayloadKind {
        return when (this) {
            "PAIRING_INVITE" -> VordainDebugPayloadKind.PAIRING_INVITE
            "PAIRING_ACCEPTANCE" -> VordainDebugPayloadKind.PAIRING_ACCEPTANCE
            "POLICY_UPDATE" -> VordainDebugPayloadKind.POLICY_UPDATE
            "SETUP_REPORT" -> VordainDebugPayloadKind.SETUP_REPORT
            "BYPASS_REPORT" -> VordainDebugPayloadKind.BYPASS_REPORT
            "CHILD_STATUS_REPORT" -> VordainDebugPayloadKind.CHILD_STATUS_REPORT
            else -> VordainDebugPayloadKind.DIAGNOSTICS
        }
    }

    private fun selectPolicyPreset(preset: PolicyPreset) {
        selectedPolicyPreset = preset
        val definition = policyPresetFactory.describe(preset)
        allowDomainsInput.setText(definition.defaultAllowedDomains.toDomainText())
        blockDomainsInput.setText(definition.defaultBlockedDomains.toDomainText())
        blockKnownProxyInput.isChecked = definition.blockKnownProxyDomains
        blockEncryptedDnsInput.isChecked = definition.blockEncryptedDnsResolvers
        blockUnknownInput.isChecked = definition.blockUnknownDomains
        presetDescriptionOutput.text = createPresetDescription()
        policySummaryOutput.text = createPolicySummary()
        stateStore.save(createSnapshot())
    }

    private fun createPresetDescription(): String {
        val definition = policyPresetFactory.describe(selectedPolicyPreset)
        return listOf(
            "Selected preset: ${definition.displayName}",
            definition.description,
            definition.warningText,
        ).joinToString(separator = "\n")
    }

    private fun createPolicySummary(): String {
        val selectedPolicy = createSelectedPolicy(policyVersionInput.text.toString().ifBlank { "debug-preview" })
        return listOf(
            "Policy summary",
            "Preset: ${policyPresetFactory.describe(selectedPolicyPreset).displayName}",
            "Allow count: ${selectedPolicy.allowedDomains.size}",
            "Block count: ${selectedPolicy.blockedDomains.size}",
            "Encrypted-DNS blocking enabled: ${blockEncryptedDnsInput.isChecked}",
            "Proxy blocking enabled: ${selectedPolicy.blockKnownProxyDomains}",
            "Unknown-domain behavior: ${if (selectedPolicy.blockUnknownDomains) "blocked" else "allowed unless listed"}",
            "DNS-only filtering is not full protection.",
        ).joinToString(separator = "\n")
    }

    private fun previewSelectedPolicy() {
        val output = runCatching {
            val domain = DomainName.from(policyPreviewDomainInput.text.toString())
            val selectedPolicy = createSelectedPolicy(policyVersionInput.text.toString().ifBlank { "debug-preview" })
            val encryptedDnsMatch = encryptedDnsResolverSeedList.classify(domain)
            val evaluation = policyPreviewEngine.evaluateDomain(domain, selectedPolicy)
            listOf(
                "Normalized domain: ${domain.value}",
                "Decision: ${if (encryptedDnsMatch != null && blockEncryptedDnsInput.isChecked) "BLOCK" else evaluation.decision.name}",
                "Reason: ${encryptedDnsMatch?.reason ?: evaluation.reason.name}",
                "Encrypted DNS seed block applies: ${encryptedDnsMatch != null && blockEncryptedDnsInput.isChecked}",
                "Event would be created: ${if (encryptedDnsMatch != null && blockEncryptedDnsInput.isChecked) "yes" else if (evaluation.shouldCreateEvent) "yes" else "no"}",
            ).joinToString(separator = "\n")
        }.getOrElse { throwable ->
            "Preview rejected: ${throwable.message}"
        }
        policyPreviewOutput.text = output
        policySummaryOutput.text = createPolicySummary()
        stateStore.save(createSnapshot())
    }

    private fun createSelectedPolicy(policyVersion: String): Policy {
        val definition = policyPresetFactory.describe(selectedPolicyPreset)
        return Policy(
            id = PolicyId("debug-$policyVersion"),
            mode = definition.lockdownMode,
            allowedDomains = allowDomainsInput.text.toString().toDomainSet(),
            blockedDomains = blockDomainsInput.text.toString().toDomainSet(),
            allowedPackages = emptySet(),
            blockedPackages = emptySet(),
            blockUnknownDomains = blockUnknownInput.isChecked,
            blockKnownProxyDomains = blockKnownProxyInput.isChecked,
        )
    }

    private fun String.toDomainSet(): Set<DomainName> {
        return split(',', '\n')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(DomainName::from)
            .toSet()
    }

    private fun Set<DomainName>.toDomainText(): String {
        return map(DomainName::value).sorted().joinToString(separator = "\n")
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

    private fun ChildSecurityActiveMode.toDisplayLabel(): String {
        return when (this) {
            ChildSecurityActiveMode.NONE -> "None"
            ChildSecurityActiveMode.BASIC_DNS_GUARD -> "Basic DNS Guard active"
            ChildSecurityActiveMode.DNS_ONLY_LAB -> "DNS-only lab active"
            ChildSecurityActiveMode.FULL_TUNNEL_LAB -> "Full-tunnel lab active"
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

    private fun String.toPolicyPreset(): PolicyPreset {
        return runCatching { PolicyPreset.valueOf(this) }.getOrDefault(PolicyPreset.CUSTOM)
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

    private fun BypassRiskSummary.itemLabel(category: BypassRiskCategory): String {
        val item = items.firstOrNull { it.category == category } ?: return "Unknown"
        return "${item.status.toDisplayLabel()} / ${item.severity} / ${item.evidenceLabel} / ${item.note}"
    }

    private companion object {
        const val DEBUG_UPDATE_TTL_MILLIS = 7L * 24L * 60L * 60L * 1_000L
        const val REPORT_HISTORY_DISPLAY_COUNT = 12
        val defaultPairingCapabilities = setOf(
            PairingCapability.POLICY_UPDATES,
            PairingCapability.HEARTBEAT_STATUS,
            PairingCapability.ENCRYPTED_ALERTS,
            PairingCapability.PARENT_REVIEW,
        )
    }
}
