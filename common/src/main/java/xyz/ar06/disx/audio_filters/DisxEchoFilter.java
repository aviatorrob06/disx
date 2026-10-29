package xyz.ar06.disx.audio_filters;

import xyz.ar06.disx.DisxAudioFormatConstants;
import xyz.ar06.disx.audio_filters.DisxAudioFilter;
import xyz.ar06.disx.audio_filters.DisxAudioFilterType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class DisxEchoFilter implements DisxAudioFilter {

    @Override
    public byte[] process(byte[] audio) {
        if (audio == null || audio.length == 0) {
            return audio;
        }

        final int sampleRate = DisxAudioFormatConstants.sampleRate;
        final int channels = DisxAudioFormatConstants.channelCount;

        // Echo parameters
        final int delayMs = 200;
        final float decay = 0.35f;

        if (sampleRate <= 0 || channels <= 0) {
            throw new IllegalStateException(
                    "Invalid audio format: sample rate and channel count must be positive"
            );
        }

        // 16-bit PCM requires an even number of bytes.
        if (audio.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "16-bit PCM audio must contain an even number of bytes"
            );
        }

        int delayFrames = sampleRate * delayMs / 1000;

        // Convert little-endian PCM bytes -> short[].
        ShortBuffer shortBuffer = ByteBuffer.wrap(audio)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer();

        short[] input = new short[shortBuffer.remaining()];
        shortBuffer.get(input);

        // Ensure the samples contain complete channel frames.
        if (input.length % channels != 0) {
            throw new IllegalArgumentException(
                    "Audio sample count is not divisible by channel count"
            );
        }

        int inputFrames = input.length / channels;

        // Extend output so the echo can ring out.
        short[] output = new short[(inputFrames + delayFrames) * channels];

        // Copy original audio.
        System.arraycopy(input, 0, output, 0, input.length);

        // Add delayed, attenuated audio.
        for (int frame = 0; frame < inputFrames; frame++) {
            int echoFrame = frame + delayFrames;

            for (int ch = 0; ch < channels; ch++) {
                int src = frame * channels + ch;
                int dst = echoFrame * channels + ch;

                int mixed = output[dst] + (int) (input[src] * decay);

                // Clamp to signed 16-bit PCM range.
                mixed = Math.max(
                        Short.MIN_VALUE,
                        Math.min(Short.MAX_VALUE, mixed)
                );

                output[dst] = (short) mixed;
            }
        }

        // Convert short[] -> little-endian PCM bytes.
        ByteBuffer out = ByteBuffer.allocate(output.length * 2)
                .order(ByteOrder.LITTLE_ENDIAN);

        for (short sample : output) {
            out.putShort(sample);
        }

        return out.array();
    }

    @Override
    public DisxAudioFilterType getFilterType() {
        return DisxAudioFilterType.ECHO;
    }
}
