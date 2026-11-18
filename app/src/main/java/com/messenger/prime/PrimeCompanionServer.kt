package com.messenger.prime

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

object PrimeCompanionServer {
    private const val TAG = "PrimeCompanionServer"
    const val PORT = 8989

    private var serverSocket: ServerSocket? = null
    private val clientSockets = CopyOnWriteArrayList<Socket>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val packetListeners = CopyOnWriteArrayList<(type: Byte, payload: ByteArray) -> Unit>()

    @JvmStatic
    fun addPacketListener(listener: (type: Byte, payload: ByteArray) -> Unit) {
        if (!packetListeners.contains(listener)) {
            packetListeners.add(listener)
        }
    }

    @JvmStatic
    fun removePacketListener(listener: (type: Byte, payload: ByteArray) -> Unit) {
        packetListeners.remove(listener)
    }

    var onPacketReceived: ((type: Byte, payload: ByteArray) -> Unit)?
        get() = packetListeners.firstOrNull()
        set(value) {
            if (value != null) {
                addPacketListener(value)
            }
        }

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
                        for (listener in packetListeners) {
                            try {
                                listener.invoke(type, payload)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in companion packet listener", e)
                            }
                        }
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
        clientSockets.forEach { try { it.close() } catch (ignored: Exception) {} }
        clientSockets.clear()
    }



}
