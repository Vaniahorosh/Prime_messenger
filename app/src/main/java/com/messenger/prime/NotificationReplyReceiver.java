package com.messenger.prime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Сохраняем класс-приемник для обратной совместимости с ранее созданными PendingIntent.
 * Делегирует обработку централизованному NotificationActionReceiver.
 */
public class NotificationReplyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        NotificationActionReceiver receiver = new NotificationActionReceiver();
        receiver.onReceive(context, intent);
    }
}
