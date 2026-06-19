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
import com.vordain.guard.data.review.ReviewRequestReason
import com.vordain.guard.vpn.service.VordainVpnServiceIntents
import com.vordain.guard.vpn.service.VpnPermissionIntentFactory
import com.vordain.guard.vpn.service.VpnPrepareResult

class ChildMainActivity : Activity() {
    private val vpnPermissionIntentFactory = VpnPermissionIntentFactory()
    private val policyDemo = ChildDebugPolicyDemo()
    private val compatibilityDemo = ChildDebugCompatibilityDemo { System.currentTimeMillis() }
    private val reviewDemo = ChildDebugReviewDemo()
    private val diagnosticsFormatter = ChildDebugDiagnosticsFormatter()
    private lateinit var statusText: TextView
    private lateinit var vpnPermissionText: TextView
    private lateinit var lastCommandText: TextView
    private lateinit var shellStatusText: TextView
    private lateinit var setupChecklistText: TextView
    private lateinit var policyDomainInput: EditText
    private lateinit var policyOutputText: TextView
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
    private var policyResult: ChildDebugPolicyResult? = null
    private var compatibilityResult: ChildDebugCompatibilityResult? = null
    private var reviewResult: ChildDebugReviewResult? = null
    private val localDebugEvents = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createSmokeTestView())
        setStatus(ChildVpnSmokeLabels.STATUS_NOT_RUNNING)
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

    private fun evaluatePolicyDomain() {
        policyResult = policyDemo.evaluate(policyDomainInput.text.toString())
        policyOutputText.text = policyResult?.asDisplayText().orEmpty()
        refreshDiagnosticsViews()
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
        refreshDiagnosticsViews()
    }

    private fun setVpnPermissionStatus(status: String) {
        vpnPermissionStatus = status
        refreshDiagnosticsViews()
    }

    private fun setLastCommand(command: String) {
        lastCommand = command
        refreshDiagnosticsViews()
    }

    private fun setShellStatus(status: String) {
        shellStatus = status
        refreshDiagnosticsViews()
    }

    private fun copyDiagnostics() {
        val diagnostics = diagnosticsFormatter.format(
            diagnostics = createDiagnostics(),
            state = createDashboardState(),
        )
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Vordain Guard diagnostics", diagnostics))
        diagnosticsText.text = ChildVpnSmokeLabels.DIAGNOSTICS_COPIED
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
            compatibilityResult = compatibilityResult,
            reviewResult = reviewResult,
            localEvents = localDebugEvents.toList(),
        )
    }

    private fun createSetupChecklist(): VpnSetupChecklist {
        return VpnSetupChecklist(
            vpnPermission = if (vpnPermissionStatus == ChildVpnSmokeLabels.PERMISSION_GRANTED) {
                SetupCheckState.CONFIGURED
            } else {
                SetupCheckState.UNKNOWN
            },
            alwaysOnVpn = SetupCheckState.UNKNOWN,
            blockConnectionsWithoutVpn = SetupCheckState.UNKNOWN,
            batteryOptimizationWarning = SetupCheckState.UNKNOWN,
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
            "Foreground notification: manual check",
            "Always-on VPN: ${checklist.alwaysOnVpn} - manual check",
            "Block connections without VPN: ${checklist.blockConnectionsWithoutVpn} - manual check",
            "Battery optimization: ${checklist.batteryOptimizationWarning} - manual check",
            "App protection: ${checklist.appProtection}",
        ).joinToString(separator = "\n")
    }

    private companion object {
        const val REQUEST_VPN_PERMISSION = 1001
    }
}
