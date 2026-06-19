package com.vordain.guard.vpn.service

data class TunPacketMetadata(
    val ipVersion: Int?,
    val protocolLabel: String,
    val sourceAddress: String?,
    val destinationAddress: String?,
    val sourcePort: Int?,
    val destinationPort: Int?,
    val byteLength: Int,
) {
    fun asSummary(): String {
        val endpointSummary = if (sourceAddress != null && destinationAddress != null) {
            val source = if (sourcePort != null) "$sourceAddress:$sourcePort" else sourceAddress
            val destination = if (destinationPort != null) "$destinationAddress:$destinationPort" else destinationAddress
            "$source -> $destination"
        } else {
            "metadata unavailable"
        }
        return "IPv${ipVersion ?: "?"} $protocolLabel $endpointSummary ($byteLength bytes)"
    }
}
