package com.jprecise;

import org.junit.Test;
import static org.junit.Assert.*;

public class VolumeEventFormatterTest {

    @Test
    public void testIsVolumeKey() {
        assertTrue(VolumeEventFormatter.isVolumeKey(24));  // KEYCODE_VOLUME_UP
        assertTrue(VolumeEventFormatter.isVolumeKey(25));  // KEYCODE_VOLUME_DOWN
        assertTrue(VolumeEventFormatter.isVolumeKey(164)); // KEYCODE_VOLUME_MUTE
        assertFalse(VolumeEventFormatter.isVolumeKey(3));  // KEYCODE_HOME
        assertFalse(VolumeEventFormatter.isVolumeKey(4));  // KEYCODE_BACK
        assertFalse(VolumeEventFormatter.isVolumeKey(26)); // KEYCODE_POWER
    }

    @Test
    public void testGetKeyCodeName() {
        assertEquals("KEYCODE_VOLUME_UP (24)", VolumeEventFormatter.getKeyCodeName(24));
        assertEquals("KEYCODE_VOLUME_DOWN (25)", VolumeEventFormatter.getKeyCodeName(25));
        assertEquals("KEYCODE_VOLUME_MUTE (164)", VolumeEventFormatter.getKeyCodeName(164));
        assertEquals("KEY_999", VolumeEventFormatter.getKeyCodeName(999));
    }

    @Test
    public void testGetActionName() {
        assertEquals("ACTION_DOWN", VolumeEventFormatter.getActionName(0));
        assertEquals("ACTION_UP", VolumeEventFormatter.getActionName(1));
        assertEquals("ACTION_MULTIPLE", VolumeEventFormatter.getActionName(2));
        assertEquals("ACTION_99", VolumeEventFormatter.getActionName(99));
    }

    @Test
    public void testFormatVolumeEvent() {
        String formatted = VolumeEventFormatter.formatVolumeEvent(
                24, 0, 0, 5, 0, 15, true, false
        );

        assertNotNull(formatted);
        assertTrue(formatted.contains("KEYCODE_VOLUME_UP (24)"));
        assertTrue(formatted.contains("ACTION_DOWN"));
        assertTrue(formatted.contains("Repeat: 0"));
        assertTrue(formatted.contains("StreamMusic: 5/15 (min: 0)"));
        assertTrue(formatted.contains("MusicActive: true"));
        assertTrue(formatted.contains("Consumed: false"));
    }

    @Test
    public void testFormatVolumeEventConsumed() {
        String formatted = VolumeEventFormatter.formatVolumeEvent(
                25, 1, 2, 1, 0, 15, false, true
        );

        assertNotNull(formatted);
        assertTrue(formatted.contains("KEYCODE_VOLUME_DOWN (25)"));
        assertTrue(formatted.contains("ACTION_UP"));
        assertTrue(formatted.contains("Repeat: 2"));
        assertTrue(formatted.contains("StreamMusic: 1/15 (min: 0)"));
        assertTrue(formatted.contains("MusicActive: false"));
        assertTrue(formatted.contains("Consumed: true"));
    }
}
