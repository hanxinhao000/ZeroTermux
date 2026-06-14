package com.termux.zerocore.workstation

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import java.io.IOException

class ZtWorkstationTerminalWebSocket(
    handshake: NanoHTTPD.IHTTPSession
) : NanoWSD.WebSocket(handshake) {

    @Volatile
    private var streamingEnabled = false

    override fun onOpen() {
        ZtWorkstationTerminalRelay.onClientConnected(this)
    }

    override fun onClose(
        code: NanoWSD.WebSocketFrame.CloseCode,
        reason: String,
        initiatedByRemote: Boolean
    ) {
        streamingEnabled = false
        ZtWorkstationTerminalRelay.onClientDisconnected(this)
    }

    override fun onMessage(message: NanoWSD.WebSocketFrame) {
        val data = message.binaryPayload ?: return
        if (data.isEmpty()) return
        ZtWorkstationTerminalHelper.handleTtyMessage(data)
    }

    override fun onPong(pong: NanoWSD.WebSocketFrame) = Unit

    override fun onException(exception: IOException) = Unit

    fun enableStreaming() {
        streamingEnabled = true
    }

    fun isStreamingEnabled(): Boolean = streamingEnabled

    fun sendBinarySafe(data: ByteArray) {
        try {
            send(data)
        } catch (_: IOException) {
            streamingEnabled = false
            ZtWorkstationTerminalRelay.onClientDisconnected(this)
        }
    }
}
