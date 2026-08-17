package com.jprecise;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.audiofx.Equalizer;
import android.util.Log;
import java.util.concurrent.CopyOnWriteArrayList;

public class SubStepVolumeController {

    private static final String TAG = "JPrecise";
    public static final String PREF_FINE_VOLUME_LEVEL = "fine_volume_level";
    public static final String PREF_FINE_STEP_SIZE = "fine_step_size";
    public static final String PREF_ENABLE_SUB_STEPS = "enable_sub_steps";
    public static final String PREF_CURVE_MODE = "curve_mode";

    public enum CurveMode {
        VARIABLE, // Dynamic: 0.05 (<0.30), 0.10 (<0.90), 1.00 (>=0.90)
        FIXED     // Fixed step size
    }

    public static final float MIN_VOLUME = VariableStepVolumeCurve.MIN_VOLUME;
    public static final float MAX_VOLUME = VariableStepVolumeCurve.MAX_VOLUME;
    public static final float DEFAULT_STEP_SIZE = 0.1f;
    public static final float LOW_LEVEL_MAX_ATTENUATION_DB = -24.0f;

    private static volatile SubStepVolumeController instance;

    private final Context context;
    private final AudioManager audioManager;
    private final SharedPreferences preferences;
    private final CopyOnWriteArrayList<OnVolumeChangedListener> listeners = new CopyOnWriteArrayList<>();

    private float currentLevel;
    private float stepSize;
    private boolean subStepsEnabled;
    private CurveMode curveMode;
    private Equalizer globalEqualizer;

    public interface OnVolumeChangedListener {
        void onVolumeChanged(float fineLevel, int baseStreamIndex, float attenuationDb, float floatGain);
    }

    private SubStepVolumeController(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        this.preferences = this.context.getSharedPreferences(VolumeAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);

        this.currentLevel = preferences.getFloat(PREF_FINE_VOLUME_LEVEL, 1.0f);
        this.stepSize = preferences.getFloat(PREF_FINE_STEP_SIZE, DEFAULT_STEP_SIZE);
        this.subStepsEnabled = preferences.getBoolean(PREF_ENABLE_SUB_STEPS, true);
        String savedMode = preferences.getString(PREF_CURVE_MODE, CurveMode.VARIABLE.name());
        try {
            this.curveMode = CurveMode.valueOf(savedMode);
        } catch (Exception e) {
            this.curveMode = CurveMode.VARIABLE;
        }

        initEqualizer();
    }

    public static SubStepVolumeController getInstance(Context context) {
        if (instance == null) {
            synchronized (SubStepVolumeController.class) {
                if (instance == null) {
                    instance = new SubStepVolumeController(context);
                }
            }
        }
        return instance;
    }

    private void initEqualizer() {
        try {
            // Audio Session 0 applies to global output mix on supported Android HALs
            globalEqualizer = new Equalizer(0, 0);
            globalEqualizer.setEnabled(true);
            Log.i(TAG, "SubStepVolumeController: Global Equalizer attached to session 0");
        } catch (Exception e) {
            Log.w(TAG, "SubStepVolumeController: Global session 0 Equalizer not supported on this device/HAL: " + e.getMessage());
            globalEqualizer = null;
        }
    }

    public synchronized void setVolumeLevel(float newLevel) {
        this.currentLevel = Math.max(MIN_VOLUME, Math.min(MAX_VOLUME, roundToDecimals(newLevel, 2)));

        preferences.edit().putFloat(PREF_FINE_VOLUME_LEVEL, currentLevel).apply();

        int baseStreamIndex = calculateBaseStreamIndex(currentLevel);
        float attenuationDb = calculateAttenuationDb(currentLevel);
        float floatGain = calculateFloatGain(currentLevel);

        // 1. Update AudioManager base volume if necessary
        if (audioManager != null) {
            int activeStreamVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (activeStreamVol != baseStreamIndex) {
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, baseStreamIndex, 0);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to set stream volume: " + e.getMessage());
                }
            }
        }

        // 2. Apply DSP / Equalizer attenuation if available
        applyEqualizerAttenuation(attenuationDb);

        Log.i(TAG, String.format(
                "SubStepVolume -> Level: %.2f / %.1f | Base STREAM_MUSIC: %d | Attenuation: %.1f dB | Gain: %.3f",
                currentLevel, MAX_VOLUME, baseStreamIndex, attenuationDb, floatGain
        ));

        // 3. Notify listeners (UI, in-app audio tone generator, etc.)
        for (OnVolumeChangedListener listener : listeners) {
            listener.onVolumeChanged(currentLevel, baseStreamIndex, attenuationDb, floatGain);
        }
    }

    public synchronized void stepUp() {
        if (curveMode == CurveMode.VARIABLE) {
            setVolumeLevel(VariableStepVolumeCurve.calculateStepUp(currentLevel));
        } else {
            setVolumeLevel(currentLevel + stepSize);
        }
    }

    public synchronized void stepDown() {
        if (curveMode == CurveMode.VARIABLE) {
            setVolumeLevel(VariableStepVolumeCurve.calculateStepDown(currentLevel));
        } else {
            setVolumeLevel(currentLevel - stepSize);
        }
    }

    public float getCurrentLevel() {
        return currentLevel;
    }

    public float getStepSize() {
        return stepSize;
    }

    public float getCurrentStepSize() {
        if (curveMode == CurveMode.VARIABLE) {
            return VariableStepVolumeCurve.getStepSizeAt(currentLevel);
        }
        return stepSize;
    }

    public void setStepSize(float stepSize) {
        this.stepSize = Math.max(0.01f, Math.min(1.0f, stepSize));
        preferences.edit().putFloat(PREF_FINE_STEP_SIZE, this.stepSize).apply();
    }

    public CurveMode getCurveMode() {
        return curveMode;
    }

    public void setCurveMode(CurveMode curveMode) {
        this.curveMode = curveMode;
        preferences.edit().putString(PREF_CURVE_MODE, curveMode.name()).apply();
    }

    public boolean isSubStepsEnabled() {
        return subStepsEnabled;
    }

    public void setSubStepsEnabled(boolean enabled) {
        this.subStepsEnabled = enabled;
        preferences.edit().putBoolean(PREF_ENABLE_SUB_STEPS, enabled).apply();
        if (!enabled) {
            // Reset DSP attenuation
            applyEqualizerAttenuation(0.0f);
        }
    }

    public void addListener(OnVolumeChangedListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeListener(OnVolumeChangedListener listener) {
        listeners.remove(listener);
    }

    private void applyEqualizerAttenuation(float attenuationDb) {
        if (globalEqualizer == null || !subStepsEnabled) {
            return;
        }

        try {
            short minLevel = globalEqualizer.getBandLevelRange()[0]; // e.g. -1500 mB (-15 dB)
            short maxLevel = globalEqualizer.getBandLevelRange()[1]; // e.g. +1500 mB (+15 dB)
            short targetMb = (short) (attenuationDb * 100.0f); // 1 dB = 100 mB

            short clampedMb = (short) Math.max(minLevel, Math.min(maxLevel, targetMb));

            short numBands = globalEqualizer.getNumberOfBands();
            for (short band = 0; band < numBands; band++) {
                globalEqualizer.setBandLevel(band, clampedMb);
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to apply equalizer attenuation: " + e.getMessage());
        }
    }

    // --- Pure Calculation Methods (Tested in Unit Tests) ---

    public static int calculateBaseStreamIndex(float fineLevel) {
        if (fineLevel <= 0.001f) {
            return 0; // Mute
        }
        if (fineLevel <= 1.0f) {
            return 1; // Base index 1 is held while sub-step attenuation is applied
        }
        return Math.min(15, (int) Math.floor(fineLevel));
    }

    public static float calculateAttenuationDb(float fineLevel) {
        if (fineLevel <= 0.001f) {
            return -96.0f; // Mute
        }
        if (fineLevel <= 1.0f) {
            // Linear dB slope: 1.0 -> 0.0 dB, 0.5 -> -12.0 dB, 0.1 -> -21.6 dB, 0.0 -> -24.0 dB
            return (fineLevel - 1.0f) * Math.abs(LOW_LEVEL_MAX_ATTENUATION_DB);
        }
        return 0.0f;
    }

    public static float calculateFloatGain(float fineLevel) {
        if (fineLevel <= 0.001f) {
            return 0.0f;
        }
        if (fineLevel <= 1.0f) {
            // Linear float gain: 0.1 -> 0.1, 0.5 -> 0.5, 1.0 -> 1.0
            return fineLevel;
        }
        return 1.0f;
    }

    private static float roundToDecimals(float value, int decimals) {
        float factor = (float) Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
