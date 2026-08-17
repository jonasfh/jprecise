package com.jprecise;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class VolumeAccessibilityService extends AccessibilityService {

    public static final String TAG = "JPrecise";
    public static final String PREFS_NAME = "jprecise_prefs";
    public static final String PREF_CONSUME_VOLUME_KEYS = "consume_volume_keys";

    private static volatile boolean isRunning = false;
    private AudioManager audioManager;
    private SharedPreferences preferences;

    public static boolean isServiceRunning() {
        return isRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Log.i(TAG, "VolumeAccessibilityService created");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        isRunning = true;

        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            info = new AccessibilityServiceInfo();
        }
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        Log.i(TAG, "VolumeAccessibilityService connected with FLAG_REQUEST_FILTER_KEY_EVENTS");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        if (VolumeEventFormatter.isVolumeKey(keyCode)) {
            boolean shouldConsume = preferences.getBoolean(PREF_CONSUME_VOLUME_KEYS, false);

            int currentVolume = -1;
            int maxVolume = -1;
            int minVolume = 0;
            boolean isMusicActive = false;

            if (audioManager != null) {
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    minVolume = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
                }
                isMusicActive = audioManager.isMusicActive();
            }

            String logMessage = VolumeEventFormatter.formatVolumeEvent(
                    keyCode,
                    event.getAction(),
                    event.getRepeatCount(),
                    currentVolume,
                    minVolume,
                    maxVolume,
                    isMusicActive,
                    shouldConsume
            );

            Log.i(TAG, logMessage);

            if (shouldConsume) {
                Log.d(TAG, "Consuming volume key event: " + VolumeEventFormatter.getKeyCodeName(keyCode));

                // Process volume adjustment on ACTION_DOWN
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    SubStepVolumeController controller = SubStepVolumeController.getInstance(this);
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        controller.stepUp();
                    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        controller.stepDown();
                    }
                }
                return true;
            }
        }

        return super.onKeyEvent(event);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No-op for this POC; we focus on key event filtering
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "VolumeAccessibilityService interrupted");
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        isRunning = false;
        Log.i(TAG, "VolumeAccessibilityService unbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        Log.i(TAG, "VolumeAccessibilityService destroyed");
        super.onDestroy();
    }
}
