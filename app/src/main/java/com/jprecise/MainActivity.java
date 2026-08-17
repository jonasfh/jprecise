package com.jprecise;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity implements SubStepVolumeController.OnVolumeChangedListener {

    private TextView tvFineVolumeLevel;
    private TextView tvFineVolumeDetails;
    private TextView tvActiveStepSize;
    private SeekBar seekbarFineVolume;
    private RadioGroup rgCurveMode;
    private LinearLayout layoutFixedSteps;
    private RadioGroup rgStepSize;
    private Button btnToggleTone;
    private TextView tvServiceStatus;
    private TextView tvAudioInfo;
    private SwitchMaterial switchConsumeKeys;

    private SubStepVolumeController volumeController;
    private AudioBenchmarkFixture audioFixture;
    private AudioManager audioManager;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        volumeController = SubStepVolumeController.getInstance(this);
        audioFixture = new AudioBenchmarkFixture();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        preferences = getSharedPreferences(VolumeAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE);

        initViews();
        setupListeners();
    }

    private void initViews() {
        tvFineVolumeLevel = findViewById(R.id.tv_fine_volume_level);
        tvFineVolumeDetails = findViewById(R.id.tv_fine_volume_details);
        tvActiveStepSize = findViewById(R.id.tv_active_step_size);
        seekbarFineVolume = findViewById(R.id.seekbar_fine_volume);
        rgCurveMode = findViewById(R.id.rg_curve_mode);
        layoutFixedSteps = findViewById(R.id.layout_fixed_steps);
        rgStepSize = findViewById(R.id.rg_step_size);
        btnToggleTone = findViewById(R.id.btn_toggle_tone);
        tvServiceStatus = findViewById(R.id.tv_service_status);
        tvAudioInfo = findViewById(R.id.tv_audio_info);
        switchConsumeKeys = findViewById(R.id.switch_consume_keys);

        float currentLevel = volumeController.getCurrentLevel();
        updateVolumeDisplay(currentLevel,
                SubStepVolumeController.calculateBaseStreamIndex(currentLevel),
                SubStepVolumeController.calculateAttenuationDb(currentLevel),
                SubStepVolumeController.calculateFloatGain(currentLevel));

        seekbarFineVolume.setProgress((int) Math.round(currentLevel * 10));

        boolean currentConsumeSetting = preferences.getBoolean(
                VolumeAccessibilityService.PREF_CONSUME_VOLUME_KEYS, false);
        switchConsumeKeys.setChecked(currentConsumeSetting);

        // Set curve mode radio buttons
        if (volumeController.getCurveMode() == SubStepVolumeController.CurveMode.VARIABLE) {
            rgCurveMode.check(R.id.rb_mode_variable);
            layoutFixedSteps.setVisibility(View.GONE);
        } else {
            rgCurveMode.check(R.id.rb_mode_fixed);
            layoutFixedSteps.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        findViewById(R.id.btn_step_down).setOnClickListener(v -> volumeController.stepDown());
        findViewById(R.id.btn_step_up).setOnClickListener(v -> volumeController.stepUp());

        findViewById(R.id.btn_open_accessibility).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        switchConsumeKeys.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit()
                    .putBoolean(VolumeAccessibilityService.PREF_CONSUME_VOLUME_KEYS, isChecked)
                    .apply();
        });

        seekbarFineVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float level = progress / 10.0f;
                    volumeController.setVolumeLevel(level);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        rgCurveMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_mode_variable) {
                volumeController.setCurveMode(SubStepVolumeController.CurveMode.VARIABLE);
                layoutFixedSteps.setVisibility(View.GONE);
            } else {
                volumeController.setCurveMode(SubStepVolumeController.CurveMode.FIXED);
                layoutFixedSteps.setVisibility(View.VISIBLE);
            }
            updateStepSizeDisplay(volumeController.getCurrentLevel());
        });

        rgStepSize.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_step_005) {
                volumeController.setStepSize(0.05f);
            } else if (checkedId == R.id.rb_step_010) {
                volumeController.setStepSize(0.10f);
            } else if (checkedId == R.id.rb_step_025) {
                volumeController.setStepSize(0.25f);
            } else if (checkedId == R.id.rb_step_100) {
                volumeController.setStepSize(1.00f);
            }
            updateStepSizeDisplay(volumeController.getCurrentLevel());
        });

        btnToggleTone.setOnClickListener(v -> toggleTestTone());
    }

    private void toggleTestTone() {
        if (audioFixture.isPlaying()) {
            audioFixture.stopTone();
            btnToggleTone.setText("Start Test Tone (440 Hz)");
        } else {
            float gain = SubStepVolumeController.calculateFloatGain(volumeController.getCurrentLevel());
            audioFixture.startTone(gain);
            btnToggleTone.setText("Stop Test Tone");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        volumeController.addListener(this);
        updateServiceStatus();
        updateAudioInfo();
    }

    @Override
    protected void onPause() {
        volumeController.removeListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (audioFixture != null) {
            audioFixture.stopTone();
        }
        super.onDestroy();
    }

    @Override
    public void onVolumeChanged(float fineLevel, int baseStreamIndex, float attenuationDb, float floatGain) {
        runOnUiThread(() -> {
            updateVolumeDisplay(fineLevel, baseStreamIndex, attenuationDb, floatGain);
            seekbarFineVolume.setProgress((int) Math.round(fineLevel * 10));
            if (audioFixture.isPlaying()) {
                audioFixture.setGain(floatGain);
            }
            updateAudioInfo();
        });
    }

    private void updateVolumeDisplay(float fineLevel, int baseStreamIndex, float attenuationDb, float floatGain) {
        tvFineVolumeLevel.setText(String.format("Level: %.2f / 15.0", fineLevel));
        tvFineVolumeDetails.setText(String.format(
                "Attenuation: %.1f dB | PCM Gain: %.3f | Base Stream: %d",
                attenuationDb, floatGain, baseStreamIndex
        ));
        updateStepSizeDisplay(fineLevel);
    }

    private void updateStepSizeDisplay(float level) {
        if (volumeController.getCurveMode() == SubStepVolumeController.CurveMode.VARIABLE) {
            float step = VariableStepVolumeCurve.getStepSizeAt(level);
            String rangeLabel;
            if (level < 0.30f) {
                rangeLabel = "Low sleep range (<0.30)";
            } else if (level < 0.90f) {
                rangeLabel = "Medium quiet range (<0.90)";
            } else {
                rangeLabel = "Standard range (>=0.90)";
            }
            tvActiveStepSize.setText(String.format("Active Step Size: %.2f (%s)", step, rangeLabel));
            tvActiveStepSize.setTextColor(0xFF2E7D32); // Green
        } else {
            tvActiveStepSize.setText(String.format("Active Step Size: %.2f (Fixed Mode)", volumeController.getStepSize()));
            tvActiveStepSize.setTextColor(0xFF1565C0); // Blue
        }
    }

    private void updateServiceStatus() {
        boolean isRunning = VolumeAccessibilityService.isServiceRunning();
        boolean isEnabledInSettings = isAccessibilityServiceEnabled(this, VolumeAccessibilityService.class);

        if (isRunning) {
            tvServiceStatus.setText("Status: ACTIVE & INTERCEPTING");
            tvServiceStatus.setTextColor(0xFF2E7D32); // Green
        } else if (isEnabledInSettings) {
            tvServiceStatus.setText("Status: ENABLED in Settings (connecting...)");
            tvServiceStatus.setTextColor(0xFFF57F17); // Amber
        } else {
            tvServiceStatus.setText("Status: DISABLED (Enable in Accessibility Settings)");
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

        String info = String.format(
                "STREAM_MUSIC Index: %d / %d (Min: %d) | isMusicActive: %b",
                currentVol, maxVol, minVol, musicActive
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
