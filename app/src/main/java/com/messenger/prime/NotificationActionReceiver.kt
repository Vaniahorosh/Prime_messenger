package com.messenger.prime

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.RemoteInput
import com.messenger.prime.events.ChatEvent
import org.json.JSONArray
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REPLY = "com.messenger.prime.ACTION_NOTIFICATION_REPLY"
        const val ACTION_MARK_READ = "com.messenger.prime.ACTION_NOTIFICATION_MARK_READ"
        const val ACTION_CANCEL_UPLOAD = "com.messenger.prime.action.CANCEL_UPLOAD"
        const val KEY_TEXT_REPLY = "key_text_reply"
        private const val TAG = "NotificationActionRx"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val senderName = intent.getStringExtra("EXTRA_CHAT_NAME")
        val deviceAddress = intent.getStringExtra("EXTRA_DEVICE_ADDRESS")
        val targetAddr = if (!deviceAddress.isNullOrEmpty()) deviceAddress else senderName ?: ""
        val chatName = if (!senderName.isNullOrEmpty()) senderName else targetAddr

        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", Math.abs(chatName.hashCode()))
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        when (intent.action) {
            ACTION_REPLY, "ACTION_NOTIFICATION_REPLY" -> {
                val results: Bundle? = RemoteInput.getResultsFromIntent(intent)
                val replyText = results?.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim()
                    ?: results?.getCharSequence("KEY_TEXT_REPLY")?.toString()?.trim()

                if (!replyText.isNullOrEmpty() && targetAddr.isNotEmpty()) {
                    val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                    val currentUser = sp.getString("current_user", "") ?: ""
                    val myDisplayName = sp.getString("${currentUser}_name", currentUser) ?: currentUser
                    val localUser = if (myDisplayName.isNotEmpty()) myDisplayName else "Пользователь"

                    val timestamp = System.currentTimeMillis()
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                    val messageId = "${localUser}_${timestamp}_${UUID.randomUUID().toString().substring(0, 4)}"

                    val msg = ChatMessage(replyText, time, localUser, true, null, timestamp, null, messageId)
                    msg.messageStatus = MessageStatus.SENT

                    val packetContent = "$messageId:::$replyText"
                    val payload = packetContent.toByteArray(StandardCharsets.UTF_8)

                    BluetoothConnectionManager.getInstance().sendPacket(targetAddr, 0x01.toByte(), payload)

                    ChatHistoryManager.saveMessage(context, chatName, msg)
                    resetUnreadCountAndUpdateLastMessage(context, chatName, targetAddr, replyText)

                    val displayRecipient = if (BluetoothAdapter.checkBluetoothAddress(chatName)) "собеседнику" else chatName
                    Toast.makeText(context, "Ответ отправлен $displayRecipient", Toast.LENGTH_SHORT).show()
                }

                if (notificationId > 0) {
                    nm?.cancel(notificationId)
                }
            }

            ACTION_MARK_READ, "ACTION_NOTIFICATION_MARK_READ" -> {
                if (targetAddr.isNotEmpty()) {
                    val receiptPayload = "READ_ALL:::MARK_READ".toByteArray(StandardCharsets.UTF_8)
                    BluetoothConnectionManager.getInstance().sendPacket(targetAddr, 0x0A.toByte(), receiptPayload)

                    ChatHistoryManager.markIncomingMessagesAsRead(context, chatName, "READ_ALL")
                    resetUnreadCountAndUpdateLastMessage(context, chatName, targetAddr, null)
                    Toast.makeText(context, "Отмечено как прочитанное", Toast.LENGTH_SHORT).show()
                }

                if (notificationId > 0) {
                    nm?.cancel(notificationId)
                }
            }

            ACTION_CANCEL_UPLOAD -> {
                if (targetAddr.isNotEmpty()) {
                    BluetoothConnectionManager.getInstance().cancelCurrentMediaSend(targetAddr)
                }
                nm?.cancel(PrimeBluetoothService.PROGRESS_NOTIFICATION_ID)
                if (notificationId > 0) {
                    nm?.cancel(notificationId)
                }
            }
        }
    }

    private fun resetUnreadCountAndUpdateLastMessage(context: Context, senderName: String, deviceAddr: String, lastMsg: String?) {
        try {
            val json = ChatHistoryManager.getPersistedChatsJson(context)
            val array = JSONArray(json)
            val newArray = JSONArray()

            val nowTs = System.currentTimeMillis()
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowTs))

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val user = obj.optString("name", "")
                val id = obj.optString("id", "")

                if ((senderName.isNotEmpty() && user.equals(senderName, ignoreCase = true)) ||
                    (deviceAddr.isNotEmpty() && deviceAddr.equals(id, ignoreCase = true))) {
                    if (lastMsg != null) {
                        obj.put("lastMessage", lastMsg)
                        obj.put("time", timeStr)
                        obj.put("timestamp", nowTs)
                        obj.put("messageStatus", "SENT")
                    }
                    obj.put("unreadCount", 0)
                }
                newArray.put(obj)
            }

            ChatHistoryManager.savePersistedChatsJson(context, newArray.toString())
            ChatListNotifier.emitEvent(ChatEvent.GeneralUpdate)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update chat list from NotificationActionReceiver", e)
        }
    }
}
