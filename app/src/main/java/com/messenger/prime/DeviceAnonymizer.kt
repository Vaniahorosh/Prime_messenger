package com.messenger.prime

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice

object DeviceAnonymizer {

    @JvmStatic
    fun isMacAddress(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return text.trim().matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"))
    }

    @JvmStatic
    fun getAnonymizedName(rawName: String?, macAddress: String?): String {
        if (!rawName.isNullOrBlank() && !isMacAddress(rawName) && rawName != "Собеседник" && !rawName.startsWith("Устройство #")) {
            return rawName.trim()
        }
        if (macAddress.isNullOrBlank()) return "Устройство #0000"
        val clean = macAddress.replace(":", "").replace("-", "").uppercase()
        val suffix = if (clean.length >= 4) clean.takeLast(4) else clean
        return "Устройство #$suffix"
    }

    @JvmStatic
    @SuppressLint("MissingPermission")
    fun isAllowedDeviceClass(device: BluetoothDevice, devName: String?, isSavedInChats: Boolean, hasPrimeUuid: Boolean): Boolean {
        val btClass = try { device.bluetoothClass } catch (_: Exception) { null }
        if (btClass != null) {
            val major = btClass.majorDeviceClass
            when (major) {
                BluetoothClass.Device.Major.AUDIO_VIDEO,
                BluetoothClass.Device.Major.WEARABLE,
                BluetoothClass.Device.Major.PERIPHERAL,
                BluetoothClass.Device.Major.HEALTH,
                BluetoothClass.Device.Major.TOY -> return false
                BluetoothClass.Device.Major.PHONE,
                BluetoothClass.Device.Major.COMPUTER -> return true
                BluetoothClass.Device.Major.UNCATEGORIZED -> {
                    return isSavedInChats || hasPrimeUuid || (devName?.contains("Prime", ignoreCase = true) == true)
                }
            }
        }
        return true
    }
}
