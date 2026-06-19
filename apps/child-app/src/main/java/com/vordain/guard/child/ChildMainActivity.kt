package com.vordain.guard.child

import android.app.Activity
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
            setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_GRANTED)
        } else {
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
        layout.addView(label(ChildVpnSmokeLabels.SUBTITLE, textSize = 18f))

        statusText = label(ChildVpnSmokeLabels.STATUS_NOT_RUNNING, textSize = 18f)
        layout.addView(statusText)

        layout.addView(button(ChildVpnSmokeLabels.REQUEST_PERMISSION_BUTTON) {
            requestVpnPermission()
        })
        layout.addView(button(ChildVpnSmokeLabels.START_BUTTON) {
            startVpnShellWhenAllowed()
        })
        layout.addView(button(ChildVpnSmokeLabels.STOP_BUTTON) {
            stopVpnShell()
        })

        layout.addView(label(ChildVpnSmokeLabels.WARNING, textSize = 16f))
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
        when (val result = vpnPermissionIntentFactory.createPrepareResult(this)) {
            VpnPrepareResult.AlreadyGranted -> setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_GRANTED)
            is VpnPrepareResult.ConsentRequired -> {
                setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_REQUIRED)
                startActivityForResult(result.intent, REQUEST_VPN_PERMISSION)
            }
        }
    }

    private fun startVpnShellWhenAllowed() {
        when (val result = vpnPermissionIntentFactory.createPrepareResult(this)) {
            VpnPrepareResult.AlreadyGranted -> {
                val intent = VordainVpnServiceIntents.startProtection(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                setStatus(ChildVpnSmokeLabels.STATUS_SHELL_ACTIVE)
            }
            is VpnPrepareResult.ConsentRequired -> {
                setStatus(ChildVpnSmokeLabels.STATUS_PERMISSION_REQUIRED)
                startActivityForResult(result.intent, REQUEST_VPN_PERMISSION)
            }
        }
    }

    private fun stopVpnShell() {
        startService(VordainVpnServiceIntents.stopProtection(this))
        setStatus(ChildVpnSmokeLabels.STATUS_STOPPED)
    }

    private fun setStatus(status: String) {
        statusText.text = status
    }

    private companion object {
        const val REQUEST_VPN_PERMISSION = 1001
    }
}
