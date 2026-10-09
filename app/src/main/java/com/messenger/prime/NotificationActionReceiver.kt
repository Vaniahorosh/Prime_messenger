package com.messenger.prime

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val targetUsername = intent.getStringExtra("EXTRA_CHAT_NAME") ?: return
        val deviceAddress = intent.getStringExtra("EXTRA_DEVICE_ADDRESS")
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", Math.abs(targetUsername.hashCode()))
        val targetAddr = if (!deviceAddress.isNullOrEmpty()) deviceAddress else targetUsername

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        when (intent.action) {
            ACTION_REPLY, "ACTION_NOTIFICATION_REPLY" -> {
                val results: Bundle? = RemoteInput.getResultsFromIntent(intent)
                val replyText = results?.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim()
                    ?: results?.getCharSequence("KEY_TEXT_REPLY")?.toString()?.trim()

                if (!replyText.isNullOrEmpty()) {
                    val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
                    val currentUser = sp.getString("current_user", "") ?: ""
                    val myDisplayName = sp.getString("${currentUser}_name", currentUser) ?: currentUser

                    val timestamp = System.currentTimeMillis()
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                    val messageId = "${myDisplayName}_${timestamp}_${UUID.randomUUID().toString().substring(0, 4)}"

                    val msg = ChatMessage(replyText, time, myDisplayName, true, null, timestamp, null, messageId)
                    msg.messageStatus = MessageStatus.SENT

                    val packetContent = "$messageId:::$replyText"
                    val payload = packetContent.toByteArray(StandardCharsets.UTF_8)

                    BluetoothConnectionManager.getInstance().sendPacket(targetAddr, 0x01.toByte(), payload)

                    ChatHistoryManager.saveMessage(context, targetUsername, msg)
                    resetUnreadCountAndUpdateLastMessage(context, targetUsername, targetAddr, replyText)

                    val displayRecipient = if (BluetoothAdapter.checkBluetoothAddress(targetUsername)) "собеседнику" else targetUsername
                    Toast.makeText(context, "Ответ отправлен $displayRecipient", Toast.LENGTH_SHORT).show()
                }

                nm?.cancel(notificationId)
            }

            ACTION_MARK_READ, "ACTION_NOTIFICATION_MARK_READ" -> {
                val receiptPayload = "READ_ALL".toByteArray(StandardCharsets.UTF_8)
                BluetoothConnectionManager.getInstance().sendPacket(targetAddr, 0x0A.toByte(), receiptPayload)

                ChatHistoryManager.markIncomingMessagesAsRead(context, targetUsername, "READ_ALL")
                resetUnreadCountAndUpdateLastMessage(context, targetUsername, targetAddr, null)

                nm?.cancel(notificationId)
                Toast.makeText(context, "Отмечено как прочитанное", Toast.LENGTH_SHORT).show()
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

                if (user.equals(senderName, ignoreCase = true) || deviceAddr.equals(id, ignoreCase = true)) {
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
        } catch (_: Exception) {}
    }
}
