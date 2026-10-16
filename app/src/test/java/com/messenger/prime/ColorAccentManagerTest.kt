package com.messenger.prime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorAccentManagerTest {

    @Test
    fun testPresetColorsList() {
        val presets = ColorAccentManager.PRESET_COLORS
        assertNotNull(presets)
        assertTrue(presets.isNotEmpty())
        assertEquals(7, presets.size)

        val brandPreset = presets.first()
        assertEquals("Фирменный", brandPreset.name)
    }

    @Test
    fun testIsBrandColor() {
        val brandLight = 0xFF154B87.toInt()
        val brandDark = 0xFF4D9FFF.toInt()
        
        assertTrue(ColorAccentManager.isBrandColor(brandLight))
        assertTrue(ColorAccentManager.isBrandColor(brandDark))
        assertFalse(ColorAccentManager.isBrandColor(0))
    }

    @Test
    fun testGetAvatarColor_deterministic() {
        val color1 = ColorAccentManager.getAvatarColor("John")
        val color2 = ColorAccentManager.getAvatarColor("John")
        assertEquals(color1, color2)

        val colorAlice = ColorAccentManager.getAvatarColor("Alice")
        assertNotNull(colorAlice)
    }
}
