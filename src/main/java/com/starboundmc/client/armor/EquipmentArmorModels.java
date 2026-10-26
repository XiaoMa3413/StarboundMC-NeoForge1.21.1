package com.starboundmc.client.armor;

import com.google.gson.*;
import com.starboundmc.armor.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.*;

/** Native biped armor geometry, driven by the same cuboid source as the editable exports. */
@EventBusSubscriber(modid = "starboundmc", value = Dist.CLIENT)
public final class EquipmentArmorModels {
    private static final ResourceLocation GEOMETRY = ResourceLocation.fromNamespaceAndPath("starboundmc", "equipment/armor_geometry.json");
    private static final Map<ModelLayerLocation, HumanoidModel<LivingEntity>> MODELS = new HashMap<>();
    public static int renderedSelections;

    private static ModelLayerLocation layer(ArmorProfile profile, boolean inner) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("starboundmc", "armor/" + profile.id()), inner ? "inner" : "outer");
    }
    @SubscribeEvent public static void definitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (var profile : ArmorProfile.values()) for (boolean inner : new boolean[]{false, true})
            event.registerLayerDefinition(layer(profile, inner), () -> geometry(profile, inner));
    }
    @SubscribeEvent public static void reset(EntityRenderersEvent.AddLayers event) { MODELS.clear(); }

    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        for (var profile : ArmorProfile.values()) {
            var extension = new IClientItemExtensions() {
                @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                        EquipmentSlot slot, HumanoidModel<?> original) {
                    renderedSelections++;
                    var location = layer(profile, slot == EquipmentSlot.LEGS);
                    return MODELS.computeIfAbsent(location, key -> new HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(key)));
                }
            };
            for (var type : ModArmor.TYPES) event.registerItem(extension, ModArmor.item(profile, type));
        }
    }
    private static LayerDefinition geometry(ArmorProfile profile, boolean inner) {
        JsonObject parts;
        try (var reader = Minecraft.getInstance().getResourceManager().openAsReader(GEOMETRY)) {
            parts = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject(profile.id()).getAsJsonObject(inner ? "inner" : "outer");
        } catch (IOException e) { throw new IllegalStateException("Cannot load approved armor geometry", e); }
        var mesh = new MeshDefinition();
        for (String part : List.of("head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg")) {
            var cubes = CubeListBuilder.create();
            var rotated = new ArrayList<JsonObject>();
            for (var node : parts.getAsJsonArray(part)) {
                var cube = node.getAsJsonObject();
                if (cube.has("rotation")) { rotated.add(cube); continue; }
                var from = cube.getAsJsonArray("from"); var size = cube.getAsJsonArray("size");
                int material = cube.get("material").getAsInt();
                cubes.texOffs(material % 4 * 64, material / 4 * 64).addBox(
                        from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat(),
                        size.get(0).getAsFloat(), size.get(1).getAsFloat(), size.get(2).getAsFloat());
            }
            PartPose pose = switch (part) {
                case "right_arm" -> PartPose.offset(-5, 2, 0);
                case "left_arm" -> PartPose.offset(5, 2, 0);
                case "right_leg" -> PartPose.offset(-1.9f, 12, 0);
                case "left_leg" -> PartPose.offset(1.9f, 12, 0);
                default -> PartPose.ZERO;
            };
            var bone = mesh.getRoot().addOrReplaceChild(part, cubes, pose);
            int rotatedIndex = 0;
            for (var cube : rotated) {
                var from = cube.getAsJsonArray("from"); var size = cube.getAsJsonArray("size");
                var pivot = cube.getAsJsonObject("rotation").getAsJsonArray("origin");
                int material = cube.get("material").getAsInt();
                var rotatedCube = CubeListBuilder.create().texOffs(material % 4 * 64, material / 4 * 64).addBox(
                        from.get(0).getAsFloat() - pivot.get(0).getAsFloat(), from.get(1).getAsFloat() - pivot.get(1).getAsFloat(),
                        from.get(2).getAsFloat() - pivot.get(2).getAsFloat(), size.get(0).getAsFloat(), size.get(1).getAsFloat(), size.get(2).getAsFloat());
                bone.addOrReplaceChild(cube.get("name").getAsString() + "_" + rotatedIndex++, rotatedCube, PartPose.offsetAndRotation(
                        pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat(), 0, 0,
                        (float)Math.toRadians(cube.getAsJsonObject("rotation").get("angle").getAsDouble())));
            }
        }
        return LayerDefinition.create(mesh, 256, 256);
    }
}
