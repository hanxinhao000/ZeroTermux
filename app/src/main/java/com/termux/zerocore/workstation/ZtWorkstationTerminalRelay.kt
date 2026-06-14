package com.termux.zerocore.workstation

import com.termux.terminal.TerminalSession
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

object ZtWorkstationTerminalRelay : TerminalSession.TerminalOutputListener {

    private val clients = CopyOnWriteArraySet<ZtWorkstationTerminalWebSocket>()
    private val sendExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ZtWorkstationTerminalSend").apply { isDaemon = true }
    }
    @Volatile
    private var listenerRegistered = false

    fun onClientConnected(client: ZtWorkstationTerminalWebSocket) {
        clients.add(client)
        ensureListenerRegistered()
        sendExecutor.execute { client.enableStreaming() }
    }

    fun onClientDisconnected(client: ZtWorkstationTerminalWebSocket) {
        clients.remove(client)
        if (clients.isEmpty()) {
            removeListenerRegistered()
        }
    }

    fun shutdown() {
        clients.clear()
        removeListenerRegistered()
    }

    override fun onTerminalOutput(data: ByteArray, offset: Int, count: Int) {
        if (clients.isEmpty() || count <= 0) return
        val payload = if (offset == 0 && count == data.size) {
            data
        } else {
            data.copyOfRange(offset, offset + count)
        }
        sendExecutor.execute {
            clients.forEach { client ->
                if (client.isStreamingEnabled()) {
                    client.sendBinarySafe(payload)
                }
            }
        }
    }

    private fun ensureListenerRegistered() {
        if (listenerRegistered) return
        TerminalSession.addOutputListener(this)
        listenerRegistered = true
    }

    private fun removeListenerRegistered() {
        if (!listenerRegistered) return
        TerminalSession.removeOutputListener(this)
        listenerRegistered = false
    }
}
