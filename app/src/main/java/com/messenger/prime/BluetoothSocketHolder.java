package com.messenger.prime;

import android.bluetooth.BluetoothSocket;

public class BluetoothSocketHolder {
    private static BluetoothSocket socket;

    public static synchronized void setSocket(BluetoothSocket s) {
        socket = s;
    }

    public static synchronized BluetoothSocket getSocket() {
        BluetoothSocket s = socket;
        socket = null; // забираем ссылку
        return s;
    }
}
