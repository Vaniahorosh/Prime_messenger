package com.messenger.prime;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BluetoothSocketHolder {
    private static BluetoothSocket socket;
    private static Object connectedThreadInstance;
    private static String activeDeviceAddress;
    private static String activeTargetUsername;

    // Multi-session support for non-disruptive reconnection across different contacts
    private static final Map<String, BluetoothSocket> socketMap = new ConcurrentHashMap<>();
    private static final Map<String, Object> threadMap = new ConcurrentHashMap<>();

    public static synchronized String getLocalDeviceId(Context context) {
        if (context == null) return "User_Device";
        SharedPreferences prefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        String id = prefs.getString("local_device_guid", null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString("local_device_guid", id).apply();
        }
        return id;
    }

    public static synchronized void registerConnection(String address, String username, BluetoothSocket s, Object thread) {
        if (s != null) {
            if (address != null && !address.isEmpty()) {
                socketMap.put(address.toUpperCase(), s);
            }
            if (username != null && !username.isEmpty()) {
                socketMap.put(username.toLowerCase(), s);
            }
            if (thread != null) {
                if (address != null && !address.isEmpty()) threadMap.put(address.toUpperCase(), thread);
                if (username != null && !username.isEmpty()) threadMap.put(username.toLowerCase(), thread);
            }
        }
        // Maintain legacy single references for backward compatibility
        socket = s;
        connectedThreadInstance = thread;
        if (address != null && !address.isEmpty()) activeDeviceAddress = address;
        if (username != null && !username.isEmpty()) activeTargetUsername = username;
    }

    public static synchronized void setSocket(BluetoothSocket s) {
        if (socket != null && socket != s) {
            try { socket.close(); } catch (Exception ignored) {}
        }
        socket = s;
        if (s != null) {
            try {
                if (s.getRemoteDevice() != null) {
                    String addr = s.getRemoteDevice().getAddress();
                    if (addr != null && !addr.isEmpty()) {
                        socketMap.put(addr.toUpperCase(), s);
                        activeDeviceAddress = addr;
                    }
                    try {
                        String name = s.getRemoteDevice().getName();
                        if (name != null && !name.isEmpty()) {
                            socketMap.put(name.toLowerCase(), s);
                            activeTargetUsername = name;
                        }
                    } catch (SecurityException ignored) {}
                }
            } catch (Exception ignored) {}
        }
    }

    public static synchronized BluetoothSocket getSocket() {
        return socket;
    }

    public static synchronized BluetoothSocket getSocketFor(String address, String name) {
        if (address != null && !address.isEmpty() && socketMap.containsKey(address.toUpperCase())) {
            BluetoothSocket s = socketMap.get(address.toUpperCase());
            if (s != null && s.isConnected()) return s;
        }
        if (name != null && !name.isEmpty() && socketMap.containsKey(name.toLowerCase())) {
            BluetoothSocket s = socketMap.get(name.toLowerCase());
            if (s != null && s.isConnected()) return s;
        }
        return socket;
    }

    public static synchronized Object getThreadFor(String address, String name) {
        if (address != null && !address.isEmpty() && threadMap.containsKey(address.toUpperCase())) {
            return threadMap.get(address.toUpperCase());
        }
        if (name != null && !name.isEmpty() && threadMap.containsKey(name.toLowerCase())) {
            return threadMap.get(name.toLowerCase());
        }
        return connectedThreadInstance;
    }

    public static synchronized void clearSocket() {
        for (BluetoothSocket s : socketMap.values()) {
            if (s != null) {
                try { s.close(); } catch (Exception ignored) {}
            }
        }
        socketMap.clear();
        threadMap.clear();

        if (socket != null) {
            try { socket.close(); } catch (Exception ignored) {}
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
        BluetoothSocket targetSocket = getSocketFor(address, name);
        Object targetThread = getThreadFor(address, name);

        if (targetSocket == null || !targetSocket.isConnected()) return false;
        if (targetThread == null) return false;

        boolean addressMatched = false;
        boolean nameMatched = false;

        try {
            if (address != null && targetSocket.getRemoteDevice() != null) {
                if (address.equalsIgnoreCase(targetSocket.getRemoteDevice().getAddress())) {
                    addressMatched = true;
                }
            }
        } catch (Exception ignored) {}

        if (address != null && activeDeviceAddress != null && address.equalsIgnoreCase(activeDeviceAddress)) {
            addressMatched = true;
        }
        if (name != null && activeTargetUsername != null && name.equalsIgnoreCase(activeTargetUsername)) {
            nameMatched = true;
        }
        if (address != null && socketMap.containsKey(address.toUpperCase())) {
            addressMatched = true;
        }
        if (name != null && socketMap.containsKey(name.toLowerCase())) {
            nameMatched = true;
        }

        if (address == null && name == null) return true;
        if (address != null && name != null) {
            return addressMatched || nameMatched;
        }
        if (address != null) return addressMatched;
        return nameMatched;
    }

    public static synchronized void notifyProfileChanged(Context context) {
        try {
            SharedPreferences sharedPrefs = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
            String currentUser = sharedPrefs.getString("current_user", "");
            String myDisplayName = sharedPrefs.getString(currentUser + "_name", currentUser);
            String localUsername = (myDisplayName != null && !myDisplayName.isEmpty()) ? myDisplayName : "Пользователь";

            String handshake = "HANDSHAKE:login=" + currentUser + ";name=" + localUsername + ";version=" + ChatPersonActivity.getAppVersionCode(context);
            byte[] handshakeBytes = handshake.getBytes(StandardCharsets.UTF_8);

            String localAvatarUri = sharedPrefs.getString(currentUser + "_avatar", "");
            if (localAvatarUri == null || localAvatarUri.isEmpty()) {
                File f = new File(context.getFilesDir(), "avatar_" + currentUser + ".jpg");
                if (f.exists()) localAvatarUri = Uri.fromFile(f).toString();
            }

            byte[] avatarBytes = new byte[0];
            if (localAvatarUri != null && !localAvatarUri.isEmpty()) {
                try {
                    InputStream is = context.getContentResolver().openInputStream(Uri.parse(localAvatarUri));
                    if (is != null) {
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        is.close();
                        if (bitmap != null) {
                            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 96, 96, false);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                            avatarBytes = baos.toByteArray();
                        }
                    }
                } catch (Exception ignored) {}
            }

            for (Object threadObj : threadMap.values()) {
                if (threadObj instanceof ChatPersonActivity.ConnectedThread) {
                    ChatPersonActivity.ConnectedThread thread = (ChatPersonActivity.ConnectedThread) threadObj;
                    if (thread.isAlive()) {
                        thread.sendPacket((byte) 0x0F, handshakeBytes); // TYPE_PROFILE_UPDATE
                        if (avatarBytes != null) {
                            thread.sendPacket((byte) 0x07, avatarBytes); // TYPE_AVATAR
                        } else {
                            thread.sendPacket((byte) 0x07, new byte[0]); // Clear avatar
                        }
                    }
                }
            }
            if (connectedThreadInstance instanceof ChatPersonActivity.ConnectedThread) {
                ChatPersonActivity.ConnectedThread thread = (ChatPersonActivity.ConnectedThread) connectedThreadInstance;
                if (thread.isAlive()) {
                    thread.sendPacket((byte) 0x0F, handshakeBytes);
                    if (avatarBytes != null) {
                        thread.sendPacket((byte) 0x07, avatarBytes);
                    } else {
                        thread.sendPacket((byte) 0x07, new byte[0]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
