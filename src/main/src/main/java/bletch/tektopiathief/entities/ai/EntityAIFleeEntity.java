package bletch.tektopiathief.entities.ai;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import bletch.tektopiathief.entities.EntityThief;
import bletch.tektopiathief.entities.EntityThief.MovementMode;
import bletch.tektopiathief.utils.LoggerUtils;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.tangotek.tektopia.ModSoundEvents;
import net.tangotek.tektopia.pathing.BasePathingNode;

public class EntityAIFleeEntity extends EntityAIMoveToBlock {
	protected final EntityThief entity;
	protected final Predicate<Entity> entityPredicate;
	protected final Predicate<EntityThief> shouldPred;
	protected Entity fleeEntity;
	protected BlockPos destinationPosition;
	protected final MovementMode moveMode;

	@SuppressWarnings("unchecked")
	public EntityAIFleeEntity(EntityThief entity, Predicate<EntityThief> shouldPred, Predicate<Entity> inPred) {
		super(entity);
		this.entity = entity;
		this.shouldPred = shouldPred;
		this.moveMode = MovementMode.RUN;
		this.setMutexBits(1);
		this.entityPredicate = Predicates.and(EntitySelectors.CAN_AI_TARGET, 
				e -> e.isEntityAlive() && this.entity.getEntitySenses().canSee(e),
				inPred);
	}

	public boolean shouldExecute() {
		if (this.entity.isAITick() && this.entity.hasVillage() && this.shouldPred.test(this.entity)) {

			double avoidDistance = this.entity.getAvoidanceDistance();
			if (avoidDistance == 0)
				return false;
			
			List<Entity> fleeEntities = this.entity.world.getEntitiesInAABBexcluding(this.entity, this.entity.getEntityBoundingBox().grow(avoidDistance, 6.0D, avoidDistance), this.entityPredicate);

			fleeEntities.sort((c1, c2) -> {
				return Double.compare(c1.getDistance(this.entity), c2.getDistance(this.entity));
			});
			
			if (!fleeEntities.isEmpty()) {
				this.fleeEntity = (Entity)fleeEntities.get(0);
				LoggerUtils.info("EntityAIFleeEntity - shouldExecute called" 
						+ "; entity=" + this.fleeEntity.getName() 
						+ "; avoidDistance=" + avoidDistance 
						+ "; distance=" + this.fleeEntity.getDistance(this.entity)
						, true);
				
				BlockPos fleeBlock = this.findRandomPositionAwayFrom(this.fleeEntity);

				if (fleeBlock != null) {
					this.destinationPosition = fleeBlock;
					return super.shouldExecute();
				}
			}
		}

		return false;
	}

	public void startExecuting() {
		LoggerUtils.info("EntityAIFleeEntity - startExecuting called", true);
		
		if (this.entity.isEnemy().test(this.fleeEntity)) {
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
		Vec3d enemyPosition = this.fleeEntity.getPositionVector();
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

	void updateMovementMode() {
		LoggerUtils.info("EntityAIFleeEntity - updateMovementMode called with mode " + this.moveMode.name(), true);
		
		this.entity.setMovementMode(this.moveMode);
	}
}
