package com.messenger.prime

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Ultra-fast zero-latency event notifier for real-time ChatList updates.
 */
object ChatListNotifier {
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun subscribe(listener: () -> Unit) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun unsubscribe(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun notifyChanged() {
        mainHandler.post {
            listeners.forEach { listener ->
                try {
                    listener.invoke()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
