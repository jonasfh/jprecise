package com.jprecise;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class AudioBenchmarkFixture {

    private static final String TAG = "JPrecise";
    private static final int SAMPLE_RATE = 44100;
    private static final double FREQUENCY = 440.0; // 440 Hz (A4 concert pitch)

    private AudioTrack audioTrack;
    private Thread playbackThread;
    private volatile boolean isPlaying = false;
    private volatile float currentGain = 1.0f;

    public synchronized void startTone(float initialGain) {
        if (isPlaying) {
            return;
        }

        this.currentGain = initialGain;
        this.isPlaying = true;

        playbackThread = new Thread(this::generateAndStreamAudio, "JPrecise-AudioBenchmark");
        playbackThread.start();
        Log.i(TAG, "AudioBenchmarkFixture: Test tone playback started at gain " + initialGain);
    }

    public synchronized void stopTone() {
        isPlaying = false;
        if (audioTrack != null) {
            try {
                audioTrack.pause();
                audioTrack.flush();
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception e) {
                Log.w(TAG, "Error stopping audioTrack: " + e.getMessage());
            } finally {
                audioTrack = null;
            }
        }
        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }
        Log.i(TAG, "AudioBenchmarkFixture: Test tone playback stopped");
    }

    public synchronized void setGain(float gain) {
        this.currentGain = Math.max(0.0f, Math.min(1.0f, gain));
        if (audioTrack != null) {
            try {
                audioTrack.setVolume(this.currentGain);
            } catch (Exception e) {
                Log.w(TAG, "Error adjusting AudioTrack gain: " + e.getMessage());
            }
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    private void generateAndStreamAudio() {
        int bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        if (bufferSize < 4096) {
            bufferSize = 4096;
        }

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        AudioFormat audioFormat = new AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build();

        audioTrack = new AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
        );

        short[] buffer = new short[bufferSize / 2];
        double phase = 0.0;
        double phaseIncrement = (2.0 * Math.PI * FREQUENCY) / SAMPLE_RATE;

        try {
            audioTrack.play();
            audioTrack.setVolume(currentGain);

            while (isPlaying && !Thread.currentThread().isInterrupted()) {
                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = (short) (Math.sin(phase) * 32767.0);
                    phase += phaseIncrement;
                    if (phase >= 2.0 * Math.PI) {
                        phase -= 2.0 * Math.PI;
                    }
                }
                audioTrack.write(buffer, 0, buffer.length);
            }
        } catch (Exception e) {
            Log.e(TAG, "Audio streaming exception: " + e.getMessage());
        } finally {
            if (audioTrack != null) {
                try {
                    audioTrack.stop();
                    audioTrack.release();
                } catch (Exception ignored) {
                }
                audioTrack = null;
            }
        }
    }
}
