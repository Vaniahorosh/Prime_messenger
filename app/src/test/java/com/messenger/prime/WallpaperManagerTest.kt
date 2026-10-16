package com.messenger.prime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WallpaperManagerTest {

    @Test
    fun testGetDefaultWallpaper() {
        val defaultWp = WallpaperManager.getDefaultWallpaper()
        assertNotNull(defaultWp)
        assertEquals("default", defaultWp.id)
        assertEquals("DEFAULT", defaultWp.type)
        assertEquals("Стандартные", defaultWp.name)
        assertNull(defaultWp.imagePath)
    }

    @Test
    fun testChatWallpaperDataClass() {
        val wp = ChatWallpaper(
            id = "wp_1",
            name = "Custom Wallpaper",
            type = "IMAGE",
            imagePath = "/path/to/img.png",
            scale = 1.2f,
            dimOpacity = 0.3f
        )

        assertEquals("wp_1", wp.id)
        assertEquals("Custom Wallpaper", wp.name)
        assertEquals("IMAGE", wp.type)
        assertEquals("/path/to/img.png", wp.imagePath)
        assertEquals(1.2f, wp.scale)
        assertEquals(0.3f, wp.dimOpacity)
    }
}
