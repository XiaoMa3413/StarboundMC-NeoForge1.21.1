package com.starboundmc.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoxelPrintingStationVisualRegressionTest {
    private static final Path MODEL = Path.of(
            "src/main/resources/assets/starboundmc/models/block/voxel_printing_station.json");
    private static final Path RENDERER = Path.of(
            "src/main/java/com/starboundmc/client/VoxelPrintingStationRenderer.java");
    private static final String[][] AXIS_FACES = {
            {"west", "east"},
            {"down", "up"},
            {"north", "south"}
    };

    @Test
    void modelHasNoPositiveVolumeOrSameFacingCoplanarOverlaps() throws IOException {
        JsonArray elements = JsonParser.parseString(Files.readString(MODEL))
                .getAsJsonObject().getAsJsonArray("elements");

        for (int firstIndex = 0; firstIndex < elements.size(); firstIndex++) {
            JsonObject first = elements.get(firstIndex).getAsJsonObject();
            for (int secondIndex = firstIndex + 1; secondIndex < elements.size(); secondIndex++) {
                JsonObject second = elements.get(secondIndex).getAsJsonObject();
                double[] overlaps = new double[3];
                for (int axis = 0; axis < 3; axis++) {
                    overlaps[axis] = overlap(first, second, axis);
                }

                assertFalse(overlaps[0] > 0.0 && overlaps[1] > 0.0 && overlaps[2] > 0.0,
                        "Elements " + firstIndex + " and " + secondIndex + " overlap in volume");

                for (int axis = 0; axis < 3; axis++) {
                    int firstOtherAxis = (axis + 1) % 3;
                    int secondOtherAxis = (axis + 2) % 3;
                    if (overlaps[firstOtherAxis] <= 0.0 || overlaps[secondOtherAxis] <= 0.0) {
                        continue;
                    }
                    for (int side = 0; side < 2; side++) {
                        String coordinate = side == 0 ? "from" : "to";
                        String face = AXIS_FACES[axis][side];
                        double firstPlane = coordinate(first, coordinate, axis);
                        double secondPlane = coordinate(second, coordinate, axis);
                        boolean bothFacesRendered = first.getAsJsonObject("faces").has(face)
                                && second.getAsJsonObject("faces").has(face);
                        assertFalse(bothFacesRendered && Math.abs(firstPlane - secondPlane) < 1.0E-9,
                                "Elements " + firstIndex + " and " + secondIndex
                                        + " render the same-facing " + face + " plane");
                    }
                }
            }
        }
    }

    @Test
    void rendererClipsAtBuildLayerAndSweepsBothInsetProbesAcrossFullWidth() throws IOException {
        String source = Files.readString(RENDERER);

        assertTrue(source.contains("pose.scale(0.32F, 0.32F, 0.32F)"));
        assertTrue(source.contains("new LayerClippedVertexConsumer"));
        assertFalse(source.contains("0.32F * formation"));
        assertTrue(source.contains("renderOverheadProbes(station, pose, buffers, scanY"));
        assertTrue(source.contains("LEFT_PROBE_PIVOT_X = 0.36F"));
        assertTrue(source.contains("RIGHT_PROBE_PIVOT_X = 0.64F"));
        assertTrue(source.contains("PROBE_PIVOT_Z = 0.50F"));
        assertTrue(source.contains("PROBE_NOZZLE_LENGTH = 0.12F"));
        assertTrue(source.contains("PROBE_FULL_SWEEP_RADIUS = 0.175F"));
        assertTrue(source.contains("interpolatedRemainingTicksAt"));
        assertTrue(source.contains("sweepOffset = Mth.sin"));
        assertTrue(source.contains("? 0.5F + sweepOffset"));
        assertTrue(source.contains("? 0.5F - sweepOffset"));
        assertTrue(source.contains("pose.mulPose(probeRotation(aim))"));
        assertTrue(source.contains("leftAim.nozzleX(), leftAim.nozzleY(), leftAim.nozzleZ()"));
        assertTrue(source.contains("rightAim.nozzleX(), rightAim.nozzleY(), rightAim.nozzleZ()"));
        assertFalse(source.contains("renderMovingProbes"));
    }

    @Test
    void rendererFinishesProbeSolidsBeforeRequestingLineBuffer() throws IOException {
        String source = Files.readString(RENDERER);
        int methodStart = source.indexOf("private void renderOverheadProbes");
        int methodEnd = source.indexOf("private static ProbeAim createProbeAim", methodStart);
        String method = source.substring(methodStart, methodEnd);

        int lastSolidDraw = method.indexOf("renderProbeSolids(pose, solids, rightAim)");
        int lineBuffer = method.indexOf("buffers.getBuffer(RenderType.lines())");
        int firstOutlineDraw = method.indexOf("renderProbeOutlines(pose, lines, leftAim)");

        assertTrue(lastSolidDraw >= 0 && lastSolidDraw < lineBuffer,
                "The debug-filled-box buffer must finish before requesting the line buffer");
        assertTrue(lineBuffer < firstOutlineDraw,
                "Probe outlines must be drawn only after the line buffer is requested");
    }

    private static double overlap(JsonObject first, JsonObject second, int axis) {
        double lower = Math.max(coordinate(first, "from", axis), coordinate(second, "from", axis));
        double upper = Math.min(coordinate(first, "to", axis), coordinate(second, "to", axis));
        return upper - lower;
    }

    private static double coordinate(JsonObject element, String endpoint, int axis) {
        return element.getAsJsonArray(endpoint).get(axis).getAsDouble();
    }
}
