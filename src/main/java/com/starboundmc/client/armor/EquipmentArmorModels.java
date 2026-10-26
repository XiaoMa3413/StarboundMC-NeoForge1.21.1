package com.starboundmc.client.armor;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.starboundmc.armor.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.*;
import org.joml.Vector3f;

/** Accepted equipment meshes, bound to vanilla armor bones and equipment slots. */
@EventBusSubscriber(modid = "starboundmc", value = Dist.CLIENT)
public final class EquipmentArmorModels {
    private static final ResourceLocation GEOMETRY = ResourceLocation.fromNamespaceAndPath("starboundmc", "equipment/armor_meshes.json");
    private record Key(ArmorProfile profile, EquipmentSlot slot) {}
    private static final Map<Key, HumanoidModel<LivingEntity>> MODELS = new HashMap<>();
    private static JsonObject geometry;
    public static int renderedSelections;

    @SubscribeEvent public static void reset(EntityRenderersEvent.AddLayers event) {
        MODELS.clear(); geometry = null;
    }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        for (var profile : ArmorProfile.values()) {
            var extension = new IClientItemExtensions() {
                @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                        EquipmentSlot slot, HumanoidModel<?> original) {
                    renderedSelections++;
                    return MODELS.computeIfAbsent(new Key(profile, slot), EquipmentArmorModels::bake);
                }
            };
            for (var type : ModArmor.TYPES) event.registerItem(extension, ModArmor.item(profile, type));
        }
    }
    private static HumanoidModel<LivingEntity> bake(Key key) {
        if (geometry == null) {
            try (var reader = Minecraft.getInstance().getResourceManager().openAsReader(GEOMETRY)) {
                geometry = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (IOException e) { throw new IllegalStateException("Cannot load approved armor geometry", e); }
        }
        var parts = geometry.getAsJsonObject(key.profile().id()).getAsJsonObject(key.slot().getName());
        Map<String, ModelPart> children = new HashMap<>();
        for (String bone : List.of("head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg")) {
            var faces = parts.getAsJsonArray(bone);
            List<ModelPart.Cube> cubes = faces.isEmpty() ? List.of() : List.of(new MeshCube(faces));
            var part = new ModelPart(cubes, Map.of());
            PartPose pose = switch (bone) {
                case "right_arm" -> PartPose.offset(-5, 2, 0);
                case "left_arm" -> PartPose.offset(5, 2, 0);
                case "right_leg" -> PartPose.offset(-1.9f, 12, 0);
                case "left_leg" -> PartPose.offset(1.9f, 12, 0);
                default -> PartPose.ZERO;
            };
            part.setInitialPose(pose); part.loadPose(pose); children.put(bone, part);
        }
        return new HumanoidModel<>(new ModelPart(List.of(), children));
    }
    /** Retains vanilla pose/visibility/glint handling while emitting arbitrary closed mesh faces. */
    private static final class MeshCube extends ModelPart.Cube {
        private record Face(float[][] vertices, Vector3f normal) {}
        private final List<Face> faces = new ArrayList<>();
        MeshCube(JsonArray source) {
            super(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, 1, 1, Set.of());
            for (var node : source) {
                var triangle = node.getAsJsonArray();
                if (triangle.size() != 3) throw new IllegalArgumentException("Expected armor triangle");
                float[][] vertices = new float[3][5];
                for (int i = 0; i < 3; i++) {
                    var vertex = triangle.get(i).getAsJsonArray();
                    if (vertex.size() != 5) throw new IllegalArgumentException("Expected position and UV");
                    for (int j = 0; j < 5; j++) {
                        float value = vertex.get(j).getAsFloat();
                        if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite armor vertex");
                        vertices[i][j] = j < 3 ? value / 16 : value;
                    }
                }
                var a = position(vertices[1]).sub(position(vertices[0]));
                var b = position(vertices[2]).sub(position(vertices[0]));
                var normal = a.cross(b);
                if (normal.lengthSquared() > 1e-15f) faces.add(new Face(vertices, normal.normalize()));
            }
        }
        private static Vector3f position(float[] v) { return new Vector3f(v[0], v[1], v[2]); }
        @Override public void compile(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
            var normal = new Vector3f(); var position = new Vector3f();
            for (var face : faces) {
                pose.transformNormal(face.normal(), normal);
                // Vanilla armor render types consume quads; repeat the final triangle vertex.
                for (int i = 0; i < 4; i++) {
                    float[] v = face.vertices()[Math.min(i, 2)];
                    pose.pose().transformPosition(v[0], v[1], v[2], position);
                    buffer.addVertex(position.x, position.y, position.z, color, v[3], v[4], overlay, light,
                            normal.x, normal.y, normal.z);
                }
            }
        }
    }
}
