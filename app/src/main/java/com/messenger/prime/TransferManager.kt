package com.messenger.prime

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap

data class ActiveTransfer(
    val id: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    var progress: Int,
    val isSending: Boolean,
    val sourceName: String,
    val sourceType: DownloadSourceType,
    val userLogin: String,
    val cancelAction: Runnable? = null
)

interface TransferListener {
    fun onTransferUpdated(transfers: List<ActiveTransfer>)
}

object TransferManager {
    private val activeTransfersMap = ConcurrentHashMap<String, ActiveTransfer>()
    private val listeners = mutableSetOf<TransferListener>()
    private val handler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun registerListener(listener: TransferListener) {
        listeners.add(listener)
        listener.onTransferUpdated(getActiveTransfers())
    }

    @JvmStatic
    fun unregisterListener(listener: TransferListener) {
        listeners.remove(listener)
    }

    @JvmStatic
    @JvmOverloads
    fun updateTransferProgress(
        id: String,
        fileName: String,
        filePath: String,
        fileSize: Long,
        progress: Int,
        isSending: Boolean,
        sourceName: String,
        sourceType: DownloadSourceType,
        userLogin: String,
        cancelAction: Runnable? = null
    ) {
        if (progress >= 100) {
            removeTransfer(id)
            return
        }

        val existing = activeTransfersMap[id]
        if (existing != null) {
            existing.progress = progress
        } else {
            val transfer = ActiveTransfer(
                id = id,
                fileName = fileName,
                filePath = filePath,
                fileSize = fileSize,
                progress = progress.coerceIn(0, 99),
                isSending = isSending,
                sourceName = sourceName,
                sourceType = sourceType,
                userLogin = userLogin,
                cancelAction = cancelAction
            )
            activeTransfersMap[id] = transfer
        }
        notifyListeners()
    }

    @JvmStatic
    fun removeTransfer(id: String) {
        if (activeTransfersMap.remove(id) != null) {
            notifyListeners()
        }
    }

    @JvmStatic
    fun cancelTransfer(id: String) {
        val transfer = activeTransfersMap.remove(id)
        if (transfer != null) {
            try {
                transfer.cancelAction?.run()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            notifyListeners()
        }
    }

    @JvmStatic
    fun getActiveTransfers(userLogin: String? = null): List<ActiveTransfer> {
        val list = activeTransfersMap.values.toList()
        return if (userLogin.isNullOrEmpty()) list else list.filter { it.userLogin == userLogin }
    }

    private fun notifyListeners() {
        val list = activeTransfersMap.values.toList()
        handler.post {
            for (listener in listeners.toList()) {
                listener.onTransferUpdated(list)
            }
        }
    }
}
