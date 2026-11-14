package com.messenger.prime

import android.os.Handler
import android.os.Looper
import com.messenger.prime.events.ChatEvent
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Ultra-fast zero-latency event notifier for real-time ChatList and Chat updates.
 */
object ChatListNotifier {
    
    interface ChatEventListener {
        fun onEvent(event: ChatEvent)
    }

    private val legacyListeners = CopyOnWriteArrayList<() -> Unit>()
    private val eventListeners = CopyOnWriteArrayList<WeakReference<ChatEventListener>>()
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
        eventListeners.removeAll { it.get() == null || it.get() === listener }
        eventListeners.add(WeakReference(listener))
    }
    
    @JvmStatic
    fun removeListener(listener: ChatEventListener) {
        eventListeners.removeAll { it.get() == null || it.get() === listener }
    }

    @JvmStatic
    fun notifyChanged() {
        emitEvent(ChatEvent.GeneralUpdate)
    }
    
    @JvmStatic
    fun emitEvent(event: ChatEvent) {
        mainHandler.post {
            // Notify event listeners
            val toRemove = mutableListOf<WeakReference<ChatEventListener>>()
            eventListeners.forEach { ref ->
                val listener = ref.get()
                if (listener != null) {
                    try {
                        listener.onEvent(event)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    toRemove.add(ref)
                }
            }
            if (toRemove.isNotEmpty()) {
                eventListeners.removeAll(toRemove)
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
