package com.messenger.prime;

import android.bluetooth.BluetoothSocket;

public class BluetoothSocketHolder {
    private static BluetoothSocket socket;
    private static Object connectedThreadInstance;
    private static String activeDeviceAddress;
    private static String activeTargetUsername;

    public static synchronized void setSocket(BluetoothSocket s) {
        socket = s;
    }

    public static synchronized BluetoothSocket getSocket() {
        return socket;
    }

    public static synchronized void clearSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
        socket = null;
        connectedThreadInstance = null;
        activeDeviceAddress = null;
        activeTargetUsername = null;
    }

    public static synchronized Object getConnectedThreadInstance() {
        return connectedThreadInstance;
    }

    public static synchronized void setConnectedThreadInstance(Object thread) {
        connectedThreadInstance = thread;
    }

    public static synchronized String getActiveDeviceAddress() {
        return activeDeviceAddress;
    }

    public static synchronized void setActiveDeviceAddress(String address) {
        activeDeviceAddress = address;
    }

    public static synchronized String getActiveTargetUsername() {
        return activeTargetUsername;
    }

    public static synchronized void setActiveTargetUsername(String username) {
        activeTargetUsername = username;
    }

    public static synchronized boolean isConnectedWith(String address, String name) {
        if (socket == null || !socket.isConnected()) return false;
        if (connectedThreadInstance == null) return false;

        try {
            if (address != null && socket.getRemoteDevice() != null) {
                if (address.equalsIgnoreCase(socket.getRemoteDevice().getAddress())) return true;
            }
        } catch (Exception ignored) {}

        if (address != null && activeDeviceAddress != null && address.equalsIgnoreCase(activeDeviceAddress)) return true;
        if (name != null && activeTargetUsername != null && name.equalsIgnoreCase(activeTargetUsername)) return true;
        return true;
    }
}
