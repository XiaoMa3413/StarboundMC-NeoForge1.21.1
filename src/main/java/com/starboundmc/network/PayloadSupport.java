package com.starboundmc.network;

import com.starboundmc.StarboundMC;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

final class PayloadSupport {
    static final int MAX_ID_LENGTH = 128;
    static final int MAX_NAME_LENGTH = 64;
    static final int MAX_LIST_ENTRIES = 256;

    private PayloadSupport() {
    }

    static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, path));
    }

    static String requireString(String value, int maxLength, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " exceeds " + maxLength + " characters");
        }
        return value;
    }

    static int readCount(FriendlyByteBuf buffer, int maximum, String fieldName) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException(fieldName + " count " + count + " exceeds " + maximum);
        }
        return count;
    }
}
