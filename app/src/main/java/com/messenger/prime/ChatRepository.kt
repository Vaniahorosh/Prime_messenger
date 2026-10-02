package com.messenger.prime

import android.content.Context
import android.os.StrictMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Central Reactive Repository & Thread Isolation (SSOT)
 */
class ChatRepository(
    private val context: Context,
    private val securityManager: SecurityManager
) {
    private val _allChats = MutableStateFlow<List<ChatModel>>(emptyList())
    val allChats: Flow<List<ChatModel>> = _allChats.asStateFlow()

    suspend fun refreshChats(newChats: List<ChatModel>) = withContext(Dispatchers.IO) {
        // Emit new state bridging it to Flow
        _allChats.emit(newChats)
    }

    suspend fun sendSecureMessage(plaintext: String) = withContext(Dispatchers.IO) {
        try {
            // E2E Encryption
            val (iv, ciphertext) = securityManager.encrypt(plaintext.toByteArray(Charsets.UTF_8))
            
            // Dispatch via Bluetooth (mocking socket logic to not break existing Java layer)
            // bluetoothService?.streamPayload(chatId, iv, ciphertext)
            
            // If Room was implemented, we would do: chatDao.updateStatus(msgId, MsgStatus.SENT)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun enforceStrictMode() {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .penaltyDeath() // Crash instantly if I/O leaks to Main Thread
                    .build()
            )
        }
    }
}