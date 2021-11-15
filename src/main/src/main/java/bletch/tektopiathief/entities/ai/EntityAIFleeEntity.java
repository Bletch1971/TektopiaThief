package bletch.tektopiathief.entities.ai;

import bletch.common.entities.ai.EntityAIMoveToBlock;
import bletch.tektopiathief.entities.EntityThief;
import bletch.tektopiathief.entities.EntityThief.MovementMode;
import bletch.tektopiathief.utils.LoggerUtils;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.tangotek.tektopia.ModSoundEvents;
import net.tangotek.tektopia.pathing.BasePathingNode;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EntityAIFleeEntity extends EntityAIMoveToBlock {
    protected final EntityThief entity;
    protected final Predicate<EntityLivingBase> entityPredicate;
    protected final Predicate<EntityThief> shouldPred;
    protected EntityLivingBase fleeEntity;
    protected BlockPos destinationPosition;
    protected final MovementMode moveMode;

    @SuppressWarnings("unchecked")
    public EntityAIFleeEntity(EntityThief entity, Predicate<EntityThief> shouldPred, Predicate<EntityLivingBase> filterPred) {
        super(entity);
        this.entity = entity;
        this.shouldPred = shouldPred;
        this.moveMode = MovementMode.RUN;
        this.setMutexBits(1);
        this.entityPredicate = Predicates.and(EntitySelectors.CAN_AI_TARGET,
                e -> e.isEntityAlive() && e.canEntityBeSeen(this.entity),
                filterPred);
    }

    public boolean shouldExecute() {
        if (this.entity.isAITick() && this.entity.hasVillage() && this.shouldPred.test(this.entity)) {

            double avoidDistance = this.entity.getAvoidanceDistance();
            if (avoidDistance == 0)
                return false;

            AxisAlignedBB boundingBox = this.entity.getEntityBoundingBox().grow(avoidDistance, 6.0F, avoidDistance);
            List<EntityLivingBase> fleeEntities = this.entity.world.getEntitiesWithinAABB(EntityLivingBase.class, boundingBox, this.entityPredicate).stream()
                    .filter((e) -> e.getDistance(this.entity) <= avoidDistance)
                    .sorted(Comparator.comparingDouble(e -> e.getDistance(this.entity)))
                    .collect(Collectors.toList());

            if (fleeEntities == null || fleeEntities.isEmpty())
                return false;

            this.fleeEntity = fleeEntities.get(0);
            LoggerUtils.info("EntityAIFleeEntity - shouldExecute called"
                            + "; entity=" + this.fleeEntity.getName()
                            + "; avoid Distance=" + avoidDistance
                            + "; distance=" + this.fleeEntity.getDistance(this.entity)
                    , true);

            BlockPos fleeBlock = this.findRandomPositionAwayFrom(this.fleeEntity);

            if (fleeBlock != null) {
                this.destinationPosition = fleeBlock;
                return super.shouldExecute();
            }
        }

        return false;
    }

    public void startExecuting() {
        LoggerUtils.info("EntityAIFleeEntity - startExecuting called", true);

        if (this.entityPredicate.test(this.fleeEntity)) {
            if (this.entity.getRNG().nextInt(2) == 0) {
                this.entity.playSound(ModSoundEvents.villagerAfraid);
            }
        }

        super.startExecuting();
    }

    public void resetTask() {
        LoggerUtils.info("EntityAIFleeEntity - resetTask called", true);

        this.fleeEntity = null;

        super.resetTask();
    }

    protected BlockPos findRandomPositionAwayFrom(Entity fleeEntity) {
        Vec3d entityPosition = this.entity.getPositionVector();
        Vec3d enemyPosition = fleeEntity.getPositionVector();
        Vec3d fleeDelta;

        if (this.entity.hasVillage()) {
            if (this.entity.getPosition().distanceSq(this.entity.getVillage().getOrigin()) < 1600.0D) {
                fleeDelta = entityPosition.subtract(enemyPosition);
            } else {
                fleeDelta = this.entity.getPositionVector().subtract(new Vec3d(this.entity.getVillage().getOrigin()));
            }
        } else {
            fleeDelta = entityPosition.subtract(enemyPosition);
        }

        Vec3d fleeDeltaNorm = fleeDelta.normalize();
        Vec3d fleeDir = new Vec3d(fleeDeltaNorm.x, 0.0D, fleeDeltaNorm.y);

        if (this.entity.hasVillage() && this.entity.getVillage().getAABB().contains(entityPosition) && entityPosition.add(fleeDir).squareDistanceTo(enemyPosition) < entityPosition.squareDistanceTo(enemyPosition)) {
            fleeDir = fleeDir.rotateYaw(60.0F);
            if (entityPosition.add(fleeDir).squareDistanceTo(enemyPosition) < entityPosition.squareDistanceTo(enemyPosition)) {
                fleeDir = fleeDir.rotateYaw(-120.0F);
            }
        }

        Vec3d fleePos = this.entity.getPositionVector().add(fleeDir.scale(16.0D));
        BlockPos fleeBlock = new BlockPos(fleePos.x, fleePos.y, fleePos.z);

        for (int i = 0; i < 20; ++i) {
            BlockPos testBlock = fleeBlock.add(this.entity.getRNG().nextInt((i + 3) * 2) - (i + 3), 0, this.entity.getRNG().nextInt((i + 3) * 2) - (i + 3));
            BasePathingNode baseNode = this.entity.getVillage().getPathingGraph().getNodeYRange(testBlock.getX(), testBlock.getY() - 5, testBlock.getY() + 5, testBlock.getZ());

            if (baseNode != null) {
                return baseNode.getBlockPos();
            }
        }

        return null;
    }

    protected BlockPos getDestinationBlock() {
        return this.destinationPosition;
    }

    protected void updateMovementMode() {
        LoggerUtils.info("EntityAIFleeEntity - updateMovementMode called with mode " + this.moveMode.name(), true);

        this.entity.setMovementMode(this.moveMode);
    }
}
