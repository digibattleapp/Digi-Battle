package com.digibattle.app.encoder;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class DigimonMiniEncoderTest {
    @Test
    public void testEncodehexString() {
        DigimonMiniEncoder encoder = new DigimonMiniEncoder();
        boolean[] expected = new boolean[DigimonMiniEncoder.MESSAGE_1_SIGNALS.length * 8];
        System.arraycopy(DigimonMiniEncoder.MESSAGE_1_SIGNALS, 0, expected, 0, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_0_SIGNALS, 0, expected, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_1_SIGNALS, 0, expected, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length*2, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_0_SIGNALS, 0, expected, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length*3, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_0_SIGNALS, 0, expected, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length*4, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_0_SIGNALS, 0, expected, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length*5, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_1_SIGNALS, 0, expected, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length*6, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_1_SIGNALS, 0, expected, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length*7, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        assertArrayEquals(expected, encoder.encode("A3"));

        assertArrayEquals(expected, encoder.encode("a3"));
    }

    @Test
    public void testHandshake() {
        DigimonMiniEncoder encoder = new DigimonMiniEncoder();
        boolean[] expectedResult = new boolean[234];
        Arrays.fill(expectedResult, false);
        assertArrayEquals(expectedResult, encoder.handshake());
    }

    @Test
    public void testRate() {
        DigimonMiniEncoder encoder = new DigimonMiniEncoder();
        assertEquals(3750, encoder.rate());
    }

    @Test
    public void testEncodeWithHandshake() {
        DigimonMiniEncoder encoder = new DigimonMiniEncoder();
        boolean[] expected = new boolean[DigimonMiniEncoder.MESSAGE_1_SIGNALS.length * 8 + 234];
        Arrays.fill(expected, false);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_1_SIGNALS, 0, expected, 234, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_0_SIGNALS, 0, expected, 234+DigimonMiniEncoder.MESSAGE_1_SIGNALS.length, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_1_SIGNALS, 0, expected, 234+DigimonMiniEncoder.MESSAGE_1_SIGNALS.length*2, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_0_SIGNALS, 0, expected, 234+DigimonMiniEncoder.MESSAGE_1_SIGNALS.length*3, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_0_SIGNALS, 0, expected, 234+DigimonMiniEncoder.MESSAGE_1_SIGNALS.length*4, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_0_SIGNALS, 0, expected, 234+DigimonMiniEncoder.MESSAGE_1_SIGNALS.length*5, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_1_SIGNALS, 0, expected, 234+DigimonMiniEncoder.MESSAGE_1_SIGNALS.length*6, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        System.arraycopy(DigimonMiniEncoder.MESSAGE_1_SIGNALS, 0, expected, 234+DigimonMiniEncoder.MESSAGE_1_SIGNALS.length*7, DigimonMiniEncoder.MESSAGE_1_SIGNALS.length);
        assertArrayEquals(expected, encoder.encodeWithHandshakeAndStartSignal("A3"));

    }
}
