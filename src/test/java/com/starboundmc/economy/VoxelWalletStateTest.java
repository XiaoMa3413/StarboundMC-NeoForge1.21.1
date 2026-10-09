package com.starboundmc.economy;

import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DynamicOps;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

class VoxelWalletStateTest {
    private static final DynamicOps<JsonElement> OPS = JsonOps.INSTANCE;

    @Test
    void defaultsToZeroBalance() {
        assertEquals(0, VoxelWalletState.DEFAULT.balance());
    }

    @Test
    void constructorRejectsNegativeBalances() {
        assertThrows(IllegalArgumentException.class, () -> new VoxelWalletState(-5));
    }

    @Test
    void addAccumulatesAndIgnoresNonPositiveAmounts() {
        VoxelWalletState wallet = VoxelWalletState.DEFAULT;
        wallet = wallet.add(5);
        wallet = wallet.add(20);
        wallet = wallet.add(0);
        wallet = wallet.add(-3);
        assertEquals(25, wallet.balance());
    }

    @Test
    void addSaturatesAtIntMax() {
        VoxelWalletState wallet = new VoxelWalletState(Integer.MAX_VALUE - 1);
        assertEquals(Integer.MAX_VALUE, wallet.add(100).balance());
    }

    @Test
    void spendRequiresAndDeductsBalance() {
        VoxelWalletState wallet = new VoxelWalletState(25);
        assertTrue(wallet.canAfford(20));
        VoxelWalletState spent = wallet.spend(20);
        assertEquals(5, spent.balance());
        assertFalse(spent.canAfford(6));
        assertSame(spent, spent.spend(6), "insufficient spend must not change state");
        assertEquals(5, spent.spend(-1).balance());
    }

    @Test
    void codecRoundTripsBalance() {
        VoxelWalletState wallet = new VoxelWalletState(1234);
        JsonElement encoded = VoxelWalletState.CODEC.encodeStart(OPS, wallet).getOrThrow();
        VoxelWalletState decoded = VoxelWalletState.CODEC.parse(OPS, encoded).getOrThrow();
        assertEquals(wallet, decoded);
    }

    @Test
    void serializedWalletRequiresANonnegativeBalance() {
        for (String json : new String[]{"{}", "{\"balance\":\"invalid\"}", "{\"balance\":-1}"}) {
            var malformed = com.google.gson.JsonParser.parseString(json);
            assertTrue(VoxelWalletState.CODEC.parse(OPS, malformed).error().isPresent(), json);
        }
    }
}
