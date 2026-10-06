package com.messenger.prime;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Единый потокобезопасный менеджер соединений Bluetooth Classic (RFCOMM) с пулом подключений (Multi-Peer).
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
        void onStateChanged(String deviceAddress, ConnectionState state, String deviceName);
        void onPacketReceived(String fromAddress, byte type, byte[] payload);
        void onSendProgress(String deviceAddress, int progress);
        void onError(String deviceAddress, String errorMessage);
    }

    private static BluetoothConnectionManager instance;

    private final CopyOnWriteArrayList<ConnectionCallback> callbacks = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AcceptThread acceptThread;
    
    // Пул активных соединений и их статусов
    private final ConcurrentHashMap<String, ConnectedThread> connectionPool = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConnectionState> deviceStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> deviceNames = new ConcurrentHashMap<>();
    // Также храним исходящие потоки ConnectThread, чтобы избежать параллельных коннектов к одному устройству
    private final ConcurrentHashMap<String, ConnectThread> connectingThreads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Handler> connectRetryHandlers = new ConcurrentHashMap<>();

    // =========================================================================
    // MAC Address DDoS & Flood Guard
    // =========================================================================

    public boolean isMacBlocked(String macAddress) {
        return false;
    }

    public void blockMacAddress(String macAddress, long durationMs, String reason) {
        // No-op for local Bluetooth RFCOMM peers
    }

    public void unblockMacAddress(String macAddress) {
        // No-op
    }

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
            // Пробрасываем текущие статусы всех активных устройств
            mainHandler.post(() -> {
                for (String address : deviceStates.keySet()) {
                    ConnectionState state = deviceStates.get(address);
                    String name = deviceNames.get(address);
                    if (state != null) {
                        callback.onStateChanged(address, state, name);
                    }
                }
            });
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
        if (!connectionPool.isEmpty()) {
            return ConnectionState.CONNECTED;
        }
        return ConnectionState.DISCONNECTED;
    }

    public synchronized ConnectionState getState(String deviceAddress) {
        if (deviceAddress == null) return getState();
        return deviceStates.getOrDefault(deviceAddress, ConnectionState.DISCONNECTED);
    }

    public synchronized String getActiveDeviceAddress() {
        if (!connectionPool.isEmpty()) {
            return connectionPool.keySet().iterator().next();
        }
        return "";
    }

    public synchronized String getActiveDeviceName() {
        String addr = getActiveDeviceAddress();
        if (!addr.isEmpty()) {
            return deviceNames.getOrDefault(addr, "Prime User");
        }
        return "";
    }

    public synchronized boolean isConnected(String deviceAddress) {
        if (deviceAddress == null || deviceAddress.isEmpty()) {
            return !connectionPool.isEmpty();
        }
        ConnectedThread thread = getThreadFor(deviceAddress);
        return thread != null && thread.isAlive();
    }

    public synchronized int getConnectedDeviceCount() {
        return connectionPool.size();
    }

    private synchronized void setState(String deviceAddress, ConnectionState newState, String deviceName) {
        if (deviceAddress == null || deviceAddress.isEmpty()) return;
        
        deviceStates.put(deviceAddress, newState);
        if (deviceName != null && !deviceName.isEmpty()) {
            deviceNames.put(deviceAddress, deviceName);
        }

        final String finalName = deviceNames.getOrDefault(deviceAddress, "Prime User");

        mainHandler.post(() -> {
            for (ConnectionCallback cb : callbacks) {
                try {
                    cb.onStateChanged(deviceAddress, newState, finalName);
                } catch (Exception e) {
                    Log.e(TAG, "Error in onStateChanged callback", e);
                }
            }
        });
    }

    private void notifyPacketReceived(String deviceAddress, byte type, byte[] payload) {
        final byte[] data = payload != null ? payload : new byte[0];
        if (type == 0x01 && data.length > 0) { // TYPE_TEXT
            try {
                String str = new String(data, StandardCharsets.UTF_8);
                String remoteName = null;
                if (str.startsWith("HANDSHAKE:")) {
                    String sub = str.substring(10).trim();
                    if (sub.contains("name=")) {
                        for (String p : sub.split(";")) {
                            if (p.startsWith("name=")) remoteName = p.substring(5);
                        }
                    } else if (!sub.isEmpty()) remoteName = sub;
                } else if (str.startsWith("HANDSHAKE_ACK:")) {
                    String sub = str.substring(14).trim();
                    if (sub.contains("name=")) {
                        for (String p : sub.split(";")) {
                            if (p.startsWith("name=")) remoteName = p.substring(5);
                        }
                    } else if (!sub.isEmpty()) remoteName = sub;
                }
                if (remoteName != null && !remoteName.isEmpty()) {
                    setRemoteUsername(deviceAddress, remoteName);
                }
            } catch (Exception ignored) {}
        }
        mainHandler.post(() -> {
            for (ConnectionCallback cb : callbacks) {
                try {
                    cb.onPacketReceived(deviceAddress, type, data);
                } catch (Exception e) {
                    Log.e(TAG, "Error in onPacketReceived callback", e);
                }
            }
        });
    }

    private void notifyError(String deviceAddress, String errorMsg) {
        mainHandler.post(() -> {
            for (ConnectionCallback cb : callbacks) {
                try {
                    cb.onError(deviceAddress, errorMsg);
                } catch (Exception e) {
                    Log.e(TAG, "Error in onError callback", e);
                }
            }
        });
    }

    private void notifySendProgress(String deviceAddress, int progress) {
        mainHandler.post(() -> {
            for (ConnectionCallback cb : callbacks) {
                try {
                    cb.onSendProgress(deviceAddress, progress);
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

        if (isMacBlocked(device.getAddress())) {
            Log.w(TAG, "DDoS Guard: Connect request blocked for MAC: " + device.getAddress());
            return;
        }

        if (isConnected(device.getAddress())) {
            Log.d(TAG, "Already connected to target device: " + device.getAddress());
            setState(device.getAddress(), ConnectionState.CONNECTED, device.getName());
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
            Handler retryHandler = connectRetryHandlers.get(device.getAddress());
            if (retryHandler == null) {
                retryHandler = new Handler(Looper.getMainLooper());
                connectRetryHandlers.put(device.getAddress(), retryHandler);
            } else {
                retryHandler.removeCallbacksAndMessages(null);
            }
            final BluetoothAdapter adp = adapter;
            final BluetoothDevice dev = device;
            final UUID uuid = serviceUuid;
            retryHandler.postDelayed(() -> {
                if (!isConnected(dev.getAddress())) {
                    Log.d(TAG, "Server wait timeout fallback: forcing client connection to " + dev.getAddress());
                    ConnectThread ct = new ConnectThread(adp, dev, uuid);
                    connectingThreads.put(dev.getAddress(), ct);
                    ct.start();
                    setState(dev.getAddress(), ConnectionState.CONNECTING, dev.getName());
                }
            }, 1500L);
            return;
        }

        ConnectThread currentCT = connectingThreads.get(device.getAddress());
        if (currentCT != null && currentCT.isAlive()) {
            currentCT.cancel();
        }
        
        ConnectThread newCT = new ConnectThread(adapter, device, serviceUuid);
        connectingThreads.put(device.getAddress(), newCT);
        newCT.start();
        setState(device.getAddress(), ConnectionState.CONNECTING, device.getName());
    }

    /**
     * Переход в состояние активного соединения после успешного коннекта.
     */
    @SuppressLint("MissingPermission")
    public synchronized void onSocketConnected(BluetoothSocket socket, BluetoothDevice device) {
        if (socket == null || !socket.isConnected() || device == null) return;

        String devAddr = device.getAddress();
        String devAddrUpper = devAddr != null ? devAddr.toUpperCase(Locale.US) : "";

        ConnectThread ct = connectingThreads.remove(devAddrUpper);
        if (ct == null && devAddr != null) ct = connectingThreads.remove(devAddr);
        if (ct != null) ct.cancel();

        ConnectedThread oldCt = connectionPool.get(devAddrUpper);
        if (oldCt == null && devAddr != null) oldCt = connectionPool.get(devAddr);
        if (oldCt != null) {
            oldCt.cancel();
        }

        String devName = "";
        try {
            devName = device.getName() != null ? device.getName() : "Prime User";
        } catch (SecurityException ignored) {}

        ConnectedThread newThread = new ConnectedThread(socket);
        if (!devAddrUpper.isEmpty()) {
            connectionPool.put(devAddrUpper, newThread);
        }
        if (devAddr != null && !devAddr.isEmpty()) {
            connectionPool.put(devAddr, newThread);
        }
        newThread.start();

        setState(devAddr, ConnectionState.CONNECTED, devName);
    }

    public synchronized ConnectedThread getThreadFor(String addressOrName) {
        if (addressOrName == null || addressOrName.isEmpty()) {
            if (connectionPool.size() == 1) {
                return connectionPool.values().iterator().next();
            }
            return null;
        }

        String upperAddr = addressOrName.toUpperCase(Locale.US);
        ConnectedThread r = connectionPool.get(upperAddr);
        if (r != null && r.isAlive()) return r;

        r = connectionPool.get(addressOrName);
        if (r != null && r.isAlive()) return r;

        for (ConnectedThread thread : connectionPool.values()) {
            if (thread != null && thread.isAlive()) {
                if (addressOrName.equalsIgnoreCase(thread.getThreadDeviceAddress()) ||
                    addressOrName.equalsIgnoreCase(thread.getThreadRemoteUsername())) {
                    return thread;
                }
            }
        }

        if (connectionPool.size() == 1) {
            ConnectedThread singleThread = connectionPool.values().iterator().next();
            if (singleThread != null && singleThread.isAlive()) {
                return singleThread;
            }
        }

        return null;
    }

    public synchronized void setRemoteUsername(String deviceAddress, String username) {
        if (username == null || username.isEmpty()) return;
        ConnectedThread t = getThreadFor(deviceAddress);
        if (t != null) {
            t.setThreadRemoteUsername(username);
        }
    }

    /**
     * Отправка пакета через активный сокет.
     */
    public synchronized void sendPacket(String deviceAddress, byte type, byte[] payload) {
        ConnectedThread r = getThreadFor(deviceAddress);
        if (r != null && r.isAlive()) {
            r.sendPacket(type, payload);
        } else {
            Log.w(TAG, "Cannot send packet, no active connected thread for " + deviceAddress);
        }
    }

    public synchronized void broadcastPacket(byte type, byte[] payload) {
        for (ConnectedThread r : connectionPool.values()) {
            if (r != null && r.isAlive()) {
                r.sendPacket(type, payload);
            }
        }
    }

    public synchronized void cancelCurrentMediaSend(String deviceAddress) {
        ConnectedThread r = getThreadFor(deviceAddress);
        if (r != null) {
            r.cancelCurrentMediaSend();
        }
    }

    /**
     * Отключение активного соединения.
     */
    public synchronized void disconnect(String deviceAddress) {
        if (deviceAddress == null) {
            disconnect();
            return;
        }
        ConnectThread ct = connectingThreads.remove(deviceAddress);
        if (ct != null) ct.cancel();

        ConnectedThread ctd = connectionPool.remove(deviceAddress);
        if (ctd != null) ctd.cancel();

        setState(deviceAddress, ConnectionState.DISCONNECTED, null);
    }

    public synchronized void disconnect() {
        stopAll();
    }

    /**
     * Полный сброс всех потоков и ресурсов.
     */
    public synchronized void stopAll() {
        for (ConnectThread ct : connectingThreads.values()) {
            ct.cancel();
        }
        connectingThreads.clear();

        for (String addr : connectionPool.keySet()) {
            ConnectedThread ctd = connectionPool.get(addr);
            if (ctd != null) ctd.cancel();
            setState(addr, ConnectionState.DISCONNECTED, null);
        }
        connectionPool.clear();
        deviceStates.clear();
        deviceNames.clear();

        stopAcceptThread();
    }

    private synchronized void stopAcceptThread() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
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
                    onSocketConnected(socket, remoteDevice);
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
        private volatile boolean isCancelled = false;

        public ConnectThread(BluetoothAdapter adapter, BluetoothDevice device, UUID uuid) {
            mmAdapter = adapter;
            mmDevice = device;
            mmUuid = uuid;
        }

        public void run() {
            if (isCancelled) return;
            if (mmAdapter != null) {
                try {
                    mmAdapter.cancelDiscovery();
                } catch (Exception ignored) {}
            }

            BluetoothSocket socket = null;

            // Stage 1: Insecure RFCOMM (Без системного PIN-кода)
            try {
                socket = mmDevice.createInsecureRfcommSocketToServiceRecord(mmUuid);
                if (socket != null) {
                    this.mmSocket = socket;
                    socket.connect();
                }
            } catch (Exception e1) {
                Log.w(TAG, "Stage 1 (Insecure RFCOMM) failed: " + e1.getMessage());
                closeSocketQuietly(socket);
                socket = null;
            }

            // Stage 2: Reflection Insecure RFCOMM
            if (socket == null && !isCancelled) {
                try {
                    socket = (BluetoothSocket) mmDevice.getClass()
                            .getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class)
                            .invoke(mmDevice, mmUuid);
                    if (socket != null) {
                        this.mmSocket = socket;
                        socket.connect();
                    }
                } catch (Exception e3) {
                    Log.w(TAG, "Stage 2 (Reflection Insecure RFCOMM) failed: " + e3.getMessage());
                    closeSocketQuietly(socket);
                    socket = null;
                }
            }

            // Stage 3: Direct RFCOMM Channel 1 Port Fallback (Для устройств без закэшированных SDP записей)
            if (socket == null && !isCancelled) {
                try {
                    socket = (BluetoothSocket) mmDevice.getClass()
                            .getMethod("createRfcommSocket", int.class)
                            .invoke(mmDevice, 1);
                    if (socket != null) {
                        this.mmSocket = socket;
                        socket.connect();
                    }
                } catch (Exception eFallback) {
                    Log.w(TAG, "Stage 3 (Channel 1 Port Fallback) failed: " + eFallback.getMessage());
                    closeSocketQuietly(socket);
                    socket = null;
                }
            }

            // Stage 4: Secure RFCOMM (Вызываем ТОЛЬКО если устройство уже спарено / BONDED)
            boolean isBonded = false;
            try {
                isBonded = mmDevice.getBondState() == BluetoothDevice.BOND_BONDED;
            } catch (SecurityException ignored) {}

            if (socket == null && !isCancelled && isBonded) {
                try {
                    socket = mmDevice.createRfcommSocketToServiceRecord(mmUuid);
                    if (socket != null) {
                        this.mmSocket = socket;
                        socket.connect();
                    }
                } catch (Exception e2) {
                    Log.w(TAG, "Stage 3 (Secure RFCOMM) failed: " + e2.getMessage());
                    closeSocketQuietly(socket);
                    socket = null;
                }
            }

            if (isCancelled) {
                closeSocketQuietly(socket);
                return;
            }

            if (socket != null && socket.isConnected()) {
                this.isHandedOff = true;
                onSocketConnected(socket, mmDevice);
            } else {
                notifyError(mmDevice.getAddress(), "Не удалось установить прямое соединение с устройством");
                setState(mmDevice.getAddress(), ConnectionState.DISCONNECTED, null);
            }
        }

        public void cancel() {
            if (isHandedOff) return;
            isCancelled = true;
            closeSocketQuietly(mmSocket);
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final DataInputStream mmInStream;
        private final DataOutputStream mmOutStream;
        private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();
        private volatile boolean isRunning = true;
        private final String threadDeviceAddress;
        private volatile String threadRemoteUsername;

        public String getThreadDeviceAddress() {
            return threadDeviceAddress;
        }

        public String getThreadRemoteUsername() {
            return threadRemoteUsername;
        }

        public void setThreadRemoteUsername(String username) {
            if (username != null && !username.isEmpty()) {
                this.threadRemoteUsername = username;
            }
        }

        @SuppressLint("MissingPermission")
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            String addr = "";
            String name = "";
            try {
                if (socket != null && socket.getRemoteDevice() != null) {
                    addr = socket.getRemoteDevice().getAddress();
                    name = socket.getRemoteDevice().getName();
                }
            } catch (Exception ignored) {}
            this.threadDeviceAddress = addr;
            this.threadRemoteUsername = name;
            
            DataInputStream tmpIn = null;
            DataOutputStream tmpOut = null;
            try {
                tmpIn = new DataInputStream(new BufferedInputStream(socket.getInputStream(), 65536));
                tmpOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 65536));
            } catch (IOException e) {
                Log.e(TAG, "Sockets streams create failed", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        private volatile boolean isMediaSendingCancelled = false;
        private final AtomicBoolean isDisconnectNotified = new AtomicBoolean(false);

        private void handleThreadDisconnect(String errorMsg, boolean isMediaPacket) {
            if (isDisconnectNotified.compareAndSet(false, true)) {
                if (isMediaPacket) {
                    notifySendProgress(threadDeviceAddress, -1);
                }
                notifyError(threadDeviceAddress, errorMsg);
                setState(threadDeviceAddress, ConnectionState.DISCONNECTED, null);
                cancel();
            }
        }

        private final Handler keepAliveHandler = new Handler(Looper.getMainLooper());
        private final Runnable keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning && mmOutStream != null) {
                    sendPacket((byte) 0x05, new byte[0]);
                    keepAliveHandler.postDelayed(this, 3500L);
                }
            }
        };

        public void run() {
            if (mmInStream == null) return;
            keepAliveHandler.postDelayed(keepAliveRunnable, 3500L);
            try {
                while (isRunning) {
                    try {
                        byte type = mmInStream.readByte();
                        int length = mmInStream.readInt();

                        if (length < 0 || length > 50 * 1024 * 1024) {
                            throw new IOException("Invalid or excessive packet length: " + length);
                        }

                        byte[] payloadData;
                        try {
                            payloadData = new byte[length];
                        } catch (OutOfMemoryError oom) {
                            throw new IOException("OOM allocating payload buffer: " + length);
                        }

                        if (length > 0) {
                            mmInStream.readFully(payloadData);
                        }

                        if (type == 0x05) { // TYPE_PING
                            continue;
                        }

                        notifyPacketReceived(threadDeviceAddress, type, payloadData);
                    } catch (IOException e) {
                        if (isRunning) {
                            Log.e(TAG, "Connection lost during read", e);
                            handleThreadDisconnect("Соединение сброшено", false);
                        }
                        break;
                    }
                }
            } finally {
                keepAliveHandler.removeCallbacks(keepAliveRunnable);
            }
        }

        public void cancelCurrentMediaSend() {
            isMediaSendingCancelled = true;
        }

        public void sendPacket(byte type, byte[] payload) {
            if (!isRunning || mmOutStream == null) return;
            boolean isMediaPacket = (type == 0x02 || type == 0x0E); // TYPE_PHOTO (0x02), TYPE_FILE (0x0E)
            if (isMediaPacket) {
                isMediaSendingCancelled = false;
            }
            writeExecutor.execute(() -> {
                try {
                    synchronized (this) {
                        mmOutStream.writeByte(type);
                        int len = payload != null ? payload.length : 0;
                        mmOutStream.writeInt(len);
                        if (len > 0 && payload != null) {
                            int offset = 0;
                            int chunkSize = 16384; // 16 KB for stability (prevents BT stack buffer overflow)
                            long lastProgressReportTime = 0L;
                            int lastReportedProgress = -1;

                            while (offset < len) {
                                if (!isRunning) break;
                                if (isMediaPacket && isMediaSendingCancelled) {
                                    Log.d(TAG, "Media send cancelled by user");
                                    isMediaSendingCancelled = false;
                                    notifySendProgress(threadDeviceAddress, -1);
                                    throw new IOException("Media send cancelled by user - socket desynced, forcing reconnect.");
                                }
                                int bytesToWrite = Math.min(chunkSize, len - offset);
                                mmOutStream.write(payload, offset, bytesToWrite);
                                mmOutStream.flush();
                                offset += bytesToWrite;

                                if (isMediaPacket) {
                                    int progress = (int) ((offset * 100L) / len);
                                    long now = SystemClock.elapsedRealtime();
                                    if (progress != lastReportedProgress && (now - lastProgressReportTime > 80L || progress == 100)) {
                                        lastReportedProgress = progress;
                                        lastProgressReportTime = now;
                                        notifySendProgress(threadDeviceAddress, progress);
                                    }
                                }
                            }
                            mmOutStream.flush();
                            if (isMediaPacket && !isMediaSendingCancelled) {
                                notifySendProgress(threadDeviceAddress, 100);
                            }
                        } else {
                            mmOutStream.flush();
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error writing packet type " + type, e);
                    if (isRunning) {
                        handleThreadDisconnect("Ошибка отправки данных: " + e.getMessage(), isMediaPacket);
                    }
                }
            });
        }

        public void cancel() {
            isRunning = false;
            isDisconnectNotified.set(true);
            keepAliveHandler.removeCallbacks(keepAliveRunnable);
            writeExecutor.shutdownNow();
            closeSocketQuietly(mmSocket);
            connectionPool.remove(threadDeviceAddress);
            connectingThreads.remove(threadDeviceAddress);
        }
    }
}
