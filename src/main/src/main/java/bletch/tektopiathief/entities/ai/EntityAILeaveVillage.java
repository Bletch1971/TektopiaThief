package bletch.tektopiathief.entities.ai;

import bletch.common.MovementMode;
import bletch.common.entities.ai.EntityAIMoveToBlock;
import bletch.tektopiathief.entities.EntityThief;
import bletch.tektopiathief.utils.LoggerUtils;
import net.minecraft.util.math.BlockPos;

import java.util.function.Function;
import java.util.function.Predicate;

public class EntityAILeaveVillage extends EntityAIMoveToBlock {
    protected final Function<EntityThief, BlockPos> whereFunc;
    protected final Predicate<EntityThief> shouldPred;
    protected final Runnable resetRunner;
    protected final Runnable startRunner;
    protected final EntityThief entity;
    protected final MovementMode moveMode;

    public EntityAILeaveVillage(EntityThief entity, Predicate<EntityThief> shouldPred, Function<EntityThief, BlockPos> whereFunc, MovementMode moveMode, Runnable startRunner, Runnable resetRunner) {
        super(entity);
        this.entity = entity;
        this.shouldPred = shouldPred;
        this.whereFunc = whereFunc;
        this.resetRunner = resetRunner;
        this.startRunner = startRunner;
        this.moveMode = moveMode;
    }

    public boolean shouldExecute() {
        if (this.entity.isAITick() && this.entity.hasVillage() && this.shouldPred.test(this.entity))
            return super.shouldExecute();
        return false;
    }

    public void startExecuting() {
        LoggerUtils.instance.info("EntityAILeaveVillage - startExecuting called", true);

        if (this.startRunner != null) {
            this.startRunner.run();
        }

        super.startExecuting();
    }

    public void resetTask() {
        LoggerUtils.instance.info("EntityAILeaveVillage - resetTask called", true);

        if (this.resetRunner != null) {
            this.resetRunner.run();
        }

        super.resetTask();
    }

    protected BlockPos getDestinationBlock() {
        return this.whereFunc.apply(this.entity);
    }

    protected boolean isNearWalkPos() {
        return this.entity.getPosition().distanceSq(this.destinationPos) < 4.0D;
    }

    protected void updateMovementMode() {
        LoggerUtils.instance.info("EntityAILeaveVillage - updateMovementMode called with mode " + this.moveMode.name(), true);

        this.entity.setMovementMode(this.moveMode);
    }
}
