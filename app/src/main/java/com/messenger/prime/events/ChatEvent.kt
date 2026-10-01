package com.messenger.prime.events

import com.messenger.prime.ChatMessage
import com.messenger.prime.MessageStatus

sealed class ChatEvent {
    data class MessageReceived(val deviceAddress: String, val message: ChatMessage) : ChatEvent()
    data class MessageSent(val deviceAddress: String, val message: ChatMessage) : ChatEvent()
    data class MessageStatusChanged(val deviceAddress: String, val messageId: String, val status: MessageStatus) : ChatEvent()
    data class ChatMetadataUpdated(val deviceAddress: String) : ChatEvent()
    data class ConnectionStateChanged(val deviceAddress: String, val isConnected: Boolean) : ChatEvent()
    
    // For legacy support when we just want to trigger a refresh of the chat list
    object GeneralUpdate : ChatEvent()
}
