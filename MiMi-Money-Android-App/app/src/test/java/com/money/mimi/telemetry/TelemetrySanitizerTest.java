package com.money.mimi.telemetry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TelemetrySanitizerTest {
    @Test
    public void identifierNormalizesToFirebaseSafeLowCardinalityValue() {
        assertEquals("wallet_send_rpc", TelemetrySanitizer.identifier(" Wallet Send/RPC ", "unknown"));
    }

    @Test
    public void identifierUsesSafeFallbackForMissingValue() {
        assertEquals("unknown_operation", TelemetrySanitizer.identifier("  ", "Unknown Operation"));
        assertEquals("unknown", TelemetrySanitizer.identifier(null, null));
    }

    @Test
    public void identifierBoundsLengthAndRemovesUnsafeCharacters() {
        String value = TelemetrySanitizer.identifier(
                "Transaction 0x1234567890abcdef1234567890abcdef12345678 / private", "unknown");

        assertTrue(value.length() <= 40);
        assertTrue(value.matches("[a-z0-9_]+"));
        assertFalse(value.endsWith("_"));
    }
}
