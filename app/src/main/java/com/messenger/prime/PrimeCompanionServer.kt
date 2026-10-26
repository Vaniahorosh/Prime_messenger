package com.messenger.prime

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList

object PrimeCompanionServer {
    private const val TAG = "PrimeCompanionServer"
    const val PORT = 8989
    const val DISCOVERY_PORT = 8988

    private var serverSocket: ServerSocket? = null
    private var discoverySocket: DatagramSocket? = null
    private val clientSockets = CopyOnWriteArrayList<Socket>()
    private val mainHandler = Handler(Looper.getMainLooper())
    var onPacketReceived: ((type: Byte, payload: ByteArray) -> Unit)? = null

    @Volatile var isRunning = false

    fun start(context: Context) {
        if (isRunning) return
        isRunning = true

        // 1. TCP Server for PC Connection
        Thread {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d(TAG, "Companion TCP Server started on port $PORT")
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    clientSockets.add(socket)
                    handleClient(socket, context)
                }
            } catch (e: Exception) {
                if (isRunning) Log.e(TAG, "TCP Server error", e)
            }
        }.start()

        // 2. UDP Discovery Responder for PC Auto-Find
        Thread {
            try {
                discoverySocket = DatagramSocket(DISCOVERY_PORT)
                val buffer = ByteArray(512)
                while (isRunning) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    discoverySocket?.receive(packet)
                    val msg = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                    if (msg.startsWith("PRIME_PC_DISCOVER")) {
                        val ip = getLocalIpAddress(context) ?: "127.0.0.1"
                        val reply = "PRIME_PHONE_HERE:$ip".toByteArray(StandardCharsets.UTF_8)
                        val resp = DatagramPacket(reply, reply.size, packet.address, packet.port)
                        discoverySocket?.send(resp)
                    }
                }
            } catch (e: Exception) {
                if (isRunning) Log.e(TAG, "UDP Discovery error", e)
            }
        }.start()
    }

    private fun handleClient(socket: Socket, context: Context) {
        Thread {
            try {
                val input = DataInputStream(socket.getInputStream())
                while (isRunning && socket.isConnected && !socket.isClosed) {
                    val type = input.readByte()
                    val len = input.readInt()
                    if (len < 0 || len > 50 * 1024 * 1024) break
                    val payload = ByteArray(len)
                    input.readFully(payload)

                    mainHandler.post {
                        onPacketReceived?.invoke(type, payload)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Client disconnected: ${socket.inetAddress}")
            } finally {
                clientSockets.remove(socket)
                try { socket.close() } catch (ignored: Exception) {}
            }
        }.start()
    }

    fun hasConnectedClients(): Boolean = clientSockets.any { it.isConnected && !it.isClosed }

    fun broadcastPacket(type: Byte, payload: ByteArray) {
        Thread {
            for (s in clientSockets) {
                try {
                    val out = DataOutputStream(s.getOutputStream())
                    out.writeByte(type.toInt())
                    out.writeInt(payload.size)
                    out.write(payload)
                    out.flush()
                } catch (e: Exception) {
                    clientSockets.remove(s)
                }
            }
        }.start()
    }

    fun stop() {
        isRunning = false
        try { serverSocket?.close() } catch (ignored: Exception) {}
        try { discoverySocket?.close() } catch (ignored: Exception) {}
        clientSockets.forEach { try { it.close() } catch (ignored: Exception) {} }
        clientSockets.clear()
    }

    fun getLocalIpAddress(context: Context): String? {
        return try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wm?.connectionInfo?.ipAddress?.let { ip ->
                Formatter.formatIpAddress(ip)
            }
        } catch (e: Exception) {
            null
        }
    }
}
