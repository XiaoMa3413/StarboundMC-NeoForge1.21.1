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
        void fire() { player.getMainHandItem().getItem().use(level, player, InteractionHand.MAIN_HAND); }
        void charge(int ticks) {
            fire();
            player.getMainHandItem().releaseUsing(level, player, 72000 - ticks);
            player.stopUsingItem();
        }
        void wall(int z) {
            for (int y = 0; y < 3; y++) level.setBlockAndUpdate(player.blockPosition().offset(0, y, z), Blocks.STONE.defaultBlockState());
        }
        public void close() { targets.forEach(Villager::discard); player.discard(); channel.finishAndReleaseAll(); }
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
            var target = f.target(0, 3); f.ammo(Items.REDSTONE, 2); f.charge(19);
            h.assertTrue(target.getHealth() == 20 && f.player.getInventory().getItem(1).getCount() == 2,
                    "Cancelled charge spent ammo or damaged target");
            h.assertTrue(f.player.getMainHandItem().getDamageValue() == 0
                    && !f.player.getCooldowns().isOnCooldown(ModItems.STARCORE_RAILGUN.get()), "Cancelled charge spent wear/cooldown");
            f.ammo(Items.REDSTONE, 1); f.charge(40);
            h.assertTrue(target.getHealth() == 20 && f.player.getInventory().getItem(1).getCount() == 1,
                    "Insufficient ammunition partially consumed or fired");
        } h.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void railgunPiercesOnlyThreeAndCombinesAmmoStacks(GameTestHelper h) {
        try (var f = new Fixture(h, ModItems.STARCORE_RAILGUN.get())) {
            var first = f.target(0, 3); var second = f.target(0, 5);
            var third = f.target(0, 7); var fourth = f.target(0, 9);
            f.ammo(Items.REDSTONE, 1); f.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.REDSTONE));
            f.charge(20);
            h.assertTrue(first.getHealth() == 2 && second.getHealth() == 2 && third.getHealth() == 2
                    && fourth.getHealth() == 20, "Minimum charge must deal 18 to at most three targets");
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
            f.wall(4); f.ammo(Items.REDSTONE, 4); f.charge(40);
            h.assertTrue(near.getHealth() == 0 && far.getHealth() == 20, "Full charge damage or wall clipping incorrect");
            f.charge(40);
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
