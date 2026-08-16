package com.jprecise;

public final class VolumeEventFormatter {

    private VolumeEventFormatter() {
        // Utility class
    }

    public static String getKeyCodeName(int keyCode) {
        switch (keyCode) {
            case 24: // KeyEvent.KEYCODE_VOLUME_UP
                return "KEYCODE_VOLUME_UP (24)";
            case 25: // KeyEvent.KEYCODE_VOLUME_DOWN
                return "KEYCODE_VOLUME_DOWN (25)";
            case 164: // KeyEvent.KEYCODE_VOLUME_MUTE
                return "KEYCODE_VOLUME_MUTE (164)";
            default:
                return "KEY_" + keyCode;
        }
    }

    public static String getActionName(int action) {
        switch (action) {
            case 0: // KeyEvent.ACTION_DOWN
                return "ACTION_DOWN";
            case 1: // KeyEvent.ACTION_UP
                return "ACTION_UP";
            case 2: // KeyEvent.ACTION_MULTIPLE
                return "ACTION_MULTIPLE";
            default:
                return "ACTION_" + action;
        }
    }

    public static boolean isVolumeKey(int keyCode) {
        return keyCode == 24 || keyCode == 25 || keyCode == 164;
    }

    public static String formatVolumeEvent(int keyCode, int action, int repeatCount,
                                           int currentVolume, int minVolume, int maxVolume,
                                           boolean isMusicActive, boolean consumed) {
        return String.format(
                "VolumeKeyEvent -> Key: %s | Action: %s | Repeat: %d | StreamMusic: %d/%d (min: %d) | MusicActive: %b | Consumed: %b",
                getKeyCodeName(keyCode),
                getActionName(action),
                repeatCount,
                currentVolume,
                maxVolume,
                minVolume,
                isMusicActive,
                consumed
        );
    }
}
