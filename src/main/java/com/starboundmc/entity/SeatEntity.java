package com.starboundmc.entity;

import com.mojang.logging.LogUtils;
import com.starboundmc.block.CaptainChairBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * Invisible, non-colliding mount used to let a player sit on the captain's chair.
 * It removes itself as soon as it has no passenger (or the chair below is gone).
 */
public class SeatEntity extends Entity
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public SeatEntity(EntityType<?> type, Level level)
    {
        super(type, level);
        this.noPhysics = true;
        this.setInvisible(true);
        LOGGER.debug("SeatEntity created id={} pos={}", this.getId(), this.position());
    }

    @Override
    public void tick()
    {
        super.tick();
        if (!this.level().isClientSide)
        {
            // The seat sits inside the chair block itself, so check the block AT the seat's
            // own position (the chair), not the block below it.
            BlockPos pos = this.blockPosition();
            if (!this.getPassengers().isEmpty()
                    && !(this.level().getBlockState(pos).getBlock() instanceof CaptainChairBlock))
            {
                LOGGER.debug("SeatEntity id={} ejecting: chair missing at {}", this.getId(), pos);
                this.ejectPassengers();
            }
            if (this.getPassengers().isEmpty())
            {
                LOGGER.debug("SeatEntity id={} discarded (no passenger)", this.getId());
                this.discard();
            }
        }
    }

    @Override
    protected void addPassenger(Entity passenger)
    {
        LOGGER.debug("SeatEntity.addPassenger vehicleId={} passenger={}", this.getId(), passenger.getScoreboardName());
        super.addPassenger(passenger);
    }

    @Override
    public double getPassengersRidingOffset()
    {
        return 0.3D;
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    @Override
    public boolean isAttackable()
    {
        return false;
    }

    @Override
    protected void defineSynchedData()
    {
        // Entity's constructor already registered the shared-flags data parameter
        // (id 0) before this hook runs, so a direct Entity subclass must leave
        // this empty. Registering DATA_SHARED_FLAGS_ID again throws
        // "Duplicate id value for 0!" the moment the seat spawns.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
    }
}
