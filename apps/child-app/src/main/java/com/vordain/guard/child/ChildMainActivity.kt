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
import android.widget.LinearLayout
import android.widget.TextView
import com.vordain.guard.vpn.service.VordainVpnServiceIntents
import com.vordain.guard.vpn.service.VpnPermissionIntentFactory
import com.vordain.guard.vpn.service.VpnPrepareResult

class ChildMainActivity : Activity() {
    private val vpnPermissionIntentFactory = VpnPermissionIntentFactory()
    private lateinit var statusText: TextView
    private lateinit var vpnPermissionText: TextView
    private lateinit var lastCommandText: TextView
    private lateinit var shellStatusText: TextView
    private lateinit var diagnosticsText: TextView
    private var vpnPermissionStatus: String = ChildVpnSmokeLabels.PERMISSION_UNKNOWN
    private var lastCommand: String = ChildVpnSmokeLabels.COMMAND_NONE
    private var shellStatus: String = ChildVpnSmokeLabels.STATUS_NOT_RUNNING

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

    private fun createSmokeTestView(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 64, 48, 48)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        layout.addView(label(ChildVpnSmokeLabels.TITLE, textSize = 28f))
        layout.addView(label(ChildVpnSmokeLabels.BUILD_LABEL, textSize = 16f))
        layout.addView(label(ChildVpnSmokeLabels.SUBTITLE, textSize = 18f))

        statusText = label(ChildVpnSmokeLabels.STATUS_NOT_RUNNING, textSize = 18f)
        layout.addView(statusText)

        vpnPermissionText = label("", textSize = 16f)
        lastCommandText = label("", textSize = 16f)
        shellStatusText = label("", textSize = 16f)
        diagnosticsText = label("", textSize = 14f)
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
        layout.addView(button(ChildVpnSmokeLabels.COPY_DIAGNOSTICS_BUTTON) {
            copyDiagnostics()
        })

        layout.addView(label(ChildVpnSmokeLabels.WARNING, textSize = 16f))
        layout.addView(diagnosticsText)
        refreshDiagnosticsViews()
        return layout
    }

    private fun label(
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

    private fun button(
        text: String,
        onClick: () -> Unit,
    ): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
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
        val diagnostics = createDiagnostics().asClipboardText()
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
        if (::diagnosticsText.isInitialized && diagnosticsText.text != ChildVpnSmokeLabels.DIAGNOSTICS_COPIED) {
            diagnosticsText.text = createDiagnostics().asClipboardText()
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

    private companion object {
        const val REQUEST_VPN_PERMISSION = 1001
    }
}
