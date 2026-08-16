package com.jprecise;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private TextView tvServiceStatus;
    private TextView tvAudioInfo;
    private SwitchMaterial switchConsumeKeys;
    private AudioManager audioManager;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        preferences = getSharedPreferences(VolumeAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);

        tvServiceStatus = findViewById(R.id.tv_service_status);
        tvAudioInfo = findViewById(R.id.tv_audio_info);
        switchConsumeKeys = findViewById(R.id.switch_consume_keys);
        Button btnOpenAccessibility = findViewById(R.id.btn_open_accessibility);
        Button btnRefreshVolume = findViewById(R.id.btn_refresh_volume);

        btnOpenAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        btnRefreshVolume.setOnClickListener(v -> updateAudioInfo());

        boolean currentConsumeSetting = preferences.getBoolean(
                VolumeAccessibilityService.PREF_CONSUME_VOLUME_KEYS, false);
        switchConsumeKeys.setChecked(currentConsumeSetting);

        switchConsumeKeys.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit()
                    .putBoolean(VolumeAccessibilityService.PREF_CONSUME_VOLUME_KEYS, isChecked)
                    .apply();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
        updateAudioInfo();
    }

    private void updateServiceStatus() {
        boolean isRunning = VolumeAccessibilityService.isServiceRunning();
        boolean isEnabledInSettings = isAccessibilityServiceEnabled(this, VolumeAccessibilityService.class);

        if (isRunning) {
            tvServiceStatus.setText("Status: ACTIVE & CONNECTED\n(Receiving key events)");
            tvServiceStatus.setTextColor(0xFF2E7D32); // Green
        } else if (isEnabledInSettings) {
            tvServiceStatus.setText("Status: ENABLED in Settings (waiting for connection)");
            tvServiceStatus.setTextColor(0xFFF57F17); // Amber
        } else {
            tvServiceStatus.setText("Status: DISABLED\n(Please enable in Accessibility Settings)");
            tvServiceStatus.setTextColor(0xFFC62828); // Red
        }
    }

    private void updateAudioInfo() {
        if (audioManager == null) {
            tvAudioInfo.setText("AudioManager not available");
            return;
        }

        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int minVol = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            minVol = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        }
        boolean musicActive = audioManager.isMusicActive();
        int mode = audioManager.getMode();

        String info = String.format(
                "STREAM_MUSIC Volume: %d\nMin: %d | Max: %d\nisMusicActive: %b\nAudio Mode: %d",
                currentVol, minVol, maxVol, musicActive, mode
        );
        tvAudioInfo.setText(info);
    }

    private static boolean isAccessibilityServiceEnabled(Context context, Class<?> serviceClass) {
        String expectedServiceName = context.getPackageName() + "/" + serviceClass.getName();
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (TextUtils.isEmpty(enabledServices)) {
            return false;
        }

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServices);

        while (colonSplitter.hasNext()) {
            String componentName = colonSplitter.next();
            if (componentName.equalsIgnoreCase(expectedServiceName)) {
                return true;
            }
        }
        return false;
    }
}
