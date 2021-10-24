package bletch.tektopiathief.entities.ai;

import bletch.tektopiathief.entities.EntityThief;
import bletch.tektopiathief.utils.LoggerUtils;
import net.minecraft.entity.ai.EntityAIBase;

public class EntityAIIdleCheck extends EntityAIBase {
	protected final EntityThief entity;
	private int idleTicks = 0;

	public EntityAIIdleCheck(EntityThief entity) {
		this.entity = entity;
		this.setMutexBits(7);
	}

	public boolean shouldExecute() {
		if (this.entity.isAITick() && this.entity.hasVillage())
			return true;
		return false;
	}

	public void startExecuting() {
		LoggerUtils.info("EntityAIIdleCheck - startExecuting called", true);
		
		this.idleTicks = 0;
	}

	public boolean shouldContinueExecuting() {
		return true;
	}

	public void updateTask() {
		++this.idleTicks;
		if (this.idleTicks % 80 == 0) {
			this.entity.setStoragePriority();
		}

		this.entity.setIdle(this.idleTicks);
		if (this.idleTicks % 1200 == 0) {
			LoggerUtils.info("Thief idle for " + this.idleTicks / 20 + " seconds", true);
		}
	}

	public void resetTask() {
		LoggerUtils.info("EntityAIIdleCheck - resetTask called", true);
		
		this.entity.setIdle(0);
		if (this.idleTicks >= 1200) {
			LoggerUtils.info("Thief was idle for " + this.idleTicks / 20 + " seconds.", true);
		}
	}
}
