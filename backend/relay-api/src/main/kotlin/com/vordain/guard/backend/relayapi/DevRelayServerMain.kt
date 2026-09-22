package com.vordain.guard.backend.relayapi

fun main(args: Array<String>) {
    val host = args.getOrNull(0)
        ?: System.getenv("VORDAIN_DEV_RELAY_HOST")
        ?: "127.0.0.1"
    val port = args.getOrNull(1)?.toIntOrNull()
        ?: System.getenv("VORDAIN_DEV_RELAY_PORT")?.toIntOrNull()
        ?: 8081
    val server = DevRelayHttpServer(bindHost = host, port = port)
    val boundPort = server.start()
    println("Vordain Guard local dev relay listening on http://$host:$boundPort")
    println("Unauthenticated cleartext debug service. Keep it on loopback unless a trusted-LAN bind is explicitly required.")
    println("Never expose this relay to the internet.")
}
