package com.messenger.prime

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Base64
import android.util.Log

object DeviceBindingManager {
    private const val TAG = "DeviceBindingManager"
    
    @JvmStatic
    @SuppressLint("MissingPermission")
    fun bindDevice(device: BluetoothDevice?): Boolean {
        if (device == null) return false
        return try {
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                val method = device.javaClass.getMethod("createBond")
                method.invoke(device) as Boolean
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    @JvmStatic
    @SuppressLint("MissingPermission")
    fun unbindDevice(device: BluetoothDevice?): Boolean {
        if (device == null) return false
        return try {
            if (device.bondState != BluetoothDevice.BOND_NONE) {
                val method = device.javaClass.getMethod("removeBond")
                method.invoke(device) as Boolean
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    @JvmStatic
    fun saveDeviceSecurely(context: Context, username: String?, macAddress: String?) {
        if (username.isNullOrBlank() || macAddress.isNullOrBlank()) return
        try {
            val (iv, encrypted) = SecurityManager.encrypt(macAddress.toByteArray(Charsets.UTF_8))
            val ivStr = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encStr = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            
            val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            sp.edit()
                .putString("${username}_mac_iv", ivStr)
                .putString("${username}_mac_enc", encStr)
                .remove("${username}_mac") // Убираем открытый вид для конфиденциальности
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save device binding securely")
        }
    }
    
    @JvmStatic
    fun getSecureDeviceMac(context: Context, username: String?): String? {
        if (username.isNullOrBlank()) return null
        try {
            val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            
            // Миграция старого открытого MAC
            val plainMac = sp.getString("${username}_mac", null)
            if (!plainMac.isNullOrEmpty()) {
                saveDeviceSecurely(context, username, plainMac)
                return plainMac
            }
            
            val ivStr = sp.getString("${username}_mac_iv", null)
            val encStr = sp.getString("${username}_mac_enc", null)
            
            if (ivStr != null && encStr != null) {
                val iv = Base64.decode(ivStr, Base64.NO_WRAP)
                val encrypted = Base64.decode(encStr, Base64.NO_WRAP)
                val decrypted = SecurityManager.decrypt(iv, encrypted)
                return String(decrypted, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve secure device binding")
        }
        return null
    }

    @JvmStatic
    fun removeSecureDeviceMac(context: Context, username: String?) {
        if (username.isNullOrBlank()) return
        try {
            val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            sp.edit()
                .remove("${username}_mac_iv")
                .remove("${username}_mac_enc")
                .remove("${username}_mac")
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove secure device binding")
        }
    }

    @JvmStatic
    @SuppressLint("MissingPermission")
    fun unbindDeviceSecurely(context: Context, username: String?, explicitMac: String?) {
        try {
            val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
            val currentUser = sp.getString("current_user", "") ?: ""

            val mac = explicitMac
                ?: getSecureDeviceMac(context, username)
                ?: (if (!username.isNullOrEmpty() && BluetoothAdapter.checkBluetoothAddress(username)) username else null)

            val bManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
            val adapter = bManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

            if (adapter != null) {
                try {
                    val bonded = adapter.bondedDevices
                    if (bonded != null) {
                        for (dev in bonded) {
                            val addr = dev.address
                            val name = dev.name
                            val matchMac = !mac.isNullOrEmpty() && addr.equals(mac, ignoreCase = true)
                            val matchName = !username.isNullOrEmpty() && name != null && name.equals(username, ignoreCase = true)
                            if (matchMac || matchName) {
                                unbindDevice(dev)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed unbonding from bondedDevices list: ${e.message}")
                }

                if (!mac.isNullOrEmpty() && BluetoothAdapter.checkBluetoothAddress(mac)) {
                    try {
                        val device = adapter.getRemoteDevice(mac.uppercase(java.util.Locale.US))
                        if (device != null) {
                            unbindDevice(device)
                        }
                    } catch (_: Exception) {}

                    try {
                        BluetoothConnectionManager.getInstance().unblockMacAddress(mac)
                        BluetoothConnectionManager.getInstance().disconnect(mac)
                        PrimeBluetoothService.cancelReconnect(context, mac)
                    } catch (_: Exception) {}
                }
            }

            if (!username.isNullOrEmpty()) {
                removeSecureDeviceMac(context, username)
                if (currentUser.isNotEmpty()) {
                    removeSecureDeviceMac(context, "${currentUser}_${username}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unbind device securely", e)
        }
    }
}
