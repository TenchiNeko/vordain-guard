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
import com.vordain.guard.core.policy.Policy
import com.vordain.guard.core.policysync.DebugPolicyUpdateCodec
import com.vordain.guard.core.policysync.PolicyUpdateSignature
import com.vordain.guard.core.policysync.PolicyVersion
import com.vordain.guard.core.policysync.SignedPolicyUpdate

class ParentMainActivity : Activity() {
    private val codec = DebugPolicyUpdateCodec()
    private lateinit var targetDeviceInput: EditText
    private lateinit var policyVersionInput: EditText
    private lateinit var allowDomainsInput: EditText
    private lateinit var blockDomainsInput: EditText
    private lateinit var blockKnownProxyInput: CheckBox
    private lateinit var blockUnknownInput: CheckBox
    private lateinit var payloadOutput: TextView
    private var lastPayload: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createView())
    }

    private fun createView(): ScrollView {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        layout.addView(centerLabel("Vordain Guard Parent", 28f))
        layout.addView(centerLabel("Debug policy handoff", 18f))
        layout.addView(centerLabel("Local debug only - no server delivery.", 16f))

        targetDeviceInput = editText("child-debug-device")
        policyVersionInput = editText("debug-1")
        allowDomainsInput = editText("school.edu")
        blockDomainsInput = editText("proxy.example")
        blockKnownProxyInput = checkBox("Block known proxy domains", checked = true)
        blockUnknownInput = checkBox("Block unknown domains", checked = false)

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
        payloadOutput = valueLabel("No debug policy update built yet", 14f)
        layout.addView(payloadOutput)
        layout.addView(valueLabel("Production policy sync will use signed encrypted delivery later.", 14f))

        return ScrollView(this).apply { addView(layout) }
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
    }
}
