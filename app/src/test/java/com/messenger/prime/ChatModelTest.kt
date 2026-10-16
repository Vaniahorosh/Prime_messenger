package com.messenger.prime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ChatModelTest {

    @Test
    fun testChatModelDefaults() {
        val chat = ChatModel(
            id = "chat_001",
            name = "Test User",
            lastMessage = "Hello World!",
            time = "12:00",
            avatarUri = null
        )

        assertEquals("chat_001", chat.id)
        assertEquals("Test User", chat.name)
        assertEquals("Hello World!", chat.lastMessage)
        assertEquals("12:00", chat.time)
        assertNull(chat.avatarUri)
        assertEquals(OnlineStatus.OFFLINE, chat.onlineStatus)
        assertEquals(MessageStatus.NONE, chat.messageStatus)
        assertEquals(0, chat.unreadCount)
        assertFalse(chat.isMuted)
        assertFalse(chat.isTyping)
        assertEquals("IDLE", chat.activityState)
    }

    @Test
    fun testChatModelCopying() {
        val chat = ChatModel(
            id = "chat_002",
            name = "Bob",
            lastMessage = "Hi",
            time = "10:30",
            avatarUri = "file:///avatar.png",
            onlineStatus = OnlineStatus.ONLINE,
            unreadCount = 3
        )

        val updated = chat.copy(
            lastMessage = "New Message",
            time = "10:31",
            unreadCount = 4,
            messageStatus = MessageStatus.SENT
        )

        assertEquals("chat_002", updated.id)
        assertEquals("Bob", updated.name)
        assertEquals("New Message", updated.lastMessage)
        assertEquals("10:31", updated.time)
        assertEquals(4, updated.unreadCount)
        assertEquals(MessageStatus.SENT, updated.messageStatus)
        assertEquals(OnlineStatus.ONLINE, updated.onlineStatus)
    }
}
