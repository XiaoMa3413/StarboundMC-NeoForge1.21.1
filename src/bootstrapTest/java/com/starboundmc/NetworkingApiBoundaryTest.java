package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class NetworkingApiBoundaryTest {

    @Test
    void removesLegacyForgeChannelCallsFromAllSources() throws IOException {
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            String sources = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(NetworkingApiBoundaryTest::readUnchecked)
                    .reduce("", String::concat);
            assertFalse(sources.contains("net.minecraftforge.network"));
            assertFalse(sources.contains("SimpleChannel"));
            assertFalse(sources.contains("ModNetwork.CHANNEL"));
        }
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
