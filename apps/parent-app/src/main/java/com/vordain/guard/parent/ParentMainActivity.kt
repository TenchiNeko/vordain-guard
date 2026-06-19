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

class ParentMainActivity : Activity() {
    private val codec = DebugPolicyUpdateCodec()
    private val inviteCodec = DebugPairingInviteCodec()
    private val acceptanceCodec = DebugPairingAcceptanceCodec()
    private val pairingEvaluator = PairingEvaluator()
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
    private var lastPayload: String = ""
    private var lastPairingInvitePayload: String = ""
    private var lastPairingAcceptancePayload: String = ""
    private var acceptedChildSummary: String? = null

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
        pairingSessionInput = editText(snapshot.pairingSessionId)
        parentDeviceInput = editText(snapshot.parentDeviceId)
        parentDisplayNameInput = editText(snapshot.parentDisplayName)
        parentFingerprintInput = editText(snapshot.parentFingerprint)
        verificationCodeInput = editText(snapshot.verificationCode)
        childAcceptanceInput = editText(lastPairingAcceptancePayload)
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
        )
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
