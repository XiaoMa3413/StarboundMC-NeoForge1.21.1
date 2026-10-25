package com.starboundmc.armor;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class ArmorGameTests {
    private static final class Fixture implements AutoCloseable {
        final ServerPlayer player;
        final EmbeddedChannel channel;
        Fixture(GameTestHelper h) {
            var level = h.getLevel();
            player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "ArmorTest"), ClientInformation.createDefault());
            var connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            var channels = new HashMap<ResourceLocation, net.neoforged.neoforge.network.registration.NetworkChannel>();
            for (var type : List.of(com.starboundmc.network.EvaStatePacket.TYPE,
                    com.starboundmc.network.EppSnapshotPacket.TYPE, com.starboundmc.network.EppVisualPacket.TYPE,
                    com.starboundmc.network.MobilityStatePacket.TYPE, com.starboundmc.network.NovaBroadcastPacket.TYPE,
                    com.starboundmc.network.HudBootstrapStatePacket.TYPE))
                channels.put(type.id(), new net.neoforged.neoforge.network.registration.NetworkChannel(
                        type.id(), com.starboundmc.network.ModNetwork.PROTOCOL_VERSION));
            // Embedded player ticks run normal EPP/EVA hooks; emulate their login negotiation.
            net.neoforged.neoforge.network.registration.ChannelAttributes.setPayloadSetup(connection,
                    new net.neoforged.neoforge.network.registration.NetworkPayloadSetup(Map.of(
                            net.minecraft.network.ConnectionProtocol.PLAY, channels)));
            player.connection = new ServerGamePacketListenerImpl(level.getServer(), connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            var pos = h.absolutePos(new BlockPos(1, 10, 1));
            player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
            level.getChunk(player.blockPosition()); player.setNoGravity(true);
        }
        void equip(ArmorProfile profile) {
            for (var type : ModArmor.TYPES) player.setItemSlot(type.getSlot(), new ItemStack(ModArmor.item(profile, type)));
            sync();
        }
        void sync() { player.doTick(); ArmorEffects.refresh(player); }
        @Override public void close() { player.discard(); channel.finishAndReleaseAll(); }
    }
    private static void eq(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < .0001, message + ": " + actual + " expected " + expected);
    }
    @GameTest(template = "shuttle_test_empty")
    public static void armorAbsorbsDamageWearsOutAndDoesNotSaveSetModifiers(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            f.equip(ArmorProfile.VANGUARD);
            // New ServerPlayers have 60 ticks of spawn invulnerability, independent of armor.
            for (int i = 0; i < 61; i++) f.player.tick();
            float before = f.player.getHealth();
            // Generic damage explicitly bypasses armor in vanilla; cactus is ordinary armor-reducible damage.
            f.player.hurt(h.getLevel().damageSources().cactus(), 10);
            h.assertTrue(f.player.getHealth() > before - 10 && f.player.getHealth() < before,
                    "Armor did not reduce real damage: " + before + " -> " + f.player.getHealth());
            for (var type : ModArmor.TYPES) h.assertTrue(f.player.getItemBySlot(type.getSlot()).getDamageValue() > 0, "Armor took no wear");
            h.assertTrue(!f.player.getAttributes().save().toString().contains("armor_set/"), "Transient set modifier persisted");
            f.player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR); ArmorEffects.refresh(f.player);
            eq(h, f.player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), .1, "Spectator retained set bonus");
            eq(h, ArmorEffects.projectileMultiplier(f.player), 1, "Spectator retained projectile bonus");
        } h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void allSixteenItemsHaveRecipesRepairTagsAndPersistence(GameTestHelper h) {
        for (var profile : ArmorProfile.values()) for (var type : ModArmor.TYPES) {
            var item = ModArmor.item(profile, type); var stack = new ItemStack(item);
            h.assertTrue(stack.getMaxDamage() == type.getDurability(profile.durability) && stack.getMaxStackSize() == 1,
                    "Invalid durability or stack size");
            h.assertTrue(item.isValidRepairItem(stack, new ItemStack(ModArmor.repairItem(profile))), "Cannot repair armor");
            h.assertTrue(!item.isValidRepairItem(stack, new ItemStack(Items.DIRT)), "Dirt repaired armor");
            var id = BuiltInRegistries.ITEM.getKey(item);
            var recipe = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("starboundmc", "print_" + id.getPath())).orElseThrow();
            h.assertTrue(recipe.value().getResultItem(h.getLevel().registryAccess()).is(item), "Wrong recipe output");
            var printing = (com.starboundmc.recipe.VoxelPrintingRecipe) recipe.value();
            var materials = new ArrayList<ItemStack>();
            for (var entry : printing.materials()) if (!entry.isVoxel()) {
                var ingredient = entry.ingredient().getItems()[0].copy(); ingredient.setCount(entry.count()); materials.add(ingredient);
            }
            h.assertTrue(printing.reserveMaterials(materials).isPresent(), "Cannot reserve recipe materials");
            h.assertTrue(com.starboundmc.recipe.PrintingCategory.SURVIVAL.matches(stack), "Armor not in Survival category");
            h.assertTrue(stack.is(net.minecraft.tags.ItemTags.ARMOR_ENCHANTABLE), "Armor enchantment tag missing");
            stack.setDamageValue(19);
            var restored = ItemStack.parse(h.getLevel().registryAccess(), stack.save(h.getLevel().registryAccess())).orElseThrow();
            h.assertTrue(restored.is(item) && restored.getDamageValue() == 19, "Save/load lost armor durability");
        } h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void pioneerPiecesAndSetRemoveWithoutHealingOrStacking(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            f.equip(ArmorProfile.PIONEER);
            eq(h, f.player.getArmorValue(), 12, "Pioneer defense");
            eq(h, f.player.getMaxHealth(), 24, "Pioneer health");
            eq(h, f.player.getHealth(), 20, "Equipping must not heal");
            eq(h, f.player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), 1.30, "Mining bonus");
            eq(h, f.player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE), 5, "Boot protection");
            for (int i = 0; i < 40; i++) ArmorEffects.refresh(f.player);
            eq(h, f.player.getMaxHealth(), 24, "Repeated refresh stacked bonuses");
            f.player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); f.sync();
            eq(h, f.player.getMaxHealth(), 22, "Three pieces kept full set");
            eq(h, f.player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), 1, "Removed helmet/set left mining");
            f.player.setHealth(22); f.player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); f.sync();
            eq(h, f.player.getMaxHealth(), 20, "Removed chest left health");
            eq(h, f.player.getHealth(), 20, "Health not clamped after removal");
        } h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void vanguardAndStarwardenSwitchWithoutStaleModifiers(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            double baseDamage = f.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            f.equip(ArmorProfile.VANGUARD);
            eq(h, f.player.getArmorValue(), 20, "Vanguard defense");
            eq(h, f.player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 8, "Vanguard toughness");
            eq(h, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), baseDamage + 3, "Vanguard melee");
            eq(h, f.player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), .2, "Vanguard knockback");
            f.equip(ArmorProfile.STARWARDEN);
            eq(h, f.player.getArmorValue(), 22, "Starwarden defense");
            eq(h, f.player.getMaxHealth(), 30, "Starwarden health");
            eq(h, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), baseDamage + 2, "Stale vanguard damage");
            eq(h, f.player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, "Stale knockback");
            eq(h, f.player.getAttributeValue(Attributes.ATTACK_SPEED), 4.4, "Starwarden speed");
            f.player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(com.starboundmc.item.ModItems.STARCORE_BLADE.get()));
            f.sync();
            eq(h, f.player.getAttributeValue(Attributes.ATTACK_SPEED), 1.76, "Set speed must multiply weapon speed by 1.10");
            f.player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            for (var type : ModArmor.TYPES) f.player.setItemSlot(type.getSlot(), ItemStack.EMPTY);
            f.sync();
            eq(h, f.player.getMaxHealth(), 20, "Unequipping left health");
            eq(h, f.player.getAttributeValue(Attributes.ATTACK_SPEED), 4, "Unequipping left speed");
        } h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void mixedEquipmentHasPiecesButNoSetAndPreservesPotions(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            f.player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 400, 1));
            double potionSpeed = f.player.getAttributeValue(Attributes.MOVEMENT_SPEED);
            f.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModArmor.item(ArmorProfile.RANGER, ArmorItem.Type.HELMET)));
            f.player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModArmor.item(ArmorProfile.STARWARDEN, ArmorItem.Type.CHESTPLATE)));
            f.player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModArmor.item(ArmorProfile.VANGUARD, ArmorItem.Type.LEGGINGS)));
            f.player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModArmor.item(ArmorProfile.PIONEER, ArmorItem.Type.BOOTS)));
            f.sync();
            for (var profile : ArmorProfile.values()) h.assertTrue(!ArmorEffects.fullSet(f.player, profile), "Mixed set counted as full");
            eq(h, f.player.getMaxHealth(), 26, "Mixed chest perk");
            eq(h, ArmorEffects.projectileMultiplier(f.player), 1.05, "Mixed ranger helmet");
            eq(h, f.player.getAttributeValue(Attributes.MOVEMENT_SPEED), potionSpeed, "Potion overwritten");
            h.assertTrue(f.player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() == 1, "Potion changed");
        } h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void rangerAndStarwardenBoostRealProjectilesButNotExplosions(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            for (var profile : List.of(ArmorProfile.RANGER, ArmorProfile.STARWARDEN)) {
                f.equip(profile);
                var target = EntityType.VILLAGER.create(h.getLevel());
                target.setPos(f.player.getX(), f.player.getY(), f.player.getZ() + 2);
                h.getLevel().addFreshEntity(target);
                try {
                    target.hurt(com.starboundmc.weapon.WeaponCombat.boltDamage(h.getLevel(), f.player), 10);
                    eq(h, target.getHealth(), 8.5, "Projectile damage bonus");
                    target.invulnerableTime = 0; target.setHealth(20);
                    target.hurt(h.getLevel().damageSources().playerAttack(f.player), 10);
                    eq(h, target.getHealth(), 10, "Projectile bonus leaked into melee");
                    target.invulnerableTime = 0; target.setHealth(20);
                    var holder = h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                                    ResourceLocation.fromNamespaceAndPath("starboundmc", "weapon_blast")));
                    target.hurt(new net.minecraft.world.damagesource.DamageSource(holder, f.player), 10);
                    eq(h, target.getHealth(), 10, "Projectile bonus leaked into splash");
                } finally { target.discard(); }
            }
            f.player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY); f.sync();
            eq(h, ArmorEffects.projectileMultiplier(f.player), 1, "Removed starwarden helmet kept ranged bonus");
        } h.succeed();
    }
}
