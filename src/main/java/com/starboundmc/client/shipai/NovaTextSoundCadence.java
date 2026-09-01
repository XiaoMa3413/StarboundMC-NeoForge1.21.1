package com.starboundmc.client.shipai;

/** Pure cadence state for the short N.O.V.A. dialogue chirp. */
final class NovaTextSoundCadence {
    static final float SILENT = -1F;
    private static final float[] PITCH_PATTERN = {0.96F, 1.02F, 0.99F, 1.04F};

    private int audibleCharactersToSkip;
    private int soundIndex;

    float pitchForCodePoint(int codePoint) {
        if (isSentenceBoundary(codePoint)) {
            audibleCharactersToSkip = 0;
            return SILENT;
        }
        if (!Character.isLetterOrDigit(codePoint))
            return SILENT;
        if (audibleCharactersToSkip > 0) {
            audibleCharactersToSkip--;
            return SILENT;
        }

        float pitch = PITCH_PATTERN[soundIndex % PITCH_PATTERN.length];
        audibleCharactersToSkip = soundIndex % 2 == 0 ? 1 : 2;
        soundIndex++;
        return pitch;
    }

    private static boolean isSentenceBoundary(int codePoint) {
        return codePoint == '.' || codePoint == '!' || codePoint == '?'
                || codePoint == '\u3002' || codePoint == '\uFF01' || codePoint == '\uFF1F';
    }
}
