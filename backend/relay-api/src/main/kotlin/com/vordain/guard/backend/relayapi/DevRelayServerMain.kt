package com.vordain.guard.backend.relayapi

fun main(args: Array<String>) {
    val host = args.getOrNull(0)
        ?: System.getenv("VORDAIN_DEV_RELAY_HOST")
        ?: "0.0.0.0"
    val port = args.getOrNull(1)?.toIntOrNull()
        ?: System.getenv("VORDAIN_DEV_RELAY_PORT")?.toIntOrNull()
        ?: 8081
    val server = DevRelayHttpServer(bindHost = host, port = port)
    val boundPort = server.start()
    println("Vordain Guard local dev relay listening on http://$host:$boundPort")
    println("Local debug only. Use on a trusted local network; production sync will use encrypted relay later.")
}
