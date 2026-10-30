/* Derived from StellarView, Copyright (c) 2024 Povstalec, MIT.
 * See third_party/stellarview/LICENSE.txt and THIRD_PARTY_NOTICES.md. */
package com.starboundmc.client.space;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Source-exact spherical generation specialized to our six-type LOD2 cinematic field. */
public final class BackgroundStarCatalog {
    public static final long SEED = 0x5B4D434CL;
    public static final double RADIUS = 1_000_000;
    public static final String PROFILE_RESOURCE_PATH = "stellarview/effects/star_info/cinematic_stars.json";
    public static final String PROFILE_PATH = "assets/starboundmc/" + PROFILE_RESOURCE_PATH;
    private BackgroundStarCatalog() {}
    public record Star(double x, double y, double z, float size, double rotation,
                       int red, int green, int blue, int brightness, float maxDistance) {}
    public record StarType(int red, int green, int blue, float minSize, float maxSize,
                           int minBrightness, int maxBrightness, float maxDistance, int weight) {
        public StarType {
            if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255
                    || !Float.isFinite(minSize) || !Float.isFinite(maxSize) || minSize <= 0 || maxSize < minSize
                    || minBrightness < 0 || maxBrightness > 255 || maxBrightness < minBrightness
                    || !Float.isFinite(maxDistance) || maxDistance < 5_000_000 || maxDistance >= 10_000_000 || weight <= 0)
                throw new IllegalArgumentException("Invalid cinematic LOD2 star type");
        }
    }
    public static List<StarType> readProfile(Reader reader) {
        var result = new ArrayList<StarType>();
        for (var element : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("star_types")) {
            var type = element.getAsJsonObject();
            var rgb = type.getAsJsonObject("rgb");
            result.add(new StarType(rgb.get("red").getAsInt(), rgb.get("green").getAsInt(), rgb.get("blue").getAsInt(),
                    type.get("min_size").getAsFloat(), type.get("max_size").getAsFloat(),
                    type.get("min_brightness").getAsInt(), type.get("max_brightness").getAsInt(),
                    type.get("max_visible_distance").getAsFloat(), type.get("weight").getAsInt()));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Empty cinematic star profile");
        return List.copyOf(result);
    }
    /** Bundled profile for tests/fixed view selection; runtime uses the active resource pack. */
    public static Star[] generate(int count) {
        try (var stream = BackgroundStarCatalog.class.getClassLoader().getResourceAsStream(PROFILE_PATH)) {
            if (stream == null) throw new IllegalStateException("Missing cinematic star profile");
            return generate(count, readProfile(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        } catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
    }
    public static Star[] generate(int count, List<StarType> types) {
        if (count <= 0 || count > 32000 || types.isEmpty()) throw new IllegalArgumentException("Invalid star budget/profile");
        int totalWeight = 0;
        for (var type : types) totalWeight = Math.addExact(totalWeight, type.weight());
        // All current types belong to source LOD2, whose stream is seed + 1.
        Random random = new Random(SEED + 1);
        Star[] stars = new Star[count];
        for (int i = 0; i < count; i++) {
            double distance = Math.cbrt(Math.abs(random.nextDouble())) * RADIUS;
            double theta = random.nextDouble() * 2F * Math.PI;
            double phi = Math.acos(2F * random.nextDouble() - 1F);
            int weight = random.nextInt(0, totalWeight), selected = 0;
            // Preserve the source's inclusive boundary and exact RNG call order.
            for (; selected < types.size() - 1; selected++) {
                weight -= types.get(selected).weight();
                if (weight <= 0) break;
            }
            var type = types.get(selected);
            int brightness = type.minBrightness() == type.maxBrightness() ? type.maxBrightness()
                    : random.nextInt(type.minBrightness(), type.maxBrightness() + 1);
            float size = type.minSize() == type.maxSize() ? type.maxSize()
                    : random.nextFloat(type.minSize(), type.maxSize());
            double rotation = random.nextDouble() * Math.PI * 2;
            stars[i] = new Star(distance * Math.sin(phi) * Math.sin(theta), distance * Math.cos(phi),
                    distance * Math.sin(phi) * Math.cos(theta), size, rotation,
                    type.red(), type.green(), type.blue(), brightness, type.maxDistance());
        }
        return stars;
    }
}
