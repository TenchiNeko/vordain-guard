package com.vordain.guard.vpn.service

import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.vordain.guard.vpn.session.VpnTunnelSpec

class AndroidVpnTunnelOpener {
    fun establish(
        vpnService: VpnService,
        spec: VpnTunnelSpec,
    ): AndroidVpnTunnelOpenResult {
        return try {
            val builder = vpnService.Builder()
                .setSession(spec.sessionName)

            spec.addresses.forEach { address ->
                builder.addAddress(address.address, address.prefixLength)
            }
            spec.routes.forEach { route ->
                builder.addRoute(route.address, route.prefixLength)
            }

            val descriptor = builder.establish()
            if (descriptor == null) {
                AndroidVpnTunnelOpenResult.Failed("VPN interface was not established")
            } else {
                AndroidVpnTunnelOpenResult.Established(AndroidVpnTunnelHandle(descriptor))
            }
        } catch (exception: SecurityException) {
            AndroidVpnTunnelOpenResult.PermissionRequired(exception.message)
        } catch (exception: IllegalArgumentException) {
            AndroidVpnTunnelOpenResult.Failed(exception.message)
        } catch (exception: IllegalStateException) {
            AndroidVpnTunnelOpenResult.Failed(exception.message)
        }
    }
}

sealed interface AndroidVpnTunnelOpenResult {
    data class Established(val handle: AndroidVpnTunnelHandle) : AndroidVpnTunnelOpenResult
    data class PermissionRequired(val message: String?) : AndroidVpnTunnelOpenResult
    data class Failed(val message: String?) : AndroidVpnTunnelOpenResult
}

class AndroidVpnTunnelHandle(
    val descriptor: ParcelFileDescriptor,
) {
    fun close() {
        descriptor.close()
    }
}
