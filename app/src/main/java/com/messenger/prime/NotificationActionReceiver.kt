package com.messenger.prime

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.RemoteInput
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

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        when (intent.action) {
            ACTION_REPLY -> {
                val results: Bundle? = RemoteInput.getResultsFromIntent(intent)
                val replyText = results?.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim()

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

                    val threadObj = BluetoothSocketHolder.getThreadFor(deviceAddress, targetUsername)
                    if (threadObj is ChatPersonActivity.ConnectedThread && threadObj.isAlive) {
                        threadObj.sendPacket(1.toByte(), payload) // TYPE_TEXT = 0x01
                    }

                    ChatHistoryManager.saveMessage(context, targetUsername, msg)

                    Toast.makeText(context, "Ответ отправлен $targetUsername", Toast.LENGTH_SHORT).show()
                }

                nm?.cancel(notificationId)
            }

            ACTION_MARK_READ -> {
                // Send read receipt if connected
                val threadObj = BluetoothSocketHolder.getThreadFor(deviceAddress, targetUsername)
                if (threadObj is ChatPersonActivity.ConnectedThread && threadObj.isAlive) {
                    val receiptPayload = "READ_ALL".toByteArray(StandardCharsets.UTF_8)
                    threadObj.sendPacket(0x0A.toByte(), receiptPayload) // TYPE_READ_RECEIPT = 0x0A
                }

                // Update unread status in messages history
                val history = ChatHistoryManager.loadMessages(context, targetUsername)
                for (m in history) {
                    if (!m.isOutgoing) {
                        m.messageStatus = MessageStatus.READ
                    }
                }
                ChatHistoryManager.saveHistoryList(context, targetUsername, history)

                // Notify chat list update
                ChatListNotifier.notifyChanged()

                nm?.cancel(notificationId)
                Toast.makeText(context, "Отмечено как прочитанное", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
