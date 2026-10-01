package com.messenger.prime

import android.os.Handler
import android.os.Looper
import com.messenger.prime.events.ChatEvent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Ultra-fast zero-latency event notifier for real-time ChatList and Chat updates.
 */
object ChatListNotifier {
    
    interface ChatEventListener {
        fun onEvent(event: ChatEvent)
    }

    private val legacyListeners = CopyOnWriteArrayList<() -> Unit>()
    private val eventListeners = CopyOnWriteArrayList<ChatEventListener>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun subscribe(listener: () -> Unit) {
        if (!legacyListeners.contains(listener)) {
            legacyListeners.add(listener)
        }
    }

    @JvmStatic
    fun unsubscribe(listener: () -> Unit) {
        legacyListeners.remove(listener)
    }
    
    @JvmStatic
    fun addListener(listener: ChatEventListener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener)
        }
    }
    
    @JvmStatic
    fun removeListener(listener: ChatEventListener) {
        eventListeners.remove(listener)
    }

    @JvmStatic
    fun notifyChanged() {
        emitEvent(ChatEvent.GeneralUpdate)
    }
    
    @JvmStatic
    fun emitEvent(event: ChatEvent) {
        mainHandler.post {
            // Notify event listeners
            eventListeners.forEach { listener ->
                try {
                    listener.onEvent(event)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // For backward compatibility, also notify legacy listeners 
            // if this is a general update or something that affects the list
            if (event is ChatEvent.GeneralUpdate || 
                event is ChatEvent.MessageReceived || 
                event is ChatEvent.MessageSent ||
                event is ChatEvent.MessageStatusChanged) {
                legacyListeners.forEach { listener ->
                    try {
                        listener.invoke()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
