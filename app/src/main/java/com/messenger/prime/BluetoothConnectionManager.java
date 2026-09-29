package com.messenger.prime;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Единый потокобезопасный менеджер соединений Bluetooth Classic (RFCOMM).
 * Содержит детерминированный арбитраж ролей (Client / Server), последовательный фоллбэк сокетов,
 * а также защищает сервер прослушивания и поток обмена сообщениями.
 */
public class BluetoothConnectionManager {

    private static final String TAG = "BTConnectionManager";

    public enum ConnectionState {
        DISCONNECTED,
        LISTENING,
        CONNECTING,
        CONNECTED
    }

    public interface ConnectionCallback {
        void onStateChanged(ConnectionState state, String deviceName, String deviceAddress);
        void onPacketReceived(byte type, byte[] payload);
        void onSendProgress(int progress);
        void onError(String errorMessage);
    }

    private static BluetoothConnectionManager instance;

    private ConnectionState currentState = ConnectionState.DISCONNECTED;
    private final CopyOnWriteArrayList<ConnectionCallback> callbacks = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private String activeDeviceName = "";
    private String activeDeviceAddress = "";

    public static synchronized BluetoothConnectionManager getInstance() {
        if (instance == null) {
            instance = new BluetoothConnectionManager();
        }
        return instance;
    }

    private BluetoothConnectionManager() {}

    public void registerCallback(ConnectionCallback callback) {
        if (callback != null && !callbacks.contains(callback)) {
            callbacks.add(callback);
            final ConnectionState state = this.currentState;
            final String name = this.activeDeviceName;
            final String addr = this.activeDeviceAddress;
            mainHandler.post(() -> callback.onStateChanged(state, name, addr));
        }
    }

    public void unregisterCallback(ConnectionCallback callback) {
        if (callback != null) {
            callbacks.remove(callback);
        }
    }

    public void setCallback(ConnectionCallback callback) {
        registerCallback(callback);
    }

    public void removeCallback(ConnectionCallback callback) {
        unregisterCallback(callback);
    }

    public synchronized ConnectionState getState() {
        return currentState;
    }

    public synchronized String getActiveDeviceAddress() {
        return activeDeviceAddress;
    }

    public synchronized String getActiveDeviceName() {
        return activeDeviceName;
    }

    public synchronized void setState(ConnectionState newState, String deviceName, String deviceAddress) {
        this.currentState = newState;
        if (deviceName != null && !deviceName.isEmpty()) this.activeDeviceName = deviceName;
        if (deviceAddress != null && !deviceAddress.isEmpty()) this.activeDeviceAddress = deviceAddress;

        final String name = this.activeDeviceName;
        final String addr = this.activeDeviceAddress;

        mainHandler.post(() -> {
            for (ConnectionCallback cb : callbacks) {
                try {
                    cb.onStateChanged(newState, name, addr);
                } catch (Exception e) {
                    Log.e(TAG, "Error in onStateChanged callback", e);
                }
            }
        });
    }

    private void notifyPacketReceived(byte type, byte[] payload) {
        final byte[] data = payload != null ? payload : new byte[0];
        mainHandler.post(() -> {
            for (ConnectionCallback cb : callbacks) {
                try {
                    cb.onPacketReceived(type, data);
                } catch (Exception e) {
                    Log.e(TAG, "Error in onPacketReceived callback", e);
                }
            }
        });
    }

    private void notifyError(String errorMsg) {
        mainHandler.post(() -> {
            for (ConnectionCallback cb : callbacks) {
                try {
                    cb.onError(errorMsg);
                } catch (Exception e) {
                    Log.e(TAG, "Error in onError callback", e);
                }
            }
        });
    }

    private void notifySendProgress(int progress) {
        mainHandler.post(() -> {
            for (ConnectionCallback cb : callbacks) {
                try {
                    cb.onSendProgress(progress);
                } catch (Exception e) {
                    Log.e(TAG, "Error in onSendProgress callback", e);
                }
            }
        });
    }

    /**
     * Арбитраж ролей по именам пользователей/устройств (localName vs remoteName).
     * Избегает возврата "02:00:00:00:00:00" на Android 6.0+.
     */
    public static boolean isClientInitiator(String localName, String remoteName) {
        if (localName == null || remoteName == null || localName.isEmpty() || remoteName.isEmpty()) {
            return true;
        }
        return localName.toUpperCase(Locale.ROOT).compareTo(remoteName.toUpperCase(Locale.ROOT)) > 0;
    }

    /**
     * Запуск сервера (AcceptThread) для прослушивания входящих подключений.
     */
    @SuppressLint("MissingPermission")
    public synchronized void startServer(BluetoothAdapter adapter, UUID serviceUuid) {
        if (adapter == null || !adapter.isEnabled()) return;
        if (acceptThread == null || !acceptThread.isAlive()) {
            acceptThread = new AcceptThread(adapter, serviceUuid);
            acceptThread.start();
            if (currentState != ConnectionState.CONNECTED && currentState != ConnectionState.CONNECTING) {
                setState(ConnectionState.LISTENING, null, null);
            }
        }
    }

    public synchronized void connectToDevice(BluetoothAdapter adapter, BluetoothDevice device, UUID serviceUuid, String localIdentifier, String remoteIdentifier) {
        connectToDevice(adapter, device, serviceUuid, localIdentifier, remoteIdentifier, false);
    }

    /**
     * Подключение к устройству с детерминированным сравнением имен и поддержкой принудительного флага forceClient.
     */
    @SuppressLint("MissingPermission")
    public synchronized void connectToDevice(BluetoothAdapter adapter, BluetoothDevice device, UUID serviceUuid, String localIdentifier, String remoteIdentifier, boolean forceClient) {
        if (adapter == null || !adapter.isEnabled() || device == null) return;

        if (currentState == ConnectionState.CONNECTED && activeDeviceAddress.equalsIgnoreCase(device.getAddress())) {
            Log.d(TAG, "Already connected to target device: " + device.getAddress());
            setState(ConnectionState.CONNECTED, device.getName(), device.getAddress());
            return;
        }

        // Всегда запускаем сервер прослушивания на обоих устройствах
        startServer(adapter, serviceUuid);

        String localName = localIdentifier;
        if (localName == null || localName.isEmpty()) {
            try { localName = adapter.getName(); } catch (SecurityException ignored) {}
        }
        if (localName == null || localName.isEmpty()) localName = "Device_A";

        String remoteName = remoteIdentifier;
        if (remoteName == null || remoteName.isEmpty()) {
            try { remoteName = device.getName(); } catch (SecurityException ignored) {}
        }
        if (remoteName == null || remoteName.isEmpty()) {
            remoteName = device.getAddress();
        }

        boolean shouldBeClient = forceClient || isClientInitiator(localName, remoteName);

        Log.d(TAG, "Role arbitration: Local='" + localName + "' vs Remote='" + remoteName + "', forceClient=" + forceClient + " -> shouldBeClient=" + shouldBeClient);

        if (!shouldBeClient) {
            Log.d(TAG, "Role arbitration: Local device (" + localName + ") acts as Server, waiting for client connection...");
            return;
        }

        stopConnectThread();
        connectThread = new ConnectThread(adapter, device, serviceUuid);
        connectThread.start();
        setState(ConnectionState.CONNECTING, device.getName(), device.getAddress());
    }

    /**
     * Переход в состояние активного соединения после успешного коннекта.
     */
    @SuppressLint("MissingPermission")
    public synchronized void onSocketConnected(BluetoothSocket socket, BluetoothDevice device) {
        if (socket == null || !socket.isConnected()) return;

        stopConnectThread();
        stopConnectedThread();

        String devName = "";
        String devAddr = "";
        try {
            if (device != null) {
                devName = device.getName() != null ? device.getName() : "Prime User";
                devAddr = device.getAddress();
            }
        } catch (SecurityException ignored) {}

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        setState(ConnectionState.CONNECTED, devName, devAddr);
    }

    /**
     * Отправка пакета через активный сокет.
     */
    public synchronized void sendPacket(byte type, byte[] payload) {
        ConnectedThread r = connectedThread;
        if (r != null && r.isAlive()) {
            r.sendPacket(type, payload);
        } else {
            Log.w(TAG, "Cannot send packet, no active connected thread");
        }
    }

    /**
     * Отключение активного соединения.
     */
    public synchronized void disconnect() {
        stopConnectThread();
        stopConnectedThread();
        setState(ConnectionState.DISCONNECTED, null, null);
    }

    /**
     * Полный сброс всех потоков и ресурсов.
     */
    public synchronized void stopAll() {
        stopConnectThread();
        stopConnectedThread();
        stopAcceptThread();
        setState(ConnectionState.DISCONNECTED, null, null);
    }

    private synchronized void stopAcceptThread() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
    }

    private synchronized void stopConnectThread() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
    }

    private synchronized void stopConnectedThread() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    private static void closeSocketQuietly(BluetoothSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    // =========================================================================
    // Inner Thread Classes
    // =========================================================================

    @SuppressLint("MissingPermission")
    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket;
        private final BluetoothAdapter mmAdapter;
        private final UUID mmUuid;
        private volatile boolean isRunning = true;

        public AcceptThread(BluetoothAdapter adapter, UUID uuid) {
            mmAdapter = adapter;
            mmUuid = uuid;
        }

        public void run() {
            while (isRunning) {
                if (mmServerSocket == null && mmAdapter != null && mmAdapter.isEnabled()) {
                    try {
                        mmServerSocket = mmAdapter.listenUsingInsecureRfcommWithServiceRecord("PrimeChat", mmUuid);
                    } catch (IOException e) {
                        Log.w(TAG, "Failed to create server socket, retrying in 500ms...", e);
                        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                        continue;
                    }
                }

                if (mmServerSocket == null) break;

                BluetoothSocket socket = null;
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    if (!isRunning) break;
                    Log.w(TAG, "Server accept() failed, recreating server socket...", e);
                    try {
                        if (mmServerSocket != null) mmServerSocket.close();
                    } catch (IOException ignored) {}
                    mmServerSocket = null;
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    continue;
                }

                if (socket != null && socket.isConnected()) {
                    BluetoothDevice remoteDevice = socket.getRemoteDevice();
                    cancel();
                    onSocketConnected(socket, remoteDevice);
                    break;
                }
            }
        }

        public void cancel() {
            isRunning = false;
            if (mmServerSocket != null) {
                try {
                    mmServerSocket.close();
                } catch (IOException ignored) {}
                mmServerSocket = null;
            }
        }
    }

    @SuppressLint("MissingPermission")
    private class ConnectThread extends Thread {
        private final BluetoothAdapter mmAdapter;
        private final BluetoothDevice mmDevice;
        private final UUID mmUuid;
        private BluetoothSocket mmSocket;
        private volatile boolean isHandedOff = false;

        public ConnectThread(BluetoothAdapter adapter, BluetoothDevice device, UUID uuid) {
            mmAdapter = adapter;
            mmDevice = device;
            mmUuid = uuid;
        }

        public void run() {
            if (mmAdapter != null) {
                try {
                    mmAdapter.cancelDiscovery();
                } catch (Exception ignored) {}
            }

            BluetoothSocket socket = null;

            // Stage 1: Insecure RFCOMM
            try {
                socket = mmDevice.createInsecureRfcommSocketToServiceRecord(mmUuid);
                if (socket != null) {
                    socket.connect();
                }
            } catch (Exception e1) {
                Log.w(TAG, "Stage 1 (Insecure RFCOMM) failed: " + e1.getMessage());
                closeSocketQuietly(socket);
                socket = null;
            }

            // Stage 2: Secure RFCOMM
            if (socket == null) {
                try {
                    socket = mmDevice.createRfcommSocketToServiceRecord(mmUuid);
                    if (socket != null) {
                        socket.connect();
                    }
                } catch (Exception e2) {
                    Log.w(TAG, "Stage 2 (Secure RFCOMM) failed: " + e2.getMessage());
                    closeSocketQuietly(socket);
                    socket = null;
                }
            }

            // Stage 3: Reflection RFCOMM
            if (socket == null) {
                try {
                    socket = (BluetoothSocket) mmDevice.getClass()
                            .getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class)
                            .invoke(mmDevice, mmUuid);
                    if (socket != null) {
                        socket.connect();
                    }
                } catch (Exception e3) {
                    Log.e(TAG, "Stage 3 (Reflection RFCOMM) failed: " + e3.getMessage());
                    closeSocketQuietly(socket);
                    socket = null;
                }
            }

            if (socket != null && socket.isConnected()) {
                this.mmSocket = socket;
                this.isHandedOff = true;
                onSocketConnected(socket, mmDevice);
            } else {
                notifyError("Не удалось установить прямое соединение с устройством");
                setState(ConnectionState.DISCONNECTED, null, null);
            }
        }

        public void cancel() {
            if (isHandedOff) return;
            closeSocketQuietly(mmSocket);
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final DataInputStream mmInStream;
        private final DataOutputStream mmOutStream;
        private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();
        private volatile boolean isRunning = true;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            DataInputStream tmpIn = null;
            DataOutputStream tmpOut = null;
            try {
                tmpIn = new DataInputStream(socket.getInputStream());
                tmpOut = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                Log.e(TAG, "Sockets streams create failed", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            if (mmInStream == null) return;
            while (isRunning) {
                try {
                    byte type = mmInStream.readByte();
                    int length = mmInStream.readInt();

                    if (length < 0 || length > 100 * 1024 * 1024) {
                        throw new IOException("Invalid packet length: " + length);
                    }

                    byte[] payload = new byte[length];
                    if (length > 0) {
                        mmInStream.readFully(payload);
                    }

                    notifyPacketReceived(type, payload);
                } catch (IOException e) {
                    if (isRunning) {
                        Log.e(TAG, "Connection lost during read", e);
                        notifyError("Соединение сброшено");
                        setState(ConnectionState.DISCONNECTED, null, null);
                        activeDeviceAddress = "";
                        activeDeviceName = "";
                    }
                    break;
                }
            }
        }

        public void sendPacket(byte type, byte[] payload) {
            if (!isRunning || mmOutStream == null) return;
            writeExecutor.execute(() -> {
                try {
                    synchronized (this) {
                        mmOutStream.writeByte(type);
                        int len = payload != null ? payload.length : 0;
                        mmOutStream.writeInt(len);
                        if (len > 0 && payload != null) {
                            int offset = 0;
                            int chunkSize = 4096; // Безопасный размер чанка для Bluetooth RFCOMM
                            while (offset < len) {
                                if (!isRunning) break;
                                int bytesToWrite = Math.min(chunkSize, len - offset);
                                mmOutStream.write(payload, offset, bytesToWrite);
                                offset += bytesToWrite;
                                mmOutStream.flush();

                                // Обновляем прогресс при больших файлах (фото/видео/документы)
                                if (len > 256 * 1024) {
                                    int progress = (int) ((offset * 100L) / len);
                                    notifySendProgress(progress);
                                }
                                
                                // Небольшая задержка, чтобы дать аппаратному буферу освободиться
                                if (len > 1024 * 1024) {
                                    try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                                }
                            }
                        } else {
                            mmOutStream.flush();
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error writing packet type " + type, e);
                    if (isRunning) {
                        notifyError("Ошибка отправки данных: " + e.getMessage());
                        setState(ConnectionState.DISCONNECTED, null, null);
                        activeDeviceAddress = "";
                        activeDeviceName = "";
                        cancel();
                    }
                }
            });
        }

        public void cancel() {
            isRunning = false;
            writeExecutor.shutdownNow();
            closeSocketQuietly(mmSocket);
        }
    }
}
