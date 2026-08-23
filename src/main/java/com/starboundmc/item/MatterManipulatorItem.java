package com.starboundmc.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MatterManipulatorItem extends Item
{
    public static final String NBT_SPEED_UPGRADES = "SpeedUpgrades";
    public static final String NBT_RANGE_UPGRADES = "RangeUpgrades";
    public static final String NBT_MINING_UPGRADES = "MiningUpgrades";
    public static final String NBT_FORTUNE_UPGRADES = "FortuneUpgrades";
    public static final String NBT_LEGACY_UPGRADES = "Upgrades";
    public static final int MAX_UPGRADES = 3;
    /** Mining tier track (2 upgrades): 0 = stone-equivalent (up to iron ore),
     *  1 = diamond-equivalent (diamond ore, gold/redstone/emerald, obsidian),
     *  2 = netherite-equivalent (ancient debris). */
    public static final int MAX_MINING_UPGRADES = 2;
    public static final int MAX_MINING_LEVEL = 2;
    /** Fortune track (3 upgrades): levels 0..3, mirrored as a real BLOCK_FORTUNE
     *  enchantment on the stack so vanilla loot tables scale the drops. */
    public static final int MAX_FORTUNE_UPGRADES = 3;
    /** Compounding mining-speed multiplier per upgrade level (1.5x per level). */
    public static final float UPGRADE_SPEED_MULTIPLIER = 1.5F;

    // Laser mining: hold right-click to irradiate the targeted block. Progress
    // follows the vanilla destroy-progress formula (hardness vs. the
    // manipulator's dig speed), so harder blocks take longer, speed upgrades
    // accelerate the beam, and a wrong mining tier charges ~3.3x slower.
    public static final double LASER_RANGE_BASE = 8.0;
    public static final double LASER_RANGE_PER_LEVEL = 2.0;
    /** Progress-rate multiplier for the beam (game feel; 1.0 = vanilla pickaxe pace). */
    public static final float LASER_PROGRESS_MULTIPLIER = 2.0F;
    /** Vanilla "infinite" use duration while the beam is held. */
    public static final int USE_DURATION = 72000;

    public MatterManipulatorItem(Properties properties)
    {
        super(properties);
    }

    /** Server-side irradiation state of one player's beam. */
    private static final class LaserProgress
    {
        BlockPos pos;
        BlockState state;
        float progress;
    }

    private static final Map<UUID, LaserProgress> LASER_PROGRESS = new HashMap<>();

    public static int getSpeedLevel(ItemStack stack)
    {
        return upgrades(stack).speed();
    }

    public static void setSpeedLevel(ItemStack stack, int level)
    {
        stack.set(ModDataComponents.MATTER_MANIPULATOR_UPGRADES.get(), upgrades(stack).withSpeed(level));
    }

    public static int getRangeLevel(ItemStack stack)
    {
        return upgrades(stack).range();
    }

    public static void setRangeLevel(ItemStack stack, int level)
    {
        stack.set(ModDataComponents.MATTER_MANIPULATOR_UPGRADES.get(), upgrades(stack).withRange(level));
    }

    /** Number of mining-tier upgrades applied (0 = stone-equivalent, 1 = diamond, 2 = netherite). */
    public static int getMiningUpgrades(ItemStack stack)
    {
        return upgrades(stack).mining();
    }

    public static void setMiningUpgrades(ItemStack stack, int upgrades)
    {
        stack.set(ModDataComponents.MATTER_MANIPULATOR_UPGRADES.get(), upgrades(stack).withMining(upgrades));
    }

    /** Player-facing mining tier: 0 = iron-ore tier (default), 1 = diamond/obsidian tier, 2 = ancient-debris tier. */
    public static int getMiningLevel(ItemStack stack)
    {
        return getMiningUpgrades(stack);
    }

    /** Fortune upgrade level (0..3), stored in the immutable upgrade component. */
    public static int getFortuneLevel(ItemStack stack)
    {
        return upgrades(stack).fortune();
    }

    public static void setFortuneLevel(ItemStack stack, int level)
    {
        stack.set(ModDataComponents.MATTER_MANIPULATOR_UPGRADES.get(), upgrades(stack).withFortune(level));
    }

    public static void setFortuneLevel(ItemStack stack, int level, Holder<Enchantment> fortune)
    {
        setFortuneLevel(stack, level);
        int clamped = getFortuneLevel(stack);
        EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(fortune, clamped));
    }

    public static MatterManipulatorUpgrades upgrades(ItemStack stack)
    {
        MatterManipulatorUpgrades value = stack.get(ModDataComponents.MATTER_MANIPULATOR_UPGRADES.get());
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (value != null)
        {
            if (customData != null && containsLegacyFields(customData.copyTag()))
            {
                CompoundTag tag = customData.copyTag();
                removeLegacyFields(tag);
                storeCustomData(stack, tag);
            }
            return value;
        }
        if (customData == null)
            return MatterManipulatorUpgrades.DEFAULT;

        CompoundTag tag = customData.copyTag();
        if (!containsLegacyFields(tag))
            return MatterManipulatorUpgrades.DEFAULT;

        value = migrateLegacyTag(tag);
        stack.set(ModDataComponents.MATTER_MANIPULATOR_UPGRADES.get(), value);
        storeCustomData(stack, tag);
        return value;
    }

    /** Reads and removes the 1.20.1 custom-data fields in one operation. */
    public static MatterManipulatorUpgrades migrateLegacyTag(CompoundTag tag)
    {
        if (!containsLegacyFields(tag))
            return MatterManipulatorUpgrades.DEFAULT;
        int speed = tag.contains(NBT_SPEED_UPGRADES, Tag.TAG_INT)
                ? tag.getInt(NBT_SPEED_UPGRADES) : tag.getInt(NBT_LEGACY_UPGRADES);
        MatterManipulatorUpgrades value = new MatterManipulatorUpgrades(
                speed, tag.getInt(NBT_RANGE_UPGRADES),
                tag.getInt(NBT_MINING_UPGRADES), tag.getInt(NBT_FORTUNE_UPGRADES));
        removeLegacyFields(tag);
        return value;
    }

    private static boolean containsLegacyFields(CompoundTag tag)
    {
        return tag.contains(NBT_SPEED_UPGRADES) || tag.contains(NBT_RANGE_UPGRADES)
                || tag.contains(NBT_MINING_UPGRADES) || tag.contains(NBT_FORTUNE_UPGRADES)
                || tag.contains(NBT_LEGACY_UPGRADES) || tag.contains("Enchantments");
    }

    private static void removeLegacyFields(CompoundTag tag)
    {
        tag.remove(NBT_SPEED_UPGRADES);
        tag.remove(NBT_RANGE_UPGRADES);
        tag.remove(NBT_MINING_UPGRADES);
        tag.remove(NBT_FORTUNE_UPGRADES);
        tag.remove(NBT_LEGACY_UPGRADES);
        tag.remove("Enchantments");
    }

    private static void storeCustomData(ItemStack stack, CompoundTag tag)
    {
        if (tag.isEmpty())
            stack.remove(DataComponents.CUSTOM_DATA);
        else
            CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        // Hold-to-use: while the button is held the beam keeps charging
        // (onUseTick advances the server-side irradiation progress).
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity)
    {
        return USE_DURATION;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration)
    {
        // onUseTick fires on both sides; only the server charges the beam.
        if (level.isClientSide || !(entity instanceof ServerPlayer serverPlayer))
            return;
        chargeLaser(serverPlayer, stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft)
    {
        // Called when the button is released or the use is interrupted.
        LASER_PROGRESS.remove(entity.getUUID());
    }

    /**
     * Server-side per-tick charge of the mining beam. Progress follows the
     * vanilla destroy-progress formula (tool speed / hardness / correct-tool
     * factor), so harder blocks take longer, speed upgrades accelerate the
     * beam, and a wrong mining tier charges ~3.3x slower and yields no drops.
     * The block breaks when the bar fills; holding the button then starts
     * charging whatever is behind it.
     */
    private static void chargeLaser(ServerPlayer player, ItemStack stack)
    {
        double range = LASER_RANGE_BASE + getRangeLevel(stack) * LASER_RANGE_PER_LEVEL;
        HitResult rawHit = player.pick(range, 0.0F, false);
        if (rawHit.getType() != HitResult.Type.BLOCK)
        {
            LASER_PROGRESS.remove(player.getUUID());
            return;
        }
        BlockHitResult hit = (BlockHitResult) rawHit;

        BlockPos pos = hit.getBlockPos();
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F)
        {
            LASER_PROGRESS.remove(player.getUUID());
            return;
        }

        LaserProgress progress = LASER_PROGRESS.get(player.getUUID());
        if (progress == null || !progress.pos.equals(pos) || progress.state != state)
        {
            progress = new LaserProgress();
            progress.pos = pos;
            progress.state = state;
            LASER_PROGRESS.put(player.getUUID(), progress);
        }

        // Progress per tick follows the vanilla destroy-progress formula
        // (tool speed / hardness / 30|100 correct-tool factor) with the
        // LASER_PROGRESS_MULTIPLIER, but WITHOUT the player-environment
        // modifiers (airborne/underwater slowdowns) — a mining beam does not
        // care how the operator stands or swims.
        float hardness = state.getDestroySpeed(level, pos);
        boolean canHarvest = !state.requiresCorrectToolForDrops()
                || isCorrectToolForDrops(state, getMiningLevel(stack));
        float factor = canHarvest ? 30.0F : 100.0F;
        progress.progress += (laserDigSpeed(stack, state) / hardness / factor) * LASER_PROGRESS_MULTIPLIER;
        if (progress.progress < 1.0F)
            return;

        LASER_PROGRESS.remove(player.getUUID());
        breakBlock(player, stack, pos, state, canHarvest);
    }

    /** Instant-remove the block; drops follow the mining-tier gate, with the
     *  manipulator as the loot tool so the fortune track applies. */
    private static void breakBlock(ServerPlayer player, ItemStack stack, BlockPos pos, BlockState state,
                                   boolean canHarvest)
    {
        Level level = player.level();
        // Level.destroyBlock would pass ItemStack.EMPTY as the loot tool (no
        // fortune), so the block entity is captured and the drops are produced
        // manually with the manipulator as the tool.
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        boolean removed = level.destroyBlock(pos, false, player);
        if (removed && canHarvest && !player.isCreative())
        {
            Block.dropResources(state, level, pos, blockEntity, player, stack);
        }
    }

    /**
     * Laser dig speed: the beam's charge rate. Speed upgrades compound a ×1.5
     * multiplier per level (1.5/2.25/3.375 at levels 1/2/3). NOTE: the
     * manipulator deliberately does NOT override getDestroySpeed /
     * isCorrectToolForDrops — left-click mining is disabled entirely (see
     * MatterManipulatorEvents), it is a laser tool, not a pickaxe.
     */
    public static float laserDigSpeed(ItemStack stack, BlockState state)
    {
        float mult = (float) Math.pow(UPGRADE_SPEED_MULTIPLIER, getSpeedLevel(stack));
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE))
        {
            return switch (getMiningTier(state))
            {
                case 3 -> 1.0F * mult;
                case 2 -> 2.0F * mult;
                case 1 -> 3.0F * mult;
                default -> Tiers.STONE.getSpeed() * mult;
            };
        }
        if (state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL) || state.is(BlockTags.MINEABLE_WITH_HOE))
            return Tiers.WOOD.getSpeed() * mult;
        return 1.0F * mult;
    }

    /**
     * Correct-tool check driven by the manipulator's mining tier:
     * 0 = stone-equivalent (up to iron ore), 1 = diamond-equivalent (diamond
     * ore, gold/redstone/emerald, obsidian), 2 = netherite-equivalent
     * (additionally ancient debris). Used by the laser for the drop gate and
     * the charge-rate factor.
     */
    public static boolean isCorrectToolForDrops(BlockState state, int level)
    {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL))
        {
            // Obsidian is diamond-tier; ancient debris is the only
            // netherite-gated block.
            return level >= (state.is(Blocks.ANCIENT_DEBRIS) ? 2 : 1);
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL))
            return level >= 1;
        if (state.is(BlockTags.NEEDS_STONE_TOOL))
            return true; // iron ore and below are always harvestable
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    private static int getMiningTier(BlockState state)
    {
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL))
            return 3;
        if (state.is(BlockTags.NEEDS_IRON_TOOL))
            return 2;
        if (state.is(BlockTags.NEEDS_STONE_TOOL))
            return 1;
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("item.starboundmc.matter_manipulator.tooltip"));
        tooltip.add(Component.translatable("item.starboundmc.matter_manipulator.speed_level",
                getSpeedLevel(stack), MAX_UPGRADES));
        tooltip.add(Component.translatable("item.starboundmc.matter_manipulator.range_level",
                getRangeLevel(stack), MAX_UPGRADES));
        tooltip.add(Component.translatable("item.starboundmc.matter_manipulator.mining_level",
                getMiningLevel(stack), MAX_MINING_LEVEL));
        tooltip.add(Component.translatable("item.starboundmc.matter_manipulator.fortune_level",
                getFortuneLevel(stack), MAX_FORTUNE_UPGRADES));
        // NOTE: super.appendHoverText deliberately not called — it would append
        // the mirrored BLOCK_FORTUNE enchantment ("Fortune III") on top of our
        // own fortune-level line, showing the upgrade twice.
    }
}
