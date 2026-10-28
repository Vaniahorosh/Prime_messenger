package com.messenger.prime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceAnonymizerTest {

    @Test
    fun testIsMacAddress_valid() {
        assertTrue(DeviceAnonymizer.isMacAddress("00:11:22:33:44:55"))
        assertTrue(DeviceAnonymizer.isMacAddress("AA:BB:CC:DD:EE:FF"))
        assertTrue(DeviceAnonymizer.isMacAddress("00-11-22-33-44-55"))
        assertTrue(DeviceAnonymizer.isMacAddress("a1:b2:c3:d4:e5:f6"))
        assertTrue(DeviceAnonymizer.isMacAddress("001122334455"))
    }

    @Test
    fun testIsMacAddress_invalid() {
        assertFalse(DeviceAnonymizer.isMacAddress(null))
        assertFalse(DeviceAnonymizer.isMacAddress(""))
        assertFalse(DeviceAnonymizer.isMacAddress("00:11:22"))
        assertFalse(DeviceAnonymizer.isMacAddress("Device_123"))
        assertFalse(DeviceAnonymizer.isMacAddress("ZZ:11:22:33:44:55"))
        assertFalse(DeviceAnonymizer.isMacAddress("Galaxy S21"))
    }

    @Test
    fun testGetAnonymizedName_normalName() {
        assertEquals("Иван Иванов", DeviceAnonymizer.getAnonymizedName("Иван Иванов", "00:11:22:33:44:55"))
        assertEquals("Alice", DeviceAnonymizer.getAnonymizedName("Alice", "AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun testGetAnonymizedName_macOrGeneric() {
        // When raw name is a MAC address itself or generic "Собеседник"
        val name1 = DeviceAnonymizer.getAnonymizedName("00:11:22:33:44:55", "00:11:22:33:44:55")
        assertTrue(name1.startsWith("Собеседник #"))
        assertFalse("Must not expose MAC suffix", name1.contains("4455"))

        val name2 = DeviceAnonymizer.getAnonymizedName("Собеседник", "AA:BB:CC:DD:EE:FF")
        assertTrue(name2.startsWith("Собеседник #"))
        assertFalse("Must not expose MAC suffix", name2.contains("EEFF"))

        val name3 = DeviceAnonymizer.getAnonymizedName(null, null)
        assertTrue(name3.startsWith("Собеседник #"))
    }

    @Test
    fun testSanitizeDisplayName_stripsEmbeddedMac() {
        val sanitized = DeviceAnonymizer.sanitizeDisplayName("Galaxy S21 (00:11:22:33:44:55)")
        assertEquals("Galaxy S21", sanitized)
    }
}