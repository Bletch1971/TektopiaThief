package bletch.tektopiathief.entities.ai;

import bletch.tektopiathief.entities.EntityThief;
import bletch.tektopiathief.entities.EntityThief.MovementMode;
import bletch.tektopiathief.storage.ItemDesire;
import bletch.tektopiathief.utils.LoggerUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.BlockPos;

public class EntityAIRetrieveFromStorage extends EntityAIMoveToBlock {
	private TileEntityChest chest = null;
	protected ItemDesire retrieveDesire = null;
	protected final EntityThief entity;
	private int pickUpTick = 0;
	protected boolean autoCheck = false;
	protected ItemStack itemTaken = null;
	protected final MovementMode moveMode;

	public EntityAIRetrieveFromStorage(EntityThief entity) {
		super(entity);
		
		this.entity = entity;
		this.moveMode = MovementMode.CREEP;
	}

	public boolean shouldExecute() {
		if ((this.entity.isAITick() || this.entity.isStoragePriority()) && this.entity.hasVillage()) {
			this.autoCheck = false;
			this.retrieveDesire = this.entity.getDesireSet().getNeededDesire(this.entity);
			
			if (this.retrieveDesire != null) {
				this.chest = this.retrieveDesire.getPickUpChest(this.entity);
				
				if (this.chest != null) {
					return super.shouldExecute();
				}
			}
		}

		return false;
	}

	public void startExecuting() {
		super.startExecuting();
	}

	public boolean shouldContinueExecuting() {
		if (this.pickUpTick > 0) {
			return true;
		} else {
			return this.chest != this.retrieveDesire.getPickUpChest(this.entity) ? false : super.shouldContinueExecuting();
		}
	}

	public void updateTask() {
		super.updateTask();
		
		--this.pickUpTick;
		if (this.pickUpTick == 15) {
			this.pickUpItems();
		} else if (this.pickUpTick == 1) {
			this.closeChest();
		}
	}

	public void resetTask() {
		this.pickUpTick = 0;
		
		if (this.itemTaken != null) {
			//this.entity.unequipActionItem(this.itemTaken);
			this.itemTaken = null;
			this.entity.setStoragePriority();
		}

		super.resetTask();
	}

	protected void closeChest() {
		TileEntity tileEntity = this.entity.world.getTileEntity(this.destinationPos);

		if (tileEntity instanceof TileEntityChest) {
			TileEntityChest tileEntityChest = (TileEntityChest)tileEntity;
			EntityPlayer player = (EntityPlayer)this.entity.world.playerEntities.stream()
					.filter(EntitySelectors.NOT_SPECTATING)
					.findFirst().orElse(null);

			if (player != null) {
				tileEntityChest.closeInventory(player);
			}
		}
	}

	protected BlockPos findWalkPos() {
		BlockPos result = super.findWalkPos();
		
		if (result == null) {
			BlockPos pos = this.destinationPos;
			
			BlockPos testPos = pos.west(2);	
			if (this.isWalkable(testPos, this.entity)) {
				return testPos;
			}

			testPos = pos.east(2);
			if (this.isWalkable(testPos, this.entity)) {
				return testPos;
			}

			testPos = pos.north(2);
			if (this.isWalkable(testPos, this.entity)) {
				return testPos;
			}

			testPos = pos.south(2);
			if (this.isWalkable(testPos, this.entity)) {
				return testPos;
			}
		}

		return result;
	}

	protected BlockPos getDestinationBlock() {
		return this.chest != null ? this.chest.getPos() : null;
	}

	public boolean isInterruptible() {
		return this.pickUpTick <= 0 || this.entity.getSeen();
	}

	protected void onArrival() {
		super.onArrival();
		this.pickUpTick = 30;
		this.openChest();
	}

	protected void openChest() {
		TileEntity tileEntity = this.entity.world.getTileEntity(this.destinationPos);

		if (tileEntity instanceof TileEntityChest) {
			TileEntityChest tileEntityChest = (TileEntityChest)tileEntity;
			EntityPlayer player = (EntityPlayer)this.entity.world.playerEntities.stream()
					.filter(EntitySelectors.NOT_SPECTATING)
					.findFirst().orElse(null);

			if (player != null) {
				tileEntityChest.openInventory(player);
			}
		}
	}

	protected void pickUpItems() {
		if (this.isNearDestination(5.0D) && !this.chest.isInvalid()) {
			this.itemTaken = this.retrieveDesire.pickUpItems(this.entity);
			
			if (this.itemTaken != ItemStack.EMPTY) {
				this.itemAcquired(this.itemTaken);
			} else {
				this.entity.getDesireSet().forceUpdate();
			}
		}
	}

	protected boolean itemAcquired(ItemStack itemStack) {
		this.autoCheck = true;
		this.entity.setStoragePriority();
		
		Boolean acquired = this.entity.getInventory().addItem(itemStack) == ItemStack.EMPTY;
		if (acquired) {
			String aquiredItemDescription = itemStack.getDisplayName() + " x " + itemStack.getCount();
			LoggerUtils.info("EntityAIRetrieveFromStorage - itemAcquired called; acquired=" + aquiredItemDescription, true);
		} else {
			LoggerUtils.info("EntityAIRetrieveFromStorage - itemAcquired called; acquire failed", true);
		}
		return acquired;
	}

	protected void updateMovementMode() {
		LoggerUtils.info("EntityAIRetrieveFromStorage - updateMovementMode called with mode " + this.moveMode.name(), true);

		this.entity.setMovementMode(this.moveMode);
	}
}
