package com.starboundmc.weapon;

import com.mojang.authlib.GameProfile;
import com.starboundmc.item.ModItems;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Real server-level item interactions, opt-in via -PweaponGameTests. */
@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class WeaponGameTests {
    private static int sequence;
    @GameTest(template = "shuttle_test_empty")
    public static void recoilAcknowledgesOnlySuccessfulShots(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.COIL_PISTOL.get())) {
            var gun = ModItems.COIL_PISTOL.get();
            var oldStack = f.player.getMainHandItem().copy();
            var wornStack = oldStack.copy(); wornStack.setDamageValue(1);
            h.assertTrue(!gun.shouldCauseReequipAnimation(oldStack, wornStack, false), "Durability lowered the gun");
            h.assertTrue(gun.shouldCauseReequipAnimation(oldStack, wornStack, true), "Real slot change lost equip animation");
            f.fire();
            h.assertTrue(f.shotAcks() == 0, "Empty gun acknowledged a shot");
            f.ammo(Items.IRON_NUGGET, 2); f.fire();
            h.assertTrue(f.shotAcks() == 1, "Successful shot must acknowledge exactly once");
            f.fire();
            h.assertTrue(f.shotAcks() == 0, "Cooldown produced recoil");
            f.elapse(gun.profile().cooldown, false); f.fire();
            h.assertTrue(f.shotAcks() == 1, "Next valid shot did not acknowledge");
        } h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void glaiveReachesNewOuterBandButNotBeyondFiveAndHalf(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.DURASTEEL_GLAIVE.get())) {
            var near = f.target(0, 4.5); var edge = f.target(0, 5.45); var outside = f.target(0, 5.6);
            f.player.recoverAttack(); f.fire();
            h.assertTrue(near.getHealth() == 8 && edge.getHealth() == 8,
                    "Glaive did not reach its extended 5.5 block range");
            h.assertTrue(outside.getHealth() == 20, "Glaive exceeded 5.5 blocks");
        } h.succeed();
    }
    private static final class CombatPlayer extends ServerPlayer {
        CombatPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "WeaponTest"), ClientInformation.createDefault());
        }
        void recoverAttack() { attackStrengthTicker = 100; }
    }
    private static final class Fixture implements AutoCloseable {
        final ServerLevel level;
        final CombatPlayer player;
        final EmbeddedChannel channel;
        final List<Villager> targets = new ArrayList<>();
        Fixture(GameTestHelper h, Item weapon) {
            level = h.getLevel();
            player = new CombatPlayer(level);
            var connection = new Connection(PacketFlow.SERVERBOUND);
            channel = new EmbeddedChannel(connection);
            net.neoforged.neoforge.network.registration.ChannelAttributes.setPayloadSetup(connection,
                    new net.neoforged.neoforge.network.registration.NetworkPayloadSetup(Map.of(
                            net.minecraft.network.ConnectionProtocol.PLAY, Map.of(GunShotPacket.TYPE.id(),
                            new net.neoforged.neoforge.network.registration.NetworkChannel(GunShotPacket.TYPE.id(),
                                    com.starboundmc.network.ModNetwork.PROTOCOL_VERSION)))));
            player.connection = new ServerGamePacketListenerImpl(level.getServer(), connection, player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false));
            ++sequence;
            // Stay in GameTest's ticking spawn region so freshly added entities are queryable.
            var base = h.absolutePos(new BlockPos(1, 10, 1));
            player.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
            level.getChunk(player.blockPosition());
            player.setYRot(0); player.setXRot(0);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(weapon));
        }
        Villager target(double x, double z) {
            var entity = EntityType.VILLAGER.create(level);
            entity.setNoAi(true); entity.setNoGravity(true);
            entity.setPos(player.getX() + x, player.getY(), player.getZ() + z);
            level.getChunk(entity.blockPosition());
            level.addFreshEntity(entity); targets.add(entity); return entity;
        }
        void ammo(Item item, int count) { player.getInventory().setItem(1, new ItemStack(item, count)); }
        int shotAcks() {
            channel.runPendingTasks();
            int count = 0; Object packet;
            while ((packet = channel.readOutbound()) != null) {
                if (packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom
                        && custom.payload() instanceof GunShotPacket shot) {
                    if (shot.slot() != player.getInventory().selected
                            || !shot.item().equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                                    player.getMainHandItem().getItem()))) throw new AssertionError("Wrong shot identity");
                    count++;
                }
                io.netty.util.ReferenceCountUtil.release(packet);
            }
            return count;
        }
        void fire() {
            if (player.getMainHandItem().getItem() instanceof EnergyGunItem) {
                GunTrigger.input(player, GunTriggerPacket.PRESS); GunTrigger.input(player, GunTriggerPacket.RELEASE);
            } else player.getMainHandItem().getItem().use(level, player, InteractionHand.MAIN_HAND);
        }
        void elapse(int ticks, boolean keepAlive) {
            for (int i = 0; i < ticks; i++) {
                ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(level.getGameTime() + 1);
                player.getCooldowns().tick();
                if (keepAlive && i % 5 == 0) GunTrigger.input(player, GunTriggerPacket.PRESS);
                GunTrigger.tick(player);
            }
        }
        void charge(int ticks) {
            GunTrigger.input(player, GunTriggerPacket.PRESS); elapse(ticks, true);
            GunTrigger.input(player, GunTriggerPacket.RELEASE);
        }
        void wall(int z) {
            for (int y = 0; y < 3; y++) level.setBlockAndUpdate(player.blockPosition().offset(0, y, z), Blocks.STONE.defaultBlockState());
        }
        public void close() { GunTrigger.input(player, GunTriggerPacket.CANCEL); targets.forEach(Villager::discard); player.discard(); channel.finishAndReleaseAll(); }
    }

    @GameTest(template = "shuttle_test_empty")
    public static void pistolConsumesOneRoundAndEnforcesCooldown(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.COIL_PISTOL.get())) {
            var near = f.target(0, 3); var far = f.target(0, 6);
            f.ammo(Items.IRON_NUGGET, 4); f.fire(); f.fire();
            h.assertTrue(near.getHealth() == 14 && far.getHealth() == 20, "Pistol must hit only nearest target once");
            h.assertTrue(f.player.getInventory().getItem(1).getCount() == 3, "Cooldown consumed extra ammunition");
            h.assertTrue(f.player.getMainHandItem().getDamageValue() == 1, "Pistol wear must be one per shot");
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void solidWallStopsShots(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.COIL_PISTOL.get())) {
            var target = f.target(0, 6); f.wall(2); f.ammo(Items.IRON_NUGGET, 2); f.fire();
            h.assertTrue(target.getHealth() == 20, "Shot crossed solid wall");
            h.assertTrue(f.player.getInventory().getItem(1).getCount() == 1, "Miss must still consume ammo");
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void plasmaIgnitesAndOffhandCannotFire(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.PLASMA_CARBINE.get())) {
            var target = f.target(0, 3); f.ammo(Items.REDSTONE, 2);
            f.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.PLASMA_CARBINE.get()));
            ModItems.PLASMA_CARBINE.get().use(f.level, f.player, InteractionHand.OFF_HAND);
            h.assertTrue(target.getHealth() == 20 && f.player.getInventory().getItem(1).getCount() == 2, "Offhand fired");
            f.fire();
            h.assertTrue(target.getHealth() == 12 && target.isOnFire(), "Plasma damage or fire missing");
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void railgunCancellationAndInsufficientAmmoAreFree(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.STARCORE_RAILGUN.get())) {
            var target = f.target(0, 3); f.ammo(Items.REDSTONE, 2); f.charge(9);
            h.assertTrue(target.getHealth() == 20 && f.player.getInventory().getItem(1).getCount() == 2,
                    "Cancelled charge spent ammo or damaged target");
            h.assertTrue(f.player.getMainHandItem().getDamageValue() == 0
                    && !f.player.getCooldowns().isOnCooldown(ModItems.STARCORE_RAILGUN.get()), "Cancelled charge spent wear/cooldown");
            f.ammo(Items.REDSTONE, 1); f.charge(20);
            h.assertTrue(target.getHealth() == 20 && f.player.getInventory().getItem(1).getCount() == 1,
                    "Insufficient ammunition partially consumed or fired");
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void railgunExplodesAtFirstHitAndCombinesAmmoStacks(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.STARCORE_RAILGUN.get())) {
            var first = f.target(0, 3); var second = f.target(1.5, 3);
            var third = f.target(0, 7); var fourth = f.target(0, 9);
            f.ammo(Items.REDSTONE, 1); f.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.REDSTONE));
            f.charge(10);
            h.assertTrue(first.getHealth() == 2 && second.getHealth() < 20 && second.getHealth() > 2
                    && third.getHealth() == 20 && fourth.getHealth() == 20,
                    "Railgun must deal 18 once to direct target, falloff splash nearby, no infinite penetration");
            h.assertTrue(f.player.getInventory().getItem(1).isEmpty() && f.player.getOffhandItem().isEmpty(), "Split ammo not consumed");
            h.assertTrue(f.player.getMainHandItem().getDamageValue() == 4, "Railgun wear mismatch");
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void friendlyTargetsAndDisabledPvpAreProtected(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.COIL_PISTOL.get()); var other = new Fixture(h, ModItems.COIL_PISTOL.get())) {
            var target = f.target(0, 3);
            var board = f.level.getScoreboard(); var team = board.addPlayerTeam("weapon" + sequence);
            board.addPlayerToTeam(f.player.getScoreboardName(), team);
            board.addPlayerToTeam(target.getScoreboardName(), team);
            try {
                f.ammo(Items.IRON_NUGGET, 2); f.fire();
                h.assertTrue(target.getHealth() == 20, "Friendly entity was damaged");
                boolean pvp = f.level.getServer().isPvpAllowed();
                try {
                    f.level.getServer().setPvpAllowed(false);
                    h.assertTrue(!WeaponCombat.canHit(f.player, other.player), "Disabled PvP ignored");
                } finally { f.level.getServer().setPvpAllowed(pvp); }
            } finally { board.removePlayerTeam(team); }
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void bladesRespectConeCooldownAndRepairTiers(GameTestHelper h) {
        for (var item : List.of(ModItems.DURASTEEL_GLAIVE.get(), ModItems.STARCORE_BLADE.get())) {
            try (var f = new Fixture(h, item)) {
                var front = f.target(0, 2); var rear = f.target(0, -2);
                f.fire();
                h.assertTrue(front.getHealth() == 20, "Unrecovered blade activated ability");
                f.player.recoverAttack();
                h.assertTrue(WeaponCombat.canHit(f.player, front), "Fixture front target not eligible");
                h.assertTrue(front.getEyePosition().subtract(f.player.getEyePosition()).normalize().dot(f.player.getLookAngle()) >= .4,
                        "Fixture target outside cone: " + f.player.getLookAngle() + " target=" + front.getEyePosition());
                h.assertTrue(f.level.getEntitiesOfClass(Villager.class, f.player.getBoundingBox().inflate(item.profile().range)).contains(front),
                        "Fixture target missing from spatial query: " + f.player.getBoundingBox() + " target=" + front.getBoundingBox());
                f.fire(); f.fire();
                h.assertTrue(front.getHealth() == 20 - item.profile().damage - 2 && rear.getHealth() == 20,
                        "Blade cone/damage or cooldown mismatch: " + item.profile() + " front=" + front.getHealth()
                                + " rear=" + rear.getHealth() + " strength=" + f.player.getAttackStrengthScale(0)
                                + " visible=" + f.player.hasLineOfSight(front));
                h.assertTrue(f.player.getMainHandItem().getDamageValue() == (item == ModItems.STARCORE_BLADE.get() ? 16 : 8),
                        "Blade ability durability mismatch");
                h.assertTrue(!item.isValidRepairItem(f.player.getMainHandItem(), new ItemStack(Items.IRON_INGOT)),
                        "Advanced blade repaired with iron");
            }
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void fullRailgunChargeStopsAtWall(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.STARCORE_RAILGUN.get())) {
            var near = f.target(0, 2); var far = f.target(0, 6);
            f.wall(4); f.ammo(Items.REDSTONE, 4); f.charge(20);
            h.assertTrue(near.getHealth() == 0 && far.getHealth() == 20, "Full charge damage or wall clipping incorrect");
            f.fire();
            h.assertTrue(f.player.getInventory().getItem(1).getCount() == 2, "Railgun bypassed post-shot cooldown");
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void bladeSkillCapsTargetsAndCannotCrossWalls(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.DURASTEEL_GLAIVE.get())) {
            var first = f.target(0, 1); var second = f.target(0, 1.5);
            var third = f.target(0, 2); var fourth = f.target(0, 2.5);
            f.player.recoverAttack(); f.fire();
            h.assertTrue(first.getHealth() == 8 && second.getHealth() == 8 && third.getHealth() == 8
                    && fourth.getHealth() == 20, "Glaive must hit at most three nearest targets");
            f.player.getCooldowns().removeCooldown(ModItems.DURASTEEL_GLAIVE.get());
            f.player.recoverAttack(); f.wall(1); f.fire();
            h.assertTrue(fourth.getHealth() == 20, "Blade hit through a wall");
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void heldLeftTriggerFiresAtFasterCadenceAndStops(GameTestHelper h) {
        for (var gun : List.of(ModItems.COIL_PISTOL.get(), ModItems.PLASMA_CARBINE.get())) {
            try (var f = new Fixture(h, gun)) {
                var target = f.target(0, 3); f.ammo(gun.ammunition(), 16);
                gun.use(f.level, f.player, InteractionHand.MAIN_HAND);
                h.assertTrue(target.getHealth() == 20, "Right click still fires");
                GunTrigger.input(f.player, GunTriggerPacket.PRESS);
                f.elapse(gun.profile().cooldown, true);
                h.assertTrue(target.getHealth() == 20 - gun.profile().damage * 2, "Fast shots swallowed by vanilla invulnerability");
                h.assertTrue(f.player.getInventory().getItem(1).getCount() == 14, "Incorrect automatic fire cadence");
                GunTrigger.input(f.player, GunTriggerPacket.RELEASE); f.elapse(10, false);
                h.assertTrue(f.player.getInventory().getItem(1).getCount() == 14, "Trigger release did not stop firing");
            }
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void triggerSpamTimeoutAndSwapCannotBypassServerState(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.STARCORE_RAILGUN.get())) {
            var target = f.target(0, 3); f.ammo(Items.REDSTONE, 8);
            GunTrigger.input(f.player, 255);
            for (int i = 0; i < 100; i++) GunTrigger.input(f.player, GunTriggerPacket.PRESS);
            GunTrigger.input(f.player, GunTriggerPacket.RELEASE);
            h.assertTrue(target.getHealth() == 20, "Packet spam counted as server charge ticks");
            GunTrigger.input(f.player, GunTriggerPacket.PRESS); f.elapse(20, false);
            GunTrigger.input(f.player, GunTriggerPacket.RELEASE);
            h.assertTrue(target.getHealth() == 20, "Timed-out charge fired");
            GunTrigger.input(f.player, GunTriggerPacket.PRESS); f.elapse(10, true);
            var stack = f.player.getMainHandItem();
            f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); GunTrigger.tick(f.player);
            f.player.setItemInHand(InteractionHand.MAIN_HAND, stack); GunTrigger.input(f.player, GunTriggerPacket.RELEASE);
            h.assertTrue(target.getHealth() == 20 && f.player.getInventory().getItem(1).getCount() == 8, "Swap retained charge or spent ammo");
            GunTrigger.input(f.player, GunTriggerPacket.PRESS); f.elapse(10, true);
            GunTrigger.input(f.player, GunTriggerPacket.CANCEL); GunTrigger.input(f.player, GunTriggerPacket.RELEASE);
            h.assertTrue(target.getHealth() == 20, "Cancelled charge fired");
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void railSplashRespectsWallsAlliesAndTerrain(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.STARCORE_RAILGUN.get())) {
            var blocked = f.target(0, 3); var friend = f.target(1, 1.5); var exposed = f.target(-1, 1.5);
            f.wall(2);
            var board = f.level.getScoreboard(); var team = board.addPlayerTeam("blast" + sequence);
            board.addPlayerToTeam(f.player.getScoreboardName(), team); board.addPlayerToTeam(friend.getScoreboardName(), team);
            try {
                f.ammo(Items.REDSTONE, 4); f.charge(20);
                h.assertTrue(blocked.getHealth() == 20 && friend.getHealth() == 20 && exposed.getHealth() < 20,
                        "Explosion ignored wall/alliance or failed to splash");
                h.assertTrue(f.player.getHealth() == 20, "Explosion damaged shooter");
                h.assertTrue(f.level.getBlockState(f.player.blockPosition().offset(0, 1, 2)).is(Blocks.STONE), "Explosion destroyed terrain");
            } finally { board.removePlayerTeam(team); }
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void registeredWeaponsHaveDurabilityAndRecipes(GameTestHelper h) {
        for (var profile : WeaponProfile.values()) {
            var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("starboundmc", profile.id());
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            var stack = new ItemStack(item);
            h.assertTrue(stack.getMaxDamage() == profile.durability && stack.getMaxStackSize() == 1, "Incorrect item components: " + id);
            var recipe = h.getLevel().getRecipeManager().byKey(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("starboundmc", "print_" + profile.id()));
            h.assertTrue(recipe.isPresent(), "Missing/invalid loaded printing recipe: " + id);
            h.assertTrue(recipe.get().value().getResultItem(h.getLevel().registryAccess()).is(item), "Recipe produces wrong weapon");
            stack.setDamageValue(17);
            var restored = ItemStack.parse(h.getLevel().registryAccess(), stack.save(h.getLevel().registryAccess())).orElseThrow();
            h.assertTrue(restored.is(item) && restored.getDamageValue() == 17 && restored.getMaxDamage() == profile.durability,
                    "Weapon save/load lost identity or durability");
            if (!profile.ranged()) h.assertTrue(stack.is(net.minecraft.tags.ItemTags.SWORDS), "Blade absent from sword enchantment tags");
            var printing = (com.starboundmc.recipe.VoxelPrintingRecipe) recipe.get().value();
            var materials = new ArrayList<ItemStack>();
            for (var entry : printing.materials()) if (!entry.isVoxel()) {
                var ingredient = entry.ingredient().getItems()[0].copy(); ingredient.setCount(entry.count()); materials.add(ingredient);
            }
            h.assertTrue(printing.reserveMaterials(materials).isPresent(), "Recipe cannot reserve its own declared materials");
        }
        h.assertTrue(new ItemStack(ModItems.SURVIVAL_KNIFE.get()).getMaxStackSize() == 1, "Knife stacks");
        h.succeed();
    }
}
