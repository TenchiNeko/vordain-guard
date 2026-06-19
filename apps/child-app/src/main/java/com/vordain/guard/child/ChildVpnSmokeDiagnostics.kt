package com.vordain.guard.child

data class ChildVpnSmokeDiagnostics(
    val buildLabel: String,
    val androidSdkVersion: Int,
    val manufacturer: String,
    val model: String,
    val vpnPermissionStatus: String,
    val lastCommand: String,
    val shellStatus: String,
    val warning: String,
    val createdAtMillis: Long,
) {
    fun asClipboardText(): String {
        return listOf(
            "App: Vordain Guard",
            "Build: $buildLabel",
            "Android SDK: $androidSdkVersion",
            "Device: $manufacturer $model",
            "VPN permission: $vpnPermissionStatus",
            "Last command: $lastCommand",
            "Shell status: $shellStatus",
            "Warning: $warning",
            "Created at millis: $createdAtMillis",
        ).joinToString(separator = "\n")
    }
}
