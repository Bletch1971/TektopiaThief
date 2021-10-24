package bletch.tektopiathief.storage;

import java.util.function.Function;
import java.util.function.Predicate;

import bletch.tektopiathief.entities.EntityThief;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;

public class ItemDesire {
	protected boolean selfDirty;
	protected boolean storageDirty;
	protected int idealCount;
	protected int requiredCount;
	protected int limitCount;
	protected int currentlyHave;
	protected Function<ItemStack, Integer> neededItemFunction;
	protected Predicate<EntityThief> shouldNeed;
	private String debugName;
	protected TileEntityChest pickUpChest;

	public ItemDesire(Block block, int required, int ideal, int limit, Predicate<EntityThief> shouldNeed) {
		this(Item.getItemFromBlock(block), required, ideal, limit, shouldNeed);
	}

	public ItemDesire(Item item, int required, int ideal, int limit, Predicate<EntityThief> shouldNeed) {
		this(item.getUnlocalizedName(), p -> (p.getItem() == item) ? 1 : -1, required, ideal, limit, shouldNeed);
	}

	public ItemDesire(String name, Function<ItemStack, Integer> itemFunction, int required, int ideal, int limit, Predicate<EntityThief> should) {
		this.selfDirty = true;
		this.storageDirty = true;
		this.neededItemFunction = itemFunction;
		this.idealCount = Math.max(ideal, required);
		this.limitCount = Math.max(this.idealCount, limit);
		this.requiredCount = required;
		this.shouldNeed = should;
		this.currentlyHave = 0;
		this.debugName = name;
	}

	public void forceUpdate() {
		this.storageDirty = true;
		this.selfDirty = true;
	}

	public int getDeliverToStorage(EntityThief entity, ItemStack itemStack) {
		this.update(entity);
		
		if (this.neededItemFunction.apply(itemStack) < 0) {
			return itemStack.getCount();
		}
		
		int thisLimit = entity.isStoragePriority() ? this.idealCount : this.limitCount;
		
		if (this.currentlyHave - thisLimit > 0) {
			return Math.min(this.currentlyHave - this.idealCount, itemStack.getCount());
		}
		
		return 0;
	}

	protected int getItemsHave(EntityThief entity) {
		return entity.getInventory().getItemCount(this.neededItemFunction);
	}

	public String getName() {
		return this.debugName;
	}

	public TileEntityChest getPickUpChest(EntityThief entity) {
		this.update(entity);
		
		if (this.pickUpChest != null && this.pickUpChest.isInvalid()) {
			this.pickUpChest = null;
		}
		
		return this.pickUpChest;
	}

	protected int getQuantityToTake(EntityThief entity, ItemStack item) {
		return Math.min(this.idealCount - this.currentlyHave, item.getCount());
	}

	protected Function<ItemStack, Integer> getStoragePickUpFunction() {
		return this.neededItemFunction;
	}

	public void onInventoryUpdated(EntityThief entity, ItemStack updatedItem) {
		if (this.neededItemFunction.apply(updatedItem) > 0) {
			this.selfDirty = true;
		}
	}

	public void onStorageUpdated(EntityThief entity, ItemStack updatedItem) {
		if (this.neededItemFunction.apply(updatedItem) > 0) {
			this.storageDirty = true;
		}
	}

	public ItemStack pickUpItems(EntityThief entity) {
		if (this.shouldPickUp(entity)) {
			TileEntityChest chest = this.getPickUpChest(entity);
			ItemStack bestItem = null;
			int bestSlot = 0;
			int bestScore = 0;
			
			for (int d = 0; d < chest.getSizeInventory(); ++d) {
				ItemStack chestStack = chest.getStackInSlot(d);
				
				if (!chestStack.isEmpty()) {
					int thisScore = this.getStoragePickUpFunction().apply(chestStack);
					
					if (thisScore > bestScore) {
						bestItem = chestStack;
						bestScore = thisScore;
						bestSlot = d;
					}
				}
			}
			
			if (bestItem != null) {
				int quantityToTake = this.getQuantityToTake(entity, bestItem);
				entity.equipActionItem(bestItem);
				ItemStack newStack = bestItem.splitStack(quantityToTake);
				
				if (newStack.isEmpty()) {
					entity.getDesireSet().forceUpdate();
				} else {
					entity.getVillage().onStorageChange(chest, bestSlot, newStack);
				}
				
				return newStack;
			}
		}
		
		return ItemStack.EMPTY;
	}

	public boolean shouldPickUp(EntityThief entity) {
		if ((this.shouldNeed == null || this.shouldNeed.test(entity)) && entity.hasVillage() && entity.isWorkTime()) {
			this.update(entity);
			
			if (this.currentlyHave < this.idealCount && this.pickUpChest != null) {
				if (entity.isStoragePriority()) {
					return true;
				}
				
				if (this.currentlyHave < this.requiredCount) {
					return true;
				}
			}
		}
		
		return false;
	}

	protected void update(EntityThief entity) {
		this.updateSelf(entity);
		this.updateStorage(entity);
	}

	protected void updateSelf(EntityThief entity) {
		if (this.selfDirty) {
			int oldHave = this.currentlyHave;
			this.currentlyHave = this.getItemsHave(entity);
			
			if (this.currentlyHave < oldHave) {
				this.storageDirty = true;
			}
			
			this.selfDirty = false;
		}
	}

	protected void updateStorage(EntityThief entity) {
		if (this.storageDirty && entity.hasVillage()) {
			if (this.currentlyHave < this.idealCount) {
				this.pickUpChest = entity.getVillage().getStorageChestWithItem(this.neededItemFunction);
			} else {
				this.pickUpChest = null;
			}
			
			this.storageDirty = false;
		}
	}
}
