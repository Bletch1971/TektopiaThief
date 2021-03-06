package bletch.tektopiathief.entities.ai;

import bletch.common.MovementMode;
import bletch.common.entities.ai.EntityAIMoveToBlock;
import bletch.tektopiathief.entities.EntityThief;
import bletch.tektopiathief.utils.LoggerUtils;
import net.minecraft.util.math.BlockPos;

import java.util.function.Function;
import java.util.function.Predicate;

public class EntityAIEscapeVillage extends EntityAIMoveToBlock {
    protected final Function<EntityThief, BlockPos> whereFunc;
    protected final Predicate<EntityThief> shouldPred;
    protected final Runnable resetRunner;
    protected final Runnable startRunner;
    protected final EntityThief entity;
    protected final MovementMode moveMode;
    protected boolean active = false;

    public EntityAIEscapeVillage(EntityThief entity, Predicate<EntityThief> shouldPred, Function<EntityThief, BlockPos> whereFunc, MovementMode moveMode, Runnable startRunner, Runnable resetRunner) {
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
        LoggerUtils.instance.info("EntityAIEscapeVillage - startExecuting called", true);

        this.active = true;
        if (this.startRunner != null) {
            this.startRunner.run();
        }

        super.startExecuting();
    }

    public boolean shouldContinueExecuting() {
        boolean result = this.active && this.navigator.canNavigate();
        if (!result)
            LoggerUtils.instance.info("EntityAIEscapeVillage - shouldContinueExecuting called; this.active=" + this.active + "; this.arrived=" + this.arrived + "; this.stuck=" + this.stuck + " (" + this.stuckCount + "); canNavigate=" + this.navigator.canNavigate(), true);
        return result;
    }

    public void resetTask() {
        LoggerUtils.instance.info("EntityAIEscapeVillage - resetTask called", true);

        if (this.resetRunner != null) {
            this.resetRunner.run();
        }
        this.active = false;

        super.resetTask();
    }

    protected boolean attemptStuckFix() {
        return true;
    }

    protected BlockPos getDestinationBlock() {
        return this.whereFunc.apply(this.entity);
    }

    protected boolean isNearWalkPos() {
        return this.entity.getPosition().distanceSq(this.destinationPos) < 4.0D;
    }

    protected void onArrival() {
        LoggerUtils.instance.info("EntityAIEscapeVillage - onArrival called", true);

        this.active = false;
        super.onArrival();
    }

    protected void onPathFailed(BlockPos pos) {
        LoggerUtils.instance.info("EntityAIEscapeVillage - onPathFailed called", true);

        this.active = false;
        super.onPathFailed(pos);
    }

    protected void onStuck() {
        LoggerUtils.instance.info("EntityAIEscapeVillage - onStuck called", true);

        this.active = false;
        super.onStuck();
    }

    protected void updateMovementMode() {
        LoggerUtils.instance.info("EntityAIEscapeVillage - updateMovementMode called with mode " + this.moveMode.name(), true);

        this.entity.setMovementMode(this.moveMode);
    }
}
