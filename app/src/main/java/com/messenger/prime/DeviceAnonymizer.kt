package com.messenger.prime

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import java.util.Locale

object DeviceAnonymizer {

    private val MAC_REGEX = Regex("(?i)^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$|^[0-9A-Fa-f]{12}$")
    private val EMBEDDED_MAC_REGEX = Regex("(?i)\\b([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})\\b|\\b[0-9A-Fa-f]{12}\\b|\\bBluetooth#[^\\s,\\]\\)]+")

    @JvmStatic
    fun isMacAddress(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val clean = text.trim()
        return MAC_REGEX.matches(clean) || 
               clean.startsWith("Bluetooth#", ignoreCase = true) ||
               clean.startsWith("peer_", ignoreCase = true)
    }

    @JvmStatic
    fun sanitizeDisplayName(rawName: String?, macAddress: String? = null): String {
        if (rawName.isNullOrBlank() || 
            rawName.equals("null", ignoreCase = true) || 
            rawName.equals("unknown", ignoreCase = true) ||
            rawName.equals("Неизвестное", ignoreCase = true) ||
            rawName == "1" ||
            isMacAddress(rawName)) {
            return getAnonymizedName(null, macAddress)
        }

        val stripped = EMBEDDED_MAC_REGEX.replace(rawName, "")
            .replace("()", "")
            .replace("[]", "")
            .trim()

        if (stripped.isBlank() || isMacAddress(stripped) || stripped.equals("Bluetooth", ignoreCase = true)) {
            return getAnonymizedName(null, macAddress)
        }

        return stripped
    }

    @JvmStatic
    fun getAnonymizedName(rawName: String?, macAddress: String? = null): String {
        val candidate = rawName?.trim()
        if (!candidate.isNullOrBlank() && 
            !isMacAddress(candidate) && 
            candidate != "Собеседник" && 
            candidate != "Prime Собеседник" &&
            !candidate.startsWith("Устройство #") && 
            !candidate.startsWith("Собеседник #")) {
            return sanitizeDisplayName(candidate, macAddress)
        }

        val seed = (macAddress ?: candidate ?: "DEVICE").trim().uppercase(Locale.US)
        val hash = Math.abs(seed.hashCode())
        val code = (hash % 9000) + 1000
        return "Собеседник #"
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