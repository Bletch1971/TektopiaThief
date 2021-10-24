package bletch.tektopiathief.entities.ai;

import bletch.tektopiathief.utils.LoggerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.tangotek.tektopia.entities.EntityVillageNavigator;
import net.tangotek.tektopia.pathing.PathNavigateVillager2;

public class EntityAIUseDoor extends EntityAIBase {
	protected EntityLiving entity;
	protected BlockDoor doorBlock;
	protected BlockPos doorPosition;
	int closeTimer;

	public EntityAIUseDoor(EntityLiving entitylivingIn) {
		this.doorPosition = BlockPos.ORIGIN;
		this.entity = entitylivingIn;
	}

	public boolean shouldExecute() {
		if (!this.entity.collidedHorizontally) {
			return false;
		}

		PathNavigateVillager2 pathNavigate = (PathNavigateVillager2)this.entity.getNavigator();
		Path path = pathNavigate.getPath();

		if (path != null && !path.isFinished() && pathNavigate.getEnterDoors()) {
			for(int i = 0; i < Math.min(path.getCurrentPathIndex() + 2, path.getCurrentPathLength()); ++i) {
				PathPoint pathpoint = path.getPathPointFromIndex(i);
				this.doorPosition = new BlockPos(pathpoint.x, pathpoint.y + 1, pathpoint.z);

				if (this.entity.getDistanceSq((double)this.doorPosition.getX(), this.entity.posY, (double)this.doorPosition.getZ()) <= 2.25D) {
					this.doorBlock = this.getBlockDoor(this.doorPosition);

					if (this.doorBlock != null) {
						return true;
					}
				}
			}

			this.doorPosition = (new BlockPos(this.entity)).up();
			this.doorBlock = this.getBlockDoor(this.doorPosition);
			return this.doorBlock != null;
		}

		return false;
	}

	public void startExecuting() {
		LoggerUtils.info("EntityAIUseDoor - startExecuting called", true);

		this.closeTimer = 25;
		this.toggleDoor(true);
	}

	public boolean shouldContinueExecuting() {
		return this.closeTimer >= 0;
	}

	public void updateTask() {
		--this.closeTimer;

		if (this.closeTimer == 0 && !this.isDoorClear()) {
			this.toggleDoor(true);
			this.closeTimer = 25;
		}

		super.updateTask();
	}

	public void resetTask() {
		LoggerUtils.info("EntityAIUseDoor - resetTask called", true);

		this.toggleDoor(false);
	}

	protected BlockDoor getBlockDoor(BlockPos pos) {
		IBlockState iblockstate = this.entity.world.getBlockState(pos);
		Block block = iblockstate.getBlock();
		return block instanceof BlockDoor && iblockstate.getMaterial() == Material.WOOD ? (BlockDoor)block : null;
	}

	protected boolean isDoorClear() {
		return this.entity.world.getEntitiesWithinAABB(EntityVillageNavigator.class, new AxisAlignedBB(this.doorPosition)).isEmpty();
	}

	protected void toggleDoor(boolean open) {
		this.doorBlock.toggleDoor(this.entity.world, this.doorPosition, open);
	}
}
