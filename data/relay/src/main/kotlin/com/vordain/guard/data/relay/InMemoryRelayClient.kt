package com.vordain.guard.data.relay

import com.vordain.guard.core.crypto.EncryptedPayload

class InMemoryRelayClient(
    private val sendResult: RelaySendResult = RelaySendResult.Success,
) : RelayClient {
    private val sentMessages = mutableListOf<RelayMessage>()

    override fun send(message: RelayMessage): RelaySendResult {
        if (sendResult is RelaySendResult.Success) {
            sentMessages += message.copyForRelay()
        }
        return sendResult
    }

    fun sent(): List<RelayMessage> {
        return sentMessages.map { message -> message.copyForRelay() }
    }

    private fun RelayMessage.copyForRelay(): RelayMessage {
        return copy(payload = payload.copyForRelay())
    }

    private fun EncryptedPayload.copyForRelay(): EncryptedPayload {
        return copy(
            ciphertext = ciphertext.copyOf(),
            nonce = nonce.copyOf(),
        )
    }
}
